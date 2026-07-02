package com.paymentslab.backend

import com.paymentslab.core.protocol.PaymentStatusDto
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory, thread-safe source of truth for orders and their payment state, keyed by `orderId`.
 *
 * This is intentionally the simplest thing that works: [ConcurrentHashMap] for records +
 * [KeySetView] for a processed-webhook-event-id set (idempotency). It is swappable for a real DB
 * (Exposed/SQLite/Postgres) later — the public surface (createOrder / recordVerification /
 * applyWebhook / get) is deliberately small so a JDBC-backed implementation can drop straight in.
 */
class PaymentStore {
    /** One record per order. `amountMinor`/`currency` are captured server-side at creation time. */
    data class PaymentRecord(
        val orderId: String,
        val catalogItemId: String,
        val gatewayId: String,
        val amountMinor: Long,
        val currency: String,
        val status: PaymentStatusDto,
        val paymentId: String? = null,
        val providerRef: String? = null,
        val updatedAtEpochMs: Long,
    )

    private val records = ConcurrentHashMap<String, PaymentRecord>()

    /** Deduplication set of already-processed webhook event ids. */
    private val processedEventIds: MutableSet<String> = ConcurrentHashMap.newKeySet()

    fun createOrder(
        orderId: String,
        catalogItemId: String,
        gatewayId: String,
        amountMinor: Long,
        currency: String,
    ): PaymentRecord {
        val record =
            PaymentRecord(
                orderId = orderId,
                catalogItemId = catalogItemId,
                gatewayId = gatewayId,
                amountMinor = amountMinor,
                currency = currency,
                status = PaymentStatusDto.CREATED,
                updatedAtEpochMs = System.currentTimeMillis(),
            )
        records[orderId] = record
        return record
    }

    fun get(orderId: String): PaymentRecord? = records[orderId]

    /**
     * Apply the outcome of a client-initiated verify. Merges the new status/paymentId/providerRef
     * into the existing record. No-op returning null if the order is unknown.
     */
    fun recordVerification(
        orderId: String,
        status: PaymentStatusDto,
        paymentId: String?,
        providerRef: String? = null,
    ): PaymentRecord? =
        records.computeIfPresent(orderId) { _, existing ->
            existing.copy(
                status = status,
                paymentId = paymentId ?: existing.paymentId,
                providerRef = providerRef ?: existing.providerRef,
                updatedAtEpochMs = System.currentTimeMillis(),
            )
        }

    /** Result of an idempotent webhook application. */
    data class WebhookResult(
        val duplicate: Boolean,
        val record: PaymentRecord?,
    )

    /**
     * Idempotently apply a webhook event. If [eventId] was already processed, returns
     * `duplicate = true` and leaves state UNCHANGED. Otherwise records the event id and applies
     * the new status (if the order exists).
     */
    fun applyWebhook(
        eventId: String,
        orderId: String,
        status: PaymentStatusDto,
        paymentId: String?,
        providerRef: String? = null,
    ): WebhookResult {
        // add() returns false if the id was already present → duplicate, do NOT mutate state.
        val firstTime = processedEventIds.add(eventId)
        if (!firstTime) {
            return WebhookResult(duplicate = true, record = records[orderId])
        }
        val updated = recordVerification(orderId, status, paymentId, providerRef)
        return WebhookResult(duplicate = false, record = updated)
    }
}
