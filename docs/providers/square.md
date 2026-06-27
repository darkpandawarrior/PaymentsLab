# Square

- **Region:** Global
- **Archetype:** A (native SDK — In-App Payments Card Entry, `sqip.CardEntry`)
- **Status shipped:** `MOCK_MODE` by default, upgrades to real when configured (same pattern as
  `PaystackAdapter` / `PayPalAdapter`).
- **Docs:** https://developer.squareup.com/docs/in-app-payments-sdk/what-it-does

## SDK coordinates

`com.squareup.sdk.in-app-payments:card-entry:1.6.8` + `:nonce-api:1.6.8`, hosted at
`https://sdk.squareup.com/public/android` — **not** on Maven Central (confirmed: 0 results on a
direct Maven Central search). The version and coordinates were cross-checked against
`square/in-app-payments-android-quickstart`'s real `build.gradle`, since a naive guess
(`com.squareup:in-app-payments-android`) does not exist.

`card-entry`'s own published `.pom` omits `nonce-api` as a transitive dependency, even though
`CardEntry`'s public API (`Callback`, `Card`, the nonce result types) requires it at compile time —
found by inspecting the actual `.jar` contents (`javap`), not by trusting the pom graph. Added
explicitly in `provider/square/build.gradle.kts`.

## Known constraint: minSdk 28

`card-entry-1.6.8.aar`'s own manifest declares `minSdkVersion 28` (confirmed by extracting and
reading `AndroidManifest.xml` directly — this is a real SDK requirement, not just the quickstart
sample's chosen `minSdkVersion`). This app's `minSdk` is 24 everywhere else, so
`provider/square/src/main/AndroidManifest.xml` uses `tools:overrideLibrary="sqip.cardentry"` to let
the manifest merge succeed. **Devices on API 24–27 will crash if they reach Square's
`CardEntryActivity`** — acceptable for a lab exploring many gateways side by side, not acceptable
for a real Square integration (which would need to either raise the app's `minSdk` to 28 or gate the
Square option behind an `Build.VERSION.SDK_INT >= 28` check).

## Real vs mock

`SquareAdapter` (backend) checks `PLAB_SQUARE_TEST_APPLICATION_ID` / `_ACCESS_TOKEN` / `_LOCATION_ID`:

- **Set:** `createProviderOrder` hands the (non-secret, client-embeddable) `application_id` down to
  the client via `providerParams`; the client's `SquareGateway` calls
  `InAppPaymentsSdk.setSquareApplicationId(...)` and launches the real `CardEntryActivity`.
  `verify` charges the resulting nonce (`POST /v2/payments`, Square sandbox host) using the access
  token, which never leaves the backend, and maps `COMPLETED` to SUCCESS. The Payments API has no
  "create intent" step, so the charge amount is cached per-orderId in-memory between
  `createProviderOrder` and `verify`.
- **Unset (default):** `createProviderOrder` returns no `application_id`; the client's
  `SquareGateway.pay` sees that absence and runs `SimulatedPayment` instead of touching the real SDK
  at all.

**Not yet exercised against the live sandbox** — no test credentials were available this session.
Request/response mapping is verified with 5 Ktor `MockEngine` tests
(`backend/src/test/kotlin/com/paymentslab/backend/SquareAdapterTest.kt`).

## Client-side bridge: legacy `onActivityResult`

Square's SDK predates AndroidX `ActivityResultContract` — `CardEntry.startCardEntryActivity` calls
`Activity.startActivityForResult` directly (confirmed via `javap` on the published SDK — its public
API is `startCardEntryActivity(Activity, ...)` / `handleActivityResult(Intent, Callback<...>)`, the
old two-method shape, not a registered launcher). `MainActivity` overrides the deprecated
`onActivityResult` to forward into `CardEntry.handleActivityResult`, which then emits to
`SquareCallbackRelay` — the same process-scoped single-slot relay pattern as
`RazorpayCallbackRelay`, and for the same underlying reason (an Activity-callback-era SDK that can't
be adapted to `AndroidPaymentHost.registerForResult`'s `ActivityResultContract` shape).

## Getting real sandbox credentials

Sign up at https://developer.squareup.com/apps — every application gets a free Sandbox
Application ID, Access Token, and test Location ID with no business verification required. Set
`PLAB_SQUARE_TEST_APPLICATION_ID`, `PLAB_SQUARE_TEST_ACCESS_TOKEN`, and
`PLAB_SQUARE_TEST_LOCATION_ID` on the backend.
