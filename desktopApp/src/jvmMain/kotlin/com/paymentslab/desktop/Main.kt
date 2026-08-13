package com.paymentslab.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.paymentslab.core.designsystem.AppShell
import com.paymentslab.core.designsystem.AppShellDestination
import com.paymentslab.core.designsystem.PaymentsLabTheme

/** ~9:19.5 portrait — the standard phone frame. Resizable, so drag a corner to test breakpoints. */
private val PhoneCanvasSize = DpSize(width = 390.dp, height = 844.dp)

private val destinations =
    listOf(
        AppShellDestination("explore", "Explore", Icons.Filled.Search),
    )

fun main() {
    // Compose Hot Reload sets this on the JVM it launches (see the `-Dcompose.reload.isActive=true`
    // entry in desktopApp/build/run/jvmMain/jvmMain.argfile). Under `hotRunJvm` the window becomes a
    // phone-shaped, always-on-top canvas that floats beside the editor — the whole point of running
    // UI on the JVM instead of booting an emulator. A plain `run` keeps a normal desktop window.
    val hotReloadCanvas = System.getProperty("compose.reload.isActive").toBoolean()

    application {
        val windowState =
            rememberWindowState(
                size = if (hotReloadCanvas) PhoneCanvasSize else DpSize(1100.dp, 800.dp),
            )
        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            alwaysOnTop = hotReloadCanvas,
            title = if (hotReloadCanvas) "PaymentsLab — Hot Reload canvas" else "PaymentsLab Design System",
        ) {
            PaymentsLabTheme {
                DesignSystemCanvas()
            }
        }
    }
}

@Composable
private fun DesignSystemCanvas() {
    var selected by remember { mutableStateOf("explore") }
    AppShell(
        destinations = destinations,
        selectedRoute = selected,
        onSelectDestination = { selected = it },
        onFabClick = {},
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("PaymentsLab design system", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Edit anything in :core:designsystem and save — Compose Hot Reload patches this " +
                    "window in place, no emulator and no APK build.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text("Display", style = MaterialTheme.typography.displaySmall)
            Text("Headline", style = MaterialTheme.typography.headlineMedium)
            Text("Title", style = MaterialTheme.typography.titleMedium)
            Text("Body", style = MaterialTheme.typography.bodyLarge)
            Text("Label", style = MaterialTheme.typography.labelSmall)
        }
    }
}
