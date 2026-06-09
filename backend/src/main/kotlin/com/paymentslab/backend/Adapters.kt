package com.paymentslab.backend

import com.paymentslab.core.protocol.CatalogItemDto
import com.paymentslab.core.protocol.PaymentStatusDto
import com.paymentslab.core.protocol.VerifyRequest

/**
 * Razorpay adapter.
 *
 * - createProviderOrder → publishable checkout params: key_id, order_id, amount (minor units),
 *   currency. The secret key never leaves the server.
 * - verify → REAL HMAC-SHA256. Razorpay signs "$orderId|$paymentId" with the account secret; we
 *   recompute it here and constant-time compare against the client-supplied signature. This is the
 *   genuine-crypto path the test asserts against.
 */
class RazorpayAdapter(
    private val keyId: String,
    private val secret: String,
) : GatewayAdapter {
    override val gatewayId: String = "razorpay"

    override fun createProviderOrder(
        orderId: String,
        item: CatalogItemDto,
    ): Map<String, String> =
        mapOf(
            "key_id" to keyId,
            "order_id" to orderId,
            "amount" to item.amountMinor.toString(),
            "currency" to item.currency,
        )

    override fun verify(req: VerifyRequest): PaymentStatusDto {
        val paymentId = req.paymentId
        val signature = req.signature
        if (paymentId.isNullOrBlank() || signature.isNullOrBlank()) return PaymentStatusDto.FAILED

        val expected = Crypto.hmacSha256Hex(secret, "${req.orderId}|$paymentId")
        return if (Crypto.constantTimeEquals(expected, signature)) {
            PaymentStatusDto.SUCCESS
        } else {
            PaymentStatusDto.FAILED
        }
    }
}

/**
 * UPI intent adapter.
 *
 * - createProviderOrder → the UPI deep-link fields: pa (payee VPA), pn (payee name), tr (txn ref =
 *   orderId), am (amount), cu (currency), mc (merchant category code).
 * - verify → a raw UPI intent RESPONSE from the payer app is NOT cryptographically verifiable by the
 *   merchant server (there is no signed callback in the bare intent flow). So we cannot trust the
 *   client here: we mark the payment PENDING and let the authoritative bank/PSP WEBHOOK resolve it to
 *   SUCCESS/FAILED. This is a real limitation of raw UPI intents, not a shortcut.
 */
class UpiIntentAdapter(
    private val payeeVpa: String,
    private val payeeName: String,
    private val merchantCategoryCode: String,
) : GatewayAdapter {
    override val gatewayId: String = "upi_intent"

    override fun createProviderOrder(
        orderId: String,
        item: CatalogItemDto,
    ): Map<String, String> {
        // UPI expects a major-unit decimal amount (e.g. "149.00").
        val amountMajor = "%.2f".format(item.amountMinor / 100.0)
        return mapOf(
            "pa" to payeeVpa,
            "pn" to payeeName,
            "tr" to orderId,
            "am" to amountMajor,
            "cu" to item.currency,
            "mc" to merchantCategoryCode,
        )
    }

    override fun verify(req: VerifyRequest): PaymentStatusDto {
        // Client-unverifiable — resolve on webhook. See class doc.
        return PaymentStatusDto.PENDING
    }
}

/**
 * Stripe adapter.
 *
 * - createProviderOrder → client_secret + publishable_key for the PaymentSheet.
 * - verify → STUBBED external call. A real implementation retrieves the PaymentIntent server-side
 *   (`stripe.paymentIntents.retrieve(id)`) and reads its `status`. That network call is a later
 *   milestone; for the demo we treat the presence of a "succeeded" marker in `extra` as success.
 */
class StripeAdapter(
    private val publishableKey: String,
    @Suppress("unused") private val secret: String,
) : GatewayAdapter {
    override val gatewayId: String = "stripe"

    override fun createProviderOrder(
        orderId: String,
        item: CatalogItemDto,
    ): Map<String, String> =
        mapOf(
            // Demo-shaped client secret; a real one comes back from stripe.paymentIntents.create(...).
            "client_secret" to "pi_${orderId}_secret_demo",
            "publishable_key" to publishableKey,
        )

    override fun verify(req: VerifyRequest): PaymentStatusDto {
        // STUB: real impl = stripe.paymentIntents.retrieve(paymentIntentId).status == "succeeded".
        val marker = req.extra["payment_intent_status"] ?: req.extra["marker"]
        return if (marker == "succeeded") PaymentStatusDto.SUCCESS else PaymentStatusDto.PENDING
    }
}

/**
 * Cashfree adapter.
 *
 * - createProviderOrder → payment_session_id + order_id for the Cashfree SDK.
 * - verify → STUBBED external call, mirrors Stripe. Real impl queries the Cashfree Orders API
 *   (`GET /orders/{order_id}`) and reads `order_status`. Later milestone.
 */
class CashfreeAdapter(
    @Suppress("unused") private val appId: String,
    @Suppress("unused") private val secret: String,
) : GatewayAdapter {
    override val gatewayId: String = "cashfree"

    override fun createProviderOrder(
        orderId: String,
        item: CatalogItemDto,
    ): Map<String, String> =
        mapOf(
            "payment_session_id" to "session_${orderId}_demo",
            "order_id" to orderId,
        )

    override fun verify(req: VerifyRequest): PaymentStatusDto {
        // STUB: real impl = GET /orders/{order_id} → order_status == "PAID".
        val marker = req.extra["order_status"] ?: req.extra["marker"]
        return if (marker == "PAID" || marker == "succeeded") PaymentStatusDto.SUCCESS else PaymentStatusDto.PENDING
    }
}
