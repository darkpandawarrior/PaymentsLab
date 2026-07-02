# Telr

- **Region:** MENA
- **Archetype:** C (hosted checkout) — per Jugnoo's reference integration (`f_telr`), no Android SDK.
- **Status shipped:** `MOCK_MODE`.
- **Docs:** https://docs.telr.com (specific API/session contract not verified against live docs this
  session — the reference page returned a 404).

## Why MOCK_MODE

Same reasoning as HyperPay/MyFatoorah/PayWay in this batch: sandbox self-serve status and the exact
checkout-session contract weren't confirmed this pass. `MOCK_MODE` + docs only, generic
`HostedWebViewAdapter`.
