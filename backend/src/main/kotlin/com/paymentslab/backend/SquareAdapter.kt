package com.paymentslab.backend

import com.paymentslab.core.config.GatewayCredentials
import com.paymentslab.core.protocol.CatalogItemDto
import com.paymentslab.core.protocol.PaymentStatusDto
import com.paymentslab.core.protocol.VerifyRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap

/**
 * Square — Tier-1, real In-App Payments SDK (client tokenizes a card to a nonce; this adapter
 * charges it via the Payments API). `credentials.enabled` (`PLAB_SQUARE_TEST_APPLICATION_ID` /
 * `_ACCESS_TOKEN` / `_LOCATION_ID`, from `core:config`) decides real vs mock, same pattern as
 * [PaystackAdapter] / [PayPalAdapter].
 *
 * **Real** (keys configured): `createProviderOrder` hands the (non-secret, client-embeddable)
 * `application_id` down to the client so it can launch `CardEntryActivity`; the resulting nonce
 * comes back as `req.paymentId`. `verify` charges it (`POST /v2/payments`) with the access token,
 * which never leaves the backend. The Payments API has no "create intent" step — the charge amount
 * is only known at `createProviderOrder` time, so it's cached per-orderId here for `verify` to use.
 *
 * **Mock** (default): `createProviderOrder` returns an empty provider-params map (no `application_id`)
 * — the client's `SquareGateway` sees that absence and runs [com.paymentslab.core.paymentsapi.SimulatedPayment]
 * instead of launching the real SDK.
 *
 * Not exercised against the live sandbox this session (no test credentials available) — covered by
 * `SquareAdapterTest` via Ktor `MockEngine`.
 * `docs: https://developer.squareup.com/reference/square/payments-api/create-payment`
 */
class SquareAdapter(
    private val credentials: GatewayCredentials,
    private val httpClient: HttpClient,
) : GatewayAdapter {
    override val gatewayId: String = "square"

    private val pendingItems = ConcurrentHashMap<String, CatalogItemDto>()

    override suspend fun createProviderOrder(
        orderId: String,
        item: CatalogItemDto,
    ): Map<String, String> {
        if (!credentials.enabled) return emptyMap()
        pendingItems[orderId] = item
        return mapOf("application_id" to credentials.keys.getValue("application_id"))
    }

    override suspend fun verify(req: VerifyRequest): PaymentStatusDto {
        if (!credentials.enabled) return PaymentStatusDto.PENDING
        val nonce = req.paymentId ?: return PaymentStatusDto.FAILED
        val item = pendingItems.remove(req.orderId) ?: return PaymentStatusDto.FAILED
        val accessToken = credentials.keys.getValue("access_token")
        val locationId = credentials.keys.getValue("location_id")
        val response: SquarePaymentResponse =
            httpClient
                .post("$SQUARE_SANDBOX_BASE/v2/payments") {
                    header("Authorization", "Bearer $accessToken")
                    header("Square-Version", "2024-01-18")
                    contentType(ContentType.Application.Json)
                    setBody(
                        SquarePaymentRequest(
                            sourceId = nonce,
                            idempotencyKey = req.orderId,
                            amountMoney = SquareMoney(amount = item.amountMinor, currency = item.currency),
                            locationId = locationId,
                        ),
                    )
                }.body()
        return when (response.payment.status) {
            "COMPLETED" -> PaymentStatusDto.SUCCESS
            "FAILED", "CANCELED" -> PaymentStatusDto.FAILED
            else -> PaymentStatusDto.PENDING
        }
    }

    private companion object {
        const val SQUARE_SANDBOX_BASE = "https://connect.squareupsandbox.com"
    }
}

@Serializable
data class SquarePaymentRequest(
    @SerialName("source_id") val sourceId: String,
    @SerialName("idempotency_key") val idempotencyKey: String,
    @SerialName("amount_money") val amountMoney: SquareMoney,
    @SerialName("location_id") val locationId: String,
)

@Serializable
data class SquareMoney(
    val amount: Long,
    val currency: String,
)

@Serializable
data class SquarePaymentResponse(
    val payment: SquarePaymentStatus,
)

@Serializable
data class SquarePaymentStatus(
    val id: String,
    val status: String,
)
