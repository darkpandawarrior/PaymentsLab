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
 * Square on iOS — the native-SDK counterpart to Android's `provider:square` `SquareGateway`, built
 * on the real `SQIPCardEntryViewController` (CocoaPods, `SquareInAppPaymentsSDK` `1.6.7`) — a
 * genuine card-entry UI, the same UX story as Android's `CardEntry` activity. Same boundary shape
 * as [StripeIosGateway]: Swift implements the Kotlin [SquareCheckoutHost] interface against the
 * real SDK.
 *
 * Same `MOCK_MODE`-by-default honesty as Android: no live Square sandbox credentials were available
 * this session, so `pay()` falls back to [SimulatedPayment] when no application id is prepared.
 */
class SquareIosGateway(
    private val checkoutHost: SquareCheckoutHost,
) : PaymentGateway {
    override val id: GatewayId = GatewayId("square")

    override val meta: GatewayMeta =
        GatewayMeta(
            displayName = "Square",
            status = GatewayStatus.MOCK_MODE,
            capabilities = setOf(Capability.ONE_TIME_PAYMENT, Capability.CARDS),
            region = "Global",
            docsPath = "docs/providers/square-ios.md",
            blurb =
                "Card entry via the real Square iOS SDK (CocoaPods — the one gateway here without " +
                    "an SPM distribution). Same card-entry-UI SDK story as Android.",
        )

    override suspend fun prepare(created: CreatedOrder): PreparedPayment {
        val applicationId = created.providerParams[KEY_APPLICATION_ID]
        return PreparedPayment(
            gatewayId = id,
            orderId = created.order.orderId,
            amount = created.order.amount,
            params = if (applicationId.isNullOrBlank()) emptyMap() else mapOf(KEY_APPLICATION_ID to applicationId),
        )
    }

    override suspend fun pay(
        host: PaymentHost,
        prepared: PreparedPayment,
    ): PaymentResult {
        val applicationId = prepared.params[KEY_APPLICATION_ID]
        if (applicationId.isNullOrBlank()) return SimulatedPayment.run(id, prepared)

        return suspendCancellableCoroutine { cont ->
            checkoutHost.presentCardEntry(applicationId) { outcome ->
                if (cont.isActive) cont.resume(outcome.toPaymentResult()) { _, _, _ -> }
            }
        }
    }

    private fun SquareCheckoutOutcome.toPaymentResult(): PaymentResult =
        when (this) {
            is SquareCheckoutOutcome.Success ->
                PaymentResult.Success(
                    paymentId = nonce,
                    verification = emptyMap(),
                    raw = Redactor.redact("square.cardentry.success", emptyMap()),
                )

            is SquareCheckoutOutcome.Error ->
                PaymentResult.Failure(
                    code = FailureCode.GATEWAY_DECLINED,
                    message = UiText.of(message),
                    raw = Redactor.redact("square.cardentry.error", mapOf("error" to message)),
                )

            SquareCheckoutOutcome.Canceled ->
                PaymentResult.Cancelled(raw = Redactor.redact("square.cardentry.canceled", emptyMap()))
        }

    private companion object {
        const val KEY_APPLICATION_ID = "application_id"
    }
}
