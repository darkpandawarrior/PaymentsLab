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
 * B2 fan-out slice 1: Mollie. `MOCK_MODE` only (not upgradeable to real like Paystack) — Mollie
 * requires a registered business account before issuing test API keys, so there's no self-serve
 * sandbox for this app to wire; see `docs/providers/mollie.md`. Proves the fan-out is mechanical:
 * the generic `HostedWebViewAdapter` built in B0 needed zero changes to add this gateway.
 */
val mollieHostedGatewayConfig =
    HostedGatewayConfig(
        gatewayId = GatewayId("mollie"),
        displayName = "Mollie",
        region = "EU",
        docsPath = "docs/providers/mollie.md",
        blurb =
            "Hosted checkout via Mollie's Payments API. Real integration would call " +
                "POST /v2/payments and redirect to _links.checkout — MOCK_MODE here since Mollie " +
                "needs a registered business account before issuing test keys.",
        capabilities = setOf(Capability.ONE_TIME_PAYMENT, Capability.CARDS),
        status = GatewayStatus.MOCK_MODE,
        buildCheckoutUrl = { params -> params["checkout_url"].orEmpty() },
        matchReturn =
            ReturnUrlMatchers.byMarker(
                successMarker = "/mock/return/success",
                failureMarker = "/mock/return/failure",
            ),
    )
