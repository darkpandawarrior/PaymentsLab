package com.paymentslab.core.data

import com.siddharth.kmp.paymentsapi.GatewayId
import com.siddharth.kmp.paymentsapi.Money
import com.siddharth.kmp.paymentsapi.PaymentStatus
import com.siddharth.kmp.paymentsapi.PendingPayment
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pure-function tests for the entity <-> domain mappers. No Room, no Android Context — runs on the
 * JVM host-test target. Verifies both directions round-trip losslessly, including the nullable
 * paymentId and every enum status.
 */
class PendingPaymentMapperTest {
    private val domain =
        PendingPayment(
            orderId = "order_123",
            catalogItemId = "item_pro",
            gatewayId = GatewayId("razorpay"),
            amount = Money(amountMinor = 49900, currency = "INR"),
            createdAtEpochMs = 1_700_000_000_000L,
            status = PaymentStatus.PENDING,
            paymentId = null,
        )

    @Test
    fun domain_to_entity_flattens_correctly() {
        val entity = domain.toEntity()

        assertEquals("order_123", entity.orderId)
        assertEquals("item_pro", entity.catalogItemId)
        assertEquals("razorpay", entity.gatewayId)
        assertEquals(49900L, entity.amountMinor)
        assertEquals("INR", entity.currency)
        assertEquals(1_700_000_000_000L, entity.createdAtEpochMs)
        assertEquals("PENDING", entity.status)
        assertEquals(null, entity.paymentId)
    }

    @Test
    fun domain_entity_round_trip_is_lossless() {
        assertEquals(domain, domain.toEntity().toDomain())
    }

    @Test
    fun resolved_payment_round_trips_with_payment_id() {
        val resolved =
            domain.copy(status = PaymentStatus.SUCCESS, paymentId = "pay_abc")

        val roundTripped = resolved.toEntity().toDomain()

        assertEquals(resolved, roundTripped)
        assertEquals("pay_abc", roundTripped.paymentId)
        assertEquals(PaymentStatus.SUCCESS, roundTripped.status)
    }

    @Test
    fun every_status_survives_the_round_trip() {
        for (status in PaymentStatus.entries) {
            val entity = domain.copy(status = status).toEntity()
            assertEquals(status.name, entity.status)
            assertEquals(status, entity.toDomain().status)
        }
    }
}
