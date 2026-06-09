plugins {
    id("paymentslab.kmp.library")
}

kotlin {
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
