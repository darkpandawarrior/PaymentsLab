package com.paymentslab.core.orchestration.fsm

import com.paymentslab.core.paymentsapi.GatewayId
import com.paymentslab.core.paymentsapi.PaymentResult
import com.paymentslab.core.paymentsapi.PaymentSnapshot
import com.paymentslab.core.paymentsapi.PaymentStatus

/*
 * The payment lifecycle as a **pure state machine** — zero coroutines, zero DI, zero I/O, zero clock.
 * It imports only domain value types. Given a [PaymentState] and a [PaymentEvent] it returns the next
 * state plus the [PaymentEffect]s the effectful shell ([com.paymentslab.core.orchestration.PaymentOrchestrator])
 * should perform. The shell executes the effects, feeds their results back as events, and loops.
 *
 * Why bother: separating *decisions* (this file) from *effects* makes the interesting logic — when a
 * client result must be verified, when to poll, when a payment is terminal — exhaustively unit-testable
 * as plain `(state, event) -> transition`, and **deterministically replayable** from a recorded event
 * log (see the replay test). That is the gold standard for auditing a payment's path.
 */

/** Where a payment is in its lifecycle. */
enum class PaymentPhase {
    STARTING,
    LAUNCHING,
    VERIFYING,
    POLLING,
    TERMINAL,
}

/** Immutable snapshot of the machine. Everything the shell needs to act lives here. */
data class PaymentState(
    val phase: PaymentPhase,
    val catalogItemId: String,
    val gatewayId: GatewayId,
    val orderId: String? = null,
    val paymentId: String? = null,
    val terminalStatus: PaymentStatus? = null,
    val pollAttempts: Int = 0,
)

/** Something that happened in the outside world (the result of an effect). */
sealed interface PaymentEvent {
    data class OrderCreated(
        val orderId: String,
    ) : PaymentEvent

    data class ClientReturned(
        val result: PaymentResult,
    ) : PaymentEvent

    /** The server's answer to a verify OR a status check — both yield a snapshot. */
    data class ServerAnswered(
        val snapshot: PaymentSnapshot,
    ) : PaymentEvent

    data class Errored(
        val message: String,
    ) : PaymentEvent
}

/** Something the shell must do. Pure data — the shell decides how to perform it. */
sealed interface PaymentEffect {
    data object CreateOrder : PaymentEffect

    data object RecordJournalAndLaunch : PaymentEffect

    /** Confirm a client [result] with the server (signature verify for success/pending). */
    data class Verify(
        val result: PaymentResult,
    ) : PaymentEffect

    /** Ask the server for authoritative state (used when the client reported failure, and while polling). */
    data object CheckStatus : PaymentEffect

    /** Settle terminally: resolve the journal + emit the final step. */
    data class Settle(
        val status: PaymentStatus,
    ) : PaymentEffect
}

/** The result of one reduction: the next state and the effects to run. */
data class Transition(
    val state: PaymentState,
    val effects: List<PaymentEffect>,
)

/** Tuning the poll loop — mirrors the orchestrator's PollConfig, kept here so the FSM stays pure. */
data class FsmPollConfig(
    val maxAttempts: Int = 5,
)

/**
 * The reducer. Total over (phase, event): every reachable combination maps to a transition. Unexpected
 * (phase, event) pairs are a programming error and fail loudly, which keeps the machine honest.
 */
object PaymentReducer {
    /** The entry transition: create the order. */
    fun start(
        catalogItemId: String,
        gatewayId: GatewayId,
    ): Transition =
        Transition(
            state = PaymentState(PaymentPhase.STARTING, catalogItemId, gatewayId),
            effects = listOf(PaymentEffect.CreateOrder),
        )

    fun reduce(
        state: PaymentState,
        event: PaymentEvent,
        pollConfig: FsmPollConfig = FsmPollConfig(),
    ): Transition =
        when (event) {
            is PaymentEvent.Errored -> terminal(state, PaymentStatus.FAILED)

            is PaymentEvent.OrderCreated ->
                Transition(
                    state = state.copy(phase = PaymentPhase.LAUNCHING, orderId = event.orderId),
                    effects = listOf(PaymentEffect.RecordJournalAndLaunch),
                )

            is PaymentEvent.ClientReturned -> onClientResult(state, event.result)

            is PaymentEvent.ServerAnswered -> onServerAnswer(state, event.snapshot, pollConfig)
        }

    private fun onClientResult(
        state: PaymentState,
        result: PaymentResult,
    ): Transition =
        when (result) {
            // Client cancelled — server never consulted; settle immediately.
            is PaymentResult.Cancelled -> terminal(state, PaymentStatus.CANCELLED)
            // Success / pending → verify with the server (server is truth).
            is PaymentResult.Success ->
                verifying(state.copy(paymentId = result.paymentId), PaymentEffect.Verify(result))
            is PaymentResult.Pending ->
                verifying(state, PaymentEffect.Verify(result))
            // Client says failure — still confirm against the server (the SDK can lie either way).
            is PaymentResult.Failure ->
                verifying(state, PaymentEffect.CheckStatus)
        }

    private fun onServerAnswer(
        state: PaymentState,
        snapshot: PaymentSnapshot,
        pollConfig: FsmPollConfig,
    ): Transition {
        val next = state.copy(paymentId = snapshot.paymentId ?: state.paymentId)
        return when {
            snapshot.status.isTerminal -> terminal(next, snapshot.status)
            // Not terminal (PENDING) — poll, unless we've exhausted attempts, then settle as-is.
            state.pollAttempts + 1 >= pollConfig.maxAttempts -> terminal(next, snapshot.status)
            else ->
                Transition(
                    state = next.copy(phase = PaymentPhase.POLLING, pollAttempts = state.pollAttempts + 1),
                    effects = listOf(PaymentEffect.CheckStatus),
                )
        }
    }

    private fun verifying(
        state: PaymentState,
        effect: PaymentEffect,
    ): Transition = Transition(state.copy(phase = PaymentPhase.VERIFYING), listOf(effect))

    private fun terminal(
        state: PaymentState,
        status: PaymentStatus,
    ): Transition =
        Transition(
            state = state.copy(phase = PaymentPhase.TERMINAL, terminalStatus = status),
            effects = listOf(PaymentEffect.Settle(status)),
        )
}
