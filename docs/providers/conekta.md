# Conekta

- **Region:** Mexico
- **Archetype:** C (hosted checkout) — rides the generic `provider:hosted-webview` config, no new
  client module (B2-style Tier-3 fan-out).
- **Status shipped:** `MOCK_MODE`.

## Why mock (and the separate KMP-native track)

Conekta's public SDKs found on Maven Central (`io.conekta:conekta-java`, `conektasdk`) are
server-side Java clients, not an Android UI SDK — so this entry ships as the same generic Tier-3
hosted-checkout config every other MOCK_MODE gateway uses.

Separately, the plan tracks a **Conekta Elements** KMP-native showcase (commonMain card
tokenization + AES/RSA + a CMP UI) as a Tier-1 architecture proof — that work demonstrates
PaymentsLab-KMP's "commonMain payment logic" thesis and is **not** what this entry represents. See
`research-notes.md` for that track's status; this `conekta` gateway id is the plain hosted-checkout
fallback only.

**Not exercised against the live API this session** — no test credentials were available.
