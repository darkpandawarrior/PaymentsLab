# PhonePe

- **Region:** India
- **Archetype:** A (native SDK) in reality — this demo runs it through the generic archetype-C mock
  checkout page instead (see "Why the mock path is a simplification" below).
- **Status shipped:** `MOCK_MODE`.
- **Real SDK (verified 2026-07-03 against [`PhonePe/phonepe-pg-android-demo`](https://github.com/PhonePe/phonepe-pg-android-demo), actively maintained, pushed 2025-04-14):**
  - Gradle: `implementation("phonepe.intentsdk.android.release:IntentSDK:5.1.0")`
  - Repo: `maven { url = "https://phonepe.mycloudrepo.io/public/repositories/phonepe-intentsdk-android" }`
    (not Maven Central — a custom repo PhonePe hosts themselves)
  - Init: `PhonePeKt.init(context, merchantId, flowId, environment)` — `PhonePeEnvironment.SANDBOX`
    exists, but per the sample's own README: *"get in touch with the PhonePe integration team
    (merchant-integration@phonepe.com) to get your secret keys"* and *"without valid input this
    sample app will not work"* — **no anonymous self-serve sandbox**, confirming this stays
    `MOCK_MODE`.
  - Transaction: `PhonePeKt.startTransaction(context, request = TransactionRequest(orderId, token,
    paymentMode), activityResultLauncher)` — takes a plain `ActivityResultLauncher<Intent>`
    (`StartActivityForResult`), which fits this app's existing `AndroidPaymentHost.registerForResult`
    bridge directly if this ever gets wired for real.
  - Result: `Activity.RESULT_OK` doesn't mean success — the demo's own comment says *"RESULT_OK means
    you need to start polling for transaction status."* This is the exact same trust-boundary lesson
    every other gateway in this app teaches: client result is a hint, server polling is truth.

## The anti-pattern story — corrected from stale folklore

The original research plan flagged PhonePe with a generic "client-salt anti-pattern" note, based on
older SDK generations (and old Jugnoo-era integrations) that computed an `X-VERIFY` checksum
client-side by hashing `merchantId + endpoint + saltKey` — meaning the salt key shipped inside the
APK, crackable via decompilation. **That's no longer how the current SDK works.** IntentSDK v5 takes
an opaque `token` the *backend* must generate (presumably via PhonePe's server-side order-creation
API) — the client never sees or computes a salt. This is a genuine security improvement worth noting
accurately rather than repeating the old anti-pattern as if it still applies to today's integration.

## Why the mock path is a simplification

Wiring the real SDK requires a backend endpoint that calls PhonePe's server-side order-creation API
to mint the `orderId`/`token` pair the client SDK needs — that real backend API wasn't verified
against live docs this session (only the client SDK's demo app was), so building it would mean
guessing at a REST contract I can't confirm. Rather than fabricate that, this ships through the
generic `HostedWebViewAdapter` mock path (same as every other Tier-2/3 `MOCK_MODE` entry) — the demo
proves the archetype and catalog entry, but the real PhonePe SDK dependency isn't actually linked in
this build. A future pass should verify PhonePe's real order-creation API before wiring
`provider:phonepe` for real, following the same real/mock-degrade pattern `PaystackAdapter` uses.
