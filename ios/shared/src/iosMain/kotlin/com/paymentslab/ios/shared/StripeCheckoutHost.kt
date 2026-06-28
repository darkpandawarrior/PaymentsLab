package com.paymentslab.ios.shared

/** The Stripe PaymentSheet result, normalized so [StripeIosGateway] never touches a Stripe-iOS type. */
sealed interface StripeCheckoutOutcome {
    data class Completed(
        val paymentIntentId: String,
    ) : StripeCheckoutOutcome

    data object Canceled : StripeCheckoutOutcome

    data class Failed(
        val message: String,
    ) : StripeCheckoutOutcome
}

/**
 * The Kotlin/Native → Swift boundary for Stripe on iOS. Kotlin/Native exports this interface as an
 * Objective-C protocol automatically; Swift implements it directly against the real
 * `StripePaymentSheet` SDK (added via SPM to `ios/iosApp`) — no cinterop against Stripe's Swift
 * framework needed, since the dependency direction is Swift-implements-Kotlin-interface, not
 * Kotlin-calls-Swift-framework.
 *
 * Deliberately callback-based, not `suspend` — a plain closure type is unambiguous, always-correct
 * Kotlin/Native ⇄ Swift interop with no compiler-version-dependent suspend-export behavior to
 * verify. [StripeIosGateway.pay] wraps the callback in `suspendCancellableCoroutine`, the same
 * pattern every Activity-callback-era Android SDK in this app already uses.
 */
interface StripeCheckoutHost {
    /** Launches PaymentSheet. Calls [onResult] exactly once when the sheet reaches a terminal state. */
    fun presentPaymentSheet(
        clientSecret: String,
        publishableKey: String,
        merchantDisplayName: String,
        onResult: (StripeCheckoutOutcome) -> Unit,
    )
}
