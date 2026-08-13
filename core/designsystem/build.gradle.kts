plugins {
    id("shared.kmp.compose")
}

kotlin {
    // Desktop/JVM target: gives PaymentsLab's UI a Compose Hot Reload canvas so a UI
    // change no longer needs an emulator. Mirrors :core:common, which already had one.
    jvm()

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        // Required for wasmJsBrowserTest: without a declared executable, the Compose Gradle
        // plugin's Skiko-runtime check fails the task outright (CMP-4906) since Compose UI
        // can't load its renderer from a bare klib. CMP 1.12.0-rc01 made this check a hard
        // build failure, so it is no longer optional.
        binaries.executable()
    }

    android {
        namespace = "com.paymentslab.core.designsystem"
        compileSdk = 37
        minSdk = 24
    }

    sourceSets {
        commonMain.dependencies {
            implementation("com.siddharth.kmp:payments-api:1.0.0")
            // StepTimeline/TimelineStep/StepState/PayloadCard/RedactionReveal/AnimatedCounter now
            // live in :designsystem (backlog #30/#31); StepMapper.kt's public toTimelineStep()
            // returns TimelineStep, so this is `api`, matching HireSignal's precedent for the
            // same coordinate.
            api("com.siddharth.kmp:designsystem:1.0.0")
            implementation(project(":core:common"))
            implementation(libs.runtime)
            implementation(libs.ui)
            implementation(libs.material3)
            implementation(libs.foundation)
            implementation(libs.material.icons.extended)
            implementation(libs.components.resources)
            implementation(libs.ui.tooling.preview.mp)
            implementation(libs.kotlinx.collections.immutable)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
