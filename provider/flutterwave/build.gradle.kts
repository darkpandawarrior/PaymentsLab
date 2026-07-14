plugins {
    id("paymentslab.android.provider")
    id("shared.test")
}

android {
    namespace = "com.paymentslab.provider.flutterwave"
}

dependencies {
    implementation("com.siddharth.kmp:payments-api:1.0.0")
    implementation(project(":core:common"))
    // Reuses the shared checkout WebView + return-URL relay already mounted at :app's nav host —
    // promoting this gateway to its own module doesn't mean reinventing the WebView bridge.
    implementation(project(":provider:hosted-webview"))
    // hosted-webview is a Compose Multiplatform module, so depending on it drags the Compose compiler
    // onto this build (the android.provider convention already enables the compose plugin), which then
    // requires the Compose runtime on the classpath. This is the only Compose this module needs —
    // FlutterwaveGateway touches only the non-@Composable relay types, it has no Compose UI of its own.
    implementation(libs.runtime)

    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.core.ktx)
}
