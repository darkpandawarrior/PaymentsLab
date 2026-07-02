plugins {
    id("paymentslab.kmp.library")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    // JVM target so the backend server links against the exact same DTOs the client sends.
    jvm()

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
