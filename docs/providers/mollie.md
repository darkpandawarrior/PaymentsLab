# Mollie

- **Region:** EU
- **Archetype:** C (hosted checkout) — no Android SDK; Mollie is a pure REST + redirect API.
- **Status shipped:** `MOCK_MODE` (Tier 2 per the plan — see `all-gateways-plan.md` Part D).
- **Docs:** https://docs.mollie.com/reference/create-payment

## Why MOCK_MODE, not SANDBOX_READY

Mollie's real flow is `POST /v2/payments` (amount, description, `redirectUrl`) → redirect the
payer to the `_links.checkout` URL in the response → Mollie redirects back to `redirectUrl`
regardless of outcome (same "hint, not truth" shape as Paystack) → the merchant calls
`GET /v2/payments/{id}` to get the authoritative status.

This app ships Mollie as `MOCK_MODE` rather than wiring the real API (the way `PaystackAdapter`
does) because Mollie account creation requires a registered business/organization before test API
keys are issued — there's no anonymous, instant self-serve test key the way Razorpay/Stripe/Paystack
offer. It rides the generic `HostedWebViewAdapter` + `/mock/checkout/{provider}` path instead, so the
full lifecycle (order → checkout page → return → settle) is still demoable end-to-end.

## If real keys become available later

Swap `HostedWebViewAdapter` for a dedicated `MollieAdapter` mirroring `PaystackAdapter`'s shape:
`createProviderOrder` calls `POST /v2/payments` and returns `_links.checkout.href` as `checkout_url`;
`verify` calls `GET /v2/payments/{id}` and maps Mollie's `status` (`paid`/`failed`/`expired`/`canceled`
vs. `open`/`pending`) into `PaymentStatusDto`.
