package com.paymentslab.core.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The wire contract between the app and the `backend/` Ktor server. These `@Serializable` types live
// in a KMP module with a JVM target so the exact same classes compile into both the Android client
// and the JVM server — the DTOs can never drift.

/** Terminal + intermediate payment states, server-authoritative. */
@Serializable
enum class PaymentStatusDto {
    @SerialName("created")
    CREATED,

    @SerialName("pending")
    PENDING,

    @SerialName("success")
    SUCCESS,

    @SerialName("failed")
    FAILED,

    @SerialName("cancelled")
    CANCELLED,

    @SerialName("refunded")
    REFUNDED,
}

/** A purchasable item. Price lives server-side; the client only ever sends [id]. */
@Serializable
data class CatalogItemDto(
    val id: String,
    val title: String,
    val description: String,
    val amountMinor: Long,
    val currency: String,
    val imageUrl: String? = null,
)

/**
 * `POST /orders` request — note: NO amount. The server resolves price from [catalogItemId].
 *
 * [idempotencyKey] is client-generated once per logical order attempt and reused across retries of
 * the SAME attempt, so a retried request dedups server-side instead of minting a second live order
 * (see `PaymentStore.createOrder`).
 */
@Serializable
data class CreateOrderRequest(
    val catalogItemId: String,
    val gatewayId: String,
    val idempotencyKey: String,
)

/**
 * `POST /orders` response. [providerParams] carries the provider-specific session material the SDK
 * needs (Razorpay `key_id`+`order_id`, Cashfree `payment_session_id`, Stripe `client_secret`, UPI
 * intent reference fields) — always publishable values, never secret keys.
 */
@Serializable
data class OrderResponse(
    val orderId: String,
    val catalogItemId: String,
    val amountMinor: Long,
    val currency: String,
    val gatewayId: String,
    val providerParams: Map<String, String> = emptyMap(),
)

/** `POST /payments/{id}/verify` — provider-specific proof the client hands back for server checking. */
@Serializable
data class VerifyRequest(
    val gatewayId: String,
    val orderId: String,
    val paymentId: String? = null,
    val signature: String? = null,
    val extra: Map<String, String> = emptyMap(),
)

@Serializable
data class VerifyResponse(
    val status: PaymentStatusDto,
    val paymentId: String? = null,
    val message: String? = null,
)

/** `GET /payments/{id}` — the polling target; server state updated by webhooks. */
@Serializable
data class PaymentStatusResponse(
    val orderId: String,
    val paymentId: String? = null,
    val status: PaymentStatusDto,
    val updatedAtEpochMs: Long,
    val providerRef: String? = null,
)

@Serializable
data class WebhookAck(
    val received: Boolean,
    val eventId: String? = null,
    val duplicate: Boolean = false,
)

@Serializable
data class ApiError(
    val code: String,
    val message: String,
)
