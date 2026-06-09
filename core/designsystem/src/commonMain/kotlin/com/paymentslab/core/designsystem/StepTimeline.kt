package com.paymentslab.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList

/** The lifecycle state of a single timeline node. */
enum class StepState { PENDING, ACTIVE, DONE, ERROR }

/**
 * One node in a [StepTimeline]: a titled step with an optional subtitle, its [state], and an
 * ordered [payload] of key/value rows revealed beneath the step.
 */
@Immutable
data class TimelineStep(
    val title: String,
    val subtitle: String?,
    val state: StepState,
    val payload: ImmutableList<Pair<String, String>>,
)

/**
 * A vertical, connected timeline — the app's centerpiece. Each step renders a coloured node
 * joined by a rail to the next; ACTIVE pulses in the primary colour, DONE shows a check in the
 * success tone, ERROR shows a cross in the danger tone, PENDING is muted. Any step's payload
 * key/value rows render inline beneath its title.
 */
@Composable
fun StepTimeline(
    steps: ImmutableList<TimelineStep>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        steps.forEachIndexed { index, step ->
            StepTimelineRow(
                step = step,
                isFirst = index == 0,
                isLast = index == steps.lastIndex,
            )
        }
    }
}

@Composable
private fun StepTimelineRow(
    step: TimelineStep,
    isFirst: Boolean,
    isLast: Boolean,
) {
    val nodeColor = step.state.nodeColor()
    val railColor = MaterialTheme.colorScheme.outlineVariant

    Row(modifier = Modifier.fillMaxWidth()) {
        // Rail + node gutter.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(NodeGutterWidth),
        ) {
            Box(
                modifier =
                    Modifier
                        .width(RailWidth)
                        .height(DesignTokens.Spacing.sm)
                        .background(if (isFirst) Color.Transparent else railColor),
            )
            StepNode(state = step.state, color = nodeColor)
            Box(
                modifier =
                    Modifier
                        .width(RailWidth)
                        .weight(1f)
                        .background(if (isLast) Color.Transparent else railColor),
            )
        }

        Spacer(Modifier.width(DesignTokens.Spacing.md))

        // Step content.
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(bottom = if (isLast) 0.dp else DesignTokens.Spacing.lg),
        ) {
            Text(
                text = step.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color =
                    if (step.state == StepState.PENDING) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
            )
            if (step.subtitle != null) {
                Text(
                    text = step.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = DesignTokens.Spacing.xs),
                )
            }
            if (step.payload.isNotEmpty()) {
                StepPayload(
                    entries = step.payload,
                    accent = nodeColor,
                    modifier = Modifier.padding(top = DesignTokens.Spacing.sm),
                )
            }
        }
    }
}

@Composable
private fun StepNode(
    state: StepState,
    color: Color,
) {
    val animatedColor by animateColorAsState(color, label = "nodeColor")

    Box(contentAlignment = Alignment.Center) {
        if (state == StepState.ACTIVE) {
            // Pulsing halo behind the active node.
            val transition = rememberInfiniteTransition(label = "activePulse")
            val pulse by transition.animateFloat(
                initialValue = 0.9f,
                targetValue = 1.7f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(durationMillis = 1100),
                        repeatMode = RepeatMode.Reverse,
                    ),
                label = "pulseScale",
            )
            Box(
                modifier =
                    Modifier
                        .size(NodeSize)
                        .scale(pulse)
                        .clip(CircleShape)
                        .background(animatedColor.copy(alpha = 0.22f)),
            )
        }
        Box(
            modifier =
                Modifier
                    .size(NodeSize)
                    .clip(CircleShape)
                    .background(animatedColor),
            contentAlignment = Alignment.Center,
        ) {
            when (state) {
                StepState.DONE ->
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(NodeIconSize),
                    )
                StepState.ERROR ->
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(NodeIconSize),
                    )
                StepState.ACTIVE ->
                    Box(
                        modifier =
                            Modifier
                                .size(NodeInnerDotSize)
                                .clip(CircleShape)
                                .background(Color.White),
                    )
                StepState.PENDING -> Unit
            }
        }
    }
}

@Composable
private fun StepPayload(
    entries: ImmutableList<Pair<String, String>>,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(DesignTokens.Radius.sm))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(DesignTokens.Spacing.md),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs),
    ) {
        entries.forEach { (key, value) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.md),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = key,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(2f),
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(3f),
                )
            }
        }
    }
}

@Composable
private fun StepState.nodeColor(): Color =
    when (this) {
        StepState.PENDING -> MaterialTheme.colorScheme.surfaceVariant
        StepState.ACTIVE -> MaterialTheme.colorScheme.primary
        StepState.DONE -> StatusColors.Success
        StepState.ERROR -> StatusColors.Danger
    }

private val NodeGutterWidth = 24.dp
private val NodeSize = 20.dp
private val NodeInnerDotSize = 8.dp
private val NodeIconSize = 14.dp
private val RailWidth = 2.dp
