# PayPal

- **Region:** Global
- **Archetype:** C (hosted checkout, REST — no Android SDK needed for the approval step) — rides
  `provider:hosted-webview` directly, no new client module.
- **Status shipped:** `MOCK_MODE` by default, upgrades to real when configured (same pattern as
  `PaystackAdapter`).
- **Docs:** https://developer.paypal.com/docs/api/orders/v2/

## Real vs mock

`PayPalAdapter` (backend) checks `PLAB_PAYPAL_TEST_CLIENT_ID` + `PLAB_PAYPAL_TEST_CLIENT_SECRET`:

- **Set:** fetches an OAuth token (`POST /v1/oauth2/token`, HTTP Basic `client_id:client_secret`),
  then `POST /v2/checkout/orders` and returns the `approve` link from the response's `links` array as
  `checkout_url` — the client WebView just opens that real PayPal sandbox URL. `verify` captures the
  order (`POST /v2/checkout/orders/{id}/capture`) using the PayPal order id and maps `COMPLETED` to
  SUCCESS.
- **Unset (default):** falls back to the generic `/mock/checkout/paypal` path.

**Not yet exercised against the live sandbox** — no test credentials were available this session.
Request/response mapping is verified with 4 Ktor `MockEngine` tests
(`backend/src/test/kotlin/com/paymentslab/backend/PayPalAdapterTest.kt`) covering the OAuth token
fetch, order creation, capture, and the mock fallback.

## Getting real sandbox credentials

Sign up at https://developer.paypal.com — sandbox Client ID/Secret are self-serve, no business
approval needed (confirmed 2026-07-03). Set `PLAB_PAYPAL_TEST_CLIENT_ID` and
`PLAB_PAYPAL_TEST_CLIENT_SECRET` on the backend.

## Known gap in the real path

PayPal's real hosted checkout redirects back with its own query params (`?token=<order_id>&PayerID=`),
not this app's usual `?payment_id=`. The client's `matchReturn` currently only extracts
`payment_id` (matching the mock path, which is what's actually been exercised). Wiring real
credentials end-to-end would need `matchReturn` to read `token` instead when in real mode — a small,
bounded fix, not done this pass since there's no live sandbox to verify it against yet.

