package com.paymentslab.ios.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.paymentslab.core.designsystem.PaymentsLabTheme
import com.paymentslab.core.paymentsapi.GatewayId
import com.paymentslab.core.paymentsapi.PaymentHost
import com.paymentslab.feature.lab.LabHomeRoot
import com.paymentslab.feature.lab.ProviderLabRoot

/**
 * A dummy [PaymentHost] — every gateway wired into [iosHostedGatewayConfigs]/
 * [iosMobileMoneyConfigs] only needs the opaque marker, never an Activity/ViewController-typed
 * cast (unlike Android's native-SDK archetype-A providers, which are correctly absent here).
 */
private object IosPaymentHost : PaymentHost

/**
 * The iOS app's whole UI: catalog → provider lab, using plain `remember`-backed state instead of
 * `jb.navigation.compose`'s full graph — a deliberately smaller nav shape than the Android app's
 * `AppNavHost`, sufficient for the two screens this module ships (see `ios/shared/build.gradle.kts`
 * doc for why the scope is narrower).
 */
@Composable
fun AppRoot() {
    PaymentsLabTheme {
        var selected by remember { mutableStateOf<SelectedProvider?>(null) }
        val current = selected

        if (current == null) {
            LabHomeRoot(onOpenProvider = { id -> selected = SelectedProvider(id) })
        } else {
            ProviderLabRoot(
                paymentHost = IosPaymentHost,
                gatewayId = current.gatewayId,
                providerName = current.gatewayId.value,
                priceLabel = "₹149",
                catalogItemId = "coffee_149",
                onBack = { selected = null },
            )
        }
    }
}

private data class SelectedProvider(
    val gatewayId: GatewayId,
)
