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

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach { jvmTarget = "21" }

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
        // detekt's default `detekt` task only scans src/main; KMP modules use commonMain/androidMain,
        // so point it at every source root we use (missing dirs are ignored).
        source.setFrom(
            "src/commonMain/kotlin",
            "src/androidMain/kotlin",
            "src/jvmMain/kotlin",
            "src/iosMain/kotlin",
            "src/main/kotlin",
            "src/main/java",
        )
    }
    // Detekt 1.23.x cannot run on JDK 23+; the daemon is pinned to JDK 17 via
    // gradle/gradle-daemon-jvm.properties so detekt runs normally.
    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach { jvmTarget = "21" }
    tasks.withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach { jvmTarget = "21" }
}

// ── Workflow task aliases: the local dev + CI verification loop ──────────────
tasks.register("fastGate") {
    description = "ktlint + detekt + JVM/common unit tests: the fast CI gate (no emulator)."
    dependsOn("ktlintCheck", "detekt")
    findProject(":backend")?.let { dependsOn(":backend:test") }
    listOf(
        ":core:orchestration",
        ":core:data",
        ":core:network",
        ":feature:lab",
        ":feature:history",
        ":feature:checkout-demo",
    ).forEach { path -> findProject(path)?.let { dependsOn("$path:testAndroidHostTest") } }
    listOf(":provider:upi-intent", ":provider:razorpay", ":provider:cashfree").forEach { path ->
        findProject(path)?.let { dependsOn("$path:testDebugUnitTest") }
    }
}
