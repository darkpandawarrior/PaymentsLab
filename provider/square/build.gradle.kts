plugins {
    id("paymentslab.android.provider")
    id("shared.test")
}

android {
    namespace = "com.paymentslab.provider.square"
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

    // In-App Payments Card Entry SDK (sdk.squareup.com/public/android — not on Maven Central).
    // card-entry's own published pom omits nonce-api as a transitive dependency even though
    // CardEntry's API surface (Callback/Card/nonce) requires it at compile time — added explicitly
    // here (verified by inspecting the actual .aar/.jar contents, not the incomplete pom graph).
    implementation("com.squareup.sdk.in-app-payments:card-entry:1.6.8")
    implementation("com.squareup.sdk.in-app-payments:nonce-api:1.6.8")
}
