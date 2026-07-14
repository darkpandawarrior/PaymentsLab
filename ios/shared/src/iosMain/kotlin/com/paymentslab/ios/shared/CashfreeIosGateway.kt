package com.paymentslab.ios.shared

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
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Cashfree on iOS — the native-SDK counterpart to Android's `provider:cashfree` `CashfreeGateway`,
 * built on the real `CashfreePGUISDK` Drop Checkout API (SPM, `core-ios-sdk`). Same boundary shape
 * as [StripeIosGateway]: Swift implements the Kotlin [CashfreeCheckoutHost] interface against the
 * real SDK, since Kotlin/Native can't cinterop against it directly.
 *
 * Backend note: `CashfreeAdapter`'s `payment_session_id` is a **stub** value
 * (`session_<orderId>_demo`), not a real Cashfree Orders API session — same limitation the Android
 * `CashfreeGateway` already has, not a regression introduced here. `verify()` only ever inspects a
 * `cf_status` marker Android also sends, never a live Cashfree order lookup.
 */
class CashfreeIosGateway(
    private val checkoutHost: CashfreeCheckoutHost,
) : PaymentGateway {
    override val id: GatewayId = GatewayId("cashfree")

    override val meta: GatewayMeta =
        GatewayMeta(
            displayName = "Cashfree",
            status = GatewayStatus.SANDBOX_READY,
            capabilities = setOf(Capability.ONE_TIME_PAYMENT, Capability.CARDS, Capability.UPI, Capability.NET_BANKING),
            region = "India",
            docsPath = "docs/providers/cashfree-ios.md",
            blurb =
                "Drop Checkout via the real Cashfree iOS SDK. Backend session id is a stub — same " +
                    "limitation the Android CashfreeGateway already ships with.",
        )

    override suspend fun prepare(created: CreatedOrder): PreparedPayment {
        val params = created.providerParams
        val paymentSessionId =
            params["payment_session_id"]
                ?: throw PaymentPreparationException("Cashfree order missing 'payment_session_id'")
        val orderId = params["order_id"] ?: throw PaymentPreparationException("Cashfree order missing 'order_id'")
        return PreparedPayment(
            gatewayId = id,
            orderId = created.order.orderId,
            amount = created.order.amount,
            params = mapOf("payment_session_id" to paymentSessionId, "order_id" to orderId),
        )
    }

    override suspend fun pay(
        host: PaymentHost,
        prepared: PreparedPayment,
    ): PaymentResult {
        val paymentSessionId =
            prepared.params["payment_session_id"]
                ?: return configMissing("Cashfree payment missing 'payment_session_id'")
        val orderId = prepared.params["order_id"] ?: return configMissing("Cashfree payment missing 'order_id'")

        return suspendCancellableCoroutine { cont ->
            checkoutHost.openDropCheckout(orderId, paymentSessionId) { outcome ->
                if (cont.isActive) cont.resume(outcome.toPaymentResult()) { _, _, _ -> }
            }
        }
    }

    private fun CashfreeCheckoutOutcome.toPaymentResult(): PaymentResult =
        when (this) {
            is CashfreeCheckoutOutcome.Success ->
                // paymentId is the order id — Cashfree's client callback returns only that; the real
                // cf_payment_id would be resolved server-side, matching Android's CashfreeGateway.
                PaymentResult.Success(
                    paymentId = orderId,
                    verification = mapOf("order_id" to orderId, "cf_status" to "verify"),
                    raw =
                        Redactor.redact(
                            "cashfree.checkout.verify",
                            mapOf("status" to "verify", "order_id" to orderId),
                        ),
                )

            is CashfreeCheckoutOutcome.Error ->
                PaymentResult.Failure(
                    code = FailureCode.GATEWAY_DECLINED,
                    message = UiText.of(message),
                    raw = Redactor.redact("cashfree.checkout.error", mapOf("error" to message, "order_id" to orderId)),
                )
        }

    private fun configMissing(message: String): PaymentResult.Failure =
        PaymentResult.Failure(
            code = FailureCode.CONFIG_MISSING,
            message = UiText.of(message),
            raw = Redactor.redact("cashfree.config.error", mapOf("error" to message)),
        )
}
