# Beyonic (+ wallet)

- **Region:** Africa
- **Archetype:** D (async mobile money, poll) — per the plan's reference (`f_beyonic`).
- **Status shipped:** `MOCK_MODE`.
- **Docs:** not located this session — minimal public English-language developer documentation.

Rides the generic `provider:mobile-money` archetype: `pay()` schedules the mock delayed flip and
returns `Pending` immediately; the orchestrator's existing poll-with-backoff resolves it.
