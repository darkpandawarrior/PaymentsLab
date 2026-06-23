package com.paymentslab.feature.lab

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.paymentslab.core.designsystem.GatewayStatusUi
import com.paymentslab.core.paymentsapi.Capability
import com.paymentslab.core.paymentsapi.GatewayId
import com.paymentslab.core.paymentsapi.PaymentGatewayRegistry
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One provider row on the Lab home — everything the card needs, no domain types leaking to UI. */
@Immutable
data class ProviderRow(
    val id: GatewayId,
    val displayName: String,
    val status: GatewayStatusUi,
    val region: String,
    val blurb: String,
    val capabilities: ImmutableList<String>,
)

/** Immutable state for [LabHomeScreen]: the catalog of registered providers. */
@Immutable
data class LabHomeUiState(
    val providers: ImmutableList<ProviderRow> = persistentListOf(),
)

/**
 * Reads the [PaymentGatewayRegistry] once and projects each registered gateway's catalog metadata
 * into a [ProviderRow]. The registry is a synchronous, in-memory contract, so no coroutine is needed
 * here — the state is derived at construction and never changes.
 */
class LabHomeViewModel(
    registry: PaymentGatewayRegistry,
) : ViewModel() {
    private val _uiState =
        MutableStateFlow(
            LabHomeUiState(
                providers =
                    registry.gateways
                        .map { gateway ->
                            ProviderRow(
                                id = gateway.id,
                                displayName = gateway.meta.displayName,
                                status = gateway.meta.status.toUi(),
                                region = gateway.meta.region,
                                blurb = gateway.meta.blurb,
                                capabilities =
                                    gateway.meta.capabilities
                                        .map { it.label() }
                                        .toImmutableList(),
                            )
                        }.toImmutableList(),
            ),
        )
    val uiState: StateFlow<LabHomeUiState> = _uiState.asStateFlow()

    private companion object {
        const val TAG = "LabHomeViewModel"
    }
}

/** Human-readable capability label for a chip. */
private fun Capability.label(): String =
    when (this) {
        Capability.ONE_TIME_PAYMENT -> "One-time"
        Capability.UPI -> "UPI"
        Capability.CARDS -> "Cards"
        Capability.WALLET -> "Wallet"
        Capability.NET_BANKING -> "Net banking"
        Capability.REFUND -> "Refund"
        Capability.MANDATE -> "Mandate"
    }
