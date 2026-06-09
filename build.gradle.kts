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

// Detekt 1.23.x rejects JDK 23+; skip on incompatible JVMs (see subprojects block for detail).
val detektJvmMajor: Int = JavaVersion.current().majorVersion.toIntOrNull() ?: 0
tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    enabled = detektJvmMajor <= 22
    jvmTarget = "17"
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
    extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        // Never lint generated code (Room KSP, etc.).
        filter { exclude { entry -> entry.file.path.contains("${"/build/"}") } }
    }
    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        buildUponDefaultConfig = true
        baseline = file("detekt-baseline.xml")
    }
    // Detekt 1.23.x cannot run on JDK 23+ (it rejects the runtime version). The Gradle JDK here is
    // newer, and no JDK <=22 toolchain is installed, so detekt is skipped on incompatible JVMs rather
    // than failing the build. It runs normally under JDK 17-22. jvmTarget capped at 22 (detekt's max).
    val detektRunnable = detektJvmMajor <= 22
    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        enabled = detektRunnable
        jvmTarget = "17"
    }
    tasks.withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach {
        enabled = detektRunnable
        jvmTarget = "17"
    }
}

// ── Workflow task aliases: the local dev + CI verification loop ──────────────
tasks.register("fastGate") {
    description = "ktlint + detekt + JVM/common unit tests: the fast CI gate (no emulator)."
    dependsOn("ktlintCheck", "detekt")
    findProject(":backend")?.let { dependsOn(":backend:test") }
    findProject(":core:orchestration")?.let { dependsOn(":core:orchestration:testAndroidHostTest") }
}
