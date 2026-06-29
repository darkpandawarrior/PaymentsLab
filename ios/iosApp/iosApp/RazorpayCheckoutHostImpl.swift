import Foundation
import PaymentsLabShared
import Razorpay
import UIKit

/// Swift's real implementation of the Kotlin `RazorpayCheckoutHost` protocol, against the real
/// `RazorpayCheckout` iOS SDK (SPM, `razorpay-pod`). Same boundary reasoning as
/// `StripeCheckoutHostImpl`: Kotlin/Native can't cinterop against the SDK directly.
final class RazorpayCheckoutHostImpl: NSObject, RazorpayCheckoutHost, RazorpayPaymentCompletionProtocolWithData {
    private var razorpay: RazorpayCheckout?
    private var pendingResult: ((RazorpayCheckoutOutcome) -> Void)?

    func openCheckout(
        keyId: String,
        orderId: String,
        amountMinor: Int64,
        currency: String,
        onResult: @escaping (RazorpayCheckoutOutcome) -> Void
    ) {
        pendingResult = onResult
        razorpay = RazorpayCheckout.initWithKey(keyId, andDelegateWithData: self)

        let options: [AnyHashable: Any] = [
            "order_id": orderId,
            "amount": amountMinor,
            "currency": currency,
            "name": "PaymentsLab",
        ]

        guard
            let rootViewController = UIApplication.shared.connectedScenes
                .compactMap({ ($0 as? UIWindowScene)?.keyWindow })
                .first?.rootViewController
        else {
            emitOnce(RazorpayCheckoutOutcomeError(code: -1, description: "No root view controller"))
            return
        }

        razorpay?.open(options, displayController: rootViewController)
    }

    func onPaymentSuccess(_ payment_id: String, andData response: [AnyHashable: Any]?) {
        let razorpayOrderId = response?["razorpay_order_id"] as? String
        let razorpaySignature = response?["razorpay_signature"] as? String
        emitOnce(
            RazorpayCheckoutOutcomeSuccess(
                paymentId: payment_id,
                razorpayOrderId: razorpayOrderId,
                razorpaySignature: razorpaySignature
            )
        )
    }

    func onPaymentError(_ code: Int32, description str: String, andData response: [AnyHashable: Any]?) {
        emitOnce(RazorpayCheckoutOutcomeError(code: code, description: str))
    }

    private func emitOnce(_ outcome: RazorpayCheckoutOutcome) {
        guard let result = pendingResult else { return }
        pendingResult = nil
        result(outcome)
    }
}
