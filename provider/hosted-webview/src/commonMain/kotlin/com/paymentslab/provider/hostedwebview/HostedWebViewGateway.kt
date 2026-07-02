package com.paymentslab.provider.hostedwebview

import com.paymentslab.core.common.UiText
import com.paymentslab.core.paymentsapi.CreatedOrder
import com.paymentslab.core.paymentsapi.FailureCode
import com.paymentslab.core.paymentsapi.GatewayId
import com.paymentslab.core.paymentsapi.GatewayMeta
import com.paymentslab.core.paymentsapi.PaymentGateway
import com.paymentslab.core.paymentsapi.PaymentHost
import com.paymentslab.core.paymentsapi.PaymentResult
import com.paymentslab.core.paymentsapi.PreparedPayment
import com.paymentslab.core.paymentsapi.Redactor
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * The archetype-C [PaymentGateway]: renders a hosted checkout page in a Compose WebView and resolves
 * on return-URL redirect, per [HostedGatewayConfig]. One instance is created per configured gateway
 * (see `di/HostedWebViewModule.kt`), all sharing the one [HostedCheckoutRelay].
 *
 * `prepare()` does no network hop of its own — the backend already built the checkout URL params
 * into [CreatedOrder.providerParams] (see `backend/HostedWebViewAdapter`). `pay()` suspends until the
 * WebView screen detects a terminal return-URL and reports it back through the relay.
 */
class HostedWebViewGateway(
    private val config: HostedGatewayConfig,
    private val relay: HostedCheckoutRelay,
) : PaymentGateway {
    override val id: GatewayId = config.gatewayId

    override val meta: GatewayMeta =
        GatewayMeta(
            displayName = config.displayName,
            status = config.status,
            capabilities = config.capabilities,
            region = config.region,
            docsPath = config.docsPath,
            blurb = config.blurb,
        )

    override suspend fun prepare(created: CreatedOrder): PreparedPayment =
        PreparedPayment(
            gatewayId = id,
            orderId = created.order.orderId,
            amount = created.order.amount,
            params = created.providerParams,
        )

    override suspend fun pay(
        host: PaymentHost,
        prepared: PreparedPayment,
    ): PaymentResult {
        val checkoutUrl = config.buildCheckoutUrl(prepared.params)
        return suspendCancellableCoroutine { cont ->
            relay.register(id) { outcome -> if (cont.isActive) cont.resume(mapOutcome(outcome)) { _, _, _ -> } }
            cont.invokeOnCancellation { relay.clear(id) }
            relay.launch(id, checkoutUrl)
        }
    }

    internal fun mapOutcome(outcome: HostedReturnOutcome): PaymentResult =
        when (outcome) {
            is HostedReturnOutcome.Success ->
                PaymentResult.Success(
                    paymentId = outcome.paymentId ?: "unknown",
                    verification = outcome.paymentId?.let { mapOf("payment_id" to it) } ?: emptyMap(),
                    raw = redact("success", outcome.paymentId?.let { mapOf("payment_id" to it) } ?: emptyMap()),
                )
            is HostedReturnOutcome.Failure ->
                PaymentResult.Failure(
                    code = FailureCode.GATEWAY_DECLINED,
                    message = UiText.of(outcome.reason ?: "Hosted checkout reported a failure"),
                    raw = redact("failure", mapOf("reason" to (outcome.reason ?: ""))),
                )
            HostedReturnOutcome.Cancelled ->
                PaymentResult.Cancelled(raw = redact("cancelled", emptyMap()))
        }

    private fun redact(
        outcome: String,
        extra: Map<String, String>,
    ) = Redactor.redact("${config.gatewayId.value}_hosted_$outcome", extra)
}
