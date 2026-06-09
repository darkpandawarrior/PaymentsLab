plugins {
    id("paymentslab.kmp.library")
}

kotlin {
    // Pinned to 21 for JVM-consumer bytecode consistency (see core:protocol for the rationale).
    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    android {
        namespace = "com.paymentslab.core.common"
        compileSdk = 37
        minSdk = 24
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines.core)
            api(libs.napier)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
