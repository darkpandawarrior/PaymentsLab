package com.paymentslab.backend

import com.paymentslab.core.protocol.CreateOrderRequest
import com.paymentslab.core.protocol.OrderResponse
import com.paymentslab.core.protocol.PaymentStatusDto
import com.paymentslab.core.protocol.PaymentStatusResponse
import com.paymentslab.core.protocol.VerifyRequest
import com.paymentslab.core.protocol.VerifyResponse
import com.paymentslab.core.protocol.WebhookAck
import io.ktor.client.request.get
import io.ktor.client.request.header
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
 * Integration tests over the real routes via [testApplication] (no bound port). Deterministic + fast.
 */
class BackendTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    // Same test defaults the module boots with — used to recompute the expected Razorpay HMAC.
    private val razorpaySecret = "test_razorpay_secret"
    private val webhookSecret = "test_razorpay_webhook_secret"

    private inline fun <reified T> decode(body: String): T = json.decodeFromString(body)

    // ── Test 1: server price is authoritative; the coffee item is ₹149.00 = 14900 minor ─────────
    @Test
    fun `order creation uses server price ignoring any client amount`() =
        testApplication {
            application { module() }

            // The wire DTO has no amount field, so a client literally cannot send one. We assert the
            // server resolves the catalog price regardless — the trust boundary in action.
            val resp =
                client.post("/orders") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        json.encodeToString(
                            CreateOrderRequest(catalogItemId = "coffee_149", gatewayId = "razorpay"),
                        ),
                    )
                }
            assertEquals(HttpStatusCode.OK, resp.status)
            val order = decode<OrderResponse>(resp.bodyAsText())
            assertEquals(14_900L, order.amountMinor)
            assertEquals("INR", order.currency)
            assertEquals("coffee_149", order.catalogItemId)
            // Provider params carry the amount from the server, not the client.
            assertEquals("14900", order.providerParams["amount"])
            assertTrue(order.providerParams["key_id"]!!.startsWith("rzp_test_"))
        }

    // ── Test 2: Razorpay verify — real HMAC SUCCESS vs wrong-signature FAILED ────────────────────
    @Test
    fun `razorpay verify succeeds with correct HMAC and fails with wrong signature`() =
        testApplication {
            application { module() }

            val orderResp =
                client.post("/orders") {
                    contentType(ContentType.Application.Json)
                    setBody(json.encodeToString(CreateOrderRequest("book_499", "razorpay")))
                }
            val orderId = decode<OrderResponse>(orderResp.bodyAsText()).orderId
            val paymentId = "pay_test_123"

            // Compute the expected signature the SAME way the server does: HMAC-SHA256("orderId|paymentId").
            val goodSig = Crypto.hmacSha256Hex(razorpaySecret, "$orderId|$paymentId")

            val okResp =
                client.post("/payments/$orderId/verify") {
                    contentType(ContentType.Application.Json)
                    setBody(json.encodeToString(VerifyRequest("razorpay", orderId, paymentId, goodSig)))
                }
            assertEquals(HttpStatusCode.OK, okResp.status)
            assertEquals(PaymentStatusDto.SUCCESS, decode<VerifyResponse>(okResp.bodyAsText()).status)

            val badResp =
                client.post("/payments/$orderId/verify") {
                    contentType(ContentType.Application.Json)
                    setBody(json.encodeToString(VerifyRequest("razorpay", orderId, paymentId, "deadbeef")))
                }
            assertEquals(PaymentStatusDto.FAILED, decode<VerifyResponse>(badResp.bodyAsText()).status)
        }

    // ── Test 3: webhook idempotency — same eventId twice → duplicate + state unchanged ──────────
    @Test
    fun `webhook is idempotent on eventId`() =
        testApplication {
            application { module() }

            val orderId =
                decode<OrderResponse>(
                    client.post("/orders") {
                        contentType(ContentType.Application.Json)
                        setBody(json.encodeToString(CreateOrderRequest("headphones_2499", "upi_intent")))
                    }.bodyAsText(),
                ).orderId

            val eventBody = """{"eventId":"evt_1","orderId":"$orderId","status":"success","paymentId":"pay_777"}"""
            val sig = Crypto.hmacSha256Hex(webhookSecret, eventBody)

            val first =
                client.post("/webhooks/razorpay") {
                    contentType(ContentType.Application.Json)
                    header("X-Razorpay-Signature", sig)
                    setBody(eventBody)
                }
            val firstAck = decode<WebhookAck>(first.bodyAsText())
            assertTrue(firstAck.received)
            assertEquals(false, firstAck.duplicate)

            // Status advanced to success after the first webhook.
            val afterFirst = decode<PaymentStatusResponse>(client.get("/payments/$orderId").bodyAsText())
            assertEquals(PaymentStatusDto.SUCCESS, afterFirst.status)

            // Redeliver the SAME event id.
            val second =
                client.post("/webhooks/razorpay") {
                    contentType(ContentType.Application.Json)
                    header("X-Razorpay-Signature", sig)
                    setBody(eventBody)
                }
            val secondAck = decode<WebhookAck>(second.bodyAsText())
            assertTrue("second delivery of same eventId must be duplicate", secondAck.duplicate)

            val afterSecond = decode<PaymentStatusResponse>(client.get("/payments/$orderId").bodyAsText())
            // State unchanged (same status + same updatedAt timestamp) — the duplicate was a no-op.
            assertEquals(PaymentStatusDto.SUCCESS, afterSecond.status)
            assertEquals(afterFirst.updatedAtEpochMs, afterSecond.updatedAtEpochMs)
            assertEquals(afterFirst.paymentId, afterSecond.paymentId)
        }

    // ── Test 4: unknown catalog item → 400 ──────────────────────────────────────────────────────
    @Test
    fun `unknown catalog item returns 400`() =
        testApplication {
            application { module() }

            val resp =
                client.post("/orders") {
                    contentType(ContentType.Application.Json)
                    setBody(json.encodeToString(CreateOrderRequest("does_not_exist", "razorpay")))
                }
            assertEquals(HttpStatusCode.BadRequest, resp.status)
            assertTrue(resp.bodyAsText().contains("unknown_catalog_item"))
        }

    // ── Bonus: unknown gateway → 400 ────────────────────────────────────────────────────────────
    @Test
    fun `unknown gateway returns 400`() =
        testApplication {
            application { module() }
            val resp =
                client.post("/orders") {
                    contentType(ContentType.Application.Json)
                    setBody(json.encodeToString(CreateOrderRequest("coffee_149", "no_such_gateway")))
                }
            assertEquals(HttpStatusCode.BadRequest, resp.status)
            assertTrue(resp.bodyAsText().contains("unknown_gateway"))
        }
}
