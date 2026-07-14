/**
 * B8: the iOS app's shared surface. Deliberately its own module rather than reusing `feature:lab`
 * directly — AGP 9's KMP library plugin can't also be `com.android.application`/host a
 * `.framework` export (see `kmp-boundaries` skill), so the "package everything for Xcode" concern
 * lives here, separate from the feature modules it aggregates.
 *
 * Scope is intentionally narrower than the Android app's full ~65-gateway catalog: only the
 * KMP-safe archetype C (hosted-webview) and D (mobile-money) providers are wired — the native-SDK
 * archetype-A gateways (Stripe/Razorpay/Cashfree/Square/Omise/GooglePay) are Android-only by
 * construction (their SDKs don't exist on iOS). Scaling this module's gateway list to the Android
 * app's full set is the same mechanical fan-out B2 already proved, just not done here.
 */
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        binaries.framework {
            baseName = "PaymentsLabShared"
            isStatic = true
        }
    }

    sourceSets {
        iosMain.dependencies {
            implementation("com.siddharth.kmp:payments-api:1.0.0")
            implementation(project(":core:protocol"))
            implementation(project(":core:common"))
            implementation(project(":core:network"))
            implementation(project(":core:data"))
            implementation(project(":core:orchestration"))
            implementation(project(":core:designsystem"))
            implementation("com.siddharth.kmp:hosted-webview:1.0.0")
            implementation(project(":provider:mobile-money"))
            implementation(project(":feature:lab"))
            implementation(project(":feature:checkout-demo"))
            implementation(project(":feature:history"))
            implementation(project(":feature:home"))

            implementation(libs.koin.core)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
        }
    }
}
