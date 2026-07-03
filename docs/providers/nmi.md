# NMI

- **Region:** Global
- **Archetype:** Tier-4 stub — catalog entry + research doc only, no working integration.
- **Status shipped:** `COMING_SOON` (`StubGateway`, no backend adapter, `pay()` always fails).

## Why stub

NMI (now part of Fortis) is a payment gateway aggregator sold through resellers/ISOs rather than a
direct-signup platform — no public Android SDK was found (verified: 0 results on a direct Maven
Central search for "nmi android"). Their integration surface is primarily a server-side REST/XML
API ("Direct Post" / "Query" APIs) intended for a merchant's own backend, not a client-embeddable
mobile SDK.

Not built this session: a real integration would be a backend-only adapter (no client SDK at all),
calling NMI's Gateway API directly with card data collected in-app — which conflicts with this app's
PCI-scope-avoidance principle (card data should tokenize client-side, never transit the backend
raw). Would need NMI's separate Collect.js/tokenization product to do this safely; not evaluated
this session.
