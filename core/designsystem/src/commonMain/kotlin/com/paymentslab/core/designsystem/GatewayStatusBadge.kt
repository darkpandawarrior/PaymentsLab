package com.paymentslab.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

/** UI-level gateway availability, mapped to a coloured pill by [GatewayStatusBadge]. */
enum class GatewayStatusUi {
    SANDBOX_READY,
    MOCK_MODE,
    KYC_GATED,
    COMING_SOON,
}

private data class BadgeSpec(val label: String, val color: Color)

private fun GatewayStatusUi.spec(): BadgeSpec =
    when (this) {
        GatewayStatusUi.SANDBOX_READY -> BadgeSpec("Sandbox ready", StatusColors.Success)
        GatewayStatusUi.MOCK_MODE -> BadgeSpec("Mock mode", StatusColors.Info)
        GatewayStatusUi.KYC_GATED -> BadgeSpec("KYC gated", StatusColors.Warning)
        GatewayStatusUi.COMING_SOON -> BadgeSpec("Coming soon", StatusColors.Neutral)
    }

/**
 * A small tinted pill communicating a gateway's availability: a status dot plus a label on a
 * low-alpha fill of the same tone.
 */
@Composable
fun GatewayStatusBadge(
    status: GatewayStatusUi,
    modifier: Modifier = Modifier,
) {
    val spec = status.spec()
    Surface(
        modifier = modifier.shimmer(active = status == GatewayStatusUi.MOCK_MODE),
        color = spec.color.copy(alpha = 0.14f),
        shape = CircleShape,
    ) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = DesignTokens.Spacing.md,
                    vertical = DesignTokens.Spacing.xs,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(DesignTokens.Spacing.sm)
                        .clip(CircleShape)
                        .background(spec.color),
            )
            Text(
                text = spec.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = spec.color,
                modifier = Modifier.padding(start = DesignTokens.Spacing.sm),
            )
        }
    }
}
