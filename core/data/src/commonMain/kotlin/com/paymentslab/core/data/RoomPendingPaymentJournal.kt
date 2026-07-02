package com.paymentslab.core.data

import com.paymentslab.core.paymentsapi.PaymentStatus
import com.paymentslab.core.paymentsapi.PendingPayment
import com.paymentslab.core.paymentsapi.PendingPaymentJournal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room-backed [PendingPaymentJournal] — the process-death insurance policy from `core:payments-api`.
 *
 * Delegates persistence to [PendingPaymentDao] and translates every row through the pure
 * [toEntity]/[toDomain] mappers, so the orchestrator only ever sees domain types. `createdAtEpochMs`
 * is supplied by the caller (already on the [PendingPayment]); no clock is needed here.
 */
class RoomPendingPaymentJournal(
    private val dao: PendingPaymentDao,
) : PendingPaymentJournal {
    override suspend fun record(entry: PendingPayment) {
        dao.upsert(entry.toEntity())
    }

    override suspend fun markResolved(
        orderId: String,
        status: PaymentStatus,
        paymentId: String?,
    ) {
        dao.updateStatus(orderId = orderId, status = status.name, paymentId = paymentId)
    }

    override suspend fun unresolved(): List<PendingPayment> = dao.unresolved().map { it.toDomain() }

    override fun observeAll(): Flow<List<PendingPayment>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }
}
