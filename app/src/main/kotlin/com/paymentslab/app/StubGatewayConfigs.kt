package com.paymentslab.app

import com.paymentslab.core.paymentsapi.GatewayId
import com.paymentslab.core.paymentsapi.PaymentGateway
import com.paymentslab.core.paymentsapi.StubGateway
import com.paymentslab.core.paymentsapi.StubGatewayConfig
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Tier-4 "stub/docs-only" fan-out (B5): catalog visibility + a web-researched doc, no working
 * integration. See each `docs/providers/<id>.md`.
 */
private fun stubConfig(
    id: String,
    displayName: String,
    region: String,
    blurb: String,
) = StubGatewayConfig(
    id = GatewayId(id),
    displayName = displayName,
    region = region,
    docsPath = "docs/providers/$id.md",
    blurb = blurb,
)

val cybersourceStubConfig =
    stubConfig(
        id = "cybersource",
        displayName = "Cybersource",
        region = "Global",
        blurb = "Visa-owned enterprise payment platform — REST/SOAP APIs, no consumer-facing Android SDK.",
    )

val nmiStubConfig =
    stubConfig(
        id = "nmi",
        displayName = "NMI",
        region = "Global",
        blurb = "Payment gateway aggregator (Fortis) — server-side/reseller-integration model, no public Android SDK.",
    )

val selcomStubConfig =
    stubConfig(
        id = "selcom",
        displayName = "Selcom",
        region = "Tanzania",
        blurb = "Tanzanian mobile money + card PSP — hosted checkout/USSD only, no public Android SDK found.",
    )

val supaGhanaPayStubConfig =
    stubConfig(
        id = "supaghanapay",
        displayName = "Supa Ghana Pay",
        region = "Ghana",
        blurb = "Ghanaian mobile money aggregator — no public documentation or Android SDK found this session.",
    )

/** One [PaymentGateway] per [StubGatewayConfig] — mirrors `mobileMoneyModule`'s list-driven shape. */
fun stubGatewayModule(configs: List<StubGatewayConfig>) =
    module {
        configs.forEach { config ->
            single<PaymentGateway>(qualifier = named(config.id.value)) { StubGateway(config) }
        }
    }
