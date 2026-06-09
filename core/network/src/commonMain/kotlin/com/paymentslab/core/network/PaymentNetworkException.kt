package com.paymentslab.core.network

/**
 * Every HTTP transport / serialization failure that escapes [KtorPaymentBackend] is wrapped in this
 * single type before it crosses back into the domain layer. The orchestrator catches it and maps
 * the payment to an `Errored` state without ever knowing it came from Ktor — HTTP concerns stop at
 * this module's edge, exactly as the DTO<->domain mapping does.
 */
class PaymentNetworkException(
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause)
