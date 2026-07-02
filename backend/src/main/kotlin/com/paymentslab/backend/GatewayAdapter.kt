package com.paymentslab.backend

import com.paymentslab.core.protocol.CatalogItemDto
import com.paymentslab.core.protocol.PaymentStatusDto
import com.paymentslab.core.protocol.VerifyRequest

/**
 * A payment-provider abstraction. Each implementation knows how to (a) mint the provider-specific
 * session material the client SDK needs to launch checkout, and (b) verify the proof the client
 * hands back afterwards.
 *
 * Only PUBLISHABLE values ever appear in [createProviderOrder] output (key_id, client_secret,
 * publishable_key, session ids) — never a secret key.
 */
interface GatewayAdapter {
    /** Stable id used to look this adapter up (e.g. "razorpay", "upi_intent", "stripe", "cashfree"). */
    val gatewayId: String

    /**
     * Build the provider params for a new order. [orderId] is the server-generated id; [item] carries
     * the server-authoritative amount/currency. `suspend` because a real gateway (Paystack) makes a
     * network call here (`POST /transaction/initialize`); the other adapters just build a map.
     */
    suspend fun createProviderOrder(
        orderId: String,
        item: CatalogItemDto,
    ): Map<String, String>

    /** Verify the client's proof. Returns the resolved server-authoritative status. */
    suspend fun verify(req: VerifyRequest): PaymentStatusDto

    /**
     * Verify an inbound webhook's authenticity from its raw body + headers. Default accepts
     * unconditionally — correct for demo/no-signature providers (UPI intent, hosted-webview, mobile
     * money); a real signature scheme (Razorpay's HMAC, Stripe's `Stripe-Signature`, Cashfree's
     * `x-webhook-signature`) overrides this. Replaces the old razorpay-only special case that used to
     * live directly in the `/webhooks/{provider}` route.
     */
    fun verifyWebhook(
        rawBody: String,
        headers: Map<String, String>,
    ): WebhookVerification = WebhookVerification.Accepted
}

/** Outcome of [GatewayAdapter.verifyWebhook]. */
sealed interface WebhookVerification {
    data object Accepted : WebhookVerification

    data class Rejected(
        val reason: String,
    ) : WebhookVerification
}

/**
 * Registry of adapters by [GatewayAdapter.gatewayId]. Returns null for an unknown id so routes can
 * map that to a 400 ApiError.
 */
class GatewayRegistry(
    adapters: List<GatewayAdapter>,
) {
    private val byId: Map<String, GatewayAdapter> = adapters.associateBy { it.gatewayId }

    fun find(gatewayId: String): GatewayAdapter? = byId[gatewayId]
}
