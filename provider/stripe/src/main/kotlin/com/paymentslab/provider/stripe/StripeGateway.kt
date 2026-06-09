package com.paymentslab.provider.stripe

import com.paymentslab.core.common.AppLog
import com.paymentslab.core.common.UiText
import com.paymentslab.core.paymentsapi.AndroidPaymentHost
import com.paymentslab.core.paymentsapi.Capability
import com.paymentslab.core.paymentsapi.CreatedOrder
import com.paymentslab.core.paymentsapi.FailureCode
import com.paymentslab.core.paymentsapi.GatewayId
import com.paymentslab.core.paymentsapi.GatewayMeta
import com.paymentslab.core.paymentsapi.GatewayStatus
import com.paymentslab.core.paymentsapi.PaymentGateway
import com.paymentslab.core.paymentsapi.PaymentHost
import com.paymentslab.core.paymentsapi.PaymentPreparationException
import com.paymentslab.core.paymentsapi.PaymentResult
import com.paymentslab.core.paymentsapi.PreparedPayment
import com.paymentslab.core.paymentsapi.Redactor
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Stripe provider built on Stripe's [PaymentSheet]. PaymentSheet is Compose-first but also exposes an
 * imperative surface (`PaymentSheet.Builder(resultCallback).build(activity)` +
 * `presentWithPaymentIntent(...)`) that must live in Activity/Compose scope — see
 * [StripePaymentLauncherHost] for the app-side wiring that owns that scope.
 *
 * Client-result-is-a-hint: a [PaymentResult.Success] here means only that the SDK reported
 * `Completed`. The orchestrator still confirms server-side (webhook + PaymentIntent status) before
 * trusting it. Nothing secret is rendered — [Redactor] gates every displayed payload, while the
 * unredacted [PaymentResult.Success.verification] map carries the server-bound fields.
 */
class StripeGateway(
    private val launcherHost: StripePaymentLauncherHost,
) : PaymentGateway {

    override val id: GatewayId = GatewayId("stripe")

    override val meta: GatewayMeta = GatewayMeta(
        displayName = "Stripe",
        status = GatewayStatus.SANDBOX_READY,
        capabilities = setOf(
            Capability.ONE_TIME_PAYMENT,
            Capability.CARDS,
            Capability.WALLET,
        ),
        region = "Global",
        docsPath = "docs/providers/stripe.md",
        blurb = "Card + wallet checkout via Stripe PaymentSheet. Test cards trigger the 3DS2 " +
            "challenge; Google Pay rides Stripe as the gateway of record.",
    )

    /**
     * Stripe's session material is the PaymentIntent client secret plus the publishable key. Both are
     * created server-side (in `POST /orders`) and arrive in [CreatedOrder.providerParams]; the client
     * never mints them. Missing either is a hard preparation failure.
     */
    override suspend fun prepare(created: CreatedOrder): PreparedPayment {
        val params = created.providerParams
        val clientSecret = params[KEY_CLIENT_SECRET]
            ?: throw PaymentPreparationException("Stripe order missing '$KEY_CLIENT_SECRET'")
        val publishableKey = params[KEY_PUBLISHABLE_KEY]
            ?: throw PaymentPreparationException("Stripe order missing '$KEY_PUBLISHABLE_KEY'")

        return PreparedPayment(
            gatewayId = id,
            orderId = created.order.orderId,
            amount = created.order.amount,
            params = mapOf(
                KEY_CLIENT_SECRET to clientSecret,
                KEY_PUBLISHABLE_KEY to publishableKey,
            ),
        )
    }

    override suspend fun pay(host: PaymentHost, prepared: PreparedPayment): PaymentResult {
        val androidHost = host as? AndroidPaymentHost
            ?: return configMissing("Stripe requires an AndroidPaymentHost")

        val clientSecret = prepared.params[KEY_CLIENT_SECRET]
            ?: return configMissing("Stripe payment missing '$KEY_CLIENT_SECRET'")
        val publishableKey = prepared.params[KEY_PUBLISHABLE_KEY]
            ?: return configMissing("Stripe payment missing '$KEY_PUBLISHABLE_KEY'")

        // PaymentConfiguration.init installs the publishable key process-wide; the SDK reads it when
        // it confirms the PaymentIntent. Safe to call again with the same activity + key.
        PaymentConfiguration.init(androidHost.activity, publishableKey)

        val configuration = buildConfiguration(prepared)

        AppLog.i(TAG, "Presenting Stripe PaymentSheet for order=${prepared.orderId}")

        return suspendCancellableCoroutine { continuation ->
            // Guard against a resume() after cancellation: if the coroutine is already cancelled,
            // drop the pending listener so a late SDK callback can't touch a dead continuation.
            continuation.invokeOnCancellation {
                launcherHost.clearPending()
                AppLog.w(TAG, "Stripe payment coroutine cancelled for order=${prepared.orderId}")
            }

            try {
                launcherHost.present(clientSecret, configuration) { result ->
                    // suspendCancellableCoroutine + isActive guard => resume exactly once.
                    if (continuation.isActive) {
                        continuation.resume(result.toPaymentResult(clientSecret))
                    }
                }
            } catch (e: IllegalStateException) {
                // No PaymentSheet attached, or a payment already in flight.
                if (continuation.isActive) {
                    continuation.resume(configMissing(e.message ?: "Stripe launcher not ready"))
                }
            }
        }
    }

    /**
     * Google Pay is enabled in the PaymentSheet options in TEST environment. Google Pay here rides
     * Stripe as the gateway — it is not a separate provider; Stripe confirms the same PaymentIntent.
     */
    private fun buildConfiguration(prepared: PreparedPayment): PaymentSheet.Configuration {
        val currency = prepared.amount.currency
        // Google Pay's countryCode is where the *merchant* is registered. INR -> IN, otherwise US as
        // a sensible showcase default. TODO(country): thread the real merchant country from config
        // once the backend exposes it; hardcoding is a showcase simplification, not production-safe.
        val country = if (currency.equals("INR", ignoreCase = true)) "IN" else "US"

        return PaymentSheet.Configuration.Builder(MERCHANT_DISPLAY_NAME)
            .googlePay(
                PaymentSheet.GooglePayConfiguration(
                    environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                    countryCode = country,
                    currencyCode = currency,
                ),
            )
            .allowsDelayedPaymentMethods(false)
            .build()
    }

    /** Map Stripe's [PaymentSheetResult] zoo into the normalized [PaymentResult]. */
    private fun PaymentSheetResult.toPaymentResult(clientSecret: String): PaymentResult = when (this) {
        is PaymentSheetResult.Completed -> {
            // The SDK does not hand back the PaymentIntent id on the imperative surface, so derive a
            // stable payment id from the client secret prefix (`pi_XXX_secret_YYY` -> `pi_XXX`). The
            // server re-derives/verifies authoritatively from the client secret it minted.
            val paymentIntentId = clientSecret.substringBefore("_secret_")
            AppLog.i(TAG, "Stripe PaymentSheet completed: $paymentIntentId")
            PaymentResult.Success(
                paymentId = paymentIntentId,
                // Unredacted, server-bound only — forwarded to PaymentBackend.verify, never displayed.
                verification = mapOf(
                    "payment_intent" to paymentIntentId,
                    "client_secret_present" to "true",
                ),
                raw = Redactor.redact(
                    label = "stripe.paymentsheet.completed",
                    raw = mapOf(
                        "status" to "completed",
                        "payment_intent" to paymentIntentId,
                        // `client_secret` contains "secret" -> Redactor masks it automatically.
                        "client_secret" to clientSecret,
                    ),
                ),
            )
        }

        is PaymentSheetResult.Canceled -> {
            AppLog.i(TAG, "Stripe PaymentSheet cancelled by user")
            PaymentResult.Cancelled(
                raw = Redactor.redact("stripe.paymentsheet.canceled", mapOf("status" to "canceled")),
            )
        }

        is PaymentSheetResult.Failed -> {
            val throwable = this.error
            val message = throwable.message ?: "Stripe payment failed"
            AppLog.w(TAG, "Stripe PaymentSheet failed: $message", throwable)
            PaymentResult.Failure(
                // A thrown SDK error is an integration/SDK problem; a declined card also surfaces here.
                // We can't reliably distinguish without inspecting Stripe error types, so treat the
                // general failure as GATEWAY_DECLINED (the user-facing common case) and log the raw.
                // TODO(failure-taxonomy): narrow to SDK_ERROR when `error` is a StripeException
                // subtype signalling a config/integration fault (e.g. missing publishable key).
                code = FailureCode.GATEWAY_DECLINED,
                message = UiText.of(message),
                raw = Redactor.redact(
                    label = "stripe.paymentsheet.failed",
                    raw = mapOf(
                        "status" to "failed",
                        "error" to message,
                        "error_type" to throwable::class.simpleName.orEmpty(),
                    ),
                ),
            )
        }
    }

    private fun configMissing(message: String): PaymentResult.Failure {
        AppLog.e(TAG, "Stripe config error: $message")
        return PaymentResult.Failure(
            code = FailureCode.CONFIG_MISSING,
            message = UiText.of(message),
            raw = Redactor.redact("stripe.config.error", mapOf("error" to message)),
        )
    }

    private companion object {
        const val TAG = "StripeGateway"
        const val MERCHANT_DISPLAY_NAME = "PaymentsLab"
        const val KEY_CLIENT_SECRET = "client_secret"
        const val KEY_PUBLISHABLE_KEY = "publishable_key"
    }
}
