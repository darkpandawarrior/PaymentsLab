# MTN MoMo

- **Region:** Africa (multi-country)
- **Archetype:** D (async mobile money) — poll-based per the plan's confirm note ("poll 15s").
- **Status shipped:** `MOCK_MODE`.
- **Docs:** https://momodeveloper.mtn.com (API contract not verified against live docs this session).

Rides the generic `provider:mobile-money` archetype: `pay()` schedules the mock delayed flip and
returns `Pending` immediately; the orchestrator's existing poll-with-backoff resolves it.
