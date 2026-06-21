package com.paymentslab.core.designsystem

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope

/**
 * Accessibility "reduce motion" — every Motion Kit component checks this before animating. A host
 * app wires it to the platform's reduce-motion setting (e.g. `Settings.Global.ANIMATOR_DURATION_SCALE`
 * on Android); off (full motion) by default so previews/tests see the intended design.
 */
val LocalReducedMotion = staticCompositionLocalOf { false }

/**
 * A moving highlight sweep — signals "real integration code, simulated live" on a [GatewayStatusBadge]
 * in `MOCK_MODE`. No-op when [active] is false or [LocalReducedMotion] is set.
 */
@Composable
fun Modifier.shimmer(active: Boolean = true): Modifier {
    if (!active || LocalReducedMotion.current) return this
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateFraction by transition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(1_400, easing = LinearEasing)),
        label = "shimmerTranslate",
    )
    return this.drawWithContent { drawShimmer(translateFraction) }
}

private fun ContentDrawScope.drawShimmer(translateFraction: Float) {
    drawContent()
    val bandWidth = size.width * 0.4f
    val start = translateFraction * size.width
    drawRect(
        brush =
            Brush.linearGradient(
                colors =
                    listOf(
                        Color.White.copy(alpha = 0f),
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0f),
                    ),
                start = Offset(start, 0f),
                end = Offset(start + bandWidth, 0f),
            ),
    )
}
