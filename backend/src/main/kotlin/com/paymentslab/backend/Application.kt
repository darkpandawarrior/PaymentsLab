package com.paymentslab.backend

import com.paymentslab.core.protocol.ApiError
import com.paymentslab.core.protocol.CreateOrderRequest
import com.paymentslab.core.protocol.OrderResponse
import com.paymentslab.core.protocol.PaymentStatusDto
import com.paymentslab.core.protocol.PaymentStatusResponse
import com.paymentslab.core.protocol.VerifyRequest
import com.paymentslab.core.protocol.VerifyResponse
import com.paymentslab.core.protocol.WebhookAck
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.slf4j.event.Level
import java.util.UUID

/** JSON config shared by ContentNegotiation and raw webhook-body decoding. */
val BackendJson: Json =
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

/**
 * Ktor application module. Wires plugins + routes against freshly-built services so a `testApplication`
 * gets an isolated in-memory store per run.
 *
 * The entrypoint [main] binds Netty; tests call `application { module() }` with no bound port.
 *
 * LongMethod/ThrowsCount are suppressed by design: this is a framework wiring function (plugin
 * installation + route table), and the throw-count is from per-route request validation nested in
 * routing lambdas, which is idiomatic Ktor rather than a complexity smell.
 */
@Suppress("LongMethod", "ThrowsCount")
fun Application.module(config: ServerConfig = ServerConfig.fromEnv()) {
    val store = PaymentStore()
    val catalog = CatalogService()
    val gateways =
        GatewayRegistry(
            listOf(
                RazorpayAdapter(keyId = config.razorpayKeyId, secret = config.razorpaySecret),
                UpiIntentAdapter(
                    payeeVpa = "paymentslab@upi",
                    payeeName = "PaymentsLab",
                    merchantCategoryCode = "5411",
                ),
                StripeAdapter(publishableKey = config.stripePublishableKey, secret = config.stripeSecret),
                CashfreeAdapter(appId = config.cashfreeAppId, secret = config.cashfreeSecret),
            ),
        )

    install(ContentNegotiation) { json(BackendJson) }

    // TAG-style request logging via slf4j (Ktor CallLogging → logback console appender).
    install(CallLogging) { level = Level.INFO }

    // Permissive CORS for local dev only — a real deployment locks this to known origins.
    install(CORS) {
        anyHost()
        allowHeader(io.ktor.http.HttpHeaders.ContentType)
        allowHeader(io.ktor.http.HttpHeaders.Authorization)
        allowMethod(io.ktor.http.HttpMethod.Get)
        allowMethod(io.ktor.http.HttpMethod.Post)
    }

    install(StatusPages) {
        exception<ApiException> { call, cause ->
            call.application.log.warn("[ApiException] ${cause.code}: ${cause.message}")
            call.respond(cause.status, ApiError(cause.code, cause.message))
        }
        exception<Throwable> { call, cause ->
            call.application.log.error("[Unhandled] ${cause.message}", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiError("internal_error", cause.message ?: "Unexpected server error"),
            )
        }
    }

    routing {
        get("/health") { call.respond(mapOf("status" to "ok")) }

        // ── Catalog ─────────────────────────────────────────────────────────
        get("/catalog") {
            call.respond(catalog.all())
        }

        // ── Create order ────────────────────────────────────────────────────
        // Price comes from CatalogService (server-side); any client-sent amount is irrelevant —
        // CreateOrderRequest carries no amount by design. This is the trust boundary.
        post("/orders") {
            val req = call.receive<CreateOrderRequest>()
            val item =
                catalog.find(req.catalogItemId)
                    ?: throw BadRequestException("unknown_catalog_item", "No catalog item: ${req.catalogItemId}")
            val adapter =
                gateways.find(req.gatewayId)
                    ?: throw BadRequestException("unknown_gateway", "No gateway: ${req.gatewayId}")

            val orderId = "order_${UUID.randomUUID()}"
            store.createOrder(
                orderId = orderId,
                catalogItemId = item.id,
                gatewayId = adapter.gatewayId,
                amountMinor = item.amountMinor,
                currency = item.currency,
            )
            val providerParams = adapter.createProviderOrder(orderId, item)
            call.application.log.info(
                "[orders] created $orderId item=${item.id} " +
                    "amount=${item.amountMinor}${item.currency} gw=${adapter.gatewayId}",
            )

            call.respond(
                OrderResponse(
                    orderId = orderId,
                    catalogItemId = item.id,
                    amountMinor = item.amountMinor,
                    currency = item.currency,
                    gatewayId = adapter.gatewayId,
                    providerParams = providerParams,
                ),
            )
        }

        // ── Verify a client-side payment result ─────────────────────────────
        post("/payments/{orderId}/verify") {
            val orderId =
                call.parameters["orderId"]
                    ?: throw BadRequestException("missing_order_id", "orderId path param required")
            val req = call.receive<VerifyRequest>()
            store.get(orderId)
                ?: throw NotFoundException("unknown_order", "No order: $orderId")
            val adapter =
                gateways.find(req.gatewayId)
                    ?: throw BadRequestException("unknown_gateway", "No gateway: ${req.gatewayId}")

            val status = adapter.verify(req.copy(orderId = orderId))
            store.recordVerification(orderId, status, req.paymentId, providerRef = req.paymentId)
            call.application.log.info("[verify] order=$orderId gw=${req.gatewayId} status=$status")

            val message =
                when (status) {
                    PaymentStatusDto.SUCCESS -> "Payment verified."
                    PaymentStatusDto.PENDING -> "Awaiting provider confirmation (webhook)."
                    PaymentStatusDto.FAILED -> "Signature verification failed."
                    else -> null
                }
            call.respond(VerifyResponse(status = status, paymentId = req.paymentId, message = message))
        }

        // ── Provider webhook (idempotent) ───────────────────────────────────
        post("/webhooks/{provider}") {
            val provider =
                call.parameters["provider"]
                    ?: throw BadRequestException("missing_provider", "provider path param required")
            val rawBody = call.receiveText()

            // Signature verification. For razorpay: REAL HMAC-SHA256 over the raw body with the
            // webhook secret, compared to the X-Razorpay-Signature header. Others: accepted for demo.
            if (provider == "razorpay") {
                val sigHeader =
                    call.request.headers["X-Razorpay-Signature"]
                        ?: throw UnauthorizedException("missing_signature", "X-Razorpay-Signature header required")
                val expected = Crypto.hmacSha256Hex(config.razorpayWebhookSecret, rawBody)
                if (!Crypto.constantTimeEquals(expected, sigHeader)) {
                    throw UnauthorizedException("bad_signature", "Webhook signature mismatch")
                }
            }

            val event = BackendJson.decodeFromString(WebhookEvent.serializer(), rawBody)
            val result =
                store.applyWebhook(
                    eventId = event.eventId,
                    orderId = event.orderId,
                    status = event.status,
                    paymentId = event.paymentId,
                    providerRef = event.paymentId,
                )
            call.application.log.info(
                "[webhook] provider=$provider event=${event.eventId} " +
                    "duplicate=${result.duplicate} status=${event.status}",
            )

            call.respond(WebhookAck(received = true, eventId = event.eventId, duplicate = result.duplicate))
        }

        // ── Poll payment status ─────────────────────────────────────────────
        get("/payments/{orderId}") {
            val orderId =
                call.parameters["orderId"]
                    ?: throw BadRequestException("missing_order_id", "orderId path param required")
            val record =
                store.get(orderId)
                    ?: throw NotFoundException("unknown_order", "No order: $orderId")

            call.respond(
                PaymentStatusResponse(
                    orderId = record.orderId,
                    paymentId = record.paymentId,
                    status = record.status,
                    updatedAtEpochMs = record.updatedAtEpochMs,
                    providerRef = record.providerRef,
                ),
            )
        }
    }
}
