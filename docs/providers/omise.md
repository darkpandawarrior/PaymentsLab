# Omise

- **Region:** SEA (Thailand)
- **Archetype:** A (native SDK — `co.omise:omise-android`'s `CreditCardActivity`)
- **Status shipped:** `MOCK_MODE` by default, upgrades to real when configured (same pattern as
  `PaystackAdapter` / `PayPalAdapter` / `SquareAdapter`).
- **Docs:** https://www.omise.co/tokens-api

## SDK version

`co.omise:omise-android:5.6.0` — on Maven Central (unlike Square). The SDK's own README's install
snippet is stale (`4.3.1`); the latest GitHub release (`v6.0.0-alpha.3`) is an alpha not published to
Maven Central, so `5.6.0` (the latest stable version actually resolvable from Maven Central) was
used instead — verified by querying Maven Central directly (`solrsearch`), not by trusting either
the README or GitHub's "latest release" label.

## Real vs mock

`OmiseAdapter` (backend) checks `PLAB_OMISE_TEST_PUBLIC_KEY` / `_SECRET_KEY`:

- **Set:** `createProviderOrder` hands the (non-secret, client-embeddable) public key down to the
  client via `providerParams`; the client's `OmiseGateway` launches the real `CreditCardActivity`
  with `EXTRA_PKEY`. `verify` charges the resulting token (`POST /charges`, HTTP Basic auth with the
  secret key as username / empty password — Omise's own convention) using the secret key, which
  never leaves the backend, and maps `status == "successful" && paid` to SUCCESS. The Charges API has
  no "create intent" step, so the charge amount is cached per-orderId in-memory between
  `createProviderOrder` and `verify` (same approach as `SquareAdapter`).
- **Unset (default):** `createProviderOrder` returns no `public_key`; the client's `OmiseGateway.pay`
  sees that absence and runs `SimulatedPayment` instead of touching the real SDK at all.

**Not yet exercised against the live sandbox** — no test credentials were available this session.
Request/response mapping is verified with 5 Ktor `MockEngine` tests
(`backend/src/test/kotlin/com/paymentslab/backend/OmiseAdapterTest.kt`).

## Client-side: no legacy bridge needed

Unlike Square, Omise's `CreditCardActivity` already speaks `ActivityResultContract` natively (its own
README recommends `registerForActivityResult`), so `OmiseGateway.pay` rides
`AndroidPaymentHost.registerForResult` directly — no relay object, no `MainActivity.onActivityResult`
override. This is the cleanest of the three Tier-1 native-SDK integrations built this session.

## Getting real sandbox credentials

Sign up at https://dashboard.omise.co/signup — every account gets test-mode public/secret keys
immediately, no business verification required. Set `PLAB_OMISE_TEST_PUBLIC_KEY` and
`PLAB_OMISE_TEST_SECRET_KEY` on the backend.
