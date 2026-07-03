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
val appBuildNumber =
    rootProject
        .file("BUILD_NUMBER")
        .readText()
        .trim()
        .toInt()

android {
    namespace = "com.paymentslab.app"

    defaultConfig {
        applicationId = "com.paymentslab.app"
        minSdk = 24
        targetSdk = 36
        versionCode = appBuildNumber
        versionName = appVersionName

        // VAPT bypass flags (mirror Dice's bypassRoo/Fri/Ssl). Default false = full protection; a
        // dedicated VAPT/compliance test variant flips them so pentesters can run on a rooted/hooked
        // device without the app hard-blocking itself. Never true in a real release.
        buildConfigField("boolean", "BYPASS_ROOT", "false")
        buildConfigField("boolean", "BYPASS_HOOK", "false")
        buildConfigField("boolean", "BYPASS_SSL", "false")
        buildConfigField("boolean", "BYPASS_DEBUGGER", "false")

        // Per-environment backend URL. Debug/vapt talk to the local dev server (emulator loopback);
        // release points at the real host. The app feeds this into networkModule(PaymentApiConfig(...)).
        buildConfigField("String", "BACKEND_URL", "\"http://10.0.2.2:8080\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            buildConfigField("String", "BACKEND_URL", "\"https://api.paymentslab.example\"")
        }

        // VAPT / pen-test variant: a debuggable build that flips the security BYPASS_* flags so a
        // tester can run on a rooted/hooked/proxied device without the app hard-blocking itself.
        // Installs alongside via the .vapt applicationId suffix. Never distributed.
        create("vapt") {
            initWith(getByName("debug"))
            matchingFallbacks += "debug" // library modules only define debug/release
            applicationIdSuffix = ".vapt"
            versionNameSuffix = "-vapt"
            buildConfigField("boolean", "BYPASS_ROOT", "true")
            buildConfigField("boolean", "BYPASS_HOOK", "true")
            buildConfigField("boolean", "BYPASS_SSL", "true")
            buildConfigField("boolean", "BYPASS_DEBUGGER", "true")
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
    implementation(project(":provider:hosted-webview"))
    implementation(project(":provider:googlepay"))
    implementation(project(":provider:mobile-money"))
    implementation(project(":provider:square"))
    implementation(project(":provider:omise"))

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
    implementation("com.squareup.sdk.in-app-payments:card-entry:1.6.8")

    // Roborazzi screenshot tests (JVM/Robolectric — no emulator). Snapshot the design system.
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi.core)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.kotlinx.collections.immutable)
    debugImplementation(libs.compose.ui.test.manifest)
}
