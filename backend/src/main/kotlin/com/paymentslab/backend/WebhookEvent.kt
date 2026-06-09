package com.paymentslab.backend

import com.paymentslab.core.protocol.PaymentStatusDto
import kotlinx.serialization.Serializable

/**
 * Normalised webhook payload the demo accepts from every provider. A production integration would
 * parse each provider's native event shape (Razorpay `payment.captured`, Stripe
 * `payment_intent.succeeded`, Cashfree `PAYMENT_SUCCESS_WEBHOOK`) into this common form.
 *
 * [eventId] is the idempotency key — the store dedupes on it so a re-delivered webhook is a no-op.
 */
@Serializable
data class WebhookEvent(
    val eventId: String,
    val orderId: String,
    val status: PaymentStatusDto,
    val paymentId: String? = null,
)
