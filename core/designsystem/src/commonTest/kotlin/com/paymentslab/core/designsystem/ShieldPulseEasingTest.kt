package com.paymentslab.core.designsystem

import com.siddharth.kmp.common.easeInQuart
import com.siddharth.kmp.common.easeOutBack
import com.siddharth.kmp.common.easeOutCubic
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class ShieldPulseEasingTest {
    @Test
    fun easeInQuart_starts_at_zero_and_ends_at_one() {
        assertTrue(abs(easeInQuart(0f) - 0f) < 0.001f)
        assertTrue(abs(easeInQuart(1f) - 1f) < 0.001f)
    }

    @Test
    fun easeOutBack_overshoots_past_one_before_settling() {
        // EaseOutBack's whole point is a small overshoot mid-curve, then it settles at 1.
        val samples = (0..10).map { easeOutBack(it / 10f) }
        assertTrue(samples.max() > 1f, "expected an overshoot above 1.0, got max=${samples.max()}")
        assertTrue(abs(easeOutBack(1f) - 1f) < 0.001f)
    }

    @Test
    fun easeOutCubic_is_monotonically_increasing() {
        val samples = (0..10).map { easeOutCubic(it / 10f) }
        assertTrue(samples.zipWithNext().all { (a, b) -> b >= a })
    }
}
