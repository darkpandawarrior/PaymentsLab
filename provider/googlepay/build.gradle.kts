plugins {
    id("paymentslab.android.provider")
    id("shared.test")
}

android {
    namespace = "com.paymentslab.provider.googlepay"
}

dependencies {
    implementation("com.siddharth.kmp:payments-api:1.0.0")
    implementation(project(":core:common"))

    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)

    implementation(libs.play.services.wallet)

    // org.json.JSONObject is a stub on plain JVM unit tests — Robolectric provides the real shadow.
    testImplementation(libs.robolectric)
}
