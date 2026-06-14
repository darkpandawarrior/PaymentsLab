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
    // Aggregate coverage from the logic-bearing modules (skip UI-only + generated-heavy ones,
    // which would just dilute the number). Guarded so the project still configures if a module moves.
    listOf(
        ":core:orchestration",
        ":core:network",
        ":core:data",
        ":backend",
    ).forEach { path -> findProject(path)?.let { kover(it) } }
}

kover {
    reports {
        filters {
            excludes {
                packages("*.BuildConfig", "*.R")
                classes("*Fake*", "*Test*")
            }
        }
        verify {
            rule {
                // Coverage floor across the aggregated logic modules — fails the build on regression.
                minBound(50)
            }
        }
    }
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "org.jetbrains.kotlinx.kover")
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
    // Detekt 1.23.x cannot run on JDK 23+; the whole build is pinned to JDK 21 (see gradle.properties).
    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach { jvmTarget = "21" }
    tasks.withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach { jvmTarget = "21" }
}

// ── Workflow task aliases: the local dev + CI verification loop ──────────────
tasks.register("fastGate") {
    description = "ktlint + detekt + unit tests + coverage floor + dependency lock: the fast CI gate."
    dependsOn("ktlintCheck", "detekt", "koverVerify")
    findProject(":app")?.let { dependsOn(":app:dependencyGuard") }
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
