import SwiftUI
import PaymentsLabShared

@main
struct IOSApp: App {
    init() {
        KoinInitKt.doInitKoin(stripeCheckoutHost: StripeCheckoutHostImpl())
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
