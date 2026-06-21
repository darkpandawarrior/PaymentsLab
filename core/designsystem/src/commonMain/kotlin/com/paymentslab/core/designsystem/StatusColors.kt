package com.paymentslab.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * Semantic status tones shared by [StepTimeline] and [GatewayStatusBadge].
 *
 * These are theme-independent so previews and non-composable code can reach them; they are
 * tuned to read AA on both the light and dark PaymentsLab surfaces.
 */
internal object StatusColors {
    val Success = Color(0xFF1E9E6A) // settlement done / sandbox ready
    val Warning = Color(0xFFB57900) // KYC gated / attention
    val Danger = Color(0xFFCE3B3B) // failed / error
    val Neutral = Color(0xFF8A8894) // pending / coming soon
    val Info = Color(0xFF3B6FCE) // mock mode — real code, simulated end-to-end
}
