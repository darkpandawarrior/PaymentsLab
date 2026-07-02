package com.paymentslab.backend

import com.paymentslab.core.protocol.PaymentStatusDto
import io.ktor.http.ContentType
import io.ktor.server.application.log
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The one generic mock path every archetype-C (hosted-webview) and archetype-D (mobile-money)
 * gateway rides — a `MOCK_MODE` gateway needs no real sandbox to run its full lifecycle end-to-end.
 * Real adapters activate automatically once a batch wires live credentials (see `core:config`); these
 * routes stay available regardless, so a WebView can always fall back to them.
 *
 * - `GET /mock/checkout/{provider}` — a Pay/Fail landing page. `provider:hosted-webview`'s
 *   `HostedGatewayConfig.buildCheckoutUrl` points here; its `matchReturn` recognizes the
 *   `/mock/return/success` / `/mock/return/failure` redirect this page links to.
 * - `POST /mock/momo/{provider}` — schedules a delayed status flip (async mobile-money has no
 *   synchronous result at all; the client polls `GET /payments/{id}` until this fires).
 */
fun Route.mockCheckoutRoutes(actor: PaymentActor) {
    get("/mock/checkout/{provider}") {
        val provider =
            call.parameters["provider"]
                ?: throw BadRequestException("missing_provider", "provider path param required")
        val orderId =
            call.request.queryParameters["orderId"]
                ?: throw BadRequestException("missing_order_id", "orderId query param required")
        call.respondText(mockCheckoutHtml(provider, orderId), ContentType.Text.Html)
    }

    get("/mock/return/success") {
        val paymentId = call.request.queryParameters["payment_id"].orEmpty()
        call.respondText(mockReturnHtml("Payment succeeded", paymentId), ContentType.Text.Html)
    }

    get("/mock/return/failure") {
        val reason = call.request.queryParameters["reason"].orEmpty()
        call.respondText(mockReturnHtml("Payment failed", reason), ContentType.Text.Html)
    }

    post("/mock/momo/{provider}") {
        val provider =
            call.parameters["provider"]
                ?: throw BadRequestException("missing_provider", "provider path param required")
        val orderId =
            call.request.queryParameters["orderId"]
                ?: throw BadRequestException("missing_order_id", "orderId query param required")
        val outcome = call.request.queryParameters["outcome"] ?: "success"
        val delayMs = call.request.queryParameters["delayMs"]?.toLongOrNull() ?: DEFAULT_MOMO_DELAY_MS

        call.application.launch {
            delay(delayMs)
            val status = if (outcome == "failure") PaymentStatusDto.FAILED else PaymentStatusDto.SUCCESS
            actor.applyWebhook(
                WebhookEvent(
                    eventId = "momo_$orderId",
                    orderId = orderId,
                    status = status,
                    paymentId = "momo_pay_$orderId",
                ),
            )
            call.application.log.info(
                "[mock-momo] $provider order=$orderId flipped to $status after ${delayMs}ms",
            )
        }
        call.respond(
            mapOf(
                "scheduled" to "true",
                "provider" to provider,
                "orderId" to orderId,
                "delayMs" to delayMs.toString(),
            ),
        )
    }
}

private const val DEFAULT_MOMO_DELAY_MS = 3_000L

private fun mockCheckoutHtml(
    provider: String,
    orderId: String,
) = """
    <html><body style="font-family: sans-serif; padding: 2rem;">
      <h2>Mock checkout &mdash; $provider</h2>
      <p>Order: $orderId</p>
      <p><a href="/mock/return/success?payment_id=mock_pay_$orderId">Pay successfully</a></p>
      <p><a href="/mock/return/failure?reason=card_declined">Simulate a decline</a></p>
    </body></html>
    """.trimIndent()

private fun mockReturnHtml(
    title: String,
    detail: String,
) = """
    <html><body style="font-family: sans-serif; padding: 2rem;">
      <h2>$title</h2>
      <p>$detail</p>
      <p>You may return to the app.</p>
    </body></html>
    """.trimIndent()
