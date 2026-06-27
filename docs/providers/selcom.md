# Selcom

- **Region:** Tanzania
- **Archetype:** Tier-4 stub — catalog entry + research doc only, no working integration.
- **Status shipped:** `COMING_SOON` (`StubGateway`, no backend adapter, `pay()` always fails).

## Why stub

Selcom is a Tanzanian mobile-money and card payment aggregator (wallet, USSD, and hosted checkout).
No public Android SDK was found (verified: 0 results on a direct Maven Central search for
"selcom"). Their public integration surface is a server-side REST API (order/checkout creation),
similar in shape to the archetype-C hosted-checkout gateways already built — a real integration
would likely follow the `HostedWebViewAdapter` pattern rather than need a dedicated client module,
but merchant onboarding (Tanzania business registration) makes self-serve sandbox testing unlikely,
unlike Paystack/PayPal/Omise.
