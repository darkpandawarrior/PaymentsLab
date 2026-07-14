plugins {
    id("paymentslab.android.provider")
    id("shared.test")
}

android {
    namespace = "com.paymentslab.provider.razorpay"
}

dependencies {
    implementation("com.siddharth.kmp:payments-api:1.0.0")
    implementation(project(":core:common"))

    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.core.ktx)
    // ComponentActivity (host.activity) for Checkout.open(); payments-api keeps activity as
    // `implementation`, so provider modules bring it in directly.
    implementation(libs.activity.compose)

    implementation(libs.razorpay.checkout)
}
