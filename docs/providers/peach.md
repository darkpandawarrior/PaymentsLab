# Peach Payments

- **Region:** Africa/Global
- **Archetype:** C (hosted checkout) — rides the generic `provider:hosted-webview` config, no new
  client module (B2-style Tier-3 fan-out).
- **Status shipped:** `MOCK_MODE` — no public Android SDK found.

## Why mock

Peach Payments' integration surface is a hosted Checkout page + server-side REST API
(`checkout.peachpayments.com`) — there is no public Android SDK (verified: 0 results on a direct
Maven Central search for "peach payments android"). The generic `HostedWebViewAdapter` /
`HostedGatewayConfig` pair used by every other Tier-3 gateway already covers this shape exactly:
`checkout_url` passthrough to a WebView, return-URL marker interception.

**Not exercised against the live API this session** — no test credentials were available. Real
upgrade would mean a `PeachAdapter` calling their Checkout API to mint a real `checkoutId`/redirect
URL, following the same real/mock auto-degrade pattern as `PaystackAdapter`.
