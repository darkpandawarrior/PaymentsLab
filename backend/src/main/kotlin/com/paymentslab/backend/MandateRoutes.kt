package com.paymentslab.backend

import com.paymentslab.core.protocol.CreateMandateRequest
import com.paymentslab.core.protocol.DebitMandateRequest
import com.paymentslab.core.protocol.MandateDebitResponse
import com.paymentslab.core.protocol.MandateResponse
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import java.util.UUID

/**
 * Mandates / subscriptions (roadmap #6) — makes `Capability.MANDATE` real for Razorpay: an order
 * setup here authorizes a recurring mandate instead of charging once, mirroring the order flow's
 * idempotency shape ([PaymentStore.createOrder] / [PayoutStore.initiate]):
 *
 * - `POST /mandates` — set up a mandate. Only the FIRST request per idempotencyKey calls the
 *   provider adapter (same guard `POST /orders` uses) and the mandate goes straight to ACTIVE —
 *   Razorpay authorizes the mandate itself via the checkout SDK, so there's no separate async
 *   authorization step to fake, unlike a KYC-gated payout settlement.
 * - `POST /mandates/{mandateId}/debits` — one recurring debit against an ACTIVE mandate, idempotent
 *   on its own key. Rejects (404 unknown, 400 not-active) against an unknown or non-ACTIVE mandate.
 *   A real recurring *schedule* (charging automatically on a cadence) needs a backend scheduler — out
 *   of scope; this is the single debit call such a scheduler would invoke once per cycle.
 * - `POST /mandates/{mandateId}/cancel` — stops future debits against the mandate.
 * - `GET /mandates/{mandateId}` — the polling target, same shape as `GET /payouts/{payoutId}`.
 */
@Suppress("ThrowsCount") // legitimate per-route request/lookup guards, mirrors payoutRoutes
fun Route.mandateRoutes(
    store: MandateStore,
    catalog: CatalogService,
    gateways: GatewayRegistry,
) {
    post("/mandates") {
        val req = call.receive<CreateMandateRequest>()
        val item =
            catalog.find(req.catalogItemId)
                ?: throw BadRequestException("unknown_catalog_item", "No catalog item: ${req.catalogItemId}")
        val adapter =
            gateways.find(req.gatewayId)
                ?: throw BadRequestException("unknown_gateway", "No gateway: ${req.gatewayId}")

        val candidateMandateId = "mandate_${UUID.randomUUID()}"
        val creation =
            store.createMandate(
                mandateId = candidateMandateId,
                catalogItemId = item.id,
                gatewayId = adapter.gatewayId,
                amountMinor = item.amountMinor,
                currency = item.currency,
                idempotencyKey = req.idempotencyKey,
            )
        var record = creation.record

        // Only the FIRST request for this idempotencyKey talks to the provider — same guard `POST
        // /orders` uses. Reuses GatewayAdapter.createProviderOrder as-is: the mandate id stands in for
        // the order id in the provider session params (same publishable key_id/order_id/amount shape
        // Razorpay Standard Checkout expects either way).
        if (creation.isNew) {
            val providerParams = adapter.createProviderOrder(record.mandateId, item)
            store.recordProviderParams(record.mandateId, providerParams)
            record = record.copy(providerParams = providerParams)
        }

        call.application.log.info(
            "[mandates] created ${record.mandateId} item=${item.id} gw=${adapter.gatewayId}" +
                if (!creation.isNew) " (idempotent replay)" else "",
        )

        call.respond(record.toResponse())
    }

    get("/mandates/{mandateId}") {
        val mandateId =
            call.parameters["mandateId"]
                ?: throw BadRequestException("missing_mandate_id", "mandateId path param required")
        val record =
            store.get(mandateId)
                ?: throw NotFoundException("unknown_mandate", "No mandate: $mandateId")
        call.respond(record.toResponse())
    }

    post("/mandates/{mandateId}/cancel") {
        val mandateId =
            call.parameters["mandateId"]
                ?: throw BadRequestException("missing_mandate_id", "mandateId path param required")
        val record =
            store.cancel(mandateId)
                ?: throw NotFoundException("unknown_mandate", "No mandate: $mandateId")
        call.application.log.info("[mandates] cancelled ${record.mandateId}")
        call.respond(record.toResponse())
    }

    post("/mandates/{mandateId}/debits") {
        val mandateId =
            call.parameters["mandateId"]
                ?: throw BadRequestException("missing_mandate_id", "mandateId path param required")
        val req = call.receive<DebitMandateRequest>()

        val debitId = "debit_${UUID.randomUUID()}"
        val result =
            store.debit(debitId, mandateId, req.idempotencyKey)
                ?: run {
                    val exists = store.get(mandateId) != null
                    if (!exists) {
                        throw NotFoundException("unknown_mandate", "No mandate: $mandateId")
                    }
                    throw BadRequestException("mandate_not_active", "Mandate $mandateId is not ACTIVE")
                }

        call.application.log.info(
            "[mandates] debit ${result.record.debitId} mandate=$mandateId" +
                if (!result.isNew) " (idempotent replay)" else "",
        )

        call.respond(result.record.toResponse())
    }
}

private fun MandateStore.MandateRecord.toResponse() =
    MandateResponse(
        mandateId = mandateId,
        catalogItemId = catalogItemId,
        gatewayId = gatewayId,
        amountMinor = amountMinor,
        currency = currency,
        status = status,
        providerParams = providerParams,
        updatedAtEpochMs = updatedAtEpochMs,
    )

private fun MandateStore.DebitRecord.toResponse() =
    MandateDebitResponse(
        debitId = debitId,
        mandateId = mandateId,
        amountMinor = amountMinor,
        currency = currency,
        status = status,
        updatedAtEpochMs = updatedAtEpochMs,
    )
