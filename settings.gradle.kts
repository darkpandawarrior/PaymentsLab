pluginManagement {
    includeBuild("build-logic")
    includeBuild("external/kmp-build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

// Resolves Java toolchains (incl. the JDK 17 daemon criteria in gradle/gradle-daemon-jvm.properties):
// detects an installed matching JDK, and can provision one if absent.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        // Cashfree nextgen SDK (com.cashfree.pg:api / :ui) is published only to Cashfree's own repo.
        maven {
            url = uri("https://maven.cashfree.com/release")
            content { includeGroup("com.cashfree.pg") }
        }
        // Square In-App Payments SDK (com.squareup.sdk.in-app-payments:*) is published only here,
        // not Maven Central — confirmed via square/in-app-payments-android-quickstart's build.gradle.
        // com.squareup.android:truststore/socket-factory are transitive deps also hosted only here
        // (card-entry's own .pom omits them; found by attempting the build, not by reading docs).
        maven {
            url = uri("https://sdk.squareup.com/public/android")
            content {
                includeGroup("com.squareup.sdk.in-app-payments")
                includeGroup("com.squareup.android")
            }
        }
    }
}

rootProject.name = "PaymentsLab"

// kmp-toolkit monorepo — one submodule replaces the former per-leaf submodules (kmp-common,
// kmp-mvi-core, kmp-security). Natural module paths (:common, :mvi-core, :security) mean the old
// ":lib" substitution-collision workaround is no longer needed.
includeBuild("external/kmp-toolkit") {
    dependencySubstitution {
        substitute(module("com.siddharth.kmp:common")).using(project(":common"))
        substitute(module("com.siddharth.kmp:mvi-core")).using(project(":mvi-core"))
        substitute(module("com.siddharth.kmp:security")).using(project(":security"))
        substitute(module("com.siddharth.kmp:network")).using(project(":network"))
        substitute(module("com.siddharth.kmp:designsystem")).using(project(":designsystem"))
        substitute(module("com.siddharth.kmp:payments-api")).using(project(":payments-api"))
        substitute(module("com.siddharth.kmp:stripe")).using(project(":provider:stripe"))
        substitute(module("com.siddharth.kmp:upi-intent")).using(project(":provider:upi-intent"))
        substitute(module("com.siddharth.kmp:cashfree")).using(project(":provider:cashfree"))
        substitute(module("com.siddharth.kmp:googlepay")).using(project(":provider:googlepay"))
        substitute(module("com.siddharth.kmp:omise")).using(project(":provider:omise"))
        substitute(module("com.siddharth.kmp:razorpay")).using(project(":provider:razorpay"))
        substitute(module("com.siddharth.kmp:square")).using(project(":provider:square"))
        substitute(module("com.siddharth.kmp:hosted-webview")).using(project(":provider:hosted-webview"))
        substitute(module("com.siddharth.kmp:flutterwave")).using(project(":provider:flutterwave"))
        substitute(module("com.siddharth.kmp:paystack")).using(project(":provider:paystack"))
        substitute(module("com.siddharth.kmp:paytm")).using(project(":provider:paytm"))
        substitute(module("com.siddharth.kmp:stripe-connect")).using(project(":provider:stripe-connect"))
        substitute(module("com.siddharth.kmp:cash")).using(project(":provider:cash"))
        substitute(module("com.siddharth.kmp:nmi")).using(project(":provider:nmi"))
        substitute(module("com.siddharth.kmp:peach")).using(project(":provider:peach"))
        substitute(module("com.siddharth.kmp:mobile-money")).using(project(":provider:mobile-money"))
        substitute(module("com.siddharth.kmp:mpesa")).using(project(":provider:mpesa"))
        substitute(module("com.siddharth.kmp:wallet")).using(project(":provider:wallet"))
        substitute(module("com.siddharth.kmp:xendit")).using(project(":provider:xendit"))
    }
}

// ── Core (KMP-ready) ────────────────────────────────────────────────────────
// :core:payments-api extracted to kmp-toolkit's :payments-api module (external/kmp-toolkit) —
// consumed as com.siddharth.kmp:payments-api via the includeBuild substitution above.
include(":core:config")
include(":core:protocol")
include(":core:common")
include(":core:orchestration")
include(":core:network")
include(":core:data")
include(":core:designsystem")
// :core:security extracted to kmp-toolkit's :security module (external/kmp-toolkit) — consumed as
// com.siddharth.kmp:security via the includeBuild substitution above.

// ── Providers (Android libraries; one per gateway) ──────────────────────────
// :provider:upi-intent extracted to kmp-toolkit's :provider:upi-intent module (external/kmp-toolkit)
// — consumed as com.siddharth.kmp:upi-intent via the includeBuild substitution above.
// :provider:stripe extracted to kmp-toolkit's :provider:stripe module (external/kmp-toolkit) —
// consumed as com.siddharth.kmp:stripe via the includeBuild substitution above.
// :provider:cashfree / :googlepay / :omise / :razorpay / :square extracted to kmp-toolkit (5c) —
// consumed as com.siddharth.kmp:<name> via the includeBuild substitution above.
// :provider:hosted-webview / :flutterwave / :paystack / :paytm / :stripe-connect extracted to
// kmp-toolkit (5c batch 2, incl. the hosted-webview iOS-RED fix) — consumed as
// com.siddharth.kmp:<name> via the includeBuild substitution above.
// :provider:cash / :nmi / :peach (pure-contract trio) extracted to kmp-toolkit (5c batch 3) —
// consumed as com.siddharth.kmp:<name> via the includeBuild substitution above.
// :provider:mobile-money / :mpesa / :wallet / :xendit (network cluster, 5c batch 4) extracted to
// kmp-toolkit — consumed as com.siddharth.kmp:<name> via the includeBuild substitution above.

// ── Features ────────────────────────────────────────────────────────────────
include(":feature:lab")
include(":feature:checkout-demo")
include(":feature:history")
include(":feature:home")

// ── iOS (B8) ─────────────────────────────────────────────────────────────────
include(":ios:shared")

// ── App + backend ───────────────────────────────────────────────────────────
include(":app")
include(":backend")
