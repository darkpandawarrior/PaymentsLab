package com.paymentslab.core.data

import com.paymentslab.core.paymentsapi.GatewayId
import com.paymentslab.core.paymentsapi.Money
import com.paymentslab.core.paymentsapi.PaymentStatus
import com.paymentslab.core.paymentsapi.PendingPayment

/**
 * Pure entity <-> domain mappers for the journal. Kept as top-level functions with no Room or
 * platform dependency so they are trivially unit-testable on the JVM (see PendingPaymentMapperTest).
 */

/** Domain -> storage. Flattens [Money] into (amountMinor, currency) and enums into their names. */
fun PendingPayment.toEntity(): PendingPaymentEntity =
    PendingPaymentEntity(
        orderId = orderId,
        catalogItemId = catalogItemId,
        gatewayId = gatewayId.value,
        amountMinor = amount.amountMinor,
        currency = amount.currency,
        createdAtEpochMs = createdAtEpochMs,
        status = status.name,
        paymentId = paymentId,
    )

/** Storage -> domain. Reconstructs [Money], [GatewayId] and [PaymentStatus] from primitives. */
fun PendingPaymentEntity.toDomain(): PendingPayment =
    PendingPayment(
        orderId = orderId,
        catalogItemId = catalogItemId,
        gatewayId = GatewayId(gatewayId),
        amount = Money(amountMinor = amountMinor, currency = currency),
        createdAtEpochMs = createdAtEpochMs,
        status = PaymentStatus.valueOf(status),
        paymentId = paymentId,
    )
