import SwiftUI
import PaymentsLabShared

@main
struct IOSApp: App {
    init() {
        KoinInitKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
