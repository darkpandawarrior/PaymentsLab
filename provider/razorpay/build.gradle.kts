plugins {
    id("paymentslab.android.provider")
    id("paymentslab.test")
}

android {
    namespace = "com.paymentslab.provider.razorpay"
}

dependencies {
    implementation(project(":core:payments-api"))
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
