# Google Pay

- **Region:** Global
- **Archetype:** A (native SDK) — but a wallet *method*, not a settlement gateway. Google Pay hands
  back an encrypted, tokenized payment method; a real processor underneath (Stripe, Braintree, etc.)
  actually charges it. In production this app would set `gateway = "stripe"` +
  `gatewayMerchantId = <your Stripe account id>`; this build uses Google's `"example"` TEST-mode
  placeholder gateway since there's no live processor account to point at.
- **Status shipped:** `SANDBOX_READY` — genuinely different from every other gateway in this batch:
  `WalletConstants.ENVIRONMENT_TEST` needs no live merchant account or business approval. Google's own
  quickstart is explicit that TEST mode works out of the box for any developer with a Google account
  and a test card on file (emulator or real device with Play Services).
- **Docs:** https://developers.google.com/pay/api/android/guides/tutorial

## Why this isn't a third-party KMP library dependency

`khalid64927/google-apple-pay` ("KPayment") looked like a ready-made Compose Multiplatform wrapper
for exactly this, but it's **not actually usable**: verified via a direct Maven Central search
(`numFound: 0` for `google-apple-payments`), the GitHub repo has exactly one release (`0.1.0`,
2024-04-28) and hasn't been touched since — 22 stars, abandoned proof-of-concept from a single blog
post, not a maintained artifact. Adding it as a Gradle dependency would fail to resolve.

Its Apache-2.0-licensed source was still useful as a reference: the request-JSON shape
(`GooglePayRequestBuilder` in this module) and config field names (`GooglePayConfig`) are adapted
from its `PaymentsUtils`/`GooglePayConfig` — that JSON shape is Google's own standard sample request
anyway, so there wasn't a "better" version to invent. Its Fragment-based `ResolverFragment` for
handling `ResolvableApiException` was **not** ported — this app's own `AndroidPaymentHost` +
`ActivityResultContracts.StartIntentSenderForResult()` bridge (the same pattern Razorpay/Stripe
already use) is a cleaner fit for this codebase's conventions.

## Not exercised on a real device

No Android emulator/device was available in the environment this was built in, so `GooglePayGateway`
compiles and type-checks against the real `play-services-wallet` API but hasn't been run through an
actual Google Pay sheet. Worth a manual smoke test before relying on it.
