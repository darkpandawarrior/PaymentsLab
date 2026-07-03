import CashfreePG
import CashfreePGCoreSDK
import CashfreePGUISDK
import Foundation
import PaymentsLabShared
import UIKit

/// Swift's real implementation of the Kotlin `CashfreeCheckoutHost` protocol, against the real
/// `CashfreePGUISDK` Drop Checkout API (SPM, `core-ios-sdk`). Same boundary reasoning as
/// `StripeCheckoutHostImpl`.
final class CashfreeCheckoutHostImpl: NSObject, CashfreeCheckoutHost, CFResponseDelegate {
    private let pgService = CFPaymentGatewayService.getInstance()
    private var pendingResult: ((CashfreeCheckoutOutcome) -> Void)?

    func openDropCheckout(
        orderId: String,
        paymentSessionId: String,
        onResult: @escaping (CashfreeCheckoutOutcome) -> Void
    ) {
        pendingResult = onResult
        pgService.setCallback(self)

        guard
            let rootViewController = UIApplication.shared.connectedScenes
                .compactMap({ ($0 as? UIWindowScene)?.keyWindow })
                .first?.rootViewController
        else {
            emitOnce(CashfreeCheckoutOutcomeError(message: "No root view controller", orderId: orderId))
            return
        }

        do {
            let session = try CFSession.CFSessionBuilder()
                .setOrderID(orderId)
                .setPaymentSessionId(paymentSessionId)
                .setEnvironment(.SANDBOX)
                .build()
            let payment = try CFDropCheckoutPayment.CFDropCheckoutPaymentBuilder()
                .setSession(session)
                .build()
            try pgService.doPayment(payment, viewController: rootViewController)
        } catch {
            emitOnce(CashfreeCheckoutOutcomeError(message: error.localizedDescription, orderId: orderId))
        }
    }

    func verifyPayment(order_id: String) {
        emitOnce(CashfreeCheckoutOutcomeSuccess(orderId: order_id))
    }

    func onError(_ error: CFErrorResponse, order_id: String) {
        emitOnce(CashfreeCheckoutOutcomeError(message: error.message ?? "Cashfree payment failed", orderId: order_id))
    }

    private func emitOnce(_ outcome: CashfreeCheckoutOutcome) {
        guard let result = pendingResult else { return }
        pendingResult = nil
        result(outcome)
    }
}
