package com.paymentslab.provider.paytm

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
 * Paytm All-in-One, promoted from the generic `provider:hosted-webview` `HostedGatewayConfig`
 * archetype to its own native module — roadmap #9, completing the India quartet
 * (Razorpay/Cashfree/Paytm/UPI) alongside the other three already-shipped native providers. Same
 * shape as the `provider:paystack`/`provider:flutterwave` promotions before it: `pay()` still
 * reuses the shared [HostedCheckoutRelay]/`HostedCheckoutHost` already mounted at `:app`'s nav
 * host — nothing about the checkout mechanics changes. What moves natively is the gateway
 * identity, [GatewayMeta], and return-URL contract, plus room to grow Paytm-specific logic (a real
 * `appinvokesdk` integration) without touching the generic fan-out module. Supersedes the
 * `paytmaioHostedGatewayConfig` entry in `HostedGatewayConfigs.kt` — both bound to
 * `GatewayId("paytmaio")` would collide in the registry.
 *
 * Paytm All-in-One genuinely spans cards, Paytm wallet, UPI, and net banking in one checkout —
 * [Capability] already has dedicated `UPI` and `NET_BANKING` values (unlike Flutterwave's
 * mobile-money gap), so all four are represented natively here rather than called out only in
 * the blurb.
 *
 * `status = MOCK_MODE` — real SDK is `appinvokesdk`, but staging MID access is flaky per research
 * (see the superseded hosted config's blurb), so this stays on the mock checkout path, same
 * honesty rule as every other MOCK_MODE gateway in this catalog.
 */
class PaytmGateway(
    private val relay: HostedCheckoutRelay,
) : PaymentGateway {
    override val id: GatewayId = GatewayId("paytmaio")

    override val meta: GatewayMeta =
        GatewayMeta(
            displayName = "Paytm All-in-One",
            status = GatewayStatus.MOCK_MODE,
            capabilities =
                setOf(
                    Capability.ONE_TIME_PAYMENT,
                    Capability.CARDS,
                    Capability.WALLET,
                    Capability.UPI,
                    Capability.NET_BANKING,
                ),
            region = "India",
            docsPath = "docs/providers/paytmaio.md",
            blurb =
                "Native provider:paytm module (promoted from the generic hosted-checkout " +
                    "archetype — roadmap #9, completing the India quartet with Razorpay/Cashfree/UPI). " +
                    "Real SDK is appinvokesdk, spanning cards, Paytm wallet, UPI and net banking in one " +
                    "checkout; staging MID access is flaky per research, so this still runs the mock " +
                    "checkout path — the backend already resolved the real `checkout_url`, or the mock " +
                    "fallback; this module owns the return-URL contract and gateway identity natively.",
        )

    /** No network hop — the backend already built `checkout_url` into `providerParams` (see PaytmAdapter). */
    override suspend fun prepare(created: CreatedOrder): PreparedPayment {
        val checkoutUrl =
            created.providerParams["checkout_url"]
                ?: throw PaymentPreparationException("Paytm order missing checkout_url")
        AppLog.d(TAG, "prepared Paytm order=${created.order.orderId}")
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
                ?: return failure(FailureCode.CONFIG_MISSING, "Missing Paytm checkout_url", "no_checkout_url")

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
                    message = UiText.of(outcome.reason ?: "Paytm checkout reported a failure"),
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
    ) = Redactor.redact("paytmaio_$outcome", extra)

    private companion object {
        const val TAG = "PaytmGateway"
    }
}
