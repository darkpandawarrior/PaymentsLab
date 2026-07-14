package com.paymentslab.core.network

import com.paymentslab.core.common.AppLog
import com.siddharth.kmp.paymentsapi.GatewayId
import com.siddharth.kmp.paymentsapi.Money
import com.siddharth.kmp.paymentsapi.PaymentApiConfig
import com.siddharth.kmp.paymentsapi.PayoutBackend
import com.siddharth.kmp.paymentsapi.PayoutSnapshot
import com.paymentslab.core.protocol.InitiatePayoutRequest
import com.paymentslab.core.protocol.PayoutResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException

/**
 * The Ktor implementation of [PayoutBackend] — mirrors [KtorPaymentBackend]'s shape (same
 * request-wrapping/error-mapping pattern) for the Transfers/payout rail.
 */
class KtorPayoutBackend(
    private val client: HttpClient,
    private val config: PaymentApiConfig,
) : PayoutBackend {
    private val base: String = config.baseUrl.trimEnd('/')

    override suspend fun initiate(
        gatewayId: GatewayId,
        recipientRef: String,
        amount: Money,
        idempotencyKey: String,
    ): PayoutSnapshot =
        request("initiate(gateway=${gatewayId.value}, recipient=$recipientRef)") {
            val response: PayoutResponse =
                client
                    .post("$base/payouts") {
                        contentType(ContentType.Application.Json)
                        setBody(
                            InitiatePayoutRequest(
                                gatewayId = gatewayId.value,
                                recipientRef = recipientRef,
                                amountMinor = amount.amountMinor,
                                currency = amount.currency,
                                idempotencyKey = idempotencyKey,
                            ),
                        )
                    }.body()
            response.toSnapshot()
        }

    override suspend fun status(payoutId: String): PayoutSnapshot =
        request("status(payoutId=$payoutId)") {
            val response: PayoutResponse = client.get("$base/payouts/$payoutId").body()
            response.toSnapshot()
        }

    private inline fun <T> request(
        label: String,
        block: () -> T,
    ): T =
        try {
            AppLog.d(TAG, "-> $label")
            block().also { AppLog.d(TAG, "<- $label ok") }
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            AppLog.e(TAG, "x $label failed: ${t.message}", t)
            throw PaymentNetworkException("Payout backend call failed: $label (${t.message})", t)
        }

    companion object {
        private const val TAG = "KtorPayoutBackend"
    }
}
