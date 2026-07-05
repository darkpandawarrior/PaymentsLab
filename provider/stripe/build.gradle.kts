plugins {
    id("paymentslab.android.provider")
    id("shared.test")
}

android {
    namespace = "com.paymentslab.provider.stripe"
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

    // Stripe PaymentSheet drives the card/wallet UI; play-services-wallet is required for the
    // Google-Pay-via-Stripe path (Google Pay rides Stripe as the gateway of record).
    implementation(libs.stripe.paymentsheet)
    implementation(libs.play.services.wallet)
}
