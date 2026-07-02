# Xendit

- **Region:** Indonesia (DANA/OVO/LinkAja e-wallets)
- **Archetype:** shipped as C (hosted checkout) for catalog consistency, but the plan flags this as
  possibly archetype D (async e-wallet, no synchronous result) — Xendit's e-wallet charge flow may be
  closer to a poll/webhook shape than a redirect. Needs its own research pass to settle definitively.
- **Status shipped:** `MOCK_MODE`.
- **Docs:** https://developers.xendit.co (archetype question above not resolved against live docs
  this pass).

Ships `MOCK_MODE` + docs, generic `HostedWebViewAdapter`, with the archetype-C/D ambiguity flagged
rather than silently assumed.
