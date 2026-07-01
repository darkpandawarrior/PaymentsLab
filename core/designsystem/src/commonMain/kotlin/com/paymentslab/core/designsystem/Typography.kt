package com.paymentslab.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import paymentslab.core.designsystem.generated.resources.Res
import paymentslab.core.designsystem.generated.resources.space_grotesk_bold
import paymentslab.core.designsystem.generated.resources.space_grotesk_medium
import paymentslab.core.designsystem.generated.resources.space_grotesk_regular
import paymentslab.core.designsystem.generated.resources.space_grotesk_semibold

/**
 * Space Grotesk — PaymentsLab's display typeface (OFL-licensed, bundled). Used for headings,
 * screen titles and money amounts; body/label text stays on the system font (Roboto/SF Pro) for
 * small-size readability. Loaded once per theme composition via [rememberDisplayFontFamily] —
 * `Font(Res.font.x)` resolves identically across Android/iOS/desktop.
 */
@Composable
private fun rememberDisplayFontFamily(): FontFamily =
    FontFamily(
        Font(Res.font.space_grotesk_regular, weight = FontWeight.Normal),
        Font(Res.font.space_grotesk_medium, weight = FontWeight.Medium),
        Font(Res.font.space_grotesk_semibold, weight = FontWeight.SemiBold),
        Font(Res.font.space_grotesk_bold, weight = FontWeight.Bold),
    )

/**
 * Builds a Material3 [Typography] with Space Grotesk patched into the display-facing slots
 * (display / headline / titleLarge) and the system default left on body/label slots. Call once
 * per [PaymentsLabTheme] composition.
 */
@Composable
internal fun rememberPaymentsLabTypography(): Typography {
    val display = rememberDisplayFontFamily()
    val base = Typography()
    return base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = display),
        displayMedium = base.displayMedium.copy(fontFamily = display),
        displaySmall = base.displaySmall.copy(fontFamily = display),
        headlineLarge = base.headlineLarge.copy(fontFamily = display),
        headlineMedium = base.headlineMedium.copy(fontFamily = display),
        headlineSmall = base.headlineSmall.copy(fontFamily = display),
        titleLarge = base.titleLarge.copy(fontFamily = display),
    )
}
