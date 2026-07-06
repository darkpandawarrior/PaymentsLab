package com.paymentslab.core.orchestration

import com.paymentslab.core.paymentsapi.DefaultPaymentGatewayRegistry
import com.paymentslab.core.paymentsapi.GatewayId
import com.paymentslab.core.paymentsapi.Money
import com.paymentslab.core.paymentsapi.PaymentResult
import com.paymentslab.core.paymentsapi.PaymentStatus
import com.paymentslab.core.paymentsapi.PaymentStep
import com.paymentslab.core.paymentsapi.PendingPayment
import com.paymentslab.core.paymentsapi.PendingReason
import com.paymentslab.core.paymentsapi.RedactedPayload
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PaymentOrchestratorTest {
    private val gid = GatewayId("fake")

    private fun orchestrator(
        result: PaymentResult = success(),
        backend: FakeBackend = FakeBackend(),
        log: InteractionLog? = null,
    ): Pair<PaymentOrchestrator, FakeJournal> {
        // One shared log threads through gateway + journal so ordering assertions see both sides.
        val gateway = FakeGateway(gid, result = result, log = log)
        val journal = FakeJournal(log)
        val orchestrator =
            PaymentOrchestrator(
                registry = DefaultPaymentGatewayRegistry(listOf(gateway)),
                backend = backend,
                journal = journal,
                pollConfig = PaymentOrchestrator.PollConfig(initialDelayMs = 10, maxDelayMs = 40, maxAttempts = 5),
                now = { 1_000L },
            )
        return orchestrator to journal
    }

    @Test
    fun happyPath_emitsFullTimeline_andSettlesSuccess() =
        runTest {
            val (orchestrator, journal) = orchestrator()
            val steps = orchestrator.pay(NoopHost, gid, "item_1", "idem_test").toList()

            assertTrue(steps[0] is PaymentStep.OrderCreated)
            assertTrue(steps[1] is PaymentStep.Launching)
            assertTrue(steps[2] is PaymentStep.ClientResult)
            assertTrue(steps[3] is PaymentStep.Verifying)
            val settled = steps[4] as PaymentStep.Settled
            assertEquals(PaymentStatus.SUCCESS, settled.status)
            assertEquals(PaymentStatus.SUCCESS, journal.recorded.single().status)
        }

    @Test
    fun journalIsWrittenBeforeGatewayLaunch() =
        runTest {
            val log = InteractionLog()
            val (orchestrator, _) = orchestrator(log = log)
            orchestrator.pay(NoopHost, gid, "item_1", "idem_test").toList()

            val recordIdx = log.events.indexOf("journal.record")
            val payIdx = log.events.indexOf("pay")
            assertTrue(recordIdx in 0 until payIdx, "journal.record must precede pay: ${log.events}")
        }

    @Test
    fun serverVerdictOverridesClientSuccess() =
        runTest {
            // Client says success, but the server verify says FAILED — server wins.
            val backend = FakeBackend(verifyStatus = PaymentStatus.FAILED)
            val (orchestrator, journal) = orchestrator(result = success(), backend = backend)
            val steps = orchestrator.pay(NoopHost, gid, "item_1", "idem_test").toList()

            val settled = steps.last() as PaymentStep.Settled
            assertEquals(PaymentStatus.FAILED, settled.status)
            assertEquals(PaymentStatus.FAILED, journal.recorded.single().status)
        }

    @Test
    fun pendingResult_pollsUntilTerminal() =
        runTest {
            val backend =
                FakeBackend(
                    verifyStatus = PaymentStatus.PENDING,
                    statusSequence = listOf(PaymentStatus.PENDING, PaymentStatus.PENDING, PaymentStatus.SUCCESS),
                )
            val pending = PaymentResult.Pending(reason = PendingReason.UPI_SUBMITTED, raw = RedactedPayload.EMPTY)
            val (orchestrator, _) = orchestrator(result = pending, backend = backend)
            val settled = orchestrator.pay(NoopHost, gid, "item_1", "idem_test").toList().last() as PaymentStep.Settled
            assertEquals(PaymentStatus.SUCCESS, settled.status)
        }

    @Test
    fun cancelled_settlesWithoutVerifying() =
        runTest {
            val backend = FakeBackend()
            val (orchestrator, journal) = orchestrator(result = PaymentResult.Cancelled(), backend = backend)
            val settled = orchestrator.pay(NoopHost, gid, "item_1", "idem_test").toList().last() as PaymentStep.Settled

            assertEquals(PaymentStatus.CANCELLED, settled.status)
            assertEquals(0, backend.verifyCalls, "cancelled payment must not hit verify")
            assertEquals(PaymentStatus.CANCELLED, journal.recorded.single().status)
        }

    @Test
    fun createOrder_forwardsTheCallersIdempotencyKeyVerbatim() =
        runTest {
            // The caller owns the key; the orchestrator must pass it through unchanged so a retried
            // attempt (same key from the caller) dedups server-side.
            val backend = FakeBackend()
            val (orchestrator, _) = orchestrator(backend = backend)
            orchestrator.pay(NoopHost, gid, "item_1", "idem_abc").toList()

            assertEquals(listOf("idem_abc"), backend.idempotencyKeysSeen)
        }

    @Test
    fun unknownGateway_emitsErrored() =
        runTest {
            val (orchestrator, _) = orchestrator()
            val steps = orchestrator.pay(NoopHost, GatewayId("nope"), "item_1", "idem_test").toList()
            assertTrue(steps.single() is PaymentStep.Errored)
        }

    @Test
    fun recoverPending_resolvesUnresolvedAgainstServer() =
        runTest {
            val journal = FakeJournal()
            val backend = FakeBackend(statusSequence = listOf(PaymentStatus.SUCCESS))
            // Simulate a crash: a payment recorded but never resolved.
            journal.record(
                PendingPayment(
                    orderId = "order_1",
                    catalogItemId = "item_1",
                    gatewayId = gid,
                    amount = Money.inr(499),
                    createdAtEpochMs = 1_000L,
                    status = PaymentStatus.CREATED,
                ),
            )
            val orchestrator =
                PaymentOrchestrator(
                    registry = DefaultPaymentGatewayRegistry(listOf(FakeGateway(gid, result = success()))),
                    backend = backend,
                    journal = journal,
                )

            val recovered = orchestrator.recoverPending()
            assertEquals(1, recovered.size)
            assertEquals(PaymentStatus.SUCCESS, journal.recorded.single().status)
        }
}
