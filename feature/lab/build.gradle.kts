plugins {
    id("shared.cmp.feature")
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
        namespace = "com.paymentslab.feature.lab"
        compileSdk = 37
        minSdk = 24
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            implementation("com.siddharth.kmp:payments-api:1.0.0")
            implementation(project(":core:orchestration"))
            implementation(project(":core:designsystem"))
            implementation(project(":core:common"))
            implementation(libs.kotlinx.collections.immutable)
            implementation("com.siddharth.kmp:mvi-core:1.0.0")
        }
        androidMain.dependencies {
            implementation(libs.lifecycle.runtime.compose)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
    }
}
