# provider:cashfree

Cashfree provider for PaymentsLab, built on the **nextgen SDK** (`com.cashfree.pg.*`,
`CFPaymentGatewayService`). Implements the frozen `core:payments-api` `PaymentGateway` contract.
`id = cashfree`, region `India`, capabilities `ONE_TIME_PAYMENT / UPI / CARDS / NET_BANKING`, status
`SANDBOX_READY`.

## Why Cashfree is a runnable sandbox

Cashfree sandbox keys are **auto-generated with no KYC** — sign up, open the sandbox dashboard, and
you get a test `x-client-id` / `x-client-secret` immediately. The backend uses those to mint a
`payment_session_id`; the app runs the full checkout against sandbox. This is why Cashfree is one of
the two fully-runnable providers in the showcase.

### The sandbox UPI simulator

The Cashfree sandbox ships a **UPI simulator**: when you pick UPI in the drop checkout, instead of
handing off to a real PSP app it shows a simulator screen where you **approve or decline** the UPI
collect request. That drives `onPaymentVerify` / `onPaymentFailure` end-to-end with zero real money
and no real UPI app installed.

## The two-step contract

- **`prepare(created)`** repackages `CreatedOrder.providerParams` into a `PreparedPayment`.
  Cashfree's session material is the **`payment_session_id`** plus the **`order_id`**, both minted
  server-side in `POST /orders`. The client never creates them. Missing the session id throws
  `PaymentPreparationException`.
- **`pay(host, prepared)`** casts the host to `AndroidPaymentHost`, builds a `CFSession` (SANDBOX
  environment) from the session id + order id, wraps it in a `CFDropCheckoutPayment`, registers a
  one-shot relay listener, then calls
  `CFPaymentGatewayService.getInstance().doPayment(activity, dropPayment)`.

## Callback wiring (the important bit)

The nextgen SDK reports terminal state through a `CFCheckoutResponseCallback`, and the SDK **requires
that callback to be set in the host Activity's `onCreate`** so it survives Activity recreation while
the checkout screen is up. A provider inside a suspend function is far too late. So the app owns the
callback registration; the gateway owns a relay (`CashfreeCheckoutRelay`).

Wire it in your host Activity's `onCreate`:

```kotlin
class MainActivity : ComponentActivity(), CFCheckoutResponseCallback {

    private val relay: CashfreeCheckoutRelay by inject() // Koin

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // REQUIRED in onCreate — the SDK re-attaches this on recreation.
        CFPaymentGatewayService.getInstance().setCheckoutCallback(this)
        // ... setContent { ... }
    }

    override fun onPaymentVerify(orderId: String) {
        relay.onPaymentVerify(orderId)
    }

    override fun onPaymentFailure(errorResponse: CFErrorResponse, orderId: String) {
        relay.onPaymentFailure(
            orderId = orderId,
            errorMessage = errorResponse.message,
            errorCode = errorResponse.code, // best-effort; see error-taxonomy TODO in the gateway
        )
    }
}
```

`CashfreeCheckoutRelay` is single-flight: `awaitResult()` rejects a second registration, and firing
either terminal clears the listener, so a duplicated or late SDK callback can never resume the
coroutine twice. Coroutine cancellation clears the pending listener via `invokeOnCancellation`.

## Result mapping

| SDK callback | PaymentResult | Notes |
|---|---|---|
| `onPaymentVerify(orderId)` | `Success` | `paymentId = orderId` (the client callback returns only the order id; the real `cf_payment_id` is resolved server-side); `verification = { order_id, cf_status:"verify" }` |
| `onPaymentFailure(err, orderId)` | `Failure(...)` | `code` mapped from the error string via `mapFailureCode` → `USER_CANCELLED` / `NETWORK_ERROR` / `GATEWAY_DECLINED` / `SDK_ERROR` |
| user cancel | `Failure(USER_CANCELLED)` | the SDK has no separate cancel callback; a cancel surfaces through `onPaymentFailure` and is detected by the error string. See the `TODO(error-taxonomy)` in the gateway |

## The client result is a hint, not proof

`onPaymentVerify` means the SDK *initiated* verification — not that the money settled. The
**orchestrator always confirms server-side** (`GET /payments/{id}` + webhook reconciliation) before
treating a `Success` as real. The `verification` map is forwarded to `PaymentBackend.verify`; the
human-readable `raw` payload is `Redactor`-gated and safe to render/log.

## No secrets in code

`payment_session_id` and `order_id` arrive at runtime in `providerParams`. The sandbox client
id/secret live server-side only. Nothing is hardcoded here.

## Known build concern — the `:ui` artifact

`CFDropCheckoutPayment` lives in `com.cashfree.pg:ui`, **not** the `com.cashfree.pg:api` artifact the
catalog wires. That artifact has no catalog alias and `gradle/libs.versions.toml` is frozen, so it
cannot be added yet. To compile the drop-in path, add
`cashfree-pg-ui = { module = "com.cashfree.pg:ui", version.ref = "cashfree" }` to the catalog and
`implementation(libs.cashfree.pg.ui)` to this module's `build.gradle.kts`. See the comments there.
