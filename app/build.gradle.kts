plugins {
    id("paymentslab.android.application")
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "com.paymentslab.app"

    defaultConfig {
        applicationId = "com.paymentslab.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    // Core
    implementation(project(":core:payments-api"))
    implementation(project(":core:common"))
    implementation(project(":core:orchestration"))
    implementation(project(":core:network"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))

    // Providers
    implementation(project(":provider:upi-intent"))
    implementation(project(":provider:razorpay"))
    implementation(project(":provider:cashfree"))
    implementation(project(":provider:stripe"))

    // Features
    implementation(project(":feature:lab"))
    implementation(project(":feature:history"))
    implementation(project(":feature:checkout-demo"))

    // Compose (androidx BOM) + navigation
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.jb.navigation.compose)
    debugImplementation(libs.compose.ui.tooling)

    // DI
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.napier)

    // Provider SDKs referenced directly by MainActivity's Activity-level callback wiring
    // (providers depend on these via `implementation`, so the types aren't transitive here).
    implementation(libs.razorpay.checkout)
    implementation(libs.stripe.paymentsheet)
    implementation(libs.cashfree.pg.api)
    implementation(libs.cashfree.pg.ui)
}
