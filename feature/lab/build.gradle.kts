plugins {
    id("shared.cmp.feature")
}

kotlin {
    android {
        namespace = "com.paymentslab.feature.lab"
        compileSdk = 37
        minSdk = 24
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:payments-api"))
            implementation(project(":core:orchestration"))
            implementation(project(":core:designsystem"))
            implementation(project(":core:common"))
            implementation(libs.kotlinx.collections.immutable)
            implementation("com.siddharth.kmp:mvi-core:1.0.0")
        }
        androidMain.dependencies {
            implementation(libs.lifecycle.runtime.compose)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
    }
}
