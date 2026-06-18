plugins {
    id("paymentslab.android.application")
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.dependency.guard)
    alias(libs.plugins.roborazzi)
}

// Locks the app's runtime dependency graph. A silent transitive version bump fails
// `./gradlew :app:dependencyGuard`; re-baseline intentionally with `:app:dependencyGuardBaseline`.
dependencyGuard {
    configuration("debugRuntimeClasspath")
}

// Single-source versioning: VERSION (semver) + BUILD_NUMBER (monotonic) at the repo root, bumped by
// `fastlane bump` and read here so versionName/versionCode never drift.
val appVersionName = rootProject.file("VERSION").readText().trim()
val appBuildNumber = rootProject.file("BUILD_NUMBER").readText().trim().toInt()

android {
    namespace = "com.paymentslab.app"

    defaultConfig {
        applicationId = "com.paymentslab.app"
        minSdk = 24
        targetSdk = 36
        versionCode = appBuildNumber
        versionName = appVersionName

        // VAPT bypass flags (common VAPT-testing bypass pattern). Default false = full protection; a
        // dedicated VAPT/compliance test variant flips them so pentesters can run on a rooted/hooked
        // device without the app hard-blocking itself. Never true in a real release.
        buildConfigField("boolean", "BYPASS_ROOT", "false")
        buildConfigField("boolean", "BYPASS_HOOK", "false")
        buildConfigField("boolean", "BYPASS_SSL", "false")
        buildConfigField("boolean", "BYPASS_DEBUGGER", "false")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    testOptions {
        unitTests {
            // Roborazzi/Robolectric render real Compose UI on the JVM — needs Android resources.
            isIncludeAndroidResources = true
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

    // WorkManager — background reconciliation of pending payments (process-death recovery)
    implementation(libs.workmanager.runtime)

    // Security suite (Keystore store, FLAG_SECURE, device-integrity, pinning config)
    implementation(project(":core:security"))

    // Provider SDKs referenced directly by MainActivity's Activity-level callback wiring
    // (providers depend on these via `implementation`, so the types aren't transitive here).
    implementation(libs.razorpay.checkout)
    implementation(libs.stripe.paymentsheet)
    implementation(libs.cashfree.pg.api)
    implementation(libs.cashfree.pg.ui)

    // Roborazzi screenshot tests (JVM/Robolectric — no emulator). Snapshot the design system.
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi.core)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.kotlinx.collections.immutable)
    debugImplementation(libs.compose.ui.test.manifest)
}
