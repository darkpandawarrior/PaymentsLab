package com.paymentslab.core.orchestration

import com.paymentslab.core.common.AppLog
import com.paymentslab.core.common.UiText
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
 * The tested heart of the app. Coordinates one payment across four collaborators — registry (which
 * gateway), backend (server truth), the gateway SDK (client hint), and the journal (crash insurance)
 * — and emits a [PaymentStep] stream the Lab renders as a live timeline.
 *
 * Two invariants it exists to enforce:
 *  1. **Journal before launch.** The pending row is written *before* the SDK opens, so a process
 *     death mid-payment is always recoverable.
 *  2. **Server is truth.** A client `Success` is never terminal on its own; it is always confirmed
 *     via [PaymentBackend.verify] (and polled if still `PENDING`) before the payment is settled.
 *
 * All collaborators are interfaces, so the whole flow is exercised in `commonTest` with fakes —
 * no Android, no network, no real SDK.
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

            var orderId: String? = null
            try {
                // 1. Server creates the order (price resolved server-side).
                val created: CreatedOrder = backend.createOrder(catalogItemId, gatewayId)
                orderId = created.order.orderId
                emit(
                    PaymentStep.OrderCreated(
                        orderId = created.order.orderId,
                        amount = created.order.amount,
                        payload =
                            Redactor.redact(
                                "order",
                                created.providerParams + mapOf("order_id" to created.order.orderId),
                            ),
                    ),
                )

                // 2. Journal BEFORE launch — the process-death insurance.
                journal.record(
                    PendingPayment(
                        orderId = created.order.orderId,
                        catalogItemId = catalogItemId,
                        gatewayId = gatewayId,
                        amount = created.order.amount,
                        createdAtEpochMs = now(),
                        status = PaymentStatus.CREATED,
                    ),
                )
                emit(PaymentStep.Launching(gatewayId))

                // 3. Hand off to the provider SDK / UPI chooser; suspend until a client-side result.
                val prepared = gateway.prepare(created)
                val result = gateway.pay(host, prepared)
                emit(PaymentStep.ClientResult(result, result.raw))

                // 4. Reconcile the client hint against the server.
                val settled = reconcile(gatewayId, created.order.orderId, result) { emit(it) }
                journal.markResolved(created.order.orderId, settled.status, settled.paymentId)
                emit(
                    PaymentStep.Settled(
                        status = settled.status,
                        snapshot = settled,
                        payload =
                            Redactor.redact(
                                "settled",
                                mapOf(
                                    "order_id" to settled.orderId,
                                    "status" to settled.status.name,
                                    "payment_id" to settled.paymentId,
                                ),
                            ),
                    ),
                )
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                AppLog.e(TAG, "Payment flow failed for order=$orderId", t)
                orderId?.let { journal.markResolved(it, PaymentStatus.FAILED, null) }
                emit(PaymentStep.Errored(UiText.of(t.message ?: "Payment failed")))
            }
        }

    /**
     * Turn a client-side [PaymentResult] into a server-authoritative [PaymentSnapshot].
     * Cancellations short-circuit; success/pending go through verification and (if still pending)
     * backoff polling; a client failure is confirmed against the server too (the SDK can lie either way).
     */
    private suspend fun reconcile(
        gatewayId: GatewayId,
        orderId: String,
        result: PaymentResult,
        emit: suspend (PaymentStep) -> Unit,
    ): PaymentSnapshot {
        if (result is PaymentResult.Cancelled) {
            return PaymentSnapshot(orderId, paymentId = null, status = PaymentStatus.CANCELLED)
        }

        emit(PaymentStep.Verifying())
        val verified =
            when (result) {
                is PaymentResult.Success ->
                    backend.verify(
                        VerificationRequest(
                            gatewayId = gatewayId,
                            orderId = orderId,
                            paymentId = result.paymentId,
                            signature = result.verification["signature"],
                            extra = result.verification,
                        ),
                    )
                is PaymentResult.Pending ->
                    backend.verify(
                        VerificationRequest(gatewayId = gatewayId, orderId = orderId, extra = result.verification),
                    )
                is PaymentResult.Failure -> backend.status(orderId)
                is PaymentResult.Cancelled -> error("unreachable")
            }

        return if (verified.status == PaymentStatus.PENDING) pollUntilTerminal(orderId, verified) else verified
    }

    /** Poll `GET /payments/{id}` with exponential backoff until terminal or attempts exhausted. */
    private suspend fun pollUntilTerminal(
        orderId: String,
        initial: PaymentSnapshot,
    ): PaymentSnapshot {
        var snapshot = initial
        var delayMs = pollConfig.initialDelayMs
        var attempts = 0
        while (!snapshot.status.isTerminal && attempts < pollConfig.maxAttempts) {
            delay(delayMs)
            snapshot = backend.status(orderId)
            delayMs = (delayMs * 2).coerceAtMost(pollConfig.maxDelayMs)
            attempts++
        }
        return snapshot
    }

    /**
     * Cold-start recovery: for every payment written to the journal but never resolved (app died
     * mid-flight), ask the server what actually happened and settle the row. Called on app launch.
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
