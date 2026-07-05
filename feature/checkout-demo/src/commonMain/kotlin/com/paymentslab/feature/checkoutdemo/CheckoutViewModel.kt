package com.paymentslab.feature.checkoutdemo

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paymentslab.core.designsystem.ClientResultCopy
import com.paymentslab.core.designsystem.ErroredCopy
import com.paymentslab.core.designsystem.LaunchingCopy
import com.paymentslab.core.designsystem.OrderCreatedCopy
import com.paymentslab.core.designsystem.SettledCopy
import com.paymentslab.core.designsystem.StepState
import com.paymentslab.core.designsystem.TimelineCopy
import com.paymentslab.core.designsystem.TimelineStep
import com.paymentslab.core.designsystem.VerifyingCopy
import com.paymentslab.core.designsystem.toTimelineStep
import com.paymentslab.core.orchestration.PaymentFlowRunner
import com.paymentslab.core.paymentsapi.GatewayId
import com.paymentslab.core.paymentsapi.GatewayStatus
import com.paymentslab.core.paymentsapi.PaymentGatewayRegistry
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

/** The checkout demo's friendlier, consumer-facing wording for the shared timeline mapping. */
private val CheckoutTimelineCopy =
    TimelineCopy(
        orderCreated =
            OrderCreatedCopy(
                title = "Order confirmed",
                subtitle = { amount -> "We asked the server for the price — $amount" },
            ),
        launching =
            LaunchingCopy(
                title = { gatewayId -> "Opening $gatewayId" },
                subtitle = "Saved a recovery note before opening the payment sheet",
            ),
        clientResult =
            ClientResultCopy(
                title = "Payment sheet returned",
                successSubtitle = "Reported success (not yet verified)",
                failureSubtitle = { code -> "Reported failure: $code" },
                pendingSubtitle = { reason -> "Reported pending: $reason" },
                cancelledSubtitle = "You cancelled the payment",
            ),
        verifying =
            VerifyingCopy(
                title = "Double-checking with the server",
                subtitle = "A success on the phone is only trusted once the server agrees",
            ),
        settled =
            SettledCopy(
                title = "Done",
                subtitle = { statusName -> "Server says: $statusName" },
            ),
        errored =
            ErroredCopy(
                title = "Something went wrong",
                fallbackSubtitle = "The checkout could not complete",
            ),
    )

/** A gateway the demo can actually run — only SANDBOX_READY providers are offered. */
@Immutable
data class CheckoutGateway(
    val id: GatewayId,
    val displayName: String,
)

/** State for [CheckoutScreen]: the selected product + gateway, the live mini-timeline, the outcome. */
@Immutable
data class CheckoutUiState(
    val products: ImmutableList<DemoProduct> = DEMO_PRODUCTS,
    val gateways: ImmutableList<CheckoutGateway> = persistentListOf(),
    val selectedProduct: DemoProduct? = null,
    val selectedGatewayId: GatewayId? = null,
    val steps: ImmutableList<TimelineStep> = persistentListOf(),
    val isRunning: Boolean = false,
    val finalStatus: PaymentStatus? = null,
) {
    /** Pay is enabled only with a product, a gateway, and no run in flight. */
    val canPay: Boolean get() = selectedProduct != null && selectedGatewayId != null && !isRunning
}

/**
 * The explained-checkout demo. Picks a demo product and a SANDBOX_READY gateway, runs the same
 * [PaymentFlowRunner] flow the Lab uses, and folds the [PaymentStep]s into a friendly mini-timeline
 * so the checkout doubles as the explainer. Concurrent runs are guarded by [CheckoutUiState.isRunning].
 */
class CheckoutViewModel(
    private val flowRunner: PaymentFlowRunner,
    registry: PaymentGatewayRegistry,
) : ViewModel() {
    private val _uiState =
        MutableStateFlow(
            CheckoutUiState(
                gateways =
                    registry.gateways
                        .filter { it.meta.status == GatewayStatus.SANDBOX_READY }
                        .map { CheckoutGateway(it.id, it.meta.displayName) }
                        .toImmutableList(),
            ),
        )
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    fun selectProduct(product: DemoProduct) {
        if (_uiState.value.isRunning) return
        _uiState.value = _uiState.value.copy(selectedProduct = product)
    }

    fun selectGateway(gatewayId: GatewayId) {
        if (_uiState.value.isRunning) return
        _uiState.value = _uiState.value.copy(selectedGatewayId = gatewayId)
    }

    fun pay(host: PaymentHost) {
        val current = _uiState.value
        if (!current.canPay) return
        val product = current.selectedProduct ?: return
        val gatewayId = current.selectedGatewayId ?: return

        _uiState.value =
            current.copy(
                isRunning = true,
                steps = persistentListOf(),
                finalStatus = null,
            )

        viewModelScope.launch {
            val accumulated = mutableListOf<PaymentStep>()
            try {
                flowRunner.run(host, gatewayId, product.catalogItemId).collect { step ->
                    accumulated += step
                    _uiState.value = _uiState.value.copy(steps = accumulated.toTimeline(runInFlight = true))
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

    private fun List<PaymentStep>.toTimeline(runInFlight: Boolean): ImmutableList<TimelineStep> {
        val mapped = map { it.toTimelineStep(CheckoutTimelineCopy) }.toMutableList()
        if (runInFlight && mapped.isNotEmpty()) {
            val lastIndex = mapped.lastIndex
            val last = mapped[lastIndex]
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
        const val TAG = "CheckoutViewModel"
    }
}
