package com.paymentslab.core.designsystem

import androidx.compose.ui.unit.dp

/**
 * Single source of truth for spacing, corner radii and elevation across PaymentsLab.
 *
 * Every composable in the design system consumes these tokens instead of hard-coded dp
 * values so the whole app shares one visual rhythm. The scale is a 4dp grid.
 */
object DesignTokens {
    /** Spacing scale on a 4dp grid. */
    object Spacing {
        val xs = 4.dp
        val sm = 8.dp
        val md = 12.dp
        val lg = 16.dp
        val xl = 24.dp
        val xxl = 32.dp
    }

    /** Corner-radius scale for cards, pills and containers. */
    object Radius {
        val sm = 8.dp
        val md = 12.dp
        val lg = 16.dp
    }

    /** Elevation scale for raised surfaces. */
    object Elevation {
        val card = 2.dp
    }
}
