package com.paymentslab.web

import com.paymentslab.feature.checkoutdemo.DEMO_PRODUCTS
import com.siddharth.kmp.paymentsapi.CreatedOrder
import com.siddharth.kmp.paymentsapi.GatewayId
import com.siddharth.kmp.paymentsapi.Money
import com.siddharth.kmp.paymentsapi.OrderRef
import com.siddharth.kmp.paymentsapi.PaymentBackend
import com.siddharth.kmp.paymentsapi.PaymentSnapshot
import com.siddharth.kmp.paymentsapi.PaymentStatus
import com.siddharth.kmp.paymentsapi.PendingPayment
import com.siddharth.kmp.paymentsapi.PendingPaymentJournal
import com.siddharth.kmp.paymentsapi.VerificationRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * The browser stand-in for the demo backend (`:backend`'s MockCheckoutRoutes): same contract, no
 * network. Prices resolve server-side-style from [DEMO_PRODUCTS] so the client still never sends an
 * amount; `createOrder` dedups on the idempotency key exactly like the real routes; short delays
 * keep the Lab timeline's "server hop" steps legible instead of collapsing instantly.
 */
class InMemoryPaymentBackend : PaymentBackend {
    private val ordersByIdempotencyKey = mutableMapOf<String, CreatedOrder>()
    private var orderSeq = 0

    override suspend fun createOrder(
        catalogItemId: String,
        gatewayId: GatewayId,
        idempotencyKey: String,
    ): CreatedOrder {
        delay(SERVER_HOP_MS)
        ordersByIdempotencyKey[idempotencyKey]?.let { return it }
        val orderId = "web_order_${++orderSeq}"
        val amount =
            DEMO_PRODUCTS.firstOrNull { it.catalogItemId == catalogItemId }?.price
                ?: Money.inr(DEFAULT_PRICE_INR)
        return CreatedOrder(
            order = OrderRef(orderId = orderId, catalogItemId = catalogItemId, amount = amount),
            gatewayId = gatewayId,
            providerParams = mapOf("checkout_url" to "https://mock.checkout.local/$orderId"),
        ).also { ordersByIdempotencyKey[idempotencyKey] = it }
    }

    override suspend fun verify(request: VerificationRequest): PaymentSnapshot {
        delay(SERVER_HOP_MS)
        return PaymentSnapshot(
            orderId = request.orderId,
            paymentId = request.paymentId ?: "mock_pay_${request.orderId}",
            status = PaymentStatus.SUCCESS,
        )
    }

    override suspend fun status(orderId: String): PaymentSnapshot =
        PaymentSnapshot(orderId = orderId, paymentId = null, status = PaymentStatus.SUCCESS)

    private companion object {
        const val SERVER_HOP_MS = 450L
        const val DEFAULT_PRICE_INR = 149L
    }
}

/** In-memory journal — page-lifetime only, which is exactly a browser preview's durability. */
class InMemoryPendingPaymentJournal : PendingPaymentJournal {
    private val entries = MutableStateFlow<List<PendingPayment>>(emptyList())

    override suspend fun record(entry: PendingPayment) {
        entries.update { list -> list.filterNot { it.orderId == entry.orderId } + entry }
    }

    override suspend fun markResolved(
        orderId: String,
        status: PaymentStatus,
        paymentId: String?,
    ) {
        entries.update { list ->
            list.map { if (it.orderId == orderId) it.copy(status = status, paymentId = paymentId) else it }
        }
    }

    override suspend fun unresolved(): List<PendingPayment> =
        entries.value.filter { it.status == PaymentStatus.CREATED || it.status == PaymentStatus.PENDING }

    override fun observeAll(): Flow<List<PendingPayment>> = entries
}
