package com.paymentslab.backend

import com.paymentslab.core.protocol.ChargeInstrumentRequest
import com.paymentslab.core.protocol.InstrumentChargeResponse
import com.paymentslab.core.protocol.PaymentStatusDto
import com.paymentslab.core.protocol.SaveInstrumentRequest
import com.paymentslab.core.protocol.SavedInstrumentDto
import com.paymentslab.core.protocol.SavedInstrumentsResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Integration tests over the Stripe Customer + vault rail (roadmap #7), mirroring
 * [PayoutRoutesTest]/[MandateRoutesTest]'s style: real routes via [testApplication], no bound port.
 */
class VaultRoutesTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    private inline fun <reified T> decode(body: String): T = json.decodeFromString(body)

    private fun saveRequest(idempotencyKey: String) =
        SaveInstrumentRequest(
            cardToken = "tok_visa_secret_do_not_leak",
            brand = "visa",
            last4 = "4242",
            idempotencyKey = idempotencyKey,
        )

    private suspend fun HttpClient.saveInstrument(
        customerId: String,
        idempotencyKey: String,
    ) = decode<SavedInstrumentDto>(
        post("/vault/$customerId/instruments") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(saveRequest(idempotencyKey)))
        }.bodyAsText(),
    )

    @Test
    fun `saving an instrument returns a masked id and never echoes the raw token`() =
        testApplication {
            application { module() }

            val body =
                client
                    .post("/vault/cust_1/instruments") {
                        contentType(ContentType.Application.Json)
                        setBody(json.encodeToString(saveRequest("vault_idem_1")))
                    }.bodyAsText()

            val instrument = decode<SavedInstrumentDto>(body)
            assertEquals("visa", instrument.brand)
            assertEquals("4242", instrument.last4)
            assertTrue(instrument.instrumentId.isNotBlank())
            assertFalse(body.contains("tok_visa_secret_do_not_leak"))
        }

    @Test
    fun `listing shows the saved instrument, masked`() =
        testApplication {
            application { module() }

            val saved = client.saveInstrument("cust_2", "vault_idem_list")

            val listBody = client.get("/vault/cust_2/instruments").bodyAsText()
            val list = decode<SavedInstrumentsResponse>(listBody)

            assertEquals(1, list.instruments.size)
            assertEquals(saved.instrumentId, list.instruments.single().instrumentId)
            assertFalse(listBody.contains("tok_visa_secret_do_not_leak"))
        }

    @Test
    fun `re-saving with the same idempotencyKey returns one instrument`() =
        testApplication {
            application { module() }

            val first = client.saveInstrument("cust_3", "vault_idem_dedup")
            val second = client.saveInstrument("cust_3", "vault_idem_dedup")

            assertEquals(first.instrumentId, second.instrumentId)
            val list = decode<SavedInstrumentsResponse>(client.get("/vault/cust_3/instruments").bodyAsText())
            assertEquals(1, list.instruments.size)
        }

    @Test
    fun `charging a saved instrument succeeds`() =
        testApplication {
            application { module() }

            val instrument = client.saveInstrument("cust_4", "vault_idem_charge")

            val resp =
                client.post("/vault/cust_4/instruments/${instrument.instrumentId}/charge") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        json.encodeToString(
                            ChargeInstrumentRequest(catalogItemId = "coffee_149", idempotencyKey = "charge_idem_1"),
                        ),
                    )
                }

            assertEquals(HttpStatusCode.OK, resp.status)
            val charge = decode<InstrumentChargeResponse>(resp.bodyAsText())
            assertEquals(PaymentStatusDto.SUCCESS, charge.status)
            assertEquals(14_900L, charge.amountMinor)
        }

    @Test
    fun `idempotent replay of a charge charges exactly once`() =
        testApplication {
            application { module() }

            val instrument = client.saveInstrument("cust_5", "vault_idem_charge_dedup")

            suspend fun charge() =
                decode<InstrumentChargeResponse>(
                    client
                        .post("/vault/cust_5/instruments/${instrument.instrumentId}/charge") {
                            contentType(ContentType.Application.Json)
                            setBody(
                                json.encodeToString(
                                    ChargeInstrumentRequest(
                                        catalogItemId = "coffee_149",
                                        idempotencyKey = "charge_idem_dedup",
                                    ),
                                ),
                            )
                        }.bodyAsText(),
                )

            val first = charge()
            val second = charge()

            assertEquals(first.chargeId, second.chargeId)
        }

    @Test
    fun `charging an unknown customer or instrument is rejected`() =
        testApplication {
            application { module() }

            val resp =
                client.post("/vault/cust_does_not_exist/instruments/instr_does_not_exist/charge") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        json.encodeToString(
                            ChargeInstrumentRequest(
                                catalogItemId = "coffee_149",
                                idempotencyKey = "charge_idem_unknown",
                            ),
                        ),
                    )
                }

            assertEquals(HttpStatusCode.NotFound, resp.status)
            assertTrue(resp.bodyAsText().contains("unknown_instrument"))
        }
}
