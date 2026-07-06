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

// mvi-core's KMP root publication is renamed "mvi-core" only at *publish* time (see its
// lib/build.gradle.kts: artifactId = "mvi-core" for the kotlinMultiplatform publication) — the
// actual subproject is ":lib". Gradle's default project-name substitution looks for
// com.siddharth.kmp:lib and fails to resolve com.siddharth.kmp:mvi-core, so the substitution
// below is required (proven on the Kursi adoption of this same library).
includeBuild("external/kmp-mvi-core") {
    dependencySubstitution {
        substitute(module("com.siddharth.kmp:mvi-core")).using(project(":lib"))
    }
}

// ── Core (KMP-ready) ────────────────────────────────────────────────────────
include(":core:payments-api")
include(":core:config")
include(":core:protocol")
include(":core:common")
include(":core:orchestration")
include(":core:network")
include(":core:data")
include(":core:designsystem")
include(":core:security")

// ── Providers (Android libraries; one per gateway) ──────────────────────────
include(":provider:upi-intent")
include(":provider:razorpay")
include(":provider:paystack")
include(":provider:cashfree")
include(":provider:stripe")
include(":provider:hosted-webview")
include(":provider:googlepay")
include(":provider:mobile-money")
include(":provider:square")
include(":provider:omise")
include(":provider:wallet")

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
