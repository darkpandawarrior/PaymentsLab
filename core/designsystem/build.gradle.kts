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
            // StepTimeline/TimelineStep/StepState/PayloadCard/RedactionReveal/AnimatedCounter now
            // live in :designsystem (backlog #30/#31); StepMapper.kt's public toTimelineStep()
            // returns TimelineStep, so this is `api`, matching HireSignal's precedent for the
            // same coordinate.
            api("com.siddharth.kmp:designsystem:1.0.0")
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
