import java.io.FileInputStream
import java.util.Properties

plugins {
    id("shared.android.application")
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.dependency.guard)
    alias(libs.plugins.roborazzi)
}

// Release signing, reads from keystore.properties (gitignored) or env vars (CI). Falls back to
// debug signing if neither is present, so `assembleRelease` still succeeds locally and in CI
// without secrets configured.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties =
    Properties().apply {
        if (keystorePropertiesFile.exists()) {
            FileInputStream(keystorePropertiesFile).use { load(it) }
        }
    }
val hasReleaseSigning =
    keystorePropertiesFile.exists() || System.getenv("RELEASE_STORE_FILE") != null

// F-Droid reproducible build flag (`./gradlew :app:assembleRelease -Pfdroid`). Disables R8/resource
// shrinking, which isn't bit-for-bit reproducible across machines.
val fdroidBuild = providers.gradleProperty("fdroid").isPresent

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

    // The shared `shared.android.application` convention plugin defaults `buildConfig = false`
    // (matches AGP 9's own default). This app relies on BuildConfig — PaymentsLabApplication reads
    // BuildConfig.BUILD_TYPE/VERSION_NAME, security/network read BACKEND_URL + BYPASS_* — so it must
    // opt back in explicitly. Without this the buildConfigField(...) calls below don't compile.
    buildFeatures {
        buildConfig = true
    }

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

        // Per-environment backend URL. Debug/vapt talk to the local dev server (emulator loopback);
        // release points at the real host. The app feeds this into networkModule(PaymentApiConfig(...)).
        buildConfigField("String", "BACKEND_URL", "\"http://10.0.2.2:8080\"")
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile =
                    file(
                        keystoreProperties.getProperty("storeFile")
                            ?: System.getenv("RELEASE_STORE_FILE"),
                    )
                storePassword =
                    keystoreProperties.getProperty("storePassword")
                        ?: System.getenv("RELEASE_STORE_PASSWORD")
                keyAlias =
                    keystoreProperties.getProperty("keyAlias")
                        ?: System.getenv("RELEASE_KEY_ALIAS")
                keyPassword =
                    keystoreProperties.getProperty("keyPassword")
                        ?: System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = !fdroidBuild
            isShrinkResources = !fdroidBuild
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            buildConfigField("String", "BACKEND_URL", "\"https://api.paymentslab.example\"")
            signingConfig =
                if (hasReleaseSigning) {
                    signingConfigs.getByName("release")
                } else {
                    signingConfigs.getByName("debug")
                }
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
    implementation(project(":provider:paystack"))
    implementation(project(":provider:flutterwave"))
    implementation(project(":provider:cashfree"))
    implementation(project(":provider:stripe"))
    implementation(project(":provider:hosted-webview"))
    implementation(project(":provider:googlepay"))
    implementation(project(":provider:mobile-money"))
    implementation(project(":provider:square"))
    implementation(project(":provider:omise"))
    implementation(project(":provider:wallet"))
    implementation(project(":provider:paytm"))
    implementation(project(":provider:cash"))

    // Features
    implementation(project(":feature:lab"))
    implementation(project(":feature:history"))
    implementation(project(":feature:checkout-demo"))
    implementation(project(":feature:home"))

    // Compose (androidx BOM) + navigation
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.material.icons.extended)
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
