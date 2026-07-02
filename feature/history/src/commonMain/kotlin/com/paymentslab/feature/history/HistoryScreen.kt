package com.paymentslab.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paymentslab.core.designsystem.DesignTokens
import com.paymentslab.core.designsystem.LabScaffold
import com.paymentslab.core.paymentsapi.PaymentStatus
import org.koin.compose.viewmodel.koinViewModel

/** Stateful entry point: resolves the ViewModel and renders the stateless [HistoryScreen]. */
@Composable
fun HistoryRoot(
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(state = state, onBack = onBack, modifier = modifier)
}

/** Stateless payment history: a list of rows with a status chip, or an empty state. */
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    LabScaffold(title = "History", onBack = onBack) { padding ->
        if (!state.isLoading && state.rows.isEmpty()) {
            EmptyState(
                modifier =
                    modifier
                        .fillMaxSize()
                        .padding(padding),
            )
        } else {
            LazyColumn(
                modifier =
                    modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = DesignTokens.Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.md),
            ) {
                items(state.rows, key = { it.orderId }) { row ->
                    HistoryCard(row = row)
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(
    row: HistoryRow,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
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
                    text = row.catalogItemId,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                StatusChip(status = row.status)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${row.gatewayId} · ${row.orderId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = row.amount,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun StatusChip(
    status: PaymentStatus,
    modifier: Modifier = Modifier,
) {
    val color = statusTone(status)
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.14f),
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = status.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier =
                Modifier.padding(
                    horizontal = DesignTokens.Spacing.md,
                    vertical = DesignTokens.Spacing.xs,
                ),
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm),
            modifier = Modifier.padding(DesignTokens.Spacing.xl),
        ) {
            Text(
                text = "No payments yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Run a payment in the Integration Lab and it will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Semantic tone per status, drawn from the active theme rather than the design system's internal
 * [com.paymentslab.core.designsystem] palette (which is not part of the public API).
 */
@Composable
private fun statusTone(status: PaymentStatus): Color =
    when (status) {
        PaymentStatus.SUCCESS, PaymentStatus.REFUNDED -> Color(0xFF1E9E6A)
        PaymentStatus.FAILED, PaymentStatus.CANCELLED -> MaterialTheme.colorScheme.error
        PaymentStatus.PENDING -> Color(0xFFB57900)
        PaymentStatus.CREATED -> MaterialTheme.colorScheme.onSurfaceVariant
    }
