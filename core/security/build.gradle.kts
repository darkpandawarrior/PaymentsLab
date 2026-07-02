plugins {
    id("paymentslab.android.library")
    id("paymentslab.test")
}

android {
    namespace = "com.paymentslab.core.security"
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)

    implementation(libs.koin.core)
    implementation(libs.koin.android)

    // OkHttp CertificatePinner (the Ktor Android engine already pulls OkHttp; used for the pinning config).
    implementation(libs.ktor.client.okhttp)

    testImplementation(libs.junit)
}
