plugins {
    id("shared.kmp.library")
}

kotlin {
    // Desktop/JVM target: gives PaymentsLab's UI a Compose Hot Reload canvas so a UI
    // change no longer needs an emulator. Mirrors :core:common, which already had one.
    jvm()

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    android {
        namespace = "com.paymentslab.core.orchestration"
        compileSdk = 37
        minSdk = 24
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            api("com.siddharth.kmp:payments-api:1.0.0")
            implementation(project(":core:common"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
    }
}
