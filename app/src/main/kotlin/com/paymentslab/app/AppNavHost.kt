package com.paymentslab.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.paymentslab.core.paymentsapi.GatewayId
import com.paymentslab.core.paymentsapi.PaymentGatewayRegistry
import com.paymentslab.core.paymentsapi.PaymentHost
import com.paymentslab.core.security.SecureScreen
import com.paymentslab.feature.checkoutdemo.CheckoutRoot
import com.paymentslab.feature.history.HistoryRoot
import com.paymentslab.feature.lab.LabHomeRoot
import com.paymentslab.feature.lab.ProviderLabRoot
import org.koin.compose.koinInject

private data class TopDestination(val route: String, val label: String, val glyph: String)

private val topDestinations = listOf(
    TopDestination("lab", "Lab", "🧪"),
    TopDestination("checkout", "Checkout", "🛒"),
    TopDestination("history", "History", "🧾"),
)

/**
 * The app's navigation. Three top-level destinations (Lab, Checkout, History) plus a per-provider
 * lab detail. Features are decoupled — this is the only place their screens are composed together,
 * and the real [PaymentHost] is threaded down to the screens that launch payments.
 */
@Composable
fun AppNavHost(paymentHost: PaymentHost) {
    val navController = rememberNavController()
    val registry = koinInject<PaymentGatewayRegistry>()

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination
            NavigationBar {
                topDestinations.forEach { dest ->
                    val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Text(dest.glyph) },
                        label = { Text(dest.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "lab",
            modifier = Modifier.padding(padding),
        ) {
            composable("lab") {
                LabHomeRoot(
                    onOpenProvider = { gatewayId -> navController.navigate("provider/${gatewayId.value}") },
                )
            }
            composable("provider/{id}") { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                val meta = registry.byId(GatewayId(id))?.meta
                // SecureScreen: block screenshots / screen-recording / recents-thumbnail on the
                // payment-bearing screens, the way banking apps do.
                SecureScreen {
                    ProviderLabRoot(
                        paymentHost = paymentHost,
                        gatewayId = GatewayId(id),
                        providerName = meta?.displayName ?: id,
                        priceLabel = "₹499",
                        catalogItemId = "book_499",
                        onBack = { navController.popBackStack() },
                    )
                }
            }
            composable("checkout") {
                SecureScreen {
                    CheckoutRoot(paymentHost = paymentHost)
                }
            }
            composable("history") {
                HistoryRoot()
            }
        }
    }
}
