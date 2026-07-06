package com.paymentslab.backend

import com.paymentslab.core.protocol.ChargeInstrumentRequest
import com.paymentslab.core.protocol.InstrumentChargeResponse
import com.paymentslab.core.protocol.SaveInstrumentRequest
import com.paymentslab.core.protocol.SavedInstrumentDto
import com.paymentslab.core.protocol.SavedInstrumentsResponse
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import java.util.UUID

/**
 * Stored instruments via a Stripe-style Customer + vault (roadmap #7) — a modern retelling of the
 * five-gateway `card_id` vault pattern: save a card token once, charge it later without
 * re-entering. Mirrors the mandates/payout rail's idempotency + lookup shape
 * ([MandateStore]/[PayoutStore]):
 *
 * - `POST /vault/{customerId}/instruments` — save a card token, idempotent on
 *   [SaveInstrumentRequest.idempotencyKey] (same putIfAbsent-race-safe dedup as order creation).
 *   Only the masked [SavedInstrumentDto] is ever returned — the raw token is never echoed back.
 * - `GET /vault/{customerId}/instruments` — list saved instruments for a customer, masked.
 * - `POST /vault/{customerId}/instruments/{instrumentId}/charge` — charge an order using a saved
 *   instrument, idempotent on its own key. Rejects 404 for an unknown customer/instrument, mirroring
 *   the mandates rail's 404 for an unknown mandate.
 */
@Suppress("ThrowsCount") // legitimate per-route request/lookup guards, mirrors payoutRoutes/mandateRoutes
fun Route.vaultRoutes(
    store: VaultStore,
    catalog: CatalogService,
) {
    post("/vault/{customerId}/instruments") {
        val customerId =
            call.parameters["customerId"]
                ?: throw BadRequestException("missing_customer_id", "customerId path param required")
        val req = call.receive<SaveInstrumentRequest>()

        val candidateInstrumentId = "instr_${UUID.randomUUID()}"
        val result =
            store.saveInstrument(
                instrumentId = candidateInstrumentId,
                customerId = customerId,
                brand = req.brand,
                last4 = req.last4,
                idempotencyKey = req.idempotencyKey,
            )

        call.application.log.info(
            "[vault] saved ${result.record.instrumentId} customer=$customerId brand=${req.brand}" +
                if (!result.isNew) " (idempotent replay)" else "",
        )

        call.respond(result.record.toDto())
    }

    get("/vault/{customerId}/instruments") {
        val customerId =
            call.parameters["customerId"]
                ?: throw BadRequestException("missing_customer_id", "customerId path param required")
        call.respond(
            SavedInstrumentsResponse(
                customerId = customerId,
                instruments = store.list(customerId).map { it.toDto() },
            ),
        )
    }

    post("/vault/{customerId}/instruments/{instrumentId}/charge") {
        val customerId =
            call.parameters["customerId"]
                ?: throw BadRequestException("missing_customer_id", "customerId path param required")
        val instrumentId =
            call.parameters["instrumentId"]
                ?: throw BadRequestException("missing_instrument_id", "instrumentId path param required")
        val req = call.receive<ChargeInstrumentRequest>()
        val item =
            catalog.find(req.catalogItemId)
                ?: throw BadRequestException("unknown_catalog_item", "No catalog item: ${req.catalogItemId}")

        val candidateChargeId = "charge_${UUID.randomUUID()}"
        val result =
            store.charge(
                chargeId = candidateChargeId,
                customerId = customerId,
                instrumentId = instrumentId,
                amountMinor = item.amountMinor,
                currency = item.currency,
                idempotencyKey = req.idempotencyKey,
            ) ?: throw NotFoundException(
                "unknown_instrument",
                "No instrument $instrumentId for customer $customerId",
            )

        call.application.log.info(
            "[vault] charge ${result.record.chargeId} customer=$customerId instrument=$instrumentId" +
                if (!result.isNew) " (idempotent replay)" else "",
        )

        call.respond(result.record.toResponse())
    }
}

private fun VaultStore.InstrumentRecord.toDto() =
    SavedInstrumentDto(
        instrumentId = instrumentId,
        customerId = customerId,
        brand = brand,
        last4 = last4,
        createdAtEpochMs = createdAtEpochMs,
    )

private fun VaultStore.ChargeRecord.toResponse() =
    InstrumentChargeResponse(
        chargeId = chargeId,
        customerId = customerId,
        instrumentId = instrumentId,
        amountMinor = amountMinor,
        currency = currency,
        status = status,
        updatedAtEpochMs = updatedAtEpochMs,
    )
