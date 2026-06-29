import Foundation
import OmiseSDK
import PaymentsLabShared
import SwiftUI
import UIKit

/// Swift's real implementation of the Kotlin `OmiseCheckoutHost` protocol. Omise's iOS SDK ships no
/// ready-made card-entry UI (unlike Android's `CreditCardActivity`) — its public API is manual
/// tokenization only (`client.createToken(payload:completionHandler:)`). This presents a small
/// SwiftUI card form of the app's own and calls the real SDK for the actual tokenization request.
final class OmiseCheckoutHostImpl: NSObject, OmiseCheckoutHost {
    func presentCardForm(
        publicKey: String,
        onResult: @escaping (OmiseCheckoutOutcome) -> Void
    ) {
        guard
            let rootViewController = UIApplication.shared.connectedScenes
                .compactMap({ ($0 as? UIWindowScene)?.keyWindow })
                .first?.rootViewController
        else {
            onResult(OmiseCheckoutOutcomeError(message: "No root view controller"))
            return
        }

        let omiseSDK = OmiseSDK(publicKey: publicKey)
        let form = OmiseCardFormView(
            onSubmit: { card in
                let cardPayload = CreateTokenPayload.Card(
                    name: card.name,
                    number: card.number,
                    expirationMonth: card.expirationMonth,
                    expirationYear: card.expirationYear,
                    securityCode: card.cvc
                )
                let payload = CreateTokenPayload(card: cardPayload)
                omiseSDK.client.createToken(payload: payload) { result in
                    DispatchQueue.main.async {
                        rootViewController.presentedViewController?.dismiss(animated: true)
                        switch result {
                        case .success(let token):
                            onResult(OmiseCheckoutOutcomeSuccess(token: token.id))
                        case .failure(let error):
                            onResult(OmiseCheckoutOutcomeError(message: error.localizedDescription))
                        }
                    }
                }
            },
            onCancel: {
                rootViewController.presentedViewController?.dismiss(animated: true)
                onResult(OmiseCheckoutOutcomeCanceled.shared)
            }
        )
        let hostingController = UIHostingController(rootView: form)
        rootViewController.present(hostingController, animated: true)
    }
}

private struct OmiseCardInput {
    let name: String
    let number: String
    let expirationMonth: Int
    let expirationYear: Int
    let cvc: String
}

private struct OmiseCardFormView: View {
    let onSubmit: (OmiseCardInput) -> Void
    let onCancel: () -> Void

    @State private var name = ""
    @State private var number = ""
    @State private var month = ""
    @State private var year = ""
    @State private var cvc = ""

    var body: some View {
        NavigationView {
            Form {
                Section("Card details (Omise test card: 4242 4242 4242 4242)") {
                    TextField("Cardholder name", text: $name)
                    TextField("Card number", text: $number).keyboardType(.numberPad)
                    TextField("Expiry month (MM)", text: $month).keyboardType(.numberPad)
                    TextField("Expiry year (YYYY)", text: $year).keyboardType(.numberPad)
                    TextField("CVC", text: $cvc).keyboardType(.numberPad)
                }
                Button("Pay") {
                    onSubmit(
                        OmiseCardInput(
                            name: name,
                            number: number,
                            expirationMonth: Int(month) ?? 0,
                            expirationYear: Int(year) ?? 0,
                            cvc: cvc
                        )
                    )
                }
            }
            .navigationTitle("Omise")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel", action: onCancel)
                }
            }
        }
    }
}
