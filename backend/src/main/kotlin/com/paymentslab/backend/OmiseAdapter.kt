package com.paymentslab.backend

import com.paymentslab.core.config.GatewayCredentials
import com.paymentslab.core.protocol.CatalogItemDto
import com.paymentslab.core.protocol.PaymentStatusDto
import com.paymentslab.core.protocol.VerifyRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.basicAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap

/**
 * Omise — Tier-1, real Charges API (client tokenizes a card to a token via the SDK's
 * `CreditCardActivity`; this adapter charges it). `credentials.enabled` (`PLAB_OMISE_TEST_PUBLIC_KEY`
 * / `_SECRET_KEY`, from `core:config`) decides real vs mock, same pattern as
 * [PaystackAdapter] / [PayPalAdapter] / [SquareAdapter].
 *
 * **Real** (keys configured): `createProviderOrder` hands the (non-secret, client-embeddable)
 * public key down to the client so it can launch `CreditCardActivity`; the resulting token comes
 * back as `req.paymentId`. `verify` charges it (`POST /charges`) using HTTP Basic auth with the
 * secret key as the username and an empty password (Omise's own convention), which never leaves the
 * backend. Omise's Charges API has no separate "create intent" step, so the charge amount is cached
 * per-orderId here between `createProviderOrder` and `verify` — same approach as [SquareAdapter].
 *
 * **Mock** (default): `createProviderOrder` returns an empty provider-params map (no `public_key`) —
 * the client's `OmiseGateway` sees that absence and runs
 * [com.siddharth.kmp.paymentsapi.SimulatedPayment] instead of launching the real SDK.
 *
 * Not exercised against the live sandbox this session (no test credentials available) — covered by
 * `OmiseAdapterTest` via Ktor `MockEngine`.
 * `docs: https://docs.opn.ooo/charges-api`
 */
class OmiseAdapter(
    private val credentials: GatewayCredentials,
    private val httpClient: HttpClient,
) : GatewayAdapter {
    override val gatewayId: String = "omise"

    private val pendingItems = ConcurrentHashMap<String, CatalogItemDto>()

    override suspend fun createProviderOrder(
        orderId: String,
        item: CatalogItemDto,
    ): Map<String, String> {
        if (!credentials.enabled) return emptyMap()
        pendingItems[orderId] = item
        return mapOf("public_key" to credentials.keys.getValue("public_key"))
    }

    override suspend fun verify(req: VerifyRequest): PaymentStatusDto {
        if (!credentials.enabled) return PaymentStatusDto.PENDING
        val token = req.paymentId ?: return PaymentStatusDto.FAILED
        val item = pendingItems.remove(req.orderId) ?: return PaymentStatusDto.FAILED
        val secretKey = credentials.keys.getValue("secret_key")
        val response: OmiseChargeResponse =
            httpClient
                .post("$OMISE_API_BASE/charges") {
                    basicAuth(secretKey, "")
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody(
                        "amount=${item.amountMinor}&currency=${item.currency.lowercase()}&card=$token",
                    )
                }.body()
        return when {
            response.paid && response.status == "successful" -> PaymentStatusDto.SUCCESS
            response.status == "failed" -> PaymentStatusDto.FAILED
            else -> PaymentStatusDto.PENDING
        }
    }

    private companion object {
        const val OMISE_API_BASE = "https://api.omise.co"
    }
}

@Serializable
data class OmiseChargeResponse(
    val id: String,
    val status: String,
    val paid: Boolean,
)
