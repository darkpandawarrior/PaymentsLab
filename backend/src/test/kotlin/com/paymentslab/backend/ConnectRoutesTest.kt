package com.paymentslab.backend

import com.paymentslab.core.protocol.ConnectAccountResponse
import com.paymentslab.core.protocol.ConnectAccountStatusDto
import com.paymentslab.core.protocol.ConnectOnboardResponse
import com.paymentslab.core.protocol.ConnectPayoutRequest
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
 * Integration tests over the Stripe Connect payout onboarding rail, mirroring [PayoutRoutesTest]'s
 * style: real routes via [testApplication], no bound port.
 */
class ConnectRoutesTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    private inline fun <reified T> decode(body: String): T = json.decodeFromString(body)

    @Test
    fun `onboard then complete connects the account`() =
        testApplication {
            application { module() }

            val onboard =
                decode<ConnectOnboardResponse>(client.post("/connect/onboard").bodyAsText())
            assertTrue(onboard.hostedOAuthUrl.contains(onboard.onboardingId))

            val completed =
                decode<ConnectAccountResponse>(
                    client.post("/mock/connect/${onboard.onboardingId}/complete").bodyAsText(),
                )
            assertEquals(ConnectAccountStatusDto.CONNECTED, completed.status)

            val polled = decode<ConnectAccountResponse>(client.get("/connect/${completed.accountId}").bodyAsText())
            assertEquals(ConnectAccountStatusDto.CONNECTED, polled.status)
        }

    @Test
    fun `completing onboarding twice is idempotent`() =
        testApplication {
            application { module() }

            val onboard = decode<ConnectOnboardResponse>(client.post("/connect/onboard").bodyAsText())

            suspend fun complete() =
                decode<ConnectAccountResponse>(
                    client.post("/mock/connect/${onboard.onboardingId}/complete").bodyAsText(),
                )

            val first = complete()
            val second = complete()

            assertEquals(first.accountId, second.accountId)
            assertEquals(ConnectAccountStatusDto.CONNECTED, second.status)
        }

    @Test
    fun `payout to a connected account settles to SUCCESS via the mock settlement webhook`() =
        testApplication {
            application { module() }

            val onboard = decode<ConnectOnboardResponse>(client.post("/connect/onboard").bodyAsText())
            val account =
                decode<ConnectAccountResponse>(
                    client.post("/mock/connect/${onboard.onboardingId}/complete").bodyAsText(),
                )

            val payout =
                decode<PayoutResponse>(
                    client
                        .post("/connect/${account.accountId}/payouts") {
                            contentType(ContentType.Application.Json)
                            setBody(
                                json.encodeToString(
                                    ConnectPayoutRequest(
                                        amountMinor = 2_500L,
                                        currency = "USD",
                                        idempotencyKey = "connect_payout_1",
                                    ),
                                ),
                            )
                        }.bodyAsText(),
                )
            assertEquals(PayoutStatusDto.PENDING, payout.status)
            assertEquals(account.accountId, payout.recipientRef)

            val scheduleResp = client.post("/mock/payouts/${payout.payoutId}/settle?delayMs=10")
            assertEquals(HttpStatusCode.OK, scheduleResp.status)

            delay(200)

            val status = decode<PayoutResponse>(client.get("/payouts/${payout.payoutId}").bodyAsText())
            assertEquals(PayoutStatusDto.SETTLED, status.status)
        }

    @Test
    fun `payout to an unknown connected account is rejected`() =
        testApplication {
            application { module() }

            val resp =
                client.post("/connect/acct_does_not_exist/payouts") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        json.encodeToString(
                            ConnectPayoutRequest(amountMinor = 1_000L, currency = "USD", idempotencyKey = "k1"),
                        ),
                    )
                }
            assertEquals(HttpStatusCode.BadRequest, resp.status)
            assertTrue(resp.bodyAsText().contains("unknown_account"))
        }

    @Test
    fun `payout to an account still onboarding_pending is rejected`() =
        testApplication {
            application { module() }

            val onboard = decode<ConnectOnboardResponse>(client.post("/connect/onboard").bodyAsText())

            val resp =
                client.post("/connect/${onboard.accountId}/payouts") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        json.encodeToString(
                            ConnectPayoutRequest(amountMinor = 1_000L, currency = "USD", idempotencyKey = "k2"),
                        ),
                    )
                }
            assertEquals(HttpStatusCode.BadRequest, resp.status)
            assertTrue(resp.bodyAsText().contains("account_not_connected"))
        }
}
