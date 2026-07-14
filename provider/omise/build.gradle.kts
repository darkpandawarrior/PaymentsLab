plugins {
    id("paymentslab.android.provider")
    id("shared.test")
}

android {
    namespace = "com.paymentslab.provider.omise"
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

    // Omise Android SDK — on Maven Central (unlike Square). 6.0.0-alpha.3 is the latest GitHub
    // release but isn't published to Maven Central; 5.6.0 is the latest stable published version
    // (verified via a direct Maven Central search, not the README's stale 4.3.1 install snippet).
    // Excludes the old kotlin-android-extensions-runtime transitive dep, which duplicates classes
    // already provided by the project's modern kotlin-parcelize-runtime (found by attempting the
    // build, which failed on :app:checkDebugDuplicateClasses).
    implementation("co.omise:omise-android:5.6.0") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-android-extensions-runtime")
    }
}
