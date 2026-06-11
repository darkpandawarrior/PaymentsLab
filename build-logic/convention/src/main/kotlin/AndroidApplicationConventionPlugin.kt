import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Convention plugin for the Android application module. Applies AGP application + Kotlin-android +
 * Compose-compiler and the shared android config (compileSdk 37, Java 21, Compose enabled).
 * App-specific config (applicationId, minSdk/targetSdk, version, buildTypes) stays in :app.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            // AGP 9 provides built-in Kotlin support — applying kotlin.android is no longer needed.
            apply("com.android.application")
            apply("org.jetbrains.kotlin.plugin.compose")
        }
        configureComposeCompilerMetrics()
        extensions.configure<ApplicationExtension> {
            compileSdk = 37
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
            buildFeatures {
                compose = true
                buildConfig = true
            }
        }
    }
}
