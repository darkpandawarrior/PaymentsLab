plugins {
    id("shared.kmp.compose")
}

kotlin {
    android {
        namespace = "com.paymentslab.core.designsystem"
        compileSdk = 37
        minSdk = 24
    }

    sourceSets {
        commonMain.dependencies {
            implementation("com.siddharth.kmp:payments-api:1.0.0")
            implementation(project(":core:common"))
            implementation(libs.runtime)
            implementation(libs.ui)
            implementation(libs.material3)
            implementation(libs.foundation)
            implementation(libs.material.icons.extended)
            implementation(libs.components.resources)
            implementation(libs.ui.tooling.preview.mp)
            implementation(libs.kotlinx.collections.immutable)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
