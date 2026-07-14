plugins {
    id("shared.kmp.library")
}

kotlin {
    // Toolchain 21 for JVM-consumer bytecode consistency (see core:protocol for the rationale).
    jvmToolchain(21)

    jvm()

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
            api(libs.koin.core)
            api("com.siddharth.kmp:common:1.0.0")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
