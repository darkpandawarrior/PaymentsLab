plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.androidKmpLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.dependency.guard) apply false
    alias(libs.plugins.roborazzi) apply false
    // Code quality — applied to root, propagated to subprojects below
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    alias(libs.plugins.ktlint)
}

detekt {
    config.setFrom(files("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    allRules = false
}

dependencies {
    // Guarded so the project still configures while modules are being scaffolded in.
    findProject(":app")?.let { kover(it) }
}

kover {
    reports {
        filters {
            excludes {
                packages("*.BuildConfig", "*.R")
            }
        }
    }
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")
    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        buildUponDefaultConfig = true
        baseline = file("detekt-baseline.xml")
    }
}

// ── Workflow task aliases: the local dev + CI verification loop ──────────────
tasks.register("fastGate") {
    description = "ktlint + detekt + JVM/common unit tests: the fast CI gate (no emulator)."
    dependsOn("ktlintCheck", "detekt")
    findProject(":backend")?.let { dependsOn(":backend:test") }
    findProject(":core:orchestration")?.let { dependsOn(":core:orchestration:testAndroidHostTest") }
}
