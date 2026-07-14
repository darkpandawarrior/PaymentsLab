package com.paymentslab.provider.mobilemoney

import com.paymentslab.core.common.AppLog
import com.paymentslab.core.network.PaymentApiConfig
import com.siddharth.kmp.paymentsapi.CreatedOrder
import com.siddharth.kmp.paymentsapi.GatewayId
import com.siddharth.kmp.paymentsapi.GatewayMeta
import com.siddharth.kmp.paymentsapi.PaymentGateway
import com.siddharth.kmp.paymentsapi.PaymentHost
import com.siddharth.kmp.paymentsapi.PaymentResult
import com.siddharth.kmp.paymentsapi.PendingReason
import com.siddharth.kmp.paymentsapi.PreparedPayment
import com.siddharth.kmp.paymentsapi.Redactor
import io.ktor.client.HttpClient
import io.ktor.client.request.post

/**
 * Archetype-D: no SDK, no WebView, no UI at all in `pay()` — mobile-money confirmation happens on
 * the payer's phone outside this app (an STK push / SMS / USSD prompt), so all this gateway does is
 * kick off the mock delayed-flip (`POST /mock/momo/{id}`, built in B0) and return `Pending`
 * immediately. The orchestrator's existing poll-with-backoff (`FsmPollConfig`, already exercised by
 * [com.paymentslab.provider.upiintent]'s `SUBMITTED` limbo) picks up the result once the backend
 * flips it — no new orchestration logic needed, this rides infrastructure that already exists.
 */
class MobileMoneyGateway(
    private val config: MobileMoneyConfig,
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
            httpClient.post(
                "$baseUrl/mock/momo/${id.value}?orderId=${prepared.orderId}&outcome=success&delayMs=$MOCK_FLIP_DELAY_MS",
            )
        }.onFailure { AppLog.w(TAG, "Could not schedule mock momo flip for ${prepared.orderId}", it) }

        return PaymentResult.Pending(
            reason = PendingReason.AWAITING_WEBHOOK,
            raw = Redactor.redact("${id.value}_pending", mapOf("order_id" to prepared.orderId, "mode" to "async")),
        )
    }

    private companion object {
        const val TAG = "MobileMoneyGateway"
        const val MOCK_FLIP_DELAY_MS = 3_000L
    }
}
