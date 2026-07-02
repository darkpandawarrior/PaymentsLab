# CardNet

- **Region:** Caribbean
- **Archetype:** C (hosted checkout) — but the plan flags CardNet specifically as needing a
  **JS-bridge** (`f_cardnet_payment_3.0`), not a plain return-URL redirect. This app's
  `HostedWebViewGateway`/`HostedCheckoutScreen` only support return-URL interception today, not an
  injected JS bridge — CardNet needs that capability added to `provider:hosted-webview` before it can
  run anything beyond the generic mock path.
- **Status shipped:** `MOCK_MODE`.
- **Docs:** not located this session.

Ships as a catalog entry + `MOCK_MODE` on the generic adapter; the JS-bridge gap is real and called
out here rather than silently pretending return-URL interception is sufficient for this gateway.
