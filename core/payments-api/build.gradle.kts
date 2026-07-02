plugins {
    id("paymentslab.kmp.library")
}

kotlin {
    // JVM target so the backend can reuse Money/OrderRef value types without duplication.
    // Pinned to 21 to match the backend's jvmToolchain(21) (see core:protocol for the rationale).
    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    android {
        namespace = "com.paymentslab.core.paymentsapi"
        compileSdk = 37
        minSdk = 24
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            api(project(":core:common"))
        }
        androidMain.dependencies {
            implementation(libs.core.ktx)
            implementation(libs.activity.compose)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
