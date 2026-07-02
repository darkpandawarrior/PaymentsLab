package com.paymentslab.feature.checkoutdemo

import com.paymentslab.core.common.UiText
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

internal fun RedactedPayload.rows(): ImmutableList<Pair<String, String>> = entries.toImmutableList()

/**
 * Map a [PaymentStep] to a friendly, checkout-flavoured [TimelineStep] — the "what's happening"
 * explainer beneath the checkout summary. Same step semantics as the Lab; the copy is gentler.
 */
internal fun PaymentStep.toTimelineStep(): TimelineStep =
    when (this) {
        is PaymentStep.OrderCreated ->
            TimelineStep(
                title = "Order confirmed",
                subtitle = "We asked the server for the price — ${amount.format()}",
                state = StepState.DONE,
                payload = payload.rows(),
            )

        is PaymentStep.Launching ->
            TimelineStep(
                title = "Opening ${gatewayId.value}",
                subtitle = "Saved a recovery note before opening the payment sheet",
                state = StepState.DONE,
                payload = payload.rows(),
            )

        is PaymentStep.ClientResult ->
            TimelineStep(
                title = "Payment sheet returned",
                subtitle = result.clientSubtitle(),
                state = StepState.DONE,
                payload = payload.rows(),
            )

        is PaymentStep.Verifying ->
            TimelineStep(
                title = "Double-checking with the server",
                subtitle = "A success on the phone is only trusted once the server agrees",
                state = StepState.DONE,
                payload = payload.rows(),
            )

        is PaymentStep.Settled ->
            TimelineStep(
                title = "Done",
                subtitle = "Server says: ${status.name}",
                state = if (status.isSettledOk()) StepState.DONE else StepState.ERROR,
                payload = payload.rows(),
            )

        is PaymentStep.Errored ->
            TimelineStep(
                title = "Something went wrong",
                subtitle = message.resolve().ifBlank { "The checkout could not complete" },
                state = StepState.ERROR,
                payload = payload.rows(),
            )
    }

internal fun PaymentStatus.isSettledOk(): Boolean = this == PaymentStatus.SUCCESS || this == PaymentStatus.REFUNDED

private fun PaymentResult.clientSubtitle(): String =
    when (this) {
        is PaymentResult.Success -> "Reported success (not yet verified)"
        is PaymentResult.Failure -> "Reported failure: ${code.name}"
        is PaymentResult.Pending -> "Reported pending: ${reason.name}"
        is PaymentResult.Cancelled -> "You cancelled the payment"
    }
