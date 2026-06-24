# Paystack

- **Region:** Africa
- **Archetype:** C (hosted checkout) — no Android SDK; REST + redirect, the B1 vertical-slice flagship.
- **Status shipped:** `MOCK_MODE` by default, upgrades to real when configured (see below).
- **Docs:** https://paystack.com/docs/payments/accept-payments/

## Real vs mock

`PaystackAdapter` (backend) checks `PLAB_PAYSTACK_TEST_SECRET_KEY` (resolved via `core:config`'s
`EnvCredentialStore`, naming convention `PLAB_<GATEWAY>_<MODE>_<KEY>`):

- **Set:** `createProviderOrder` calls the real `POST /transaction/initialize` and returns Paystack's
  genuine `authorization_url`; `verify` calls the real `GET /transaction/verify/{reference}` and maps
  `success`/`failed`/`abandoned` to `PaymentStatusDto`.
- **Unset (default):** falls back to the generic `GET /mock/checkout/paystack` path every other
  `MOCK_MODE` hosted gateway rides.

**Not yet exercised against the live API in this repo** — no test key was available when this was
built. The request/response mapping is instead verified against a mocked HTTP engine
(`backend/src/test/kotlin/com/paymentslab/backend/PaystackAdapterTest.kt`).

## The trust-boundary gotcha

Paystack's checkout **always** redirects to the one `callback_url` you configured, regardless of
whether the payment succeeded or failed — there's no separate success/failure redirect URL the way a
naive integration might assume. The redirect landing on `/mock/return/success` is therefore just a
page label, not a security decision. Only `GET /transaction/verify/{reference}` is authoritative;
the client's return-URL is a hint, exactly like every other gateway in this app.

## To get a real test key

Sign up at https://dashboard.paystack.com — test-mode secret/public keys are self-serve, no business
KYC required. Set `PLAB_PAYSTACK_TEST_SECRET_KEY` in the backend's environment. Note Paystack's
primary test currencies are NGN/GHS/ZAR/USD/KES — the catalog's `ebook_usd_9` item (USD) is the one
to test with; the INR items will likely be rejected by the real API.
