plugins {
    id("shared.kmp.library")
}

kotlin {
    android {
        namespace = "com.paymentslab.provider.wallet"
        compileSdk = 37
        minSdk = 24
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            implementation("com.siddharth.kmp:payments-api:1.0.0")
            implementation(project(":core:common"))
            implementation(project(":core:network"))
            implementation(project(":core:protocol"))
            implementation(libs.koin.core)
            implementation(libs.ktor.client.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }
    }
}
