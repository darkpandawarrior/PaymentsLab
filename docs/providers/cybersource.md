# Cybersource

- **Region:** Global
- **Archetype:** Tier-4 stub — catalog entry + research doc only, no working integration.
- **Status shipped:** `COMING_SOON` (`StubGateway`, no backend adapter, `pay()` always fails).

## Correction to the "no SDK" assumption

A real Maven Central artifact **does** exist: `com.cybersource:flex-api-android-client:1.0.2`
(verified via a direct Maven Central search — not assumed from Cybersource's own docs). This is
Cybersource's **Flex Microform** client-side tokenization SDK (captures card data into a token
without it transiting the merchant's server), not a full hosted-checkout UI like Stripe/Razorpay.

This session shipped it as a Tier-4 stub (catalog visibility + this doc) rather than a working
`provider:cybersource` module — real wiring is future work, not done here. A real integration would
need: `Client.createPayment()`-style Flex Microform token capture client-side, then a backend
adapter calling the Cybersource REST API (HMAC-SHA256 request signing) with that token to create the
actual payment.
