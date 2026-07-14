package com.paymentslab.core.orchestration.fsm

import com.paymentslab.core.paymentsapi.GatewayId
import com.paymentslab.core.paymentsapi.PaymentResult
import com.paymentslab.core.paymentsapi.PaymentSnapshot
import com.paymentslab.core.paymentsapi.PaymentStatus
import com.paymentslab.core.paymentsapi.PendingReason
import com.paymentslab.core.paymentsapi.RedactedPayload
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Exhaustive, synchronous tests of the pure decision machine — no coroutines, no fakes, no I/O.
 * These pin the *decisions* independently of how the orchestrator performs the effects.
 */
class PaymentReducerTest {
    private val gid = GatewayId("fake")

    private fun start() = PaymentReducer.start("item_1", gid).state.copy(orderId = "order_1")

    private fun success() = PaymentResult.Success("pay_1", mapOf("signature" to "abc"), RedactedPayload.EMPTY)

    @Test
    fun start_createsOrder() {
        val t = PaymentReducer.start("item_1", gid)
        assertEquals(PaymentPhase.STARTING, t.state.phase)
        assertEquals(listOf(PaymentEffect.CreateOrder), t.effects)
    }

    @Test
    fun orderCreated_recordsJournalAndLaunches() {
        val t = PaymentReducer.reduce(start(), PaymentEvent.OrderCreated("order_1"))
        assertEquals(PaymentPhase.LAUNCHING, t.state.phase)
        assertEquals("order_1", t.state.orderId)
        assertEquals(listOf(PaymentEffect.RecordJournalAndLaunch), t.effects)
    }

    @Test
    fun clientSuccess_verifies() {
        val t =
            PaymentReducer.reduce(
                start().copy(phase = PaymentPhase.LAUNCHING),
                PaymentEvent.ClientReturned(success()),
            )
        assertEquals(PaymentPhase.VERIFYING, t.state.phase)
        assertEquals("pay_1", t.state.paymentId)
        assertTrue(t.effects.single() is PaymentEffect.Verify)
    }

    @Test
    fun clientCancelled_settlesWithoutServer() {
        val t = PaymentReducer.reduce(start(), PaymentEvent.ClientReturned(PaymentResult.Cancelled()))
        assertEquals(PaymentPhase.TERMINAL, t.state.phase)
        assertEquals(PaymentEffect.Settle(PaymentStatus.CANCELLED), t.effects.single())
    }

    @Test
    fun clientFailure_stillChecksServer() {
        val failure =
            PaymentResult.Failure(
                com.paymentslab.core.paymentsapi.FailureCode.GATEWAY_DECLINED,
                com.siddharth.kmp.common.UiText.Empty,
                RedactedPayload.EMPTY,
            )
        val t = PaymentReducer.reduce(start(), PaymentEvent.ClientReturned(failure))
        assertEquals(PaymentPhase.VERIFYING, t.state.phase)
        assertEquals(PaymentEffect.CheckStatus, t.effects.single())
    }

    @Test
    fun serverTerminal_settlesWithThatStatus() {
        val verifying = start().copy(phase = PaymentPhase.VERIFYING)
        val t =
            PaymentReducer.reduce(
                verifying,
                PaymentEvent.ServerAnswered(PaymentSnapshot("order_1", "pay_1", PaymentStatus.SUCCESS)),
            )
        assertEquals(PaymentPhase.TERMINAL, t.state.phase)
        assertEquals(PaymentEffect.Settle(PaymentStatus.SUCCESS), t.effects.single())
        assertEquals("pay_1", t.state.paymentId)
    }

    @Test
    fun serverPending_pollsUntilAttemptsExhausted() {
        val cfg = FsmPollConfig(maxAttempts = 3)
        var state = start().copy(phase = PaymentPhase.VERIFYING)
        val pending = PaymentSnapshot("order_1", null, PaymentStatus.PENDING)

        // 1st pending → poll (attempt 1)
        var t = PaymentReducer.reduce(state, PaymentEvent.ServerAnswered(pending), cfg)
        assertEquals(PaymentPhase.POLLING, t.state.phase)
        assertEquals(1, t.state.pollAttempts)
        state = t.state

        // 2nd pending → poll (attempt 2)
        t = PaymentReducer.reduce(state, PaymentEvent.ServerAnswered(pending), cfg)
        assertEquals(PaymentPhase.POLLING, t.state.phase)
        assertEquals(2, t.state.pollAttempts)
        state = t.state

        // 3rd pending → attempts exhausted (2+1 >= 3) → settle as-is (still PENDING)
        t = PaymentReducer.reduce(state, PaymentEvent.ServerAnswered(pending), cfg)
        assertEquals(PaymentPhase.TERMINAL, t.state.phase)
        assertEquals(PaymentEffect.Settle(PaymentStatus.PENDING), t.effects.single())
    }

    @Test
    fun serverPendingThenSuccess_settlesSuccess() {
        val cfg = FsmPollConfig(maxAttempts = 5)
        val verifying = start().copy(phase = PaymentPhase.VERIFYING)
        val afterPending =
            PaymentReducer.reduce(
                verifying,
                PaymentEvent.ServerAnswered(PaymentSnapshot("order_1", null, PaymentStatus.PENDING)),
                cfg,
            )
        val settled =
            PaymentReducer.reduce(
                afterPending.state,
                PaymentEvent.ServerAnswered(PaymentSnapshot("order_1", "pay_9", PaymentStatus.SUCCESS)),
                cfg,
            )
        assertEquals(PaymentEffect.Settle(PaymentStatus.SUCCESS), settled.effects.single())
    }

    @Test
    fun anyErrorEvent_settlesFailed() {
        val t = PaymentReducer.reduce(start().copy(phase = PaymentPhase.VERIFYING), PaymentEvent.Errored("boom"))
        assertEquals(PaymentEffect.Settle(PaymentStatus.FAILED), t.effects.single())
    }

    @Suppress("unused")
    private fun pending() = PaymentResult.Pending(PendingReason.UPI_SUBMITTED)
}
