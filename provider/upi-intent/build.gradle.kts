plugins {
    id("paymentslab.android.provider")
    id("shared.test")
}

android {
    namespace = "com.paymentslab.provider.upiintent"
}

dependencies {
    implementation("com.siddharth.kmp:payments-api:1.0.0")
    implementation(project(":core:common"))

    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.core.ktx)
    // ActivityResult APIs (ActivityResultContracts) used by pay(); payments-api keeps activity as
    // `implementation`, so provider modules bring it in directly.
    implementation(libs.activity.compose)
}
