import Foundation
import PaymentsLabShared
import SquareInAppPaymentsSDK
import UIKit

/// Swift's real implementation of the Kotlin `SquareCheckoutHost` protocol, against the real
/// `SQIPCardEntryViewController` (CocoaPods, `SquareInAppPaymentsSDK`) — a genuine card-entry UI,
/// the same UX story as Android's `CardEntry` activity. Same boundary reasoning as
/// `StripeCheckoutHostImpl`.
final class SquareCheckoutHostImpl: NSObject, SquareCheckoutHost {
    private var pendingResult: ((SquareCheckoutOutcome) -> Void)?

    func presentCardEntry(
        applicationId: String,
        onResult: @escaping (SquareCheckoutOutcome) -> Void
    ) {
        pendingResult = onResult
        SQIPInAppPaymentsSDK.squareApplicationID = applicationId

        guard
            let rootViewController = UIApplication.shared.connectedScenes
                .compactMap({ ($0 as? UIWindowScene)?.keyWindow })
                .first?.rootViewController
        else {
            emitOnce(SquareCheckoutOutcomeError(message: "No root view controller"))
            return
        }

        let theme = SQIPTheme()
        theme.saveButtonTitle = "Pay"
        let cardEntryViewController = SQIPCardEntryViewController(theme: theme)
        cardEntryViewController.delegate = self

        let navigationController = UINavigationController(rootViewController: cardEntryViewController)
        rootViewController.present(navigationController, animated: true)
    }

    private func emitOnce(_ outcome: SquareCheckoutOutcome) {
        guard let result = pendingResult else { return }
        pendingResult = nil
        result(outcome)
    }
}

extension SquareCheckoutHostImpl: SQIPCardEntryViewControllerDelegate {
    func cardEntryViewController(
        _ cardEntryViewController: SQIPCardEntryViewController,
        didObtain cardDetails: SQIPCardDetails,
        completionHandler: @escaping (Error?) -> Void
    ) {
        emitOnce(SquareCheckoutOutcomeSuccess(nonce: cardDetails.nonce))
        completionHandler(nil)
    }

    func cardEntryViewController(
        _ cardEntryViewController: SQIPCardEntryViewController,
        didCompleteWith status: SQIPCardEntryCompletionStatus
    ) {
        cardEntryViewController.dismiss(animated: true) {
            if status == .canceled {
                self.emitOnce(SquareCheckoutOutcomeCanceled.shared)
            }
        }
    }
}
