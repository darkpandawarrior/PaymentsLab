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
     * the server-authoritative amount/currency.
     */
    fun createProviderOrder(
        orderId: String,
        item: CatalogItemDto,
    ): Map<String, String>

    /** Verify the client's proof. Returns the resolved server-authoritative status. */
    fun verify(req: VerifyRequest): PaymentStatusDto
}

/**
 * Registry of adapters by [GatewayAdapter.gatewayId]. Returns null for an unknown id so routes can
 * map that to a 400 ApiError.
 */
class GatewayRegistry(adapters: List<GatewayAdapter>) {
    private val byId: Map<String, GatewayAdapter> = adapters.associateBy { it.gatewayId }

    fun find(gatewayId: String): GatewayAdapter? = byId[gatewayId]
}
