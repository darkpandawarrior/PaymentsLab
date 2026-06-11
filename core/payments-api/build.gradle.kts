plugins {
    id("paymentslab.kmp.library")
}

kotlin {
    // Toolchain 17 so the JVM target compiles to Java 17 bytecode (see core:protocol for rationale).
    jvmToolchain(17)

    // JVM target so the backend can reuse Money/OrderRef value types without duplication.
    jvm()

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
