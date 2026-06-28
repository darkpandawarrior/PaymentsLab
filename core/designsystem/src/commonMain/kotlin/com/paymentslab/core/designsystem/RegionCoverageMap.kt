package com.paymentslab.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList

/** One region's share of the catalog — [count] drives the tile's relative size in [RegionCoverageMap]. */
@Immutable
data class RegionCount(
    val region: String,
    val count: Int,
)

/**
 * The "N gateways across the globe" hero: a tappable grid of region tiles, each sized by its share
 * of the catalog rather than actual geography — sidesteps hand-authoring accurate world-map SVG
 * paths while still delivering the "coverage at a glance, tap to filter" interaction the catalog
 * needs. Selected regions get a filled accent background; others stay outlined.
 */
@Composable
fun RegionCoverageMap(
    regions: ImmutableList<RegionCount>,
    selectedRegions: Set<String>,
    onToggleRegion: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val maxCount = regions.maxOfOrNull { it.count } ?: 1
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm),
    ) {
        regions.forEach { region ->
            RegionTile(
                region = region,
                weight = region.count.toFloat() / maxCount,
                selected = region.region in selectedRegions,
                onClick = { onToggleRegion(region.region) },
            )
        }
    }
}

@Composable
private fun RegionTile(
    region: RegionCount,
    weight: Float,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background by
        animateColorAsState(
            targetValue =
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.06f + weight * 0.18f)
                },
            animationSpec = tween(DesignTokens.Motion.SHORT_MS),
            label = "regionTileBackground",
        )
    val contentColor =
        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    // Tile "height" (via padding) scales with weight so a region's visual footprint reflects its
    // share of the catalog — the closest honest analog to a choropleth map without real geometry.
    val verticalPadding = (DesignTokens.Spacing.sm.value + weight * 10f).dp

    Column(
        modifier =
            modifier
                .widthIn(min = 84.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(background)
                .border(
                    width = if (selected) 0.dp else 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = MaterialTheme.shapes.medium,
                ).selectable(selected = selected, onClick = onClick)
                .padding(horizontal = DesignTokens.Spacing.md, vertical = verticalPadding),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs),
    ) {
        Text(
            text = region.region,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
        )
        Text(
            text = "${region.count} gateway${if (region.count == 1) "" else "s"}",
            style = MaterialTheme.typography.labelSmall,
            color = contentColor.copy(alpha = 0.8f),
        )
    }
}
