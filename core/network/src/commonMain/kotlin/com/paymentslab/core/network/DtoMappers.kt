package com.paymentslab.core.network

import com.paymentslab.core.protocol.CreateOrderRequest
import com.paymentslab.core.protocol.OrderResponse
import com.paymentslab.core.protocol.PaymentStatusDto
import com.paymentslab.core.protocol.PaymentStatusResponse
import com.paymentslab.core.protocol.PayoutResponse
import com.paymentslab.core.protocol.PayoutStatusDto
import com.paymentslab.core.protocol.VerifyRequest
import com.paymentslab.core.protocol.VerifyResponse
import com.siddharth.kmp.paymentsapi.CreatedOrder
import com.siddharth.kmp.paymentsapi.GatewayId
import com.siddharth.kmp.paymentsapi.Money
import com.siddharth.kmp.paymentsapi.OrderRef
import com.siddharth.kmp.paymentsapi.PaymentSnapshot
import com.siddharth.kmp.paymentsapi.PaymentStatus
import com.siddharth.kmp.paymentsapi.PayoutSnapshot
import com.siddharth.kmp.paymentsapi.PayoutStatus
import com.siddharth.kmp.paymentsapi.VerificationRequest

// Pure, side-effect-free translation between the `core:protocol` wire DTOs and the
// `core:payments-api` domain types. This is the mapping boundary the whole architecture leans on:
// the orchestrator never sees a DTO or an HTTP concern, so every DTO<->domain conversion lives here
// and nowhere else. These functions are deliberately trivial to unit-test — no I/O, no coroutines.

/** Server-authoritative status: wire enum → domain enum. Total (every DTO value maps). */
fun PaymentStatusDto.toDomain(): PaymentStatus =
    when (this) {
        PaymentStatusDto.CREATED -> PaymentStatus.CREATED
        PaymentStatusDto.PENDING -> PaymentStatus.PENDING
        PaymentStatusDto.SUCCESS -> PaymentStatus.SUCCESS
        PaymentStatusDto.FAILED -> PaymentStatus.FAILED
        PaymentStatusDto.CANCELLED -> PaymentStatus.CANCELLED
        PaymentStatusDto.REFUNDED -> PaymentStatus.REFUNDED
    }

/** Inverse of [toDomain]. Total, so the pair round-trips both directions (verified in tests). */
fun PaymentStatus.toDto(): PaymentStatusDto =
    when (this) {
        PaymentStatus.CREATED -> PaymentStatusDto.CREATED
        PaymentStatus.PENDING -> PaymentStatusDto.PENDING
        PaymentStatus.SUCCESS -> PaymentStatusDto.SUCCESS
        PaymentStatus.FAILED -> PaymentStatusDto.FAILED
        PaymentStatus.CANCELLED -> PaymentStatusDto.CANCELLED
        PaymentStatus.REFUNDED -> PaymentStatusDto.REFUNDED
    }

/** `POST /orders` request body. Note: no amount — the server resolves price from [catalogItemId]. */
fun createOrderRequest(
    catalogItemId: String,
    gatewayId: GatewayId,
    idempotencyKey: String,
): CreateOrderRequest =
    CreateOrderRequest(
        catalogItemId = catalogItemId,
        gatewayId = gatewayId.value,
        idempotencyKey = idempotencyKey,
    )

/** `POST /orders` response → domain [CreatedOrder] (order + provider session material for the SDK). */
fun OrderResponse.toDomain(): CreatedOrder =
    CreatedOrder(
        order =
            OrderRef(
                orderId = orderId,
                catalogItemId = catalogItemId,
                amount = Money(amountMinor = amountMinor, currency = currency),
            ),
        gatewayId = GatewayId(gatewayId),
        providerParams = providerParams,
    )

/** Domain verification request → `POST /payments/{id}/verify` request body. */
fun VerificationRequest.toDto(): VerifyRequest =
    VerifyRequest(
        gatewayId = gatewayId.value,
        orderId = orderId,
        paymentId = paymentId,
        signature = signature,
        extra = extra,
    )

/**
 * `POST /payments/{id}/verify` response → domain [PaymentSnapshot].
 *
 * [orderId] isn't on [VerifyResponse] (the caller already knows which order it verified), so it's
 * threaded through from the request. `providerRef` isn't part of the verify response shape.
 */
fun VerifyResponse.toSnapshot(orderId: String): PaymentSnapshot =
    PaymentSnapshot(
        orderId = orderId,
        paymentId = paymentId,
        status = status.toDomain(),
        providerRef = null,
    )

/** `GET /payments/{id}` response → domain [PaymentSnapshot]. */
fun PaymentStatusResponse.toSnapshot(): PaymentSnapshot =
    PaymentSnapshot(
        orderId = orderId,
        paymentId = paymentId,
        status = status.toDomain(),
        providerRef = providerRef,
    )

/** Payout lifecycle: wire enum → domain enum. Total (every DTO value maps). */
fun PayoutStatusDto.toDomain(): PayoutStatus =
    when (this) {
        PayoutStatusDto.PENDING -> PayoutStatus.PENDING
        PayoutStatusDto.SETTLED -> PayoutStatus.SETTLED
        PayoutStatusDto.FAILED -> PayoutStatus.FAILED
    }

/** `POST /payouts` or `GET /payouts/{id}` response → domain [PayoutSnapshot]. */
fun PayoutResponse.toSnapshot(): PayoutSnapshot =
    PayoutSnapshot(
        payoutId = payoutId,
        gatewayId = GatewayId(gatewayId),
        recipientRef = recipientRef,
        amount = Money(amountMinor = amountMinor, currency = currency),
        status = status.toDomain(),
    )
