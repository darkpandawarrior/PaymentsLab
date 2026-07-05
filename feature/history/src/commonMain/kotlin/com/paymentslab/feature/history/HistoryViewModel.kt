package com.paymentslab.feature.history

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paymentslab.core.designsystem.format
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

/** Immutable state for [HistoryScreen]: the filtered payment history stream, newest first. */
@Immutable
data class HistoryUiState(
    val rows: ImmutableList<HistoryRow> = persistentListOf(),
    val isLoading: Boolean = true,
    val selectedStatuses: Set<PaymentStatus> = emptySet(),
)

/**
 * Collects the journal's full history stream and projects each [PendingPayment] into a display
 * [HistoryRow], newest first, filtered by [HistoryUiState.selectedStatuses] (empty = show all —
 * mirrors [com.paymentslab.feature.lab.LabHomeViewModel]'s status-filter shape).
 */
class HistoryViewModel(
    private val journal: PendingPaymentJournal,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private var allPayments: List<PendingPayment> = emptyList()

    init {
        viewModelScope.launch {
            journal.observeAll().collect { payments ->
                allPayments = payments
                recompute()
            }
        }
    }

    fun onToggleStatusFilter(status: PaymentStatus) {
        val current = _uiState.value.selectedStatuses
        recompute(selected = if (status in current) current - status else current + status)
    }

    // Takes `selected` explicitly (rather than always reading it back from `_uiState.value`) so a
    // toggle writes `_uiState` exactly once — folding the selection change and the row recompute
    // into the same assignment avoids a second, separate emission per toggle.
    private fun recompute(selected: Set<PaymentStatus> = _uiState.value.selectedStatuses) {
        val filtered = if (selected.isEmpty()) allPayments else allPayments.filter { it.status in selected }
        _uiState.value =
            _uiState.value.copy(
                rows =
                    filtered
                        .sortedByDescending { it.createdAtEpochMs }
                        .map { it.toRow() }
                        .toImmutableList(),
                isLoading = false,
                selectedStatuses = selected,
            )
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
