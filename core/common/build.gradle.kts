plugins {
    id("paymentslab.kmp.library")
}

kotlin {
    // Toolchain 17 for JVM-consumer bytecode consistency (see core:protocol for the rationale).
    jvmToolchain(17)

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
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
