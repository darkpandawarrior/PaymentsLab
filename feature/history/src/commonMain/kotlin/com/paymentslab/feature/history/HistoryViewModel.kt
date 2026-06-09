package com.paymentslab.feature.history

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paymentslab.core.paymentsapi.PaymentStatus
import com.paymentslab.core.paymentsapi.PendingPayment
import com.paymentslab.core.paymentsapi.PendingPaymentJournal
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** One row in the payment history, fully formatted for display. */
@Immutable
data class HistoryRow(
    val orderId: String,
    val catalogItemId: String,
    val gatewayId: String,
    val amount: String,
    val status: PaymentStatus,
    val createdAtEpochMs: Long,
)

/** Immutable state for [HistoryScreen]: the full payment history stream, newest first. */
@Immutable
data class HistoryUiState(
    val rows: ImmutableList<HistoryRow> = persistentListOf(),
    val isLoading: Boolean = true,
)

/**
 * Collects the journal's full history stream and projects each [PendingPayment] into a display
 * [HistoryRow], newest first. The stream is hot and driven by Room, so the state updates live as
 * payments are recorded and resolved.
 */
class HistoryViewModel(
    journal: PendingPaymentJournal,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            journal.observeAll().collect { payments ->
                _uiState.value =
                    HistoryUiState(
                        rows =
                            payments
                                .sortedByDescending { it.createdAtEpochMs }
                                .map { it.toRow() }
                                .toImmutableList(),
                        isLoading = false,
                    )
            }
        }
    }

    private companion object {
        const val TAG = "HistoryViewModel"
    }
}

private fun PendingPayment.toRow(): HistoryRow =
    HistoryRow(
        orderId = orderId,
        catalogItemId = catalogItemId,
        gatewayId = gatewayId.value,
        amount = amount.format(),
        status = status,
        createdAtEpochMs = createdAtEpochMs,
    )
