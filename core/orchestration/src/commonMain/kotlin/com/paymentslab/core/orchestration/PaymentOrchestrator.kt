package com.paymentslab.core.orchestration

import com.paymentslab.core.common.AppLog
import com.paymentslab.core.common.UiText
import com.paymentslab.core.orchestration.fsm.FsmPollConfig
import com.paymentslab.core.orchestration.fsm.PaymentEffect
import com.paymentslab.core.orchestration.fsm.PaymentEvent
import com.paymentslab.core.orchestration.fsm.PaymentPhase
import com.paymentslab.core.orchestration.fsm.PaymentReducer
import com.paymentslab.core.orchestration.fsm.PaymentState
import com.paymentslab.core.paymentsapi.CreatedOrder
import com.paymentslab.core.paymentsapi.GatewayId
import com.paymentslab.core.paymentsapi.PaymentBackend
import com.paymentslab.core.paymentsapi.PaymentGatewayRegistry
import com.paymentslab.core.paymentsapi.PaymentHost
import com.paymentslab.core.paymentsapi.PaymentResult
import com.paymentslab.core.paymentsapi.PaymentSnapshot
import com.paymentslab.core.paymentsapi.PaymentStatus
import com.paymentslab.core.paymentsapi.PaymentStep
import com.paymentslab.core.paymentsapi.PendingPayment
import com.paymentslab.core.paymentsapi.PendingPaymentJournal
import com.paymentslab.core.paymentsapi.Redactor
import com.paymentslab.core.paymentsapi.VerificationRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * The effectful shell around the pure [PaymentReducer]. The reducer decides *what* to do next; this
 * class *does* it — creating orders, journaling, launching the gateway, verifying and polling — feeds
 * each result back into the reducer as an event, and emits a [PaymentStep] the Lab renders as a
 * live timeline. It holds no branching decisions of its own; that logic is the (fully unit-tested)
 * reducer's job. See `core/orchestration/.../fsm/PaymentFsm.kt`.
 *
 * Invariants (enforced by the reducer + this interpreter):
 *  1. **Journal before launch** — the pending row is written before the SDK opens (process-death insurance).
 *  2. **Server is truth** — a client `Success` is never terminal on its own; it is always confirmed
 *     via [PaymentBackend.verify] (and polled if still `PENDING`) before the payment settles.
 *
 * Every collaborator is an interface, so the whole flow is exercised in `commonTest` with fakes.
 */
class PaymentOrchestrator(
    private val registry: PaymentGatewayRegistry,
    private val backend: PaymentBackend,
    private val journal: PendingPaymentJournal,
    private val pollConfig: PollConfig = PollConfig(),
    private val now: () -> Long = ::systemNowMs,
) {
    fun pay(
        host: PaymentHost,
        gatewayId: GatewayId,
        catalogItemId: String,
    ): Flow<PaymentStep> =
        flow {
            val gateway = registry.byId(gatewayId)
            if (gateway == null) {
                emit(PaymentStep.Errored(UiText.Dynamic("No gateway registered for '${gatewayId.value}'")))
                return@flow
            }

            val fsmPoll = FsmPollConfig(maxAttempts = pollConfig.maxAttempts)
            var transition = PaymentReducer.start(catalogItemId, gatewayId)
            var created: CreatedOrder? = null

            try {
                // Drive the machine: execute the current effect, feed its result back as an event, repeat.
                while (transition.state.phase != PaymentPhase.TERMINAL) {
                    val effect = transition.effects.single()
                    val event =
                        when (effect) {
                            PaymentEffect.CreateOrder -> {
                                val c = backend.createOrder(catalogItemId, gatewayId)
                                created = c
                                emit(
                                    PaymentStep.OrderCreated(
                                        orderId = c.order.orderId,
                                        amount = c.order.amount,
                                        payload =
                                            Redactor.redact(
                                                "order",
                                                c.providerParams + mapOf("order_id" to c.order.orderId),
                                            ),
                                    ),
                                )
                                PaymentEvent.OrderCreated(c.order.orderId)
                            }

                            PaymentEffect.RecordJournalAndLaunch -> {
                                val c = requireNotNull(created)
                                // Journal BEFORE launch — the process-death insurance.
                                journal.record(
                                    PendingPayment(
                                        orderId = c.order.orderId,
                                        catalogItemId = catalogItemId,
                                        gatewayId = gatewayId,
                                        amount = c.order.amount,
                                        createdAtEpochMs = now(),
                                        status = PaymentStatus.CREATED,
                                    ),
                                )
                                emit(PaymentStep.Launching(gatewayId))
                                val prepared = gateway.prepare(c)
                                val result = gateway.pay(host, prepared)
                                emit(PaymentStep.ClientResult(result, result.raw))
                                PaymentEvent.ClientReturned(result)
                            }

                            is PaymentEffect.Verify -> {
                                emit(PaymentStep.Verifying())
                                val snapshot =
                                    backend.verify(
                                        verificationRequest(gatewayId, transition.state.orderId!!, effect.result),
                                    )
                                PaymentEvent.ServerAnswered(snapshot)
                            }

                            PaymentEffect.CheckStatus -> {
                                val orderId = transition.state.orderId!!
                                val snapshot =
                                    if (transition.state.phase == PaymentPhase.POLLING) {
                                        // Polling loop — back off, then ask the server for authoritative state.
                                        delay(backoffDelayMs(transition.state.pollAttempts))
                                        backend.status(orderId)
                                    } else {
                                        // First server consult after a client-reported failure (server can disagree).
                                        emit(PaymentStep.Verifying())
                                        backend.status(orderId)
                                    }
                                PaymentEvent.ServerAnswered(snapshot)
                            }

                            is PaymentEffect.Settle -> error("Settle is terminal and handled after the loop")
                        }
                    transition = PaymentReducer.reduce(transition.state, event, fsmPoll)
                }

                settle(transition.state) { emit(it) }
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                AppLog.e(TAG, "Payment flow failed for order=${transition.state.orderId}", t)
                transition.state.orderId?.let { journal.markResolved(it, PaymentStatus.FAILED, null) }
                emit(PaymentStep.Errored(UiText.of(t.message ?: "Payment failed")))
            }
        }

    /** Run the terminal [PaymentEffect.Settle]: resolve the journal and emit the final step. */
    private suspend fun settle(
        state: PaymentState,
        emit: suspend (PaymentStep) -> Unit,
    ) {
        val status = requireNotNull(state.terminalStatus)
        val orderId = state.orderId
        orderId?.let { journal.markResolved(it, status, state.paymentId) }
        val snapshot = PaymentSnapshot(orderId ?: "", state.paymentId, status)
        emit(
            PaymentStep.Settled(
                status = status,
                snapshot = snapshot,
                payload =
                    Redactor.redact(
                        "settled",
                        mapOf("order_id" to orderId, "status" to status.name, "payment_id" to state.paymentId),
                    ),
            ),
        )
    }

    private fun verificationRequest(
        gatewayId: GatewayId,
        orderId: String,
        result: PaymentResult,
    ): VerificationRequest =
        when (result) {
            is PaymentResult.Success ->
                VerificationRequest(
                    gatewayId = gatewayId,
                    orderId = orderId,
                    paymentId = result.paymentId,
                    signature = result.verification["signature"],
                    extra = result.verification,
                )
            is PaymentResult.Pending ->
                VerificationRequest(gatewayId = gatewayId, orderId = orderId, extra = result.verification)
            else -> VerificationRequest(gatewayId = gatewayId, orderId = orderId)
        }

    private fun backoffDelayMs(attempt: Int): Long {
        var delayMs = pollConfig.initialDelayMs
        repeat((attempt - 1).coerceAtLeast(0)) { delayMs = (delayMs * 2).coerceAtMost(pollConfig.maxDelayMs) }
        return delayMs
    }

    /**
     * Cold-start recovery: for every payment written to the journal but never resolved (app died
     * mid-flight), ask the server what actually happened and settle the row. Called on app launch
     * and by the WorkManager reconciliation worker.
     */
    suspend fun recoverPending(): List<PaymentSnapshot> {
        val recovered = mutableListOf<PaymentSnapshot>()
        for (pending in journal.unresolved()) {
            try {
                val snapshot = backend.status(pending.orderId)
                if (snapshot.status.isTerminal) {
                    journal.markResolved(pending.orderId, snapshot.status, snapshot.paymentId)
                    recovered += snapshot
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                AppLog.w(TAG, "Recovery failed for order=${pending.orderId}", t)
            }
        }
        return recovered
    }

    data class PollConfig(
        val initialDelayMs: Long = 1_000,
        val maxDelayMs: Long = 8_000,
        val maxAttempts: Int = 5,
    )

    private companion object {
        const val TAG = "PaymentOrchestrator"
    }
}

@OptIn(ExperimentalTime::class)
private fun systemNowMs(): Long = Clock.System.now().toEpochMilliseconds()
