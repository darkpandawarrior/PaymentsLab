package com.paymentslab.core.designsystem

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
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

    /**
     * Elevation scale for raised surfaces. [card] is kept for existing call sites (every current
     * `ElevatedCard` in the app); the other three are for the redesign's new depth hierarchy —
     * [floating] for the hero card and FAB, [overlay] for the hosted-webview overlay.
     */
    object Elevation {
        val flat = 0.dp
        val raised = 2.dp
        val card = raised // alias kept for existing call sites; do not remove raised.
        val floating = 6.dp
        val overlay = 12.dp
    }

    /**
     * Durations/easing shared by every animated component (Motion Kit). Centralized so a
     * reduce-motion switch (see [LocalReducedMotion]) is one place, not scattered `tween()` calls.
     */
    object Motion {
        const val SHORT_MS = 150
        const val MEDIUM_MS = 400
        const val LONG_MS = 900
        val standardEasing: Easing = FastOutSlowInEasing
    }
}
