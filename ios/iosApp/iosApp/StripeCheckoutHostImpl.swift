import Foundation
import PaymentsLabShared
import StripePaymentSheet
import UIKit

/// Swift's real implementation of the Kotlin `StripeCheckoutHost` protocol — the actual
/// `StripePaymentSheet` call lives here because Kotlin/Native can't cinterop against Stripe's
/// Swift-only SDK. Kotlin/Native exports the interface as a plain Objective-C protocol, so Swift
/// conforms to it directly with no bridging code needed on either side.
final class StripeCheckoutHostImpl: NSObject, StripeCheckoutHost {
    func presentPaymentSheet(
        clientSecret: String,
        publishableKey: String,
        merchantDisplayName: String,
        onResult: @escaping (StripeCheckoutOutcome) -> Void
    ) {
        StripeAPI.defaultPublishableKey = publishableKey

        var configuration = PaymentSheet.Configuration()
        configuration.merchantDisplayName = merchantDisplayName
        let paymentSheet = PaymentSheet(paymentIntentClientSecret: clientSecret, configuration: configuration)

        guard
            let rootViewController = UIApplication.shared.connectedScenes
                .compactMap({ ($0 as? UIWindowScene)?.keyWindow })
                .first?.rootViewController
        else {
            onResult(StripeCheckoutOutcomeFailed(message: "No root view controller to present PaymentSheet from"))
            return
        }

        paymentSheet.present(from: rootViewController) { result in
            switch result {
            case .completed:
                // The server-minted client secret (`pi_XXX_secret_YYY`) is the only PaymentIntent
                // reference the imperative PaymentSheet API hands back on this platform too — the
                // backend's StripeAdapter re-derives/verifies authoritatively from it either way.
                let paymentIntentId = clientSecret.components(separatedBy: "_secret_").first ?? clientSecret
                onResult(StripeCheckoutOutcomeCompleted(paymentIntentId: paymentIntentId))
            case .canceled:
                onResult(StripeCheckoutOutcomeCanceled.shared)
            case .failed(let error):
                onResult(StripeCheckoutOutcomeFailed(message: error.localizedDescription))
            }
        }
    }
}
