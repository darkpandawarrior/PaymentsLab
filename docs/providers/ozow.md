# Ozow

- **Region:** South Africa
- **Archetype:** C (hosted checkout / instant-EFT redirect) — no Android SDK, REST + hosted redirect.
- **Status shipped:** `MOCK_MODE`.
- **Docs:** https://ozow.com/integrations (verify before any real integration attempt — see below)

## Why MOCK_MODE

Per `research-notes.md` (2026-07-02 pass): Ozow's staging environment (`stagingapi.ozow.com`) is
merchant-approval-gated, not an anonymous self-serve sandbox. There's no zero-KYC test path a solo
developer can wire against, so this ships as `MOCK_MODE` + docs only, riding the generic
`HostedWebViewAdapter` — same shape as every other Tier-3 config, just honestly labeled.
