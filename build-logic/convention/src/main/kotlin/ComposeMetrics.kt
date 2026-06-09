import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

/**
 * Opt-in Compose compiler metrics + stability reports. Off by default (zero cost on normal builds).
 * Pass `-Pcompose.metrics` to emit per-module composable metrics + stability reports for spotting
 * unstable parameters / unnecessary recompositions.
 */
internal fun Project.configureComposeCompilerMetrics() {
    if (!providers.gradleProperty("compose.metrics").isPresent) return

    extensions.configure<ComposeCompilerGradlePluginExtension> {
        metricsDestination.set(layout.buildDirectory.dir("compose-metrics"))
        reportsDestination.set(layout.buildDirectory.dir("compose-reports"))
    }
}
