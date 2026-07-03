import SwiftUI
import PaymentsLabShared

@main
struct IOSApp: App {
    init() {
        KoinInitKt.doInitKoin(
            stripeCheckoutHost: StripeCheckoutHostImpl(),
            razorpayCheckoutHost: RazorpayCheckoutHostImpl(),
            cashfreeCheckoutHost: CashfreeCheckoutHostImpl(),
            omiseCheckoutHost: OmiseCheckoutHostImpl()
        )
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
