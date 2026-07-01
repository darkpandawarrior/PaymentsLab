package com.paymentslab.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

// ─────────────────────────── Easing helpers (ported from Kursi's Primitives.kt) ────────────────

internal fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t.coerceIn(0f, 1f)

/** EaseInQuart — sharp initial acceleration. */
internal fun easeInQuart(t: Float): Float = t * t * t * t

/** EaseOutCubic — smooth deceleration. */
internal fun easeOutCubic(t: Float): Float {
    val c = t - 1f
    return 1f + c * c * c
}

/** EaseOutBack — slight overshoot settle. s=1.70158, same constant Kursi uses. */
internal fun easeOutBack(t: Float): Float {
    val s = 1.70158f
    val c = t - 1f
    return c * c * ((s + 1f) * c + s) + 1f
}

/**
 * A brief shield icon draw-in-and-settle, played once on mount. Visual reassurance that a
 * payment-bearing screen is protected — it does not itself do anything security-relevant (that's
 * `core:security`'s `SecureScreen`, an Android-only `FLAG_SECURE` mechanism this composable knows
 * nothing about); the two are used together on Android screens, and this one alone on iOS.
 *
 * Timeline for internal progress 0->1 (mirrors Kursi's RubberStamp phase split):
 *   Phase A (0.00-0.55): icon descends from 1.6x scale, 0->1 alpha, via EaseInQuart.
 *   Phase B (0.55-0.78): overshoot to 0.92x (the "press").
 *   Phase C (0.78-1.00): settle to 1.0x via EaseOutBack.
 */
@Composable
fun ShieldPulse(modifier: Modifier = Modifier) {
    val reducedMotion = LocalReducedMotion.current
    val progress = remember { Animatable(if (reducedMotion) 1f else 0f) }

    LaunchedEffect(Unit) {
        if (!reducedMotion) {
            progress.animateTo(1f, tween(DesignTokens.Motion.MEDIUM_MS + 200, easing = LinearEasing))
        }
    }

    val p = progress.value
    val scale =
        when {
            p < 0.55f -> lerp(1.6f, 1.0f, easeInQuart(p / 0.55f))
            p < 0.78f -> lerp(1.0f, 0.92f, (p - 0.55f) / 0.23f)
            else -> lerp(0.92f, 1.0f, easeOutBack((p - 0.78f) / 0.22f))
        }
    val alpha = (p / 0.4f).coerceAtMost(1f)

    Icon(
        imageVector = Icons.Filled.Shield,
        contentDescription = "This screen is protected",
        tint = MaterialTheme.colorScheme.secondary,
        modifier =
            modifier
                .size(20.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                },
    )
}
