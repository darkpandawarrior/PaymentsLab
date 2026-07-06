package com.paymentslab.backend

import com.paymentslab.core.protocol.ConnectAccountResponse
import com.paymentslab.core.protocol.ConnectAccountStatusDto
import com.paymentslab.core.protocol.ConnectOnboardResponse
import com.paymentslab.core.protocol.ConnectPayoutRequest
import com.paymentslab.core.protocol.PayoutResponse
import io.ktor.http.ContentType
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import java.util.UUID

/**
 * Stripe Connect payout onboarding (roadmap #11) — the Jugnoo driver-app "onboard a payout account,
 * then pay out to it" flow redone properly: mock hosted OAuth + the existing payout rail. Mirrors
 * [PayoutRoutes]'s idempotency/lookup shape:
 *
 * - `POST /connect/onboard` — starts onboarding, returns a mock hosted-OAuth URL
 *   (`GET /mock/connect/{onboardingId}`, a Pay/Fail-style landing page like `/mock/checkout/{provider}`)
 *   plus the onboarding id. Idempotent on [onboardingId] being reused by a client retry.
 * - `GET /mock/connect/{onboardingId}` — the mock OAuth consent landing page the hosted WebView
 *   relay renders; links to the complete callback, same idiom as `/mock/checkout`.
 * - `GET /mock/connect/{onboardingId}/complete` — the mock OAuth callback, reachable as a plain link
 *   click from the consent page (mirrors `/mock/return/success` being a GET): flips the account to
 *   CONNECTED and renders the return page. Idempotent — completing twice is a no-op re-render.
 * - `POST /mock/connect/{onboardingId}/complete` — same transition, callable directly by a client/test
 *   without rendering HTML (returns the [ConnectAccountResponse] JSON body).
 * - `GET /connect/{accountId}` — poll a connected account's status.
 * - `POST /connect/{accountId}/payouts` — pay out to a connected account, reusing [PayoutStore]'s
 *   initiate/PENDING/mock-settle semantics. Rejects an unknown or not-yet-CONNECTED account.
 */
@Suppress("ThrowsCount", "LongMethod") // per-route guards + multi-route mount, mirrors payoutRoutes
fun Route.connectRoutes(
    connectStore: ConnectStore,
    payoutStore: PayoutStore,
) {
    post("/connect/onboard") {
        val onboardingId = "conn_${UUID.randomUUID()}"
        val record = connectStore.startOnboarding(onboardingId)
        call.application.log.info("[connect] onboarding started ${record.onboardingId} acct=${record.accountId}")
        call.respond(
            ConnectOnboardResponse(
                onboardingId = record.onboardingId,
                hostedOAuthUrl = "/mock/connect/${record.onboardingId}",
                accountId = record.accountId,
            ),
        )
    }

    get("/mock/connect/{onboardingId}") {
        val onboardingId =
            call.parameters["onboardingId"]
                ?: throw BadRequestException("missing_onboarding_id", "onboardingId path param required")
        call.respondText(mockConnectHtml(onboardingId), ContentType.Text.Html)
    }

    get("/mock/connect/{onboardingId}/complete") {
        val onboardingId =
            call.parameters["onboardingId"]
                ?: throw BadRequestException("missing_onboarding_id", "onboardingId path param required")
        val record =
            connectStore.completeOnboarding(onboardingId)
                ?: throw NotFoundException("unknown_onboarding", "No onboarding: $onboardingId")
        call.application.log.info("[connect] onboarding completed ${record.onboardingId} acct=${record.accountId}")
        call.respondText(mockConnectReturnHtml(record.accountId), ContentType.Text.Html)
    }

    post("/mock/connect/{onboardingId}/complete") {
        val onboardingId =
            call.parameters["onboardingId"]
                ?: throw BadRequestException("missing_onboarding_id", "onboardingId path param required")
        val record =
            connectStore.completeOnboarding(onboardingId)
                ?: throw NotFoundException("unknown_onboarding", "No onboarding: $onboardingId")
        call.application.log.info("[connect] onboarding completed ${record.onboardingId} acct=${record.accountId}")
        call.respond(record.toResponse())
    }

    get("/mock/connect/cancelled") {
        call.respondText(mockConnectCancelledHtml(), ContentType.Text.Html)
    }

    get("/connect/{accountId}") {
        val accountId =
            call.parameters["accountId"]
                ?: throw BadRequestException("missing_account_id", "accountId path param required")
        val record =
            connectStore.get(accountId)
                ?: throw NotFoundException("unknown_account", "No connected account: $accountId")
        call.respond(record.toResponse())
    }

    post("/connect/{accountId}/payouts") {
        val accountId =
            call.parameters["accountId"]
                ?: throw BadRequestException("missing_account_id", "accountId path param required")
        val account =
            connectStore.get(accountId)
                ?: throw BadRequestException("unknown_account", "No connected account: $accountId")
        if (account.status != ConnectAccountStatusDto.CONNECTED) {
            throw BadRequestException("account_not_connected", "Account $accountId is not CONNECTED")
        }

        val req = call.receive<ConnectPayoutRequest>()
        val payoutId = "payout_${UUID.randomUUID()}"
        val creation =
            payoutStore.initiate(
                payoutId = payoutId,
                gatewayId = "stripe_connect",
                recipientRef = accountId,
                amountMinor = req.amountMinor,
                currency = req.currency,
                idempotencyKey = req.idempotencyKey,
            )
        val record = creation.record

        call.application.log.info(
            "[connect] payout ${record.payoutId} -> $accountId amount=${record.amountMinor}${record.currency}" +
                if (!creation.isNew) " (idempotent replay)" else "",
        )

        call.respond(
            PayoutResponse(
                payoutId = record.payoutId,
                gatewayId = record.gatewayId,
                recipientRef = record.recipientRef,
                amountMinor = record.amountMinor,
                currency = record.currency,
                status = record.status,
                updatedAtEpochMs = record.updatedAtEpochMs,
            ),
        )
    }
}

private fun ConnectStore.OnboardingRecord.toResponse() =
    ConnectAccountResponse(accountId = accountId, status = status, updatedAtEpochMs = updatedAtEpochMs)

private fun mockConnectHtml(onboardingId: String) =
    """
    <html><body style="font-family: sans-serif; padding: 2rem;">
      <h2>Mock Stripe Connect &mdash; onboarding</h2>
      <p>Onboarding: $onboardingId</p>
      <p><a href="/mock/connect/$onboardingId/complete">Authorize payouts account</a></p>
      <p><a href="/mock/connect/cancelled">Cancel</a></p>
    </body></html>
    """.trimIndent()

private fun mockConnectReturnHtml(accountId: String) =
    """
    <html><body style="font-family: sans-serif; padding: 2rem;">
      <h2>Payouts account connected</h2>
      <p>account_id=$accountId</p>
      <p>You may return to the app.</p>
    </body></html>
    """.trimIndent()

private fun mockConnectCancelledHtml() =
    """
    <html><body style="font-family: sans-serif; padding: 2rem;">
      <h2>Onboarding cancelled</h2>
      <p>You may return to the app.</p>
    </body></html>
    """.trimIndent()
