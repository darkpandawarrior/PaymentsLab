import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Convention plugin for a Kotlin Multiplatform **Compose** library targeting Android + iOS.
 *
 * Builds on [KmpLibraryConventionPlugin] and adds the Compose Multiplatform + Compose compiler
 * plugins. Consuming modules declare only their `android { namespace/... }` block + source sets.
 */
class KmpComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("paymentslab.kmp.library")
            apply("org.jetbrains.compose")
            apply("org.jetbrains.kotlin.plugin.compose")
        }
        configureComposeCompilerMetrics()
    }
}
