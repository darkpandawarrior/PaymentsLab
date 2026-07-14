package com.paymentslab.core.orchestration.fsm

import com.siddharth.kmp.paymentsapi.GatewayId
import com.siddharth.kmp.paymentsapi.PaymentResult
import com.siddharth.kmp.paymentsapi.PaymentSnapshot
import com.siddharth.kmp.paymentsapi.PaymentStatus
import com.siddharth.kmp.paymentsapi.RedactedPayload
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Because [PaymentReducer] is pure, a payment's path is fully described by its **event log** and can
 * be replayed deterministically — the auditing property that makes a state-machine core valuable for
 * money movement. This test replays recorded logs and asserts the terminal outcome, and that two
 * replays of the same log are byte-for-byte identical.
 */
class PaymentReplayTest {
    private val gid = GatewayId("razorpay")

    /** Fold an event log through the reducer from the start state to the terminal state. */
    private fun replay(
        events: List<PaymentEvent>,
        cfg: FsmPollConfig = FsmPollConfig(),
    ): PaymentState {
        var transition = PaymentReducer.start("book_499", gid)
        for (event in events) {
            transition = PaymentReducer.reduce(transition.state, event, cfg)
        }
        return transition.state
    }

    @Test
    fun happyPathLog_settlesSuccess() {
        val log =
            listOf(
                PaymentEvent.OrderCreated("order_42"),
                PaymentEvent.ClientReturned(
                    PaymentResult.Success("pay_42", mapOf("signature" to "sig"), RedactedPayload.EMPTY),
                ),
                PaymentEvent.ServerAnswered(PaymentSnapshot("order_42", "pay_42", PaymentStatus.SUCCESS)),
            )
        val end = replay(log)
        assertEquals(PaymentPhase.TERMINAL, end.phase)
        assertEquals(PaymentStatus.SUCCESS, end.terminalStatus)
        assertEquals("pay_42", end.paymentId)
        assertEquals("order_42", end.orderId)
    }

    @Test
    fun upiSubmittedThenWebhookLog_settlesSuccess() {
        // UPI came back SUBMITTED (pending), verify said PENDING, two polls, then a webhook flipped it.
        val log =
            listOf(
                PaymentEvent.OrderCreated("order_upi"),
                PaymentEvent.ClientReturned(
                    PaymentResult.Pending(com.siddharth.kmp.paymentsapi.PendingReason.UPI_SUBMITTED),
                ),
                PaymentEvent.ServerAnswered(PaymentSnapshot("order_upi", null, PaymentStatus.PENDING)),
                PaymentEvent.ServerAnswered(PaymentSnapshot("order_upi", null, PaymentStatus.PENDING)),
                PaymentEvent.ServerAnswered(PaymentSnapshot("order_upi", "cf_1", PaymentStatus.SUCCESS)),
            )
        assertEquals(PaymentStatus.SUCCESS, replay(log).terminalStatus)
    }

    @Test
    fun serverOverridesClientLog_settlesFailed() {
        val log =
            listOf(
                PaymentEvent.OrderCreated("order_x"),
                PaymentEvent.ClientReturned(PaymentResult.Success("pay_x", emptyMap(), RedactedPayload.EMPTY)),
                PaymentEvent.ServerAnswered(PaymentSnapshot("order_x", "pay_x", PaymentStatus.FAILED)),
            )
        assertEquals(PaymentStatus.FAILED, replay(log).terminalStatus)
    }

    @Test
    fun replayIsDeterministic() {
        val log =
            listOf(
                PaymentEvent.OrderCreated("order_1"),
                PaymentEvent.ClientReturned(
                    PaymentResult.Pending(com.siddharth.kmp.paymentsapi.PendingReason.UPI_SUBMITTED),
                ),
                PaymentEvent.ServerAnswered(PaymentSnapshot("order_1", null, PaymentStatus.PENDING)),
                PaymentEvent.ServerAnswered(PaymentSnapshot("order_1", "p", PaymentStatus.SUCCESS)),
            )
        // Same log → identical terminal state, every time. This is the audit guarantee.
        assertEquals(replay(log), replay(log))
    }
}
