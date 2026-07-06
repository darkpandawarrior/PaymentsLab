package com.paymentslab.core.network

import com.paymentslab.core.common.AppLog
import com.paymentslab.core.paymentsapi.InstrumentCharge
import com.paymentslab.core.paymentsapi.Money
import com.paymentslab.core.paymentsapi.SavedInstrument
import com.paymentslab.core.paymentsapi.VaultBackend
import com.paymentslab.core.protocol.ChargeInstrumentRequest
import com.paymentslab.core.protocol.InstrumentChargeResponse
import com.paymentslab.core.protocol.SaveInstrumentRequest
import com.paymentslab.core.protocol.SavedInstrumentDto
import com.paymentslab.core.protocol.SavedInstrumentsResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException

/**
 * The Ktor implementation of [VaultBackend] — mirrors [KtorPayoutBackend]'s shape (same
 * request-wrapping/error-mapping pattern) for the Stripe Customer + vault rail (roadmap #7).
 */
class KtorVaultBackend(
    private val client: HttpClient,
    private val config: PaymentApiConfig,
) : VaultBackend {
    private val base: String = config.baseUrl.trimEnd('/')

    override suspend fun save(
        customerId: String,
        cardToken: String,
        brand: String,
        last4: String,
        idempotencyKey: String,
    ): SavedInstrument =
        request("save(customer=$customerId, brand=$brand)") {
            val response: SavedInstrumentDto =
                client
                    .post("$base/vault/$customerId/instruments") {
                        contentType(ContentType.Application.Json)
                        setBody(
                            SaveInstrumentRequest(
                                cardToken = cardToken,
                                brand = brand,
                                last4 = last4,
                                idempotencyKey = idempotencyKey,
                            ),
                        )
                    }.body()
            response.toDomain()
        }

    override suspend fun list(customerId: String): List<SavedInstrument> =
        request("list(customer=$customerId)") {
            val response: SavedInstrumentsResponse = client.get("$base/vault/$customerId/instruments").body()
            response.instruments.map { it.toDomain() }
        }

    override suspend fun charge(
        customerId: String,
        instrumentId: String,
        catalogItemId: String,
        idempotencyKey: String,
    ): InstrumentCharge =
        request("charge(customer=$customerId, instrument=$instrumentId)") {
            val response: InstrumentChargeResponse =
                client
                    .post("$base/vault/$customerId/instruments/$instrumentId/charge") {
                        contentType(ContentType.Application.Json)
                        setBody(
                            ChargeInstrumentRequest(
                                catalogItemId = catalogItemId,
                                idempotencyKey = idempotencyKey,
                            ),
                        )
                    }.body()
            response.toDomain()
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
            throw PaymentNetworkException("Vault backend call failed: $label (${t.message})", t)
        }

    companion object {
        private const val TAG = "KtorVaultBackend"
    }
}

private fun SavedInstrumentDto.toDomain() =
    SavedInstrument(
        instrumentId = instrumentId,
        customerId = customerId,
        brand = brand,
        last4 = last4,
    )

private fun InstrumentChargeResponse.toDomain() =
    InstrumentCharge(
        chargeId = chargeId,
        customerId = customerId,
        instrumentId = instrumentId,
        amount = Money(amountMinor, currency),
        status = status.toDomain(),
    )
