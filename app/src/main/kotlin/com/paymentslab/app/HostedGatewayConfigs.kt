package com.paymentslab.app

import com.paymentslab.core.paymentsapi.Capability
import com.paymentslab.core.paymentsapi.GatewayId
import com.paymentslab.core.paymentsapi.GatewayStatus
import com.paymentslab.provider.hostedwebview.HostedGatewayConfig
import com.paymentslab.provider.hostedwebview.ReturnUrlMatchers

/**
 * The B1 vertical slice: Paystack riding `provider:hosted-webview` end-to-end. `buildCheckoutUrl`
 * is a pure passthrough — the backend already resolved the real `checkout_url` (either a genuine
 * Paystack `authorization_url` or the generic `/mock/checkout/paystack` fallback, see
 * `PaystackAdapter`); the client never constructs gateway URLs itself.
 *
 * `status = MOCK_MODE` until a real `PLAB_PAYSTACK_TEST_SECRET_KEY` is configured on the backend —
 * honest by default rather than claiming sandbox-readiness this app can't currently prove.
 */
val paystackHostedGatewayConfig =
    HostedGatewayConfig(
        gatewayId = GatewayId("paystack"),
        displayName = "Paystack",
        region = "Africa",
        docsPath = "docs/providers/paystack.md",
        blurb =
            "Hosted checkout via Paystack's Standard Checkout. The archetype-C vertical slice: " +
                "one WebView + return-URL interception, no per-gateway module.",
        capabilities = setOf(Capability.ONE_TIME_PAYMENT, Capability.CARDS),
        status = GatewayStatus.MOCK_MODE,
        buildCheckoutUrl = { params -> params["checkout_url"].orEmpty() },
        matchReturn =
            ReturnUrlMatchers.byMarker(
                successMarker = "/mock/return/success",
                failureMarker = "/mock/return/failure",
            ),
    )

/**
 * Shared shape for every B2 fan-out gateway shipping `MOCK_MODE` on the generic
 * `HostedWebViewAdapter`: passthrough `checkout_url`, the standard return-URL markers. Only the
 * identity/blurb differs per gateway — see each `docs/providers/<id>.md` for why it's mock, not real.
 */
private fun mockHostedGatewayConfig(
    id: String,
    displayName: String,
    region: String,
    blurb: String,
    capabilities: Set<Capability> = setOf(Capability.ONE_TIME_PAYMENT, Capability.CARDS),
) = HostedGatewayConfig(
    gatewayId = GatewayId(id),
    displayName = displayName,
    region = region,
    docsPath = "docs/providers/$id.md",
    blurb = blurb,
    capabilities = capabilities,
    status = GatewayStatus.MOCK_MODE,
    buildCheckoutUrl = { params -> params["checkout_url"].orEmpty() },
    matchReturn =
        ReturnUrlMatchers.byMarker(
            successMarker = "/mock/return/success",
            failureMarker = "/mock/return/failure",
        ),
)

/**
 * B2 fan-out slice 1: Mollie. Not upgradeable to real like Paystack in this pass — Mollie requires a
 * registered business account before issuing test API keys, so there's no self-serve sandbox to
 * wire; see `docs/providers/mollie.md`. Proves the fan-out is mechanical: the generic
 * `HostedWebViewAdapter` built in B0 needed zero changes to add this gateway.
 */
val mollieHostedGatewayConfig =
    mockHostedGatewayConfig(
        id = "mollie",
        displayName = "Mollie",
        region = "EU",
        blurb =
            "Hosted checkout via Mollie's Payments API. Real integration would call " +
                "POST /v2/payments and redirect to _links.checkout — MOCK_MODE here since Mollie " +
                "needs a registered business account before issuing test keys.",
    )

/** B2 batch 2 — see docs/providers/culqi.md, ozow.md, sslcommerz.md, bkash.md for per-gateway notes. */
val culqiHostedGatewayConfig =
    mockHostedGatewayConfig(
        id = "culqi",
        displayName = "Culqi",
        region = "Peru",
        blurb =
            "Hosted checkout via Culqi's Checkout v4 (WebView + Culqi3DS). Self-serve test " +
                "keys exist (CulqiPanel) — a good future upgrade to real, not wired yet this pass.",
    )

val ozowHostedGatewayConfig =
    mockHostedGatewayConfig(
        id = "ozow",
        displayName = "Ozow",
        region = "South Africa",
        blurb =
            "Hosted instant-EFT redirect. Staging is merchant-approval-gated — no anonymous " +
                "self-serve sandbox exists for this app to wire.",
    )

val sslcommerzHostedGatewayConfig =
    mockHostedGatewayConfig(
        id = "sslcommerz",
        displayName = "SSLCommerz",
        region = "Bangladesh",
        blurb =
            "Hosted checkout. A sandbox registration flow exists, but the real session-create " +
                "contract wasn't verified against live docs this pass.",
    )

val bkashHostedGatewayConfig =
    mockHostedGatewayConfig(
        id = "bkash",
        displayName = "bKash",
        region = "Bangladesh",
        blurb =
            "Hosted/URL-based checkout. Sandbox self-serve status wasn't confirmed against " +
                "live docs this pass.",
    )

/**
 * B2 batch 3 — see `docs/providers/<id>.md` for each. All unverified-against-live-docs this pass
 * (several doc sites 404'd or didn't resolve); archetype classification is a best-effort guess
 * from general hosted-checkout conventions, not a live API check. `MOCK_MODE` only, no
 * fabricated real-API shapes.
 */
val hyperpayHostedGatewayConfig =
    mockHostedGatewayConfig(
        id = "hyperpay",
        displayName = "HyperPay",
        region = "MENA",
        blurb = "Hosted checkout (+ Payfort/MADA). API contract not verified against live docs this pass.",
    )

val telrHostedGatewayConfig =
    mockHostedGatewayConfig(
        id = "telr",
        displayName = "Telr",
        region = "MENA",
        blurb = "Hosted checkout. API contract not verified against live docs this pass.",
    )

val myfatoorahHostedGatewayConfig =
    mockHostedGatewayConfig(
        id = "myfatoorah",
        displayName = "MyFatoorah",
        region = "Kuwait/MENA",
        blurb = "Hosted checkout (InvoiceURL-style redirect). API contract not verified this pass.",
    )

val paywayHostedGatewayConfig =
    mockHostedGatewayConfig(
        id = "payway",
        displayName = "PayWay",
        region = "Cambodia",
        blurb = "Hosted checkout. API contract not verified against live docs this pass.",
    )

val wipayHostedGatewayConfig =
    mockHostedGatewayConfig(
        id = "wipay",
        displayName = "WiPay",
        region = "Caribbean",
        blurb = "Hosted checkout. API contract not verified against live docs this pass.",
    )

val paymarkHostedGatewayConfig =
    mockHostedGatewayConfig(
        id = "paymark",
        displayName = "Paymark",
        region = "New Zealand",
        blurb = "Hosted checkout. API contract not verified against live docs this pass.",
    )

val vistamoneyHostedGatewayConfig =
    mockHostedGatewayConfig(
        id = "vistamoney",
        displayName = "VistaMoney",
        region = "LATAM",
        blurb = "Hosted checkout. Minimal public docs located this pass — catalog entry only.",
    )

val cmiHostedGatewayConfig =
    mockHostedGatewayConfig(
        id = "cmi",
        displayName = "CMI",
        region = "Morocco",
        blurb = "Hosted checkout. API contract not verified against live docs this pass.",
    )
