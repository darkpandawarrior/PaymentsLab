plugins {
    id("paymentslab.android.provider")
    id("shared.test")
}

android {
    namespace = "com.paymentslab.provider.cashfree"
}

dependencies {
    implementation(project(":core:payments-api"))
    implementation(project(":core:common"))

    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)

    // Cashfree nextgen SDK. The catalog alias `cashfree-pg-api` -> `com.cashfree.pg:api` is the core
    // (headless) artifact: CFPaymentGatewayService, CFSession, CFCheckoutResponseCallback.
    implementation(libs.cashfree.pg.api)

    // Cashfree's drop-in UI (CFDropCheckoutPayment: UPI + cards + net-banking) ships in the separate
    // `com.cashfree.pg:ui` artifact, not `:api` above.
    implementation(libs.cashfree.pg.ui)
}
