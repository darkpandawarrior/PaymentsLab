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
    }
}

rootProject.name = "PaymentsLab"

// ── Core (KMP-ready) ────────────────────────────────────────────────────────
include(":core:payments-api")
include(":core:protocol")
include(":core:common")
include(":core:orchestration")
// -- BELOW MODULES ADDED AS AGENTS CREATE THEM (temporarily scoped for core validation) --
// include(":core:network")
// include(":core:data")
// include(":core:designsystem")
// include(":provider:upi-intent")
// include(":provider:razorpay")
// include(":provider:cashfree")
// include(":provider:stripe")
// include(":feature:lab")
// include(":feature:checkout-demo")
// include(":feature:history")
// include(":app")
// include(":backend")
