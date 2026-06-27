# Areeba

- **Region:** Lebanon/Levant
- **Archetype:** C (hosted checkout) — rides the generic `provider:hosted-webview` config, no new
  client module (B2-style Tier-3 fan-out).
- **Status shipped:** `MOCK_MODE` — no public Android SDK found.

## Why mock

Areeba (the Levant-region card acquirer/PSP spun out of BLOM Bank) integrates via a hosted payment
page (MasterCard Payment Gateway Services-based, similar to other MPGS-family acquirers) — no public
Android SDK was found (verified: 0 results on a direct Maven Central search for "areeba"). The
generic `HostedWebViewAdapter` / `HostedGatewayConfig` pair covers this shape exactly.

**Not exercised against the live API this session** — no test credentials were available, and
Areeba's merchant onboarding is regionally gated (Lebanon/Levant business registration), unlike
Paystack/PayPal/Omise's self-serve global sandbox signup.
