package com.paymentslab.feature.lab

import com.paymentslab.core.common.UiText
import com.paymentslab.core.designsystem.FlowHop
import com.paymentslab.core.designsystem.StepState
import com.paymentslab.core.designsystem.TimelineStep
import com.paymentslab.core.paymentsapi.PaymentResult
import com.paymentslab.core.paymentsapi.PaymentStatus
import com.paymentslab.core.paymentsapi.PaymentStep
import com.paymentslab.core.paymentsapi.RedactedPayload
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/** Resolve a [UiText] to a display string at the UI edge. */
internal fun UiText.resolve(): String =
    when (this) {
        is UiText.Dynamic -> value
        UiText.Empty -> ""
    }

/** Project a redacted payload's entries into the immutable pairs the timeline renders. */
internal fun RedactedPayload.rows(): ImmutableList<Pair<String, String>> = entries.toImmutableList()

/**
 * Map one [PaymentStep] to its [TimelineStep]. In-flight steps (order created, launching, client
 * result, verifying) render DONE — they are complete once the *next* step arrives; the caller marks
 * the trailing step ACTIVE while the flow is still running. Terminal steps carry their own state:
 * [PaymentStep.Settled] is DONE on SUCCESS/REFUNDED and ERROR otherwise; [PaymentStep.Errored] is
 * always ERROR.
 */
internal fun PaymentStep.toTimelineStep(): TimelineStep =
    when (this) {
        is PaymentStep.OrderCreated ->
            TimelineStep(
                title = "Order created",
                subtitle = "Server resolved the price · ${amount.format()}",
                state = StepState.DONE,
                payload = payload.rows(),
            )

        is PaymentStep.Launching ->
            TimelineStep(
                title = "Launching ${gatewayId.value}",
                subtitle = "Journal row written before the SDK opens",
                state = StepState.DONE,
                payload = payload.rows(),
            )

        is PaymentStep.ClientResult ->
            TimelineStep(
                title = "Client result",
                subtitle = result.clientSubtitle(),
                state = StepState.DONE,
                payload = payload.rows(),
            )

        is PaymentStep.Verifying ->
            TimelineStep(
                title = "Verifying",
                subtitle = "Confirming against the server — a client success is only a hint",
                state = StepState.DONE,
                payload = payload.rows(),
            )

        is PaymentStep.Settled ->
            TimelineStep(
                title = "Settled",
                subtitle = "Server-authoritative: ${status.name}",
                state = if (status.isSettledOk()) StepState.DONE else StepState.ERROR,
                payload = payload.rows(),
            )

        is PaymentStep.Errored ->
            TimelineStep(
                title = "Error",
                subtitle = message.resolve().ifBlank { "The flow broke before settling" },
                state = StepState.ERROR,
                payload = payload.rows(),
            )
    }

/** A terminal server status that counts as a positive outcome for timeline colouring. */
internal fun PaymentStatus.isSettledOk(): Boolean = this == PaymentStatus.SUCCESS || this == PaymentStatus.REFUNDED

/**
 * Where this step sits on the [PaymentFlowDiagram]'s spine. [PaymentStep.Errored] stays at APP — the
 * flow broke before reaching anywhere authoritative, so there's nothing to point at further along.
 */
internal fun PaymentStep.toFlowHop(): FlowHop =
    when (this) {
        is PaymentStep.OrderCreated -> FlowHop.APP
        is PaymentStep.Launching -> FlowHop.GATEWAY
        is PaymentStep.ClientResult -> FlowHop.GATEWAY
        is PaymentStep.Verifying -> FlowHop.BACKEND
        is PaymentStep.Settled -> FlowHop.BACKEND
        is PaymentStep.Errored -> FlowHop.APP
    }

/** Only [PaymentStep.Settled] means the backend has actually spoken — everything before is a hint. */
internal fun PaymentStep.isVerified(): Boolean = this is PaymentStep.Settled

private fun PaymentResult.clientSubtitle(): String =
    when (this) {
        is PaymentResult.Success -> "SDK reported success (unverified)"
        is PaymentResult.Failure -> "SDK reported failure: ${code.name}"
        is PaymentResult.Pending -> "SDK reported pending: ${reason.name}"
        is PaymentResult.Cancelled -> "User cancelled"
    }
