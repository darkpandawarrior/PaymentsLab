package com.paymentslab.ios.shared

/** The Razorpay Standard Checkout result, normalized so [RazorpayIosGateway] never touches a
 *  Razorpay-iOS type directly. */
sealed interface RazorpayCheckoutOutcome {
    data class Success(
        val paymentId: String,
        val razorpayOrderId: String?,
        val razorpaySignature: String?,
    ) : RazorpayCheckoutOutcome

    data class Error(
        val code: Int,
        val description: String,
    ) : RazorpayCheckoutOutcome
}

/**
 * The Kotlin/Native → Swift boundary for Razorpay on iOS — same shape as [StripeCheckoutHost]:
 * callback-based (not `suspend`) so Kotlin/Native's Objective-C-protocol export is unambiguous,
 * implemented in Swift against the real `RazorpayCheckout` SDK (SPM, `razorpay-pod`).
 */
interface RazorpayCheckoutHost {
    fun openCheckout(
        keyId: String,
        orderId: String,
        amountMinor: Long,
        currency: String,
        onResult: (RazorpayCheckoutOutcome) -> Unit,
    )
}
