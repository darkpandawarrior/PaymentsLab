# bKash

- **Region:** Bangladesh
- **Archetype:** C (hosted/URL-based checkout) — bKash's own docs describe "Hosted Checkout",
  "Tokenized Checkout", and "Checkout (URL Based)" patterns rather than a single native SDK.
- **Status shipped:** `MOCK_MODE`.
- **Docs:** https://developer.bka.sh/

## Why MOCK_MODE

Sandbox/test-account self-serve status wasn't confirmed against live docs in this pass — bKash's
developer portal describes the integration shapes but not the account-approval process. Ships
`MOCK_MODE` + docs only, riding the generic `HostedWebViewAdapter`, honest about the unverified
sandbox-access question rather than assuming it's open.
