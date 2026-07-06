package com.paymentslab.backend

import com.paymentslab.core.protocol.CreateMandateRequest
import com.paymentslab.core.protocol.DebitMandateRequest
import com.paymentslab.core.protocol.MandateDebitResponse
import com.paymentslab.core.protocol.MandateResponse
import com.paymentslab.core.protocol.MandateStatusDto
import com.paymentslab.core.protocol.PaymentStatusDto
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Integration tests over the mandates/subscriptions rail (roadmap #6 — Razorpay recurring),
 * mirroring [PayoutRoutesTest]'s style: real routes via [testApplication], no bound port.
 */
class MandateRoutesTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    private inline fun <reified T> decode(body: String): T = json.decodeFromString(body)

    private fun mandateRequest(idempotencyKey: String) =
        CreateMandateRequest(
            catalogItemId = "coffee_149",
            gatewayId = "razorpay",
            idempotencyKey = idempotencyKey,
        )

    private suspend fun HttpClient.createMandate(idempotencyKey: String) =
        decode<MandateResponse>(
            post("/mandates") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(mandateRequest(idempotencyKey)))
            }.bodyAsText(),
        )

    @Test
    fun `MANDATE order creates an ACTIVE mandate, not a one-time charge`() =
        testApplication {
            application { module() }

            val mandate = client.createMandate("mandate_idem_1")

            assertEquals(MandateStatusDto.ACTIVE, mandate.status)
            assertEquals(14_900L, mandate.amountMinor)
            assertEquals("razorpay", mandate.gatewayId)
            assertTrue(mandate.providerParams.containsKey("order_id"))
        }

    @Test
    fun `re-creating a mandate with the same idempotencyKey returns the same mandate`() =
        testApplication {
            application { module() }

            val first = client.createMandate("mandate_idem_dedup")
            val second = client.createMandate("mandate_idem_dedup")

            assertEquals(first.mandateId, second.mandateId)
        }

    @Test
    fun `a recurring debit against an active mandate succeeds`() =
        testApplication {
            application { module() }

            val mandate = client.createMandate("mandate_idem_debit")

            val resp =
                client.post("/mandates/${mandate.mandateId}/debits") {
                    contentType(ContentType.Application.Json)
                    setBody(json.encodeToString(DebitMandateRequest(idempotencyKey = "debit_idem_1")))
                }

            assertEquals(HttpStatusCode.OK, resp.status)
            val debit = decode<MandateDebitResponse>(resp.bodyAsText())
            assertEquals(PaymentStatusDto.SUCCESS, debit.status)
            assertEquals(mandate.amountMinor, debit.amountMinor)
        }

    @Test
    fun `idempotent replay of a recurring debit charges exactly once`() =
        testApplication {
            application { module() }

            val mandate = client.createMandate("mandate_idem_debit_dedup")

            suspend fun debit() =
                decode<MandateDebitResponse>(
                    client
                        .post("/mandates/${mandate.mandateId}/debits") {
                            contentType(ContentType.Application.Json)
                            setBody(json.encodeToString(DebitMandateRequest(idempotencyKey = "debit_idem_dedup")))
                        }.bodyAsText(),
                )

            val first = debit()
            val second = debit()

            assertEquals(first.debitId, second.debitId)
        }

    @Test
    fun `debit against an unknown mandate is rejected`() =
        testApplication {
            application { module() }

            val resp =
                client.post("/mandates/mandate_does_not_exist/debits") {
                    contentType(ContentType.Application.Json)
                    setBody(json.encodeToString(DebitMandateRequest(idempotencyKey = "debit_idem_unknown")))
                }

            assertEquals(HttpStatusCode.NotFound, resp.status)
            assertTrue(resp.bodyAsText().contains("unknown_mandate"))
        }

    @Test
    fun `debit against a cancelled mandate is rejected`() =
        testApplication {
            application { module() }

            val mandate = client.createMandate("mandate_idem_cancel")
            client.post("/mandates/${mandate.mandateId}/cancel")

            val resp =
                client.post("/mandates/${mandate.mandateId}/debits") {
                    contentType(ContentType.Application.Json)
                    setBody(json.encodeToString(DebitMandateRequest(idempotencyKey = "debit_idem_cancelled")))
                }

            assertEquals(HttpStatusCode.BadRequest, resp.status)
            assertTrue(resp.bodyAsText().contains("mandate_not_active"))
        }
}
