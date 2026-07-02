plugins {
    id("paymentslab.kmp.library")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    // JVM target so the backend server links against the exact same DTOs the client sends.
    // Pinned to 21 to match the backend's jvmToolchain(21) — otherwise the JVM classes compile to
    // newer bytecode than the backend test runtime and fail with UnsupportedClassVersionError.
    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    android {
        namespace = "com.paymentslab.core.protocol"
        compileSdk = 37
        minSdk = 24
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
