package com.paymentslab.core.orchestration

import com.siddharth.kmp.paymentsapi.Capability
import com.siddharth.kmp.paymentsapi.CreatedOrder
import com.siddharth.kmp.paymentsapi.GatewayId
import com.siddharth.kmp.paymentsapi.GatewayMeta
import com.siddharth.kmp.paymentsapi.GatewayStatus
import com.siddharth.kmp.paymentsapi.InsufficientWalletBalanceException
import com.siddharth.kmp.paymentsapi.Money
import com.siddharth.kmp.paymentsapi.OrderRef
import com.siddharth.kmp.paymentsapi.PaymentBackend
import com.siddharth.kmp.paymentsapi.PaymentGateway
import com.siddharth.kmp.paymentsapi.PaymentHost
import com.siddharth.kmp.paymentsapi.PaymentPreparationException
import com.siddharth.kmp.paymentsapi.PaymentResult
import com.siddharth.kmp.paymentsapi.PaymentSnapshot
import com.siddharth.kmp.paymentsapi.PaymentStatus
import com.siddharth.kmp.paymentsapi.PendingPayment
import com.siddharth.kmp.paymentsapi.PendingPaymentJournal
import com.siddharth.kmp.paymentsapi.PreparedPayment
import com.siddharth.kmp.paymentsapi.RedactedPayload
import com.siddharth.kmp.paymentsapi.VerificationRequest
import com.siddharth.kmp.paymentsapi.WalletLedgerPort
import kotlinx.coroutines.awaitCancellation
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

/**
 * A gateway whose [pay] never returns until cancelled — models a hosted/redirect checkout where the
 * process dies before the WebView return-URL (or GPay/UPI intent result) ever arrives.
 */
class HangingGateway(
    override val id: GatewayId,
    override val meta: GatewayMeta = testMeta(),
) : PaymentGateway {
    override suspend fun prepare(created: CreatedOrder): PreparedPayment =
        PreparedPayment(id, created.order.orderId, created.order.amount, created.providerParams)

    override suspend fun pay(
        host: PaymentHost,
        prepared: PreparedPayment,
    ): PaymentResult = awaitCancellation()
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

    /** Every [idempotencyKey] seen by [createOrder], in call order — lets tests assert stability. */
    val idempotencyKeysSeen = mutableListOf<String>()

    override suspend fun createOrder(
        catalogItemId: String,
        gatewayId: GatewayId,
        idempotencyKey: String,
    ): CreatedOrder {
        log?.record("createOrder")
        idempotencyKeysSeen += idempotencyKey
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

/**
 * A tiny in-memory ledger shared by [FakeWalletGateway] (the wallet leg's debit) and
 * [FakeWalletLedgerPort] (the orchestrator's compensating credit) — mirrors the real
 * `LedgerStore`/`WalletGateway` split closely enough to exercise split-payment idempotency and
 * insufficient-balance guards without any HTTP.
 */
class FakeLedger(
    initialBalanceMinor: Long,
) {
    var balanceMinor: Long = initialBalanceMinor
        private set
    private val postedKeys = mutableMapOf<String, Long>() // idempotencyKey -> txnId ordinal
    var txnCounter = 0
        private set

    /** Idempotent debit — replaying the same key never moves the balance twice. */
    fun debit(
        idempotencyKey: String,
        amountMinor: Long,
    ): String {
        postedKeys[idempotencyKey]?.let { return "txn_$it" }
        check(balanceMinor >= amountMinor) { "insufficient balance" }
        balanceMinor -= amountMinor
        val id = ++txnCounter
        postedKeys[idempotencyKey] = id.toLong()
        return "txn_$id"
    }

    /** Idempotent credit — the compensating refund. */
    fun refund(
        idempotencyKey: String,
        amountMinor: Long,
    ): String {
        postedKeys[idempotencyKey]?.let { return "txn_$it" }
        balanceMinor += amountMinor
        val id = ++txnCounter
        postedKeys[idempotencyKey] = id.toLong()
        return "txn_$id"
    }
}

/** Wallet-leg gateway: prepares against [FakeLedger]'s balance, pays by debiting it — like [WalletGateway]. */
class FakeWalletGateway(
    override val id: GatewayId,
    private val ledger: FakeLedger,
    override val meta: GatewayMeta = testMeta("Wallet"),
) : PaymentGateway {
    override suspend fun prepare(created: CreatedOrder): PreparedPayment {
        if (ledger.balanceMinor < created.order.amount.amountMinor) {
            throw PaymentPreparationException("Insufficient wallet balance")
        }
        return PreparedPayment(id, created.order.orderId, created.order.amount, created.providerParams)
    }

    override suspend fun pay(
        host: PaymentHost,
        prepared: PreparedPayment,
    ): PaymentResult {
        val txnId = ledger.debit("pay_${prepared.orderId}", prepared.amount.amountMinor)
        return PaymentResult.Success(
            paymentId = txnId,
            verification = mapOf("txn_id" to txnId),
            raw = RedactedPayload.of("wallet_debit", "txn_id" to txnId),
        )
    }
}

/** [WalletLedgerPort] over the same [FakeLedger] — the orchestrator's compensation seam. */
class FakeWalletLedgerPort(
    private val ledger: FakeLedger,
) : WalletLedgerPort {
    val refundCalls = mutableListOf<Pair<String, Long>>() // idempotencyKey to amountMinor

    override suspend fun debit(
        walletAccountId: String,
        idempotencyKey: String,
        amountMinor: Long,
    ): String =
        try {
            ledger.debit(idempotencyKey, amountMinor)
        } catch (e: IllegalStateException) {
            throw InsufficientWalletBalanceException(walletAccountId)
        }

    override suspend fun refund(
        walletAccountId: String,
        idempotencyKey: String,
        amountMinor: Long,
    ): String {
        refundCalls += idempotencyKey to amountMinor
        return ledger.refund(idempotencyKey, amountMinor)
    }
}

/**
 * [PaymentBackend] fake for split-payment tests: prices EVERY order at [totalAmount] (both legs'
 * `createOrder` calls resolve the same total; the orchestrator caps the wallet leg's amount itself).
 *
 * Verify/status status is PER-GATEWAY ([statusByGateway], default [defaultStatus]) — a split test
 * needs the wallet leg to verify SUCCESS while the gateway leg fails, so a single shared status
 * can't model it. The orchestrator tracks which order belongs to which gateway via [createOrder].
 */
class FakeSplitBackend(
    private val totalAmount: Money,
    private val defaultStatus: PaymentStatus = PaymentStatus.SUCCESS,
    private val statusByGateway: Map<GatewayId, PaymentStatus> = emptyMap(),
) : PaymentBackend {
    // idempotencyKey -> orderId, mirrors the real backend's dedup-by-key so replaying the same
    // split call yields the SAME order id per leg (the property per-leg idempotency depends on).
    private val orderIdsByKey = mutableMapOf<String, String>()
    private val gatewayByOrderId = mutableMapOf<String, GatewayId>()
    val idempotencyKeysSeen = mutableListOf<String>()

    override suspend fun createOrder(
        catalogItemId: String,
        gatewayId: GatewayId,
        idempotencyKey: String,
    ): CreatedOrder {
        idempotencyKeysSeen += idempotencyKey
        val orderId = orderIdsByKey.getOrPut(idempotencyKey) { "order_$idempotencyKey" }
        gatewayByOrderId[orderId] = gatewayId
        return CreatedOrder(OrderRef(orderId, catalogItemId, totalAmount), gatewayId, emptyMap())
    }

    private fun statusFor(orderId: String): PaymentStatus = statusByGateway[gatewayByOrderId[orderId]] ?: defaultStatus

    override suspend fun verify(request: VerificationRequest): PaymentSnapshot =
        PaymentSnapshot(request.orderId, request.paymentId, statusFor(request.orderId))

    override suspend fun status(orderId: String): PaymentSnapshot = PaymentSnapshot(orderId, null, statusFor(orderId))
}
