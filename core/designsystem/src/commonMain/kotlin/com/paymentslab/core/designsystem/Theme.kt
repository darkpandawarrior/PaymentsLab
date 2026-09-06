package com.paymentslab.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * PaymentsLab-KMP visual identity — a modern fintech palette.
 *
 * Primary is a deep indigo/violet ("Ledger Indigo"), the trustworthy anchor colour of the
 * app; secondary is an electric teal ("Settlement Teal") used for interactive accents.
 * Tertiary is a warm amber reserved for gated / attention states. The palette is intentionally
 * calm and credible — no neon, high contrast, AA-clean on both surfaces.
 */
private val LedgerIndigo = Color(0xFF4F46E5)
private val LedgerIndigoLightContainer = Color(0xFFE0E1FF)
private val LedgerIndigoDark = Color(0xFFB9BBFF)
private val LedgerIndigoDarkContainer = Color(0xFF37348F)

private val SettlementTeal = Color(0xFF0E9F8E)
private val SettlementTealContainer = Color(0xFFB9F1E6)
private val SettlementTealDark = Color(0xFF64DDCB)
private val SettlementTealDarkContainer = Color(0xFF00504A)

private val GatedAmber = Color(0xFFB57900)
private val GatedAmberContainer = Color(0xFFFFDEA6)
private val GatedAmberDark = Color(0xFFF7C066)
private val GatedAmberDarkContainer = Color(0xFF5A3D00)

/**
 * The "hero" accent — used sparingly (Home's hero card, the center Pay FAB, success
 * celebrations) on top of the calm indigo/teal everyday palette. Same in light and dark theme;
 * it's an accent moment, not a surface color, so it doesn't need theme-aware variants.
 */
private val HeroVioletStart = Color(0xFF7C3AED)
private val HeroPinkEnd = Color(0xFFEC4899)

/** A left-to-right sweep from [HeroVioletStart] to [HeroPinkEnd] — the hero-moment brush. */
val PaymentsLabHeroGradient: Brush =
    Brush.linearGradient(colors = listOf(HeroVioletStart, HeroPinkEnd))

private val PaymentsLabLightColors =
    lightColorScheme(
        primary = LedgerIndigo,
        onPrimary = Color.White,
        primaryContainer = LedgerIndigoLightContainer,
        onPrimaryContainer = Color(0xFF12106B),
        secondary = SettlementTeal,
        onSecondary = Color.White,
        secondaryContainer = SettlementTealContainer,
        onSecondaryContainer = Color(0xFF00201C),
        tertiary = GatedAmber,
        onTertiary = Color.White,
        tertiaryContainer = GatedAmberContainer,
        onTertiaryContainer = Color(0xFF3A2600),
        background = Color(0xFFFBFAFF),
        onBackground = Color(0xFF1B1B23),
        surface = Color(0xFFFBFAFF),
        onSurface = Color(0xFF1B1B23),
        surfaceVariant = Color(0xFFE4E1EC),
        onSurfaceVariant = Color(0xFF47464F),
        surfaceContainer = Color(0xFFF1EFF7),
        surfaceContainerHigh = Color(0xFFEBE9F3),
        outline = Color(0xFF787680),
        outlineVariant = Color(0xFFC8C5D0),
        error = Color(0xFFBA1A1A),
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
    )

private val PaymentsLabDarkColors =
    darkColorScheme(
        primary = LedgerIndigoDark,
        onPrimary = Color(0xFF1E1B6B),
        primaryContainer = LedgerIndigoDarkContainer,
        onPrimaryContainer = LedgerIndigoLightContainer,
        secondary = SettlementTealDark,
        onSecondary = Color(0xFF003731),
        secondaryContainer = SettlementTealDarkContainer,
        onSecondaryContainer = SettlementTealContainer,
        tertiary = GatedAmberDark,
        onTertiary = Color(0xFF3A2600),
        tertiaryContainer = GatedAmberDarkContainer,
        onTertiaryContainer = GatedAmberContainer,
        background = Color(0xFF121218),
        onBackground = Color(0xFFE4E1E9),
        surface = Color(0xFF121218),
        onSurface = Color(0xFFE4E1E9),
        surfaceVariant = Color(0xFF47464F),
        onSurfaceVariant = Color(0xFFC8C5D0),
        surfaceContainer = Color(0xFF1E1E26),
        surfaceContainerHigh = Color(0xFF292931),
        outline = Color(0xFF928F9A),
        outlineVariant = Color(0xFF47464F),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
    )

private var curatedLogosRegistered = false

/**
 * Root theme for every PaymentsLab-KMP surface. Wraps content in a Material3 theme using the
 * fintech palette, resolving light/dark automatically from the system by default.
 */
@Composable
fun PaymentsLabTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    displayFontFamily: androidx.compose.ui.text.font.FontFamily? = null,
    content: @Composable () -> Unit,
) {
    if (!curatedLogosRegistered) {
        registerCuratedGatewayLogos()
        curatedLogosRegistered = true
    }
    MaterialTheme(
        colorScheme = if (darkTheme) PaymentsLabDarkColors else PaymentsLabLightColors,
        typography = rememberPaymentsLabTypography(displayFontFamily),
        content = content,
    )
}
