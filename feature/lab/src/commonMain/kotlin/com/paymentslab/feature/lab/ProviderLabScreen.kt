package com.paymentslab.feature.lab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paymentslab.core.designsystem.DesignTokens
import com.paymentslab.core.designsystem.LabScaffold
import com.paymentslab.core.designsystem.PrimaryButton
import com.paymentslab.core.designsystem.SectionHeader
import com.paymentslab.core.designsystem.StepTimeline
import com.paymentslab.core.paymentsapi.GatewayId
import com.paymentslab.core.paymentsapi.PaymentHost
import org.koin.compose.viewmodel.koinViewModel

/**
 * Stateful entry point for a provider's live lab. [paymentHost] is the platform payment host the
 * app supplies (the real `AndroidPaymentHost`) — the screen never constructs it, keeping Activity
 * references out of feature code.
 */
@Composable
fun ProviderLabRoot(
    paymentHost: PaymentHost,
    gatewayId: GatewayId,
    providerName: String,
    priceLabel: String,
    catalogItemId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProviderLabViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProviderLabScreen(
        state = state,
        providerName = providerName,
        priceLabel = priceLabel,
        onPay = { viewModel.start(paymentHost, gatewayId, catalogItemId) },
        onBack = onBack,
        modifier = modifier,
    )
}

/**
 * Stateless live-payment lab: a big [StepTimeline] of the orchestrator's steps and a
 * [PrimaryButton] that kicks off (or replays) the sandbox payment.
 */
@Composable
fun ProviderLabScreen(
    state: ProviderLabUiState,
    providerName: String,
    priceLabel: String,
    onPay: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LabScaffold(title = providerName, onBack = onBack) { padding ->
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = DesignTokens.Spacing.lg)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.md),
        ) {
            SectionHeader(text = "Live payment timeline")

            if (state.steps.isEmpty()) {
                Text(
                    text =
                        "Tap the button below to run a sandbox payment and watch every hop — " +
                            "order creation, launch, client result, verification and the server-authoritative outcome.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                StepTimeline(
                    steps = state.steps,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            val buttonText =
                if (state.hasRun && !state.isRunning) {
                    "Run again"
                } else {
                    "Pay $priceLabel (sandbox)"
                }
            PrimaryButton(
                text = buttonText,
                onClick = onPay,
                enabled = !state.isRunning,
                modifier = Modifier.fillMaxWidth(),
            )

            state.finalStatus?.let { status ->
                Text(
                    text = "Final server status: ${status.name}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
