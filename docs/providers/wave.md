# Wave

- **Region:** Senegal
- **Archetype:** D (async mobile money, poll) — per the plan's reference (`wl_woyo`).
- **Status shipped:** `MOCK_MODE`.
- **Docs:** https://docs.wave.com (API contract not verified against live docs this session).

Rides the generic `provider:mobile-money` archetype: `pay()` schedules the mock delayed flip and
returns `Pending` immediately; the orchestrator's existing poll-with-backoff resolves it.
