# AcceptCard

- **Region:** Global
- **Archetype:** C (hosted checkout) — the plan flags this as likely Mastercard Payment Gateway
  Services (MPGS) Hosted Checkout, a real, well-known global product, but that identification wasn't
  confirmed against live MPGS docs this session (the reference URL didn't resolve).
- **Status shipped:** `MOCK_MODE`.
- **Docs:** MPGS docs are typically bank-hosted (e.g. `<bank>.gateway.mastercard.com`) rather than a
  single global URL — not tracked down this session.

Catalog entry per the plan's "nothing dropped silently" rule. `MOCK_MODE`, generic
`HostedWebViewAdapter`. If the MPGS identification is confirmed later, this is a strong candidate for
a real-API upgrade (MPGS Hosted Checkout has genuine self-serve test merchant accounts with some
acquirers).
