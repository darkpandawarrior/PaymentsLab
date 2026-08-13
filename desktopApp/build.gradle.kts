/**
 * `:desktopApp` — a Compose Desktop canvas for PaymentsLab's design system.
 *
 * Its reason to exist is the feedback loop, not distribution: Compose Multiplatform bundles Compose
 * Hot Reload, so `./gradlew :desktopApp:hotRunJvm` renders `:core:designsystem` in a phone-shaped
 * JVM window that live-reloads on save. Every other repo in the family already had a JVM target and
 * therefore this loop; PaymentsLab's UI modules were android+wasmJs only, so a UI tweak here meant
 * booting an emulator or a webpack dev server.
 *
 * Deliberately scoped to the design system. Rendering the feature roots (LabHomeRoot / CheckoutRoot)
 * additionally needs the Koin graph and in-memory fakes that today live in `:web`'s wasmJsMain
 * (WebKoin.kt / WebFakes.kt / WebGatewayConfigs.kt — 279 lines, none of which touch a browser API).
 * Promoting those to a shared source set would light up the feature screens here too; that is a
 * separate change, because `:web`'s wasmJsBrowserDistribution is a shipped artifact embedded by
 * cv-siddharth and is not worth destabilising for a dev-loop improvement.
 */
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvm()

    sourceSets {
        jvmMain.dependencies {
            implementation(project(":core:designsystem"))
            implementation(compose.desktop.currentOs)
            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.ui)
            implementation(libs.material3)
            implementation(libs.material.icons.extended)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.paymentslab.desktop.MainKt"
    }
}
