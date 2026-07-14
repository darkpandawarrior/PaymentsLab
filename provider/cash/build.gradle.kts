plugins {
    id("shared.kmp.library")
}

kotlin {
    android {
        namespace = "com.paymentslab.provider.cash"
        compileSdk = 37
        minSdk = 24
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            implementation("com.siddharth.kmp:payments-api:1.0.0")
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
