package com.paymentslab.feature.lab

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paymentslab.core.designsystem.FlowHop
import com.paymentslab.core.designsystem.StepState
import com.paymentslab.core.designsystem.TimelineStep
import com.paymentslab.core.paymentsapi.GatewayId
import com.paymentslab.core.paymentsapi.PaymentHost
import com.paymentslab.core.paymentsapi.PaymentStatus
import com.paymentslab.core.paymentsapi.PaymentStep
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * State for [ProviderLabScreen]: the live timeline built from the orchestrator's [PaymentStep]
 * stream, whether a run is in flight, and the terminal server status once settled.
 */
@Immutable
data class ProviderLabUiState(
    val steps: ImmutableList<TimelineStep> = persistentListOf(),
    val isRunning: Boolean = false,
    val finalStatus: PaymentStatus? = null,
    val hasRun: Boolean = false,
    /** Where the [PaymentFlowDiagram] packet currently sits — null until the first step lands. */
    val currentHop: FlowHop? = null,
    /** Whether [currentHop] has been backend-confirmed yet, vs. still just a client hint. */
    val verified: Boolean = false,
)

/**
 * Drives one provider's live payment through [PaymentFlowRunner] and folds the emitted
 * [PaymentStep]s into a growing [TimelineStep] list. While the flow is in flight the trailing step
 * is shown ACTIVE (pulsing); when a terminal step lands, the trailing step takes its own terminal
 * colour. Concurrent runs are guarded by [ProviderLabUiState.isRunning] — a second [start] while one
 * is running is ignored. "Run again" is [start] itself, which resets the timeline first.
 */
class ProviderLabViewModel(
    private val flowRunner: PaymentFlowRunner,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProviderLabUiState())
    val uiState: StateFlow<ProviderLabUiState> = _uiState.asStateFlow()

    fun start(
        host: PaymentHost,
        gatewayId: GatewayId,
        catalogItemId: String,
    ) {
        if (_uiState.value.isRunning) return
        _uiState.value = ProviderLabUiState(isRunning = true, hasRun = true)

        viewModelScope.launch {
            val accumulated = mutableListOf<PaymentStep>()
            try {
                flowRunner.run(host, gatewayId, catalogItemId).collect { step ->
                    accumulated += step
                    _uiState.value =
                        _uiState.value.copy(
                            steps = accumulated.toTimeline(runInFlight = true),
                            currentHop = step.toFlowHop(),
                            verified = step.isVerified(),
                        )
                }
                _uiState.value =
                    _uiState.value.copy(
                        steps = accumulated.toTimeline(runInFlight = false),
                        isRunning = false,
                        finalStatus = accumulated.terminalStatus(),
                    )
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                _uiState.value = _uiState.value.copy(isRunning = false)
            }
        }
    }

    /**
     * Build the timeline from accumulated steps. While the run is in flight the last non-terminal
     * step pulses ACTIVE; once the flow completes every step keeps its mapped state.
     */
    private fun List<PaymentStep>.toTimeline(runInFlight: Boolean): ImmutableList<TimelineStep> {
        val mapped = map { it.toTimelineStep() }.toMutableList()
        if (runInFlight && mapped.isNotEmpty()) {
            val lastIndex = mapped.lastIndex
            val last = mapped[lastIndex]
            // Only in-flight (non-terminal) steps get promoted to ACTIVE; a terminal step keeps its colour.
            if (last.state == StepState.DONE) {
                mapped[lastIndex] = last.copy(state = StepState.ACTIVE)
            }
        }
        return mapped.toImmutableList()
    }

    private fun List<PaymentStep>.terminalStatus(): PaymentStatus? =
        when (val last = lastOrNull()) {
            is PaymentStep.Settled -> last.status
            is PaymentStep.Errored -> PaymentStatus.FAILED
            else -> null
        }

    private companion object {
        const val TAG = "ProviderLabViewModel"
    }
}
