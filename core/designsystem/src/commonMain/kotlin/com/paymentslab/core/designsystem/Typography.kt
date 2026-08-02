package com.paymentslab.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

/**
 * Builds a Material3 [Typography] with [displayFontFamily] patched into the display-facing slots
 * (display / headline / titleLarge) and the system default left on body and label slots — small text
 * reads better in the platform font, and a display face is where brand actually registers.
 *
 * Passing null yields stock Material typography. That is a real supported state, not a fallback for
 * a broken build: only shells that can supply a font do, and the design system does not require one.
 *
 * ### Why the font is a parameter and not owned here
 *
 * This module used to carry Space Grotesk in its own `composeResources` and load it via
 * `Font(Res.font.…)`. That contradicted its own contract — a brand-neutral design system that ships
 * a brand typeface — and it broke on Compose Multiplatform beta02 and beta03, where the generated
 * `Res` class for a KMP *library* targeting android + wasmJs never lands on the androidMain
 * compilation source path. Holding the whole family on beta01 to keep a font inside a library was
 * the wrong trade. The font now comes from whoever composes the app.
 */
@Composable
internal fun rememberPaymentsLabTypography(displayFontFamily: FontFamily?): Typography {
    val base = Typography()
    if (displayFontFamily == null) return base
    return base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = displayFontFamily),
        displayMedium = base.displayMedium.copy(fontFamily = displayFontFamily),
        displaySmall = base.displaySmall.copy(fontFamily = displayFontFamily),
        headlineLarge = base.headlineLarge.copy(fontFamily = displayFontFamily),
        headlineMedium = base.headlineMedium.copy(fontFamily = displayFontFamily),
        headlineSmall = base.headlineSmall.copy(fontFamily = displayFontFamily),
        titleLarge = base.titleLarge.copy(fontFamily = displayFontFamily),
    )
}
