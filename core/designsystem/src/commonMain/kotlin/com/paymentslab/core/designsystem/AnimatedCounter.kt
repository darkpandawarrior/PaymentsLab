package com.paymentslab.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import kotlin.math.roundToInt

/**
 * Counts up from zero to [target] on first composition — the non-money sibling of
 * [AnimatedAmount], for stats like "62 gateways integrated" or a "94%" success rate.
 * [suffix] is appended verbatim after the number (e.g. "%").
 */
@Composable
fun AnimatedCounter(
    target: Int,
    modifier: Modifier = Modifier,
    suffix: String = "",
    style: TextStyle = MaterialTheme.typography.headlineSmall,
) {
    val animated = remember { Animatable(0f) }
    val reducedMotion = LocalReducedMotion.current
    LaunchedEffect(target) {
        if (reducedMotion) {
            animated.snapTo(target.toFloat())
        } else {
            animated.animateTo(
                target.toFloat(),
                tween(DesignTokens.Motion.MEDIUM_MS, easing = DesignTokens.Motion.standardEasing),
            )
        }
    }
    Text(text = "${animated.value.roundToInt()}$suffix", style = style, modifier = modifier)
}
