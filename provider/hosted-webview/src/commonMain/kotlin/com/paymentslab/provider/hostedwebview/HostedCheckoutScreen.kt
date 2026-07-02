package com.paymentslab.provider.hostedwebview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewState

/**
 * Renders one hosted gateway's checkout page and watches every navigation for the return-URL
 * pattern. `docs: https://github.com/KevinnZou/compose-webview-multiplatform` — `state.lastLoadedUrl`
 * is the interception point; the moment [matchReturn] recognizes a redirect as terminal, [onResult]
 * fires exactly once (the `reported` guard survives re-navigation inside the same page load).
 */
@Composable
fun HostedCheckoutScreen(
    checkoutUrl: String,
    matchReturn: (String) -> HostedReturnOutcome?,
    onResult: (HostedReturnOutcome) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberWebViewState(checkoutUrl)
    val navigator = rememberWebViewNavigator()
    var reported by remember { mutableStateOf(false) }

    LaunchedEffect(state.lastLoadedUrl) {
        if (reported) return@LaunchedEffect
        val url = state.lastLoadedUrl ?: return@LaunchedEffect
        matchReturn(url)?.let { outcome ->
            reported = true
            onResult(outcome)
        }
    }

    WebView(state = state, navigator = navigator, modifier = modifier.fillMaxSize())
}

/**
 * Subscribes to [HostedCheckoutRelay.requests] and renders [HostedCheckoutScreen] for whichever
 * hosted gateway is currently mid-checkout; reports the outcome back through the same relay so
 * [HostedWebViewGateway.pay] resumes. `configs` is the app's assembled list of [HostedGatewayConfig].
 */
@Composable
fun HostedCheckoutHost(
    relay: HostedCheckoutRelay,
    configs: List<HostedGatewayConfig>,
    modifier: Modifier = Modifier,
) {
    val request by relay.requests.collectAsState()
    val activeRequest = request ?: return
    val config = configs.firstOrNull { it.gatewayId == activeRequest.gatewayId } ?: return

    HostedCheckoutScreen(
        checkoutUrl = activeRequest.checkoutUrl,
        matchReturn = config.matchReturn,
        onResult = { outcome -> relay.reportResult(activeRequest.gatewayId, outcome) },
        modifier = modifier,
    )
}
