package com.paymentslab.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/** The four hops every payment makes — the teaching diagram's fixed spine. */
enum class FlowHop { APP, GATEWAY, BACKEND, WEBHOOK }

/**
 * An animated packet travels App → Gateway → Backend → Webhook, colour-coded by whether the backend
 * has actually confirmed the payment yet. This is the app's #1 lesson made visual: a client `Success`
 * (packet reaching GATEWAY) is only a hint — the packet stays the "unverified" colour until
 * [verified] is true, which only happens once the flow has actually reached BACKEND/WEBHOOK.
 */
@Composable
fun PaymentFlowDiagram(
    activeHop: FlowHop,
    verified: Boolean,
    modifier: Modifier = Modifier,
) {
    val hops = FlowHop.entries
    val targetIndex = hops.indexOf(activeHop)
    val progress = remember { Animatable(targetIndex.toFloat()) }
    val reducedMotion = LocalReducedMotion.current

    LaunchedEffect(activeHop) {
        if (reducedMotion) {
            progress.snapTo(targetIndex.toFloat())
        } else {
            progress.animateTo(
                targetIndex.toFloat(),
                tween(DesignTokens.Motion.MEDIUM_MS, easing = DesignTokens.Motion.standardEasing),
            )
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            hops.forEach { hop -> FlowNode(hop, isActiveOrPast = hops.indexOf(hop) <= targetIndex) }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().height(RailHeight)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(RailThickness)
                    .align(Alignment.CenterStart)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            val fraction = (progress.value / (hops.size - 1)).coerceIn(0f, 1f)
            val packetColor = if (verified) StatusColors.Success else MaterialTheme.colorScheme.primary
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (maxWidth - PacketSize) * fraction)
                    .size(PacketSize)
                    .clip(CircleShape)
                    .background(packetColor),
            )
        }
        if (activeHop == FlowHop.GATEWAY && !verified) {
            Text(
                text = "Client says success — the backend hasn't verified yet",
                style = MaterialTheme.typography.labelSmall,
                color = StatusColors.Warning,
                modifier = Modifier.padding(top = DesignTokens.Spacing.xs),
            )
        }
    }
}

@Composable
private fun FlowNode(
    hop: FlowHop,
    isActiveOrPast: Boolean,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(NodeDotSize)
                .clip(CircleShape)
                .background(
                    if (isActiveOrPast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                ),
        )
        Text(
            text = hop.name.lowercase().replaceFirstChar(Char::uppercase),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = DesignTokens.Spacing.xs),
        )
    }
}

private val RailHeight = 24.dp
private val RailThickness = 3.dp
private val PacketSize = 14.dp
private val NodeDotSize = 10.dp
