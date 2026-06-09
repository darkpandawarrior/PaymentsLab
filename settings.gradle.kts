pluginManagement {
    includeBuild("build-logic")
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
    }
}

rootProject.name = "PaymentsLab"

// ── Core (KMP-ready) ────────────────────────────────────────────────────────
include(":core:payments-api")
include(":core:protocol")
include(":core:common")
include(":core:orchestration")
include(":core:network")
include(":core:data")
include(":core:designsystem")

// ── Providers (Android libraries; one per gateway) ──────────────────────────
include(":provider:upi-intent")
include(":provider:razorpay")
include(":provider:cashfree")
include(":provider:stripe")

// ── Features ────────────────────────────────────────────────────────────────
include(":feature:lab")
include(":feature:checkout-demo")
include(":feature:history")

// ── App + backend ───────────────────────────────────────────────────────────
include(":app")
include(":backend")
