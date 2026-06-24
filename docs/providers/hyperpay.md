# HyperPay (+ Payfort/MADA)

- **Region:** MENA
- **Archetype:** C (hosted checkout) — per general hosted-checkout provider conventions, no Android SDK.
- **Status shipped:** `MOCK_MODE`.
- **Docs:** https://www.hyperpay.com (specific API endpoint/session-create contract not verified
  against live docs this session — the docs site did not resolve cleanly).

## Why MOCK_MODE

Sandbox self-serve status and the exact checkout-session API shape weren't confirmed against live
docs this pass. Ships `MOCK_MODE` + docs only, riding the generic `HostedWebViewAdapter` — honest
about what wasn't verified rather than assuming a real-API contract.
