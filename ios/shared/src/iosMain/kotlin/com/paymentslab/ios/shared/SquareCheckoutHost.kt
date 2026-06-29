package com.paymentslab.ios.shared

/** The Square Card Entry result, normalized so [SquareIosGateway] never touches a Square-iOS SDK
 *  type directly. */
sealed interface SquareCheckoutOutcome {
    data class Success(
        val nonce: String,
    ) : SquareCheckoutOutcome

    data class Error(
        val message: String,
    ) : SquareCheckoutOutcome

    data object Canceled : SquareCheckoutOutcome
}

/**
 * The Kotlin/Native → Swift boundary for Square on iOS — same shape as [StripeCheckoutHost].
 * Implemented in Swift against `SQIPCardEntryViewController` (CocoaPods, `SquareInAppPaymentsSDK`
 * `1.6.7`) — unlike the other four gateways, Square's iOS SDK has no SPM distribution, so this one
 * needed a `Podfile`/`pod install` rather than an `XCRemoteSwiftPackageReference` (see
 * `docs/providers/square-ios.md`).
 */
interface SquareCheckoutHost {
    fun presentCardEntry(
        applicationId: String,
        onResult: (SquareCheckoutOutcome) -> Unit,
    )
}
