plugins {
    id("paymentslab.kmp.library")
}

kotlin {
    // Toolchain 21 so the JVM target compiles to Java 21 bytecode (see core:protocol for rationale).
    jvmToolchain(21)

    // JVM target so the backend can resolve credentials from System.getenv with the same logic.
    jvm()

    android {
        namespace = "com.paymentslab.core.config"
        compileSdk = 37
        minSdk = 24
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core:payments-api"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
