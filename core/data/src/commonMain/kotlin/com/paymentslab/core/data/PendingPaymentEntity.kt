package com.paymentslab.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Durable row for one payment attempt — the process-death journal's storage shape.
 *
 * Written *before* the gateway SDK launches so a mid-payment kill (OEM battery kill, task-swipe,
 * low-memory during the bank's 3DS WebView) leaves a recoverable record. Enum-like fields
 * ([gatewayId], [status]) are stored as `TEXT` (converter-free string pattern), and [amountMinor]
 * keeps money in minor units as a `Long` — never floating point.
 */
@Entity(tableName = "pending_payments")
data class PendingPaymentEntity(
    @PrimaryKey
    val orderId: String,
    val catalogItemId: String,
    val gatewayId: String,
    val amountMinor: Long,
    val currency: String,
    val createdAtEpochMs: Long,
    val status: String,
    val paymentId: String? = null,
)
