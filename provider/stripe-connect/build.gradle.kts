plugins {
    id("paymentslab.android.provider")
    id("shared.test")
}

android {
    namespace = "com.paymentslab.provider.stripeconnect"
}

dependencies {
    implementation("com.siddharth.kmp:payments-api:1.0.0")
    implementation(project(":core:common"))
    // Reuses the shared checkout WebView + return-URL relay already mounted at :app's nav host for
    // the mock hosted-OAuth redirect — same idiom as a hosted checkout return-URL (see
    // provider:paystack, which reuses the identical relay for the same reason).
    implementation(project(":provider:hosted-webview"))
    // Unlike provider:paystack (relay-only, no UI), this module DOES render its own small
    // StripeConnectCheckoutHost composable (reusing hosted-webview's HostedCheckoutScreen), so it
    // needs the Compose UI artifact (Modifier) on top of the runtime.
    implementation(libs.runtime)
    implementation(libs.ui)

    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.core.ktx)
}
