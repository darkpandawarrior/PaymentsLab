package com.paymentslab.provider.paystack

import com.paymentslab.core.common.AppLog
import com.siddharth.kmp.common.UiText
import com.siddharth.kmp.paymentsapi.Capability
import com.siddharth.kmp.paymentsapi.CreatedOrder
import com.siddharth.kmp.paymentsapi.FailureCode
import com.siddharth.kmp.paymentsapi.GatewayId
import com.siddharth.kmp.paymentsapi.GatewayMeta
import com.siddharth.kmp.paymentsapi.GatewayStatus
import com.siddharth.kmp.paymentsapi.PaymentGateway
import com.siddharth.kmp.paymentsapi.PaymentHost
import com.siddharth.kmp.paymentsapi.PaymentPreparationException
import com.siddharth.kmp.paymentsapi.PaymentResult
import com.siddharth.kmp.paymentsapi.PreparedPayment
import com.siddharth.kmp.paymentsapi.Redactor
import com.paymentslab.provider.hostedwebview.HostedCheckoutRelay
import com.paymentslab.provider.hostedwebview.HostedReturnOutcome
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Paystack, promoted from the generic `provider:hosted-webview` `HostedGatewayConfig` archetype to
 * its own native module — the same roadmap step every other archetype-C gateway can eventually take
 * (see plan roadmap #4). This does NOT mean reinventing the WebView bridge: the checkout page is still
 * a hosted Paystack URL, so `pay()` reuses the shared [HostedCheckoutRelay]/`HostedCheckoutHost`
 * already mounted at `:app`'s nav host. What moves natively is everything else a real per-gateway
 * module owns: its own build target, its own [GatewayMeta], its own return-URL contract, and room to
 * grow provider-specific logic (e.g. a real Paystack SDK) without touching the ~55-gateway generic
 * fan-out module.
 *
 * `status = MOCK_MODE` until a real `PLAB_PAYSTACK_TEST_SECRET_KEY` is configured on the backend — see
 * `PaystackAdapter`. Identical honesty rule as the hosted config it replaces.
 */
class PaystackGateway(
    private val relay: HostedCheckoutRelay,
) : PaymentGateway {
    override val id: GatewayId = GatewayId("paystack")

    override val meta: GatewayMeta =
        GatewayMeta(
            displayName = "Paystack",
            status = GatewayStatus.MOCK_MODE,
            capabilities = setOf(Capability.ONE_TIME_PAYMENT, Capability.CARDS),
            region = "Africa",
            docsPath = "docs/providers/paystack.md",
            blurb =
                "Native provider:paystack module (promoted from the generic hosted-checkout " +
                    "archetype). Still a hosted checkout page under the hood — the backend already " +
                    "resolved the real `checkout_url` (genuine Paystack authorization_url, or the mock " +
                    "fallback); this module owns the return-URL contract and gateway identity natively.",
        )

    /** No network hop — the backend already built `checkout_url` into `providerParams` (see PaystackAdapter). */
    override suspend fun prepare(created: CreatedOrder): PreparedPayment {
        val checkoutUrl =
            created.providerParams["checkout_url"]
                ?: throw PaymentPreparationException("Paystack order missing checkout_url")
        AppLog.d(TAG, "prepared Paystack order=${created.order.orderId}")
        return PreparedPayment(
            gatewayId = id,
            orderId = created.order.orderId,
            amount = created.order.amount,
            params = mapOf("checkout_url" to checkoutUrl),
        )
    }

    override suspend fun pay(
        host: PaymentHost,
        prepared: PreparedPayment,
    ): PaymentResult {
        val checkoutUrl =
            prepared.params["checkout_url"]
                ?: return failure(FailureCode.CONFIG_MISSING, "Missing Paystack checkout_url", "no_checkout_url")

        return suspendCancellableCoroutine { cont ->
            relay.register(id) { outcome -> if (cont.isActive) cont.resume(mapOutcome(outcome)) { _, _, _ -> } }
            cont.invokeOnCancellation { relay.clear(id) }
            relay.launch(id, checkoutUrl)
        }
    }

    internal fun mapOutcome(outcome: HostedReturnOutcome): PaymentResult =
        when (outcome) {
            is HostedReturnOutcome.Success ->
                PaymentResult.Success(
                    paymentId = outcome.paymentId ?: "unknown",
                    verification = outcome.paymentId?.let { mapOf("payment_id" to it) } ?: emptyMap(),
                    raw = redact("success", outcome.paymentId?.let { mapOf("payment_id" to it) } ?: emptyMap()),
                )
            is HostedReturnOutcome.Failure ->
                PaymentResult.Failure(
                    code = FailureCode.GATEWAY_DECLINED,
                    message = UiText.of(outcome.reason ?: "Paystack checkout reported a failure"),
                    raw = redact("failure", mapOf("reason" to (outcome.reason ?: ""))),
                )
            HostedReturnOutcome.Cancelled ->
                PaymentResult.Cancelled(raw = redact("cancelled", emptyMap()))
        }

    private fun failure(
        code: FailureCode,
        message: String,
        rawReason: String,
    ): PaymentResult.Failure =
        PaymentResult.Failure(
            code = code,
            message = UiText.of(message),
            raw = redact("failure", mapOf("error" to rawReason)),
        )

    private fun redact(
        outcome: String,
        extra: Map<String, String>,
    ) = Redactor.redact("paystack_$outcome", extra)

    private companion object {
        const val TAG = "PaystackGateway"
    }
}
