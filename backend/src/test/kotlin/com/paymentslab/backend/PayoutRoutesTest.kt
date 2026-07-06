package com.paymentslab.backend

import com.paymentslab.core.protocol.InitiatePayoutRequest
import com.paymentslab.core.protocol.PayoutResponse
import com.paymentslab.core.protocol.PayoutStatusDto
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Integration tests over the Transfers/payout rail, mirroring [BackendTest]'s style: real routes via
 * [testApplication], no bound port.
 */
class PayoutRoutesTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    private inline fun <reified T> decode(body: String): T = json.decodeFromString(body)

    private fun payoutRequest(idempotencyKey: String) =
        InitiatePayoutRequest(
            gatewayId = "paystack",
            recipientRef = "recipient_acct_1",
            amountMinor = 5_000L,
            currency = "NGN",
            idempotencyKey = idempotencyKey,
        )

    @Test
    fun `initiating a payout returns PENDING`() =
        testApplication {
            application { module() }

            val resp =
                client.post("/payouts") {
                    contentType(ContentType.Application.Json)
                    setBody(json.encodeToString(payoutRequest("payout_idem_1")))
                }

            assertEquals(HttpStatusCode.OK, resp.status)
            val payout = decode<PayoutResponse>(resp.bodyAsText())
            assertEquals(PayoutStatusDto.PENDING, payout.status)
            assertEquals(5_000L, payout.amountMinor)
        }

    @Test
    fun `re-initiating with the same idempotencyKey returns the same payout`() =
        testApplication {
            application { module() }

            suspend fun initiate() =
                decode<PayoutResponse>(
                    client
                        .post("/payouts") {
                            contentType(ContentType.Application.Json)
                            setBody(json.encodeToString(payoutRequest("payout_idem_dedup")))
                        }.bodyAsText(),
                )

            val first = initiate()
            val second = initiate()

            assertEquals(first.payoutId, second.payoutId)
        }

    @Test
    fun `payout settles to SUCCESS via the mock settlement webhook after its delay`() =
        testApplication {
            application { module() }

            val payout =
                decode<PayoutResponse>(
                    client
                        .post("/payouts") {
                            contentType(ContentType.Application.Json)
                            setBody(json.encodeToString(payoutRequest("payout_idem_settle")))
                        }.bodyAsText(),
                )

            val scheduleResp = client.post("/mock/payouts/${payout.payoutId}/settle?delayMs=10")
            assertEquals(HttpStatusCode.OK, scheduleResp.status)

            delay(200)

            val status = decode<PayoutResponse>(client.get("/payouts/${payout.payoutId}").bodyAsText())
            assertEquals(PayoutStatusDto.SETTLED, status.status)
        }

    @Test
    fun `unknown payout returns 404`() =
        testApplication {
            application { module() }

            val resp = client.get("/payouts/payout_does_not_exist")
            assertEquals(HttpStatusCode.NotFound, resp.status)
            assertTrue(resp.bodyAsText().contains("unknown_payout"))
        }
}
