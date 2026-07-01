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
 *   Phase A (0.00–PHASE_A_END): icon descends from INITIAL_SCALE via EaseInQuart.
 *   Phase B (PHASE_A_END–PHASE_B_END): overshoot to PRESS_SCALE via linear snap (uneasedfor quick impulse).
 *   Phase C (PHASE_B_END–1.00): settle to 1.0x via EaseOutBack.
 *   Alpha reaches full opacity at ALPHA_COMPLETION_PROGRESS (40%), before phase A settles, so the icon
 *   shows up partway through its descent rather than popping in at the very end.
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
            p < PHASE_A_END -> lerp(INITIAL_SCALE, 1.0f, easeInQuart(p / PHASE_A_END))
            p < PHASE_B_END -> lerp(1.0f, PRESS_SCALE, (p - PHASE_A_END) / PHASE_B_DURATION)
            else -> lerp(PRESS_SCALE, 1.0f, easeOutBack((p - PHASE_B_END) / PHASE_C_DURATION))
        }
    val alpha = (p / ALPHA_COMPLETION_PROGRESS).coerceAtMost(1f)

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

// Phase timeline constants for ShieldPulse animation
private const val PHASE_A_END = 0.55f
private const val PHASE_B_END = 0.78f
private const val PHASE_B_DURATION = 0.23f // PHASE_B_END - PHASE_A_END
private const val PHASE_C_DURATION = 0.22f // 1.0f - PHASE_B_END
private const val INITIAL_SCALE = 1.6f
private const val PRESS_SCALE = 0.92f
private const val ALPHA_COMPLETION_PROGRESS = 0.4f
