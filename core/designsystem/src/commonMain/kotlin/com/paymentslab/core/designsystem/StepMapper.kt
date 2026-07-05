package com.paymentslab.core.designsystem

import com.paymentslab.core.common.UiText
import com.paymentslab.core.paymentsapi.PaymentResult
import com.paymentslab.core.paymentsapi.PaymentStatus
import com.paymentslab.core.paymentsapi.PaymentStep
import com.paymentslab.core.paymentsapi.RedactedPayload
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/** Resolve a [UiText] to a display string at the UI edge. */
fun UiText.resolve(): String =
    when (this) {
        is UiText.Dynamic -> value
        UiText.Empty -> ""
    }

/** Project a redacted payload's entries into the immutable pairs the timeline renders. */
fun RedactedPayload.rows(): ImmutableList<Pair<String, String>> = entries.toImmutableList()

/** A terminal server status that counts as a positive outcome for timeline colouring. */
fun PaymentStatus.isSettledOk(): Boolean = this == PaymentStatus.SUCCESS || this == PaymentStatus.REFUNDED

data class OrderCreatedCopy(
    val title: String,
    val subtitle: (formattedAmount: String) -> String,
)

data class LaunchingCopy(
    val title: (gatewayId: String) -> String,
    val subtitle: String,
)

data class ClientResultCopy(
    val title: String,
    val successSubtitle: String,
    val failureSubtitle: (code: String) -> String,
    val pendingSubtitle: (reason: String) -> String,
    val cancelledSubtitle: String,
)

data class VerifyingCopy(
    val title: String,
    val subtitle: String,
)

data class SettledCopy(
    val title: String,
    val subtitle: (statusName: String) -> String,
)

data class ErroredCopy(
    val title: String,
    val fallbackSubtitle: String,
)

/**
 * Feature-specific wording for [PaymentStep.toTimelineStep] — every feature runs the identical flow
 * and state logic, only the copy differs (e.g. Lab's "Order created" vs Checkout's "Order confirmed").
 */
data class TimelineCopy(
    val orderCreated: OrderCreatedCopy,
    val launching: LaunchingCopy,
    val clientResult: ClientResultCopy,
    val verifying: VerifyingCopy,
    val settled: SettledCopy,
    val errored: ErroredCopy,
)

/**
 * Map one [PaymentStep] to its [TimelineStep] using [copy]'s wording. In-flight steps (order
 * created, launching, client result, verifying) render DONE — they are complete once the *next*
 * step arrives; the caller marks the trailing step ACTIVE while the flow is still running. Terminal
 * steps carry their own state: [PaymentStep.Settled] is DONE on SUCCESS/REFUNDED and ERROR
 * otherwise; [PaymentStep.Errored] is always ERROR.
 */
fun PaymentStep.toTimelineStep(copy: TimelineCopy): TimelineStep =
    when (this) {
        is PaymentStep.OrderCreated ->
            TimelineStep(
                title = copy.orderCreated.title,
                subtitle = copy.orderCreated.subtitle(amount.format()),
                state = StepState.DONE,
                payload = payload.rows(),
            )

        is PaymentStep.Launching ->
            TimelineStep(
                title = copy.launching.title(gatewayId.value),
                subtitle = copy.launching.subtitle,
                state = StepState.DONE,
                payload = payload.rows(),
            )

        is PaymentStep.ClientResult ->
            TimelineStep(
                title = copy.clientResult.title,
                subtitle = result.clientSubtitle(copy.clientResult),
                state = StepState.DONE,
                payload = payload.rows(),
            )

        is PaymentStep.Verifying ->
            TimelineStep(
                title = copy.verifying.title,
                subtitle = copy.verifying.subtitle,
                state = StepState.DONE,
                payload = payload.rows(),
            )

        is PaymentStep.Settled ->
            TimelineStep(
                title = copy.settled.title,
                subtitle = copy.settled.subtitle(status.name),
                state = if (status.isSettledOk()) StepState.DONE else StepState.ERROR,
                payload = payload.rows(),
            )

        is PaymentStep.Errored ->
            TimelineStep(
                title = copy.errored.title,
                subtitle = message.resolve().ifBlank { copy.errored.fallbackSubtitle },
                state = StepState.ERROR,
                payload = payload.rows(),
            )
    }

private fun PaymentResult.clientSubtitle(copy: ClientResultCopy): String =
    when (this) {
        is PaymentResult.Success -> copy.successSubtitle
        is PaymentResult.Failure -> copy.failureSubtitle(code.name)
        is PaymentResult.Pending -> copy.pendingSubtitle(reason.name)
        is PaymentResult.Cancelled -> copy.cancelledSubtitle
    }
