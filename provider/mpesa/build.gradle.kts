plugins {
    id("shared.kmp.library")
}

kotlin {
    android {
        namespace = "com.paymentslab.provider.mpesa"
        compileSdk = 37
        minSdk = 24
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:payments-api"))
            implementation(project(":core:common"))
            implementation(project(":core:network"))
            implementation(libs.koin.core)
            implementation(libs.ktor.client.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
