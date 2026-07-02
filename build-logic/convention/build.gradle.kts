plugins {
    `kotlin-dsl`
}

group = "com.paymentslab.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // compileOnly avoids ClassCastException when the same plugin class is loaded by two
    // classloaders at different versions.
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.composeCompiler.gradlePlugin)
}

tasks.validatePlugins {
    enableStricterValidation = true
    failOnWarning = true
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "paymentslab.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("kmpCompose") {
            id = "paymentslab.kmp.compose"
            implementationClass = "KmpComposeConventionPlugin"
        }
        register("cmpFeature") {
            id = "paymentslab.cmp.feature"
            implementationClass = "CmpFeatureConventionPlugin"
        }
        register("androidApplication") {
            id = "paymentslab.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidProvider") {
            id = "paymentslab.android.provider"
            implementationClass = "AndroidProviderConventionPlugin"
        }
        register("test") {
            id = "paymentslab.test"
            implementationClass = "TestConventionPlugin"
        }
    }
}
