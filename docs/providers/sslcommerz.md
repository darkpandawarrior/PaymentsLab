# SSLCommerz

- **Region:** Bangladesh
- **Archetype:** C (hosted checkout) — no Android SDK.
- **Status shipped:** `MOCK_MODE`.
- **Docs:** https://developer.sslcommerz.com/

## Why MOCK_MODE, not SANDBOX_READY (yet)

SSLCommerz does advertise a sandbox registration flow (`developer.sslcommerz.com/registration/`),
which looks more self-serve than Ozow's merchant-approval-gated staging — but the exact integration
shape (session-create endpoint, redirect URL field name) wasn't verified against live docs in this
pass, only confirmed to exist. Shipped `MOCK_MODE` for now rather than guessing at an unverified real
API shape; a future pass should register for the sandbox and verify the real create-session/redirect
contract before upgrading this to a Paystack-style real+mock adapter.
