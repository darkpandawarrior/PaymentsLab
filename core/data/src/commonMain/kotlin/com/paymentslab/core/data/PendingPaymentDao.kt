package com.paymentslab.core.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * DAO for the [PendingPaymentEntity] journal.
 *
 * [unresolved] is the cold-start recovery work list: every row whose [PendingPaymentEntity.status]
 * is not one of the terminal states ([com.siddharth.kmp.paymentsapi.PaymentStatus.isTerminal] —
 * SUCCESS, FAILED, CANCELLED, REFUNDED). The terminal set is inlined as a `NOT IN` literal because
 * Room cannot bind an enum collection into a compiled `IN` clause.
 */
@Dao
interface PendingPaymentDao {
    @Upsert
    suspend fun upsert(entity: PendingPaymentEntity)

    @Query(
        "UPDATE pending_payments SET status = :status, paymentId = :paymentId WHERE orderId = :orderId",
    )
    suspend fun updateStatus(
        orderId: String,
        status: String,
        paymentId: String?,
    )

    @Query(
        "SELECT * FROM pending_payments " +
            "WHERE status NOT IN ('SUCCESS', 'FAILED', 'CANCELLED', 'REFUNDED') " +
            "ORDER BY createdAtEpochMs DESC",
    )
    suspend fun unresolved(): List<PendingPaymentEntity>

    @Query("SELECT * FROM pending_payments ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<PendingPaymentEntity>>
}
