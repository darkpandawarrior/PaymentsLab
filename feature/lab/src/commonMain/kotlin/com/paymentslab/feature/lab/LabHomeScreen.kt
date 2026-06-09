package com.paymentslab.feature.lab

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paymentslab.core.designsystem.DesignTokens
import com.paymentslab.core.designsystem.GatewayStatusBadge
import com.paymentslab.core.designsystem.LabScaffold
import com.paymentslab.core.designsystem.SectionHeader
import com.paymentslab.core.paymentsapi.GatewayId
import kotlinx.collections.immutable.ImmutableList
import org.koin.compose.viewmodel.koinViewModel

/** Stateful entry point: resolves the ViewModel and hands its state to the stateless [LabHomeScreen]. */
@Composable
fun LabHomeRoot(
    onOpenProvider: (GatewayId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LabHomeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LabHomeScreen(
        state = state,
        onOpenProvider = onOpenProvider,
        modifier = modifier,
    )
}

/** Stateless catalog of payment providers. Tapping a card opens that provider's live lab. */
@Composable
fun LabHomeScreen(
    state: LabHomeUiState,
    onOpenProvider: (GatewayId) -> Unit,
    modifier: Modifier = Modifier,
) {
    LabScaffold(title = "Integration Lab") { padding ->
        LazyColumn(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = DesignTokens.Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.md),
        ) {
            item {
                SectionHeader(text = "Providers")
            }
            items(state.providers, key = { it.id.value }) { provider ->
                ProviderCard(
                    provider = provider,
                    onClick = { onOpenProvider(provider.id) },
                )
            }
        }
    }
}

@Composable
private fun ProviderCard(
    provider: ProviderRow,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = DesignTokens.Elevation.card),
    ) {
        Column(
            modifier = Modifier.padding(DesignTokens.Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = provider.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                GatewayStatusBadge(status = provider.status)
            }
            Text(
                text = provider.blurb,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = provider.region,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CapabilityChips(capabilities = provider.capabilities)
        }
    }
}

@Composable
private fun CapabilityChips(
    capabilities: ImmutableList<String>,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs),
    ) {
        capabilities.forEach { label ->
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier =
                        Modifier.padding(
                            horizontal = DesignTokens.Spacing.sm,
                            vertical = DesignTokens.Spacing.xs,
                        ),
                )
            }
        }
    }
}
