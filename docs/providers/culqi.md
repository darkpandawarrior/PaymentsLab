# Culqi

- **Region:** Peru (LATAM)
- **Archetype:** C (hosted checkout) — Checkout v4 is a WebView + Culqi3DS; the legacy 2016
  `conektasdk`-style native SDK is dead, do not port it.
- **Status shipped:** `MOCK_MODE`.
- **Docs:** https://docs.culqi.com/es/documentacion/checkout/

## Why MOCK_MODE, not SANDBOX_READY (yet)

Per `research-notes.md` (2026-07-02 pass), Culqi test keys are genuinely self-serve via CulqiPanel —
no business KYC needed, closer to Razorpay/Stripe/Paystack than to Mollie. This gateway is a good
candidate for a future real-API upgrade (mirroring `PaystackAdapter`'s real/mock split). Shipped
`MOCK_MODE` in this batch to keep the fan-out pace mechanical (generic `HostedWebViewAdapter`, no new
backend code) rather than doing per-gateway real-API integration for every entry in one pass.

## Real integration shape (future upgrade)

Checkout v4 is a hosted WebView, not a REST-create-then-redirect flow like Paystack/Mollie — the
checkout token/session is created server-side and the WebView loads Culqi's own checkout UI directly
rather than redirecting to an `authorization_url`. Needs its own research pass before wiring for real.
