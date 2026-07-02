# M-Pesa

- **Region:** Kenya/Tanzania
- **Archetype:** D (async mobile money) — an STK push to the payer's phone; no synchronous client
  result exists at all, per the plan's own confirm note ("FCM push").
- **Status shipped:** `MOCK_MODE`.
- **Docs:** https://developer.safaricom.co.ke (Daraja API; real integration contract not verified
  against live docs this session).

Rides the generic `provider:mobile-money` archetype: `pay()` schedules the mock delayed flip and
returns `Pending` immediately; the orchestrator's existing poll-with-backoff resolves it. A real
integration would replace the mock flip with Safaricom's Daraja STK-push callback.
