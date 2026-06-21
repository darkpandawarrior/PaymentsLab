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
    private val webhookSecret: String,
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

    /** REAL HMAC-SHA256 over the raw webhook body, compared to `X-Razorpay-Signature`. */
    override fun verifyWebhook(
        rawBody: String,
        headers: Map<String, String>,
    ): WebhookVerification {
        val sigHeader =
            headers["X-Razorpay-Signature"]
                ?: return WebhookVerification.Rejected("missing X-Razorpay-Signature header")
        val expected = Crypto.hmacSha256Hex(webhookSecret, rawBody)
        return if (Crypto.constantTimeEquals(expected, sigHeader)) {
            WebhookVerification.Accepted
        } else {
            WebhookVerification.Rejected("signature mismatch")
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

/**
 * Config for one archetype-C (hosted-webview) or archetype-D (mobile-money) gateway. One backend
 * adapter class serves every gateway of that archetype — matches `provider:hosted-webview`'s
 * one-module-N-configs shape (plan Part C/B4) instead of a backend class per gateway.
 */
data class HostedGatewayServerConfig(
    val gatewayId: String,
    val displayName: String,
)

/**
 * Archetype-C adapter: `createProviderOrder` hands the client a `checkout_url` pointing at
 * `GET /mock/checkout/{provider}` (see `MockCheckoutRoutes.kt`) instead of SDK session material.
 *
 * `verify` mirrors [UpiIntentAdapter]'s honesty: a client-reported return-URL redirect is NOT
 * cryptographically verifiable the way a signed HMAC is — it's just "the browser landed on our
 * success page", which anyone could forge by hand-crafting the URL. So this always answers PENDING;
 * the mock webhook/momo-flip (or, later, a real provider webhook) is what actually resolves it.
 */
class HostedWebViewAdapter(
    private val config: HostedGatewayServerConfig,
    private val baseUrl: String,
) : GatewayAdapter {
    override val gatewayId: String = config.gatewayId

    override fun createProviderOrder(
        orderId: String,
        item: CatalogItemDto,
    ): Map<String, String> = mapOf("checkout_url" to "$baseUrl/mock/checkout/${config.gatewayId}?orderId=$orderId")

    override fun verify(req: VerifyRequest): PaymentStatusDto = PaymentStatusDto.PENDING
}

/**
 * Archetype-D adapter (async mobile money: M-Pesa push, MTN MoMo poll). There is no synchronous
 * client result at all — `createProviderOrder` returns a reference the client polls
 * `GET /payments/{id}` against, and only a (mock or real) webhook / momo-flip ever resolves it.
 */
class MobileMoneyAdapter(
    private val config: HostedGatewayServerConfig,
) : GatewayAdapter {
    override val gatewayId: String = config.gatewayId

    override fun createProviderOrder(
        orderId: String,
        item: CatalogItemDto,
    ): Map<String, String> = mapOf("momo_ref" to "momo_$orderId")

    override fun verify(req: VerifyRequest): PaymentStatusDto = PaymentStatusDto.PENDING
}
