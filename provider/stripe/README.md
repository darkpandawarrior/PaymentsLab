# provider:stripe

Stripe provider for PaymentsLab, built on Stripe's [PaymentSheet]. Implements the frozen
`core:payments-api` `PaymentGateway` contract. `id = stripe`, region `Global`, capabilities
`ONE_TIME_PAYMENT / CARDS / WALLET`, status `SANDBOX_READY`.

## Why Stripe is the easy sandbox

Stripe test mode is **indefinite and needs no KYC** — create an account, grab the test publishable
key + secret key, and you can run real 3DS2 challenges and Google Pay flows forever without business
onboarding. This is why Stripe is one of the two fully-runnable providers in the showcase.

## The two-step contract

- **`prepare(created)`** repackages `CreatedOrder.providerParams` into a `PreparedPayment`. Stripe's
  session material is the **PaymentIntent `client_secret`** plus the **`publishable_key`**, both
  minted server-side in `POST /orders`. The client never creates them. Missing either throws
  `PaymentPreparationException`.
- **`pay(host, prepared)`** casts the host to `AndroidPaymentHost`, calls
  `PaymentConfiguration.init(activity, publishableKey)`, then presents PaymentSheet with the client
  secret and bridges the `PaymentSheetResult` callback into a `suspendCancellableCoroutine`.

## PaymentSheet-in-Compose wiring (the important bit)

`PaymentSheet` registers an `ActivityResultLauncher` internally, so it **must be created before the
host reaches `STARTED`** — i.e. in an `Activity`/Compose scope, not deep inside a suspend function.
The gateway therefore owns nothing but a relay: `StripePaymentLauncherHost`. The app owns the
`PaymentSheet`.

Wire it once in your hosting composable (or `MainActivity`):

```kotlin
@Composable
fun PaymentHostSurface() {
    val launcherHost: StripePaymentLauncherHost = koinInject()

    // rememberPaymentSheet builds the sheet in Compose scope and routes its result back.
    val paymentSheet = rememberPaymentSheet(
        paymentResultCallback = { result -> launcherHost.onResult(result) },
    )

    DisposableEffect(paymentSheet) {
        launcherHost.attach { clientSecret, configuration ->
            paymentSheet.presentWithPaymentIntent(clientSecret, configuration)
        }
        onDispose { launcherHost.detach() }
    }

    // ... the rest of your checkout UI; the orchestrator drives StripeGateway.pay(host, prepared)
}
```

The imperative equivalent (Activity `onCreate`, before `STARTED`):

```kotlin
val paymentSheet = PaymentSheet.Builder(
    resultCallback = { result -> launcherHost.onResult(result) },
).build(this)
launcherHost.attach { secret, config -> paymentSheet.presentWithPaymentIntent(secret, config) }
```

`StripePaymentLauncherHost` is single-flight: `present()` fails fast if a payment is already in
flight, and `onResult()` clears the listener after firing, so a duplicated or late SDK callback can
never resume the coroutine twice. Coroutine cancellation clears the pending listener via
`invokeOnCancellation`.

## Result mapping

| PaymentSheetResult | PaymentResult | Notes |
|---|---|---|
| `Completed` | `Success` | `paymentId` = PaymentIntent id derived from the client-secret prefix (`pi_XXX_secret_YYY` → `pi_XXX`); `verification = { payment_intent, client_secret_present:"true" }` |
| `Canceled` | `Cancelled` | user dismissed the sheet |
| `Failed(error)` | `Failure(GATEWAY_DECLINED, …)` | declined card / SDK error; raw carries masked error + type. See the `TODO(failure-taxonomy)` in `StripeGateway` — narrowing to `SDK_ERROR` for integration faults needs Stripe error-type inspection |

## Google Pay via Stripe

Google Pay is enabled as a PaymentSheet option in the **Test** environment
(`GooglePayConfiguration.Environment.Test`). **Google Pay here rides Stripe as the gateway of
record** — it is not a separate provider; Stripe confirms the same PaymentIntent. The merchant
`countryCode` is a showcase default (`IN` for INR, else `US`) — see `TODO(country)` in the gateway.
`play-services-wallet` is on the classpath for this path.

## 3DS2 test cards

Stripe's test PANs trigger the on-device 3D Secure 2 challenge sheet:

- `4000 0025 0000 3155` — requires 3DS2 authentication on every transaction.
- `4000 0000 0000 3220` — 3DS2 challenge flow.
- `4242 4242 4242 4242` — succeeds with no authentication (baseline).

Any future expiry + any CVC + any postal code.

## The client result is a hint, not proof

A `Success` from this gateway means only that PaymentSheet reported `Completed`. The **orchestrator
always confirms server-side** (PaymentIntent status + webhook reconciliation) before treating the
payment as real. The `verification` map is forwarded to `PaymentBackend.verify`; the human-readable
`raw` payload is `Redactor`-gated and safe to render/log (the client secret is auto-masked).

## No secrets in code

Publishable key and client secret arrive at runtime in `providerParams`. Nothing is hardcoded here.
