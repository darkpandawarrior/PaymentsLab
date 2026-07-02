package com.paymentslab.core.orchestration

import com.paymentslab.core.paymentsapi.Capability
import com.paymentslab.core.paymentsapi.CreatedOrder
import com.paymentslab.core.paymentsapi.GatewayId
import com.paymentslab.core.paymentsapi.GatewayMeta
import com.paymentslab.core.paymentsapi.GatewayStatus
import com.paymentslab.core.paymentsapi.Money
import com.paymentslab.core.paymentsapi.OrderRef
import com.paymentslab.core.paymentsapi.PaymentBackend
import com.paymentslab.core.paymentsapi.PaymentGateway
import com.paymentslab.core.paymentsapi.PaymentHost
import com.paymentslab.core.paymentsapi.PaymentResult
import com.paymentslab.core.paymentsapi.PaymentSnapshot
import com.paymentslab.core.paymentsapi.PaymentStatus
import com.paymentslab.core.paymentsapi.PendingPayment
import com.paymentslab.core.paymentsapi.PendingPaymentJournal
import com.paymentslab.core.paymentsapi.PreparedPayment
import com.paymentslab.core.paymentsapi.RedactedPayload
import com.paymentslab.core.paymentsapi.VerificationRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

object NoopHost : PaymentHost

fun testMeta(name: String = "Fake") =
    GatewayMeta(
        displayName = name,
        status = GatewayStatus.SANDBOX_READY,
        capabilities = setOf(Capability.ONE_TIME_PAYMENT),
        region = "Test",
        docsPath = "docs/fake.md",
        blurb = "fake gateway",
    )

/** Records the order of key interactions so tests can assert journal-before-launch. */
class InteractionLog {
    val events = mutableListOf<String>()

    fun record(event: String) {
        events.add(event)
    }
}

class FakeGateway(
    override val id: GatewayId,
    override val meta: GatewayMeta = testMeta(),
    private val result: PaymentResult,
    private val log: InteractionLog? = null,
) : PaymentGateway {
    override suspend fun prepare(created: CreatedOrder): PreparedPayment {
        log?.record("prepare")
        return PreparedPayment(id, created.order.orderId, created.order.amount, created.providerParams)
    }

    override suspend fun pay(
        host: PaymentHost,
        prepared: PreparedPayment,
    ): PaymentResult {
        log?.record("pay")
        return result
    }
}

class FakeBackend(
    private val orderId: String = "order_1",
    private val amount: Money = Money.inr(499),
    private val providerParams: Map<String, String> = mapOf("key_id" to "rzp_test_x"),
    private val verifyStatus: PaymentStatus = PaymentStatus.SUCCESS,
    /** Sequence of statuses returned by successive [status] calls (for polling tests). */
    private val statusSequence: List<PaymentStatus> = listOf(PaymentStatus.SUCCESS),
    private val log: InteractionLog? = null,
) : PaymentBackend {
    var verifyCalls = 0
        private set
    private var statusIdx = 0

    override suspend fun createOrder(
        catalogItemId: String,
        gatewayId: GatewayId,
    ): CreatedOrder {
        log?.record("createOrder")
        return CreatedOrder(OrderRef(orderId, catalogItemId, amount), gatewayId, providerParams)
    }

    override suspend fun verify(request: VerificationRequest): PaymentSnapshot {
        verifyCalls++
        log?.record("verify")
        return PaymentSnapshot(request.orderId, request.paymentId, verifyStatus)
    }

    override suspend fun status(orderId: String): PaymentSnapshot {
        val s = statusSequence[statusIdx.coerceAtMost(statusSequence.lastIndex)]
        statusIdx++
        return PaymentSnapshot(orderId, paymentId = null, status = s)
    }
}

class FakeJournal(
    private val log: InteractionLog? = null,
) : PendingPaymentJournal {
    private val state = MutableStateFlow<List<PendingPayment>>(emptyList())
    val recorded get() = state.value

    override suspend fun record(entry: PendingPayment) {
        log?.record("journal.record")
        state.update { it + entry }
    }

    override suspend fun markResolved(
        orderId: String,
        status: PaymentStatus,
        paymentId: String?,
    ) {
        log?.record("journal.markResolved:$status")
        state.update { list ->
            list.map { if (it.orderId == orderId) it.copy(status = status, paymentId = paymentId) else it }
        }
    }

    override suspend fun unresolved(): List<PendingPayment> = state.value.filter { !it.status.isTerminal }

    override fun observeAll(): Flow<List<PendingPayment>> = state
}

fun success(paymentId: String = "pay_1") =
    PaymentResult.Success(
        paymentId = paymentId,
        verification = mapOf("signature" to "abc", "payment_id" to paymentId),
        raw = RedactedPayload.of("client", "payment_id" to paymentId),
    )
