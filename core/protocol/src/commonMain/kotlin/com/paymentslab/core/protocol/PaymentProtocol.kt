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

// ── Wallet ledger (provider:wallet, the internal-rail archetype) ───────────────────────────────

/** `GET /wallet/{accountId}/balance` response. */
@Serializable
data class WalletBalanceResponse(
    val accountId: String,
    val balanceMinor: Long,
)

/** `POST /wallet/{accountId}/debit` request — the ledger "pay" movement, carries an idempotency key. */
@Serializable
data class WalletDebitRequest(
    val idempotencyKey: String,
    val amountMinor: Long,
)

/** `POST /wallet/{accountId}/refund` request — the ledger "refund" movement (a credit back). */
@Serializable
data class WalletRefundRequest(
    val idempotencyKey: String,
    val amountMinor: Long,
)

@Serializable
data class WalletTransactionResponse(
    val txnId: String,
    val accountId: String,
    val balanceMinor: Long,
)

// ── Payouts / Transfers (the first real payout rail, MOCK_MODE/KYC_GATED — see PayoutStatusDto) ───

/** Payout lifecycle. Real payout rails are KYC-gated, so SETTLED only ever arrives via the mock webhook. */
@Serializable
enum class PayoutStatusDto {
    @SerialName("pending")
    PENDING,

    @SerialName("settled")
    SETTLED,

    @SerialName("failed")
    FAILED,
}

/**
 * `POST /payouts` request. [idempotencyKey] follows the same dedup contract as
 * [CreateOrderRequest.idempotencyKey] — a retried initiate for the same logical attempt must not mint
 * a second live payout.
 */
@Serializable
data class InitiatePayoutRequest(
    val gatewayId: String,
    val recipientRef: String,
    val amountMinor: Long,
    val currency: String,
    val idempotencyKey: String,
)

@Serializable
data class PayoutResponse(
    val payoutId: String,
    val gatewayId: String,
    val recipientRef: String,
    val amountMinor: Long,
    val currency: String,
    val status: PayoutStatusDto,
    val updatedAtEpochMs: Long,
)

// ── Mandates / subscriptions (roadmap #6 — Razorpay recurring) ─────────────────────────────────

/** Mandate lifecycle. A mandate is authorized once (SETUP → ACTIVE), then debited repeatedly. */
@Serializable
enum class MandateStatusDto {
    @SerialName("created")
    CREATED,

    @SerialName("active")
    ACTIVE,

    @SerialName("failed")
    FAILED,

    @SerialName("cancelled")
    CANCELLED,
}

/**
 * `POST /mandates` request — sets up a recurring mandate (authorize, not a one-time charge).
 * Same idempotency-key dedup contract as [CreateOrderRequest.idempotencyKey].
 */
@Serializable
data class CreateMandateRequest(
    val catalogItemId: String,
    val gatewayId: String,
    val idempotencyKey: String,
)

@Serializable
data class MandateResponse(
    val mandateId: String,
    val catalogItemId: String,
    val gatewayId: String,
    val amountMinor: Long,
    val currency: String,
    val status: MandateStatusDto,
    val providerParams: Map<String, String> = emptyMap(),
    val updatedAtEpochMs: Long,
)

/**
 * `POST /mandates/{mandateId}/debits` request — one recurring debit charged against an ACTIVE
 * mandate. [idempotencyKey] follows the same dedup contract as order/payout creation: a retried
 * debit for the same logical attempt must not charge twice. A real recurring schedule (charging
 * automatically on a cadence) needs a backend scheduler — out of scope here; this models a single
 * debit call, which is what a scheduler would invoke per cycle.
 */
@Serializable
data class DebitMandateRequest(
    val idempotencyKey: String,
)

@Serializable
data class MandateDebitResponse(
    val debitId: String,
    val mandateId: String,
    val amountMinor: Long,
    val currency: String,
    val status: PaymentStatusDto,
    val updatedAtEpochMs: Long,
)

// ── Vault (roadmap #7 — Stripe Customer + stored instruments) ──────────────────────────────────

/**
 * `POST /vault/{customerId}/instruments` request — saves a card token against a Stripe-style
 * Customer. [cardToken] is the raw opaque token from the client SDK; the server never echoes it
 * back — only [SavedInstrumentDto] (masked last4/brand) is ever returned. [idempotencyKey] follows
 * the same dedup contract as [CreateOrderRequest.idempotencyKey].
 */
@Serializable
data class SaveInstrumentRequest(
    val cardToken: String,
    val brand: String,
    val last4: String,
    val idempotencyKey: String,
)

/** A saved instrument as the client ever sees it — masked, never the raw token. */
@Serializable
data class SavedInstrumentDto(
    val instrumentId: String,
    val customerId: String,
    val brand: String,
    val last4: String,
    val createdAtEpochMs: Long,
)

@Serializable
data class SavedInstrumentsResponse(
    val customerId: String,
    val instruments: List<SavedInstrumentDto>,
)

/**
 * `POST /vault/{customerId}/instruments/{instrumentId}/charge` request — charges an order using a
 * previously saved instrument, mirroring how a stored `card_id` was charged. [idempotencyKey]
 * follows the same dedup contract as [CreateOrderRequest.idempotencyKey].
 */
@Serializable
data class ChargeInstrumentRequest(
    val catalogItemId: String,
    val idempotencyKey: String,
)

@Serializable
data class InstrumentChargeResponse(
    val chargeId: String,
    val customerId: String,
    val instrumentId: String,
    val amountMinor: Long,
    val currency: String,
    val status: PaymentStatusDto,
    val updatedAtEpochMs: Long,
)
