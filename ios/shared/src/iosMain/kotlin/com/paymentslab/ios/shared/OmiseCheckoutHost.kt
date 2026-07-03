package com.paymentslab.ios.shared

/** The Omise card tokenization result, normalized so [OmiseIosGateway] never touches an
 *  Omise-iOS SDK type directly. */
sealed interface OmiseCheckoutOutcome {
    data class Success(
        val token: String,
    ) : OmiseCheckoutOutcome

    data class Error(
        val message: String,
    ) : OmiseCheckoutOutcome

    data object Canceled : OmiseCheckoutOutcome
}

/**
 * The Kotlin/Native → Swift boundary for Omise on iOS — same shape as [StripeCheckoutHost].
 * Unlike Android's `CreditCardActivity`, Omise's iOS SDK ships no ready-made card-entry UI (its
 * public API is manual tokenization only — `client.createToken(payload:completionHandler:)`), so
 * the card form itself is this app's own SwiftUI, calling the real `OmiseSDK` (SPM, `omise-ios`)
 * for the actual tokenization call.
 */
interface OmiseCheckoutHost {
    fun presentCardForm(
        publicKey: String,
        onResult: (OmiseCheckoutOutcome) -> Unit,
    )
}
