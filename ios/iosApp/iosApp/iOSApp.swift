import SwiftUI
import PaymentsLabShared

@main
struct IOSApp: App {
    init() {
        KoinInitKt.doInitKoin(
            stripeCheckoutHost: StripeCheckoutHostImpl(),
            razorpayCheckoutHost: RazorpayCheckoutHostImpl(),
            cashfreeCheckoutHost: CashfreeCheckoutHostImpl(),
            omiseCheckoutHost: OmiseCheckoutHostImpl(),
            squareCheckoutHost: SquareCheckoutHostImpl()
        )
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
