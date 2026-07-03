# Midtrans

- **Region:** Indonesia
- **Archetype:** C (hosted checkout — Snap Checkout) — rides the generic `provider:hosted-webview`
  config, no new client module.
- **Status shipped:** `MOCK_MODE`.

## Why archetype C, not the native SDK — deliberately

Midtrans itself announced that its native mobile SDKs — `com.midtrans:uikit` on Android, MidtransKit
(CocoaPods) on iOS — are sunsetting starting **June 2026** (see
`docs.midtrans.com/reference/android-sdk`). Building a new integration on a deprecating SDK the
month before its own vendor stops recommending it would be building on borrowed time.

**Snap Checkout** (a hosted payment page, opened in a WebView) is Midtrans's own recommended
replacement — and it happens to be exactly this app's archetype-C shape. So this isn't a "no SDK
found, fall back to mock" story like Peach/Areeba; it's a "the real SDK exists but choosing not to
use it is the correct call" story, which is arguably the more interesting one to have documented.

**Not exercised against the live API this session** — no test credentials were available. A real
upgrade would mean a `MidtransAdapter` calling the Snap API (`POST /snap/v1/transactions`) to mint a
real `redirect_url`, following the same real/mock auto-degrade pattern as `PaystackAdapter`.
