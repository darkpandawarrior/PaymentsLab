plugins {
    id("shared.cmp.feature")
}

kotlin {
    android {
        namespace = "com.paymentslab.provider.hostedwebview"
        compileSdk = 37
        minSdk = 24
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:payments-api"))
            implementation(project(":core:common"))
            implementation(libs.compose.webview.multiplatform)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
