# Orange Money

- **Region:** Africa (multi-country)
- **Archetype:** D (async mobile money, poll) — per the plan's reference (`wl_woyo`).
- **Status shipped:** `MOCK_MODE`.
- **Docs:** https://developer.orange.com (API contract not verified against live docs this session).

Rides the generic `provider:mobile-money` archetype: `pay()` schedules the mock delayed flip and
returns `Pending` immediately; the orchestrator's existing poll-with-backoff resolves it.
