package com.paymentslab.ios.shared

/** The Cashfree Drop Checkout result, normalized so [CashfreeIosGateway] never touches a
 *  Cashfree-iOS SDK type directly. */
sealed interface CashfreeCheckoutOutcome {
    data class Success(
        val orderId: String,
    ) : CashfreeCheckoutOutcome

    data class Error(
        val message: String,
        val orderId: String,
    ) : CashfreeCheckoutOutcome
}

/**
 * The Kotlin/Native → Swift boundary for Cashfree on iOS — same shape as [StripeCheckoutHost]:
 * callback-based, implemented in Swift against the real `CashfreePGUISDK` (SPM, `core-ios-sdk`)
 * Drop Checkout API.
 */
interface CashfreeCheckoutHost {
    fun openDropCheckout(
        orderId: String,
        paymentSessionId: String,
        onResult: (CashfreeCheckoutOutcome) -> Unit,
    )
}
