package com.paymentslab.feature.history

import com.paymentslab.core.paymentsapi.PaymentStatus
import com.paymentslab.core.paymentsapi.PendingPayment
import com.paymentslab.core.paymentsapi.PendingPaymentJournal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** In-memory [PendingPaymentJournal] whose [observeAll] stream tests can drive via [emit]. */
class FakePendingPaymentJournal(
    initial: List<PendingPayment> = emptyList(),
) : PendingPaymentJournal {
    private val state = MutableStateFlow(initial)

    fun emit(payments: List<PendingPayment>) {
        state.value = payments
    }

    override suspend fun record(entry: PendingPayment) {
        state.update { it + entry }
    }

    override suspend fun markResolved(
        orderId: String,
        status: PaymentStatus,
        paymentId: String?,
    ) {
        state.update { list ->
            list.map { if (it.orderId == orderId) it.copy(status = status, paymentId = paymentId) else it }
        }
    }

    override suspend fun unresolved(): List<PendingPayment> = state.value.filter { !it.status.isTerminal }

    override fun observeAll(): Flow<List<PendingPayment>> = state
}
