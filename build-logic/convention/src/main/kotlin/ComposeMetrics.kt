import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

/**
 * Opt-in Compose compiler metrics + stability reports. Off by default (zero cost on normal builds).
 * Pass `-Pcompose.metrics` to emit per-module composable metrics + stability reports for spotting
 * unstable parameters / unnecessary recompositions.
 */
internal fun Project.configureComposeCompilerMetrics() {
    extensions.configure<ComposeCompilerGradlePluginExtension> {
        // Always applied: tell the compiler which external types are stable (fewer recompositions).
        val stabilityConfig = rootProject.layout.projectDirectory.file("compose_stability.conf")
        if (stabilityConfig.asFile.exists()) {
            stabilityConfigurationFiles.add(stabilityConfig)
        }
        // Opt-in metrics/reports (zero cost otherwise): pass -Pcompose.metrics.
        if (providers.gradleProperty("compose.metrics").isPresent) {
            metricsDestination.set(layout.buildDirectory.dir("compose-metrics"))
            reportsDestination.set(layout.buildDirectory.dir("compose-reports"))
        }
    }
}
