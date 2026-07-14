package com.paymentslab.ios.shared

import com.siddharth.kmp.common.UiText
import com.paymentslab.core.paymentsapi.Capability
import com.paymentslab.core.paymentsapi.CreatedOrder
import com.paymentslab.core.paymentsapi.FailureCode
import com.paymentslab.core.paymentsapi.GatewayId
import com.paymentslab.core.paymentsapi.GatewayMeta
import com.paymentslab.core.paymentsapi.GatewayStatus
import com.paymentslab.core.paymentsapi.PaymentGateway
import com.paymentslab.core.paymentsapi.PaymentHost
import com.paymentslab.core.paymentsapi.PaymentResult
import com.paymentslab.core.paymentsapi.PreparedPayment
import com.paymentslab.core.paymentsapi.Redactor
import com.paymentslab.core.paymentsapi.SimulatedPayment
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Omise on iOS — the native-SDK counterpart to Android's `provider:omise` `OmiseGateway`, built on
 * the real `OmiseSDK` (SPM, `omise-ios` `5.6.3`) rather than a WebView fallback. Same boundary shape
 * as [StripeIosGateway]: Swift implements the Kotlin [OmiseCheckoutHost] interface against the real
 * SDK's `client.createToken(payload:completionHandler:)` call. Unlike Android's `CreditCardActivity`,
 * Omise's iOS SDK ships no ready-made card-entry UI, so the card form is this app's own SwiftUI —
 * the tokenization call itself is the real SDK, the form around it is not vendor-provided.
 *
 * Same `MOCK_MODE`-by-default honesty as Android: no live Omise sandbox credentials were available
 * this session either, so `pay()` falls back to [SimulatedPayment] when no public key is prepared.
 */
class OmiseIosGateway(
    private val checkoutHost: OmiseCheckoutHost,
) : PaymentGateway {
    override val id: GatewayId = GatewayId("omise")

    override val meta: GatewayMeta =
        GatewayMeta(
            displayName = "Omise",
            status = GatewayStatus.MOCK_MODE,
            capabilities = setOf(Capability.ONE_TIME_PAYMENT, Capability.CARDS),
            region = "SEA",
            docsPath = "docs/providers/omise-ios.md",
            blurb =
                "Card tokenization via the real Omise iOS SDK (no vendor card-entry UI on iOS — " +
                    "this app's own SwiftUI form, real SDK tokenization call).",
        )

    override suspend fun prepare(created: CreatedOrder): PreparedPayment {
        val publicKey = created.providerParams[KEY_PUBLIC_KEY]
        return PreparedPayment(
            gatewayId = id,
            orderId = created.order.orderId,
            amount = created.order.amount,
            params = if (publicKey.isNullOrBlank()) emptyMap() else mapOf(KEY_PUBLIC_KEY to publicKey),
        )
    }

    override suspend fun pay(
        host: PaymentHost,
        prepared: PreparedPayment,
    ): PaymentResult {
        val publicKey = prepared.params[KEY_PUBLIC_KEY]
        if (publicKey.isNullOrBlank()) return SimulatedPayment.run(id, prepared)

        return suspendCancellableCoroutine { cont ->
            checkoutHost.presentCardForm(publicKey) { outcome ->
                if (cont.isActive) cont.resume(outcome.toPaymentResult()) { _, _, _ -> }
            }
        }
    }

    private fun OmiseCheckoutOutcome.toPaymentResult(): PaymentResult =
        when (this) {
            is OmiseCheckoutOutcome.Success ->
                PaymentResult.Success(
                    paymentId = token,
                    verification = emptyMap(),
                    raw = Redactor.redact("omise.tokenize.success", emptyMap()),
                )

            is OmiseCheckoutOutcome.Error ->
                PaymentResult.Failure(
                    code = FailureCode.GATEWAY_DECLINED,
                    message = UiText.of(message),
                    raw = Redactor.redact("omise.tokenize.error", mapOf("error" to message)),
                )

            OmiseCheckoutOutcome.Canceled ->
                PaymentResult.Cancelled(raw = Redactor.redact("omise.tokenize.canceled", emptyMap()))
        }

    private companion object {
        const val KEY_PUBLIC_KEY = "public_key"
    }
}
