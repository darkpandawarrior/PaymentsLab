package com.paymentslab.provider.mpesa

import com.paymentslab.core.common.AppLog
import com.paymentslab.core.network.PaymentApiConfig
import com.paymentslab.core.paymentsapi.CreatedOrder
import com.paymentslab.core.paymentsapi.GatewayId
import com.paymentslab.core.paymentsapi.GatewayMeta
import com.paymentslab.core.paymentsapi.PaymentGateway
import com.paymentslab.core.paymentsapi.PaymentHost
import com.paymentslab.core.paymentsapi.PaymentResult
import com.paymentslab.core.paymentsapi.PendingReason
import com.paymentslab.core.paymentsapi.PreparedPayment
import com.paymentslab.core.paymentsapi.Redactor
import io.ktor.client.HttpClient
import io.ktor.client.request.post

/**
 * Archetype-D, same shape as [com.paymentslab.provider.mobilemoney.MobileMoneyGateway]: `pay()`
 * kicks off the mock delayed-flip (`POST /mock/mpesa/{orderId}/settle`) and returns `Pending`
 * immediately — the orchestrator's existing poll-with-backoff resolves it once the backend flips it.
 */
class MpesaGateway(
    private val config: MpesaConfig,
    private val httpClient: HttpClient,
    private val apiConfig: PaymentApiConfig,
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
        val baseUrl = apiConfig.baseUrl.trimEnd('/')
        runCatching {
            httpClient.post("$baseUrl/mock/mpesa/${prepared.orderId}/settle?delayMs=$MOCK_FLIP_DELAY_MS")
        }.onFailure { AppLog.w(TAG, "Could not schedule mock mpesa flip for ${prepared.orderId}", it) }

        return PaymentResult.Pending(
            reason = PendingReason.AWAITING_WEBHOOK,
            raw = Redactor.redact("${id.value}_pending", mapOf("order_id" to prepared.orderId, "mode" to "async")),
        )
    }

    private companion object {
        const val TAG = "MpesaGateway"
        const val MOCK_FLIP_DELAY_MS = 3_000L
    }
}
