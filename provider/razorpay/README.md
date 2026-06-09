# provider:razorpay

Razorpay Standard Checkout integration behind the `core:payments-api` `PaymentGateway` contract.

## Why this module has a callback relay

Razorpay's `Checkout.open(activity, options)` is **fire-and-forget**. The SDK reports its outcome by
calling `com.razorpay.PaymentResultWithDataListener` **on the Activity** — there is no per-call
callback parameter. Our architecture keeps `Activity` references out of gateways (`PaymentGateway.pay`
is a single suspending function). We bridge the two worlds like this:

```
MainActivity (implements PaymentResultWithDataListener)
        │  onPaymentSuccess / onPaymentError
        ▼
RazorpayCallbackRelay  (process-scoped, single-slot)
        │  emit(RazorpayCallbackResult)
        ▼
RazorpayGateway.pay { suspendCancellableCoroutine }  → resumes with PaymentResult
```

## App-side wiring (REQUIRED)

The app's single `MainActivity` must implement Razorpay's listener and forward results to the relay.
`RazorpayCallbackRelay` is bound in Koin to `PaymentActivityCallbacks`, so resolve it there:

```kotlin
class MainActivity : ComponentActivity(), PaymentResultWithDataListener {

    // Injected via Koin: single { RazorpayCallbackRelay } bind PaymentActivityCallbacks::class
    private val paymentCallbacks: PaymentActivityCallbacks by inject()

    override fun onPaymentSuccess(razorpayPaymentId: String?, data: PaymentData?) {
        paymentCallbacks.onRazorpayResult(
            RazorpayCallbackResult.Success(
                razorpayPaymentId = razorpayPaymentId,
                razorpayOrderId = data?.orderId,
                razorpaySignature = data?.signature,
            ),
        )
    }

    override fun onPaymentError(code: Int, description: String?, data: PaymentData?) {
        paymentCallbacks.onRazorpayResult(
            RazorpayCallbackResult.Error(code = code, description = description),
        )
    }
}
```

Notes:
- `PaymentData` (`getOrderId()`, `getPaymentId()`, `getSignature()`, `getData()`) is flattened at the
  Activity boundary so the Razorpay dependency stays out of the gateway's mapping logic.
- The `AndroidPaymentHost.activity` handed to `pay()` must be this same `MainActivity` instance so its
  listener actually receives the SDK callbacks.

## prepare / pay

- `prepare(created)` repackages the backend's `providerParams` (`key_id`, `order_id`, `amount`,
  `currency`) into `PreparedPayment`. No network hop — the order already exists server-side.
- `pay(host, prepared)` registers a one-shot relay listener, suspends, then
  `Checkout.open(host.activity, options)`. It resumes exactly once (guarded) and clears the relay
  slot on completion or coroutine cancellation.

## Sandbox keys

Use test keys of the form `rzp_test_XXXXXXXXXXXX`. **`key_id` always arrives from the backend via
`providerParams` — never hardcode a key or secret in this module.** The `key_secret` lives only on
the server.

## Signature verification is server-side

`onPaymentSuccess` gives you `razorpay_order_id`, `razorpay_payment_id`, and `razorpay_signature`.
The signature is `HMAC_SHA256(order_id + "|" + payment_id, key_secret)` and can only be verified with
the secret — i.e. **on the backend**. A client `PaymentResult.Success` is a hint only; the
orchestrator forwards the `verification` map to `PaymentBackend.verify` and the server decides truth.

The signature is placed:
- in `PaymentResult.Success.verification` **unredacted** (server-bound, never displayed), and
- in `PaymentResult.Success.raw` **masked** via `Redactor` (safe to render in the Lab timeline).

## Error-code → FailureCode mapping

| Razorpay `Checkout` code   | FailureCode        | Meaning                                   |
|----------------------------|--------------------|-------------------------------------------|
| `PAYMENT_CANCELED`         | `USER_CANCELLED`   | User dismissed the checkout sheet         |
| `NETWORK_ERROR`, `TLS_ERROR` | `NETWORK_ERROR`  | Connectivity / TLS failure                |
| `INVALID_OPTIONS`          | `CONFIG_MISSING`   | Bad key/order/amount we passed            |
| anything else              | `GATEWAY_DECLINED` | Bank / gateway rejected the payment       |

## Koin

Module: `razorpayModule` in `di/RazorpayModule.kt`.

```kotlin
val razorpayModule = module {
    single { RazorpayCallbackRelay } bind PaymentActivityCallbacks::class
    single { RazorpayGateway(get()) } bind PaymentGateway::class
}
```

`bind PaymentGateway::class` lets the orchestrator collect every provider via
`getAll<PaymentGateway>()` without a definition override.
