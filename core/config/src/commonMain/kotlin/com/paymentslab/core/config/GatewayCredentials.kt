package com.paymentslab.core.config

import com.paymentslab.core.paymentsapi.GatewayId

/** Whether a resolved credential set is for the provider's sandbox or its live environment. */
enum class CredentialMode {
    TEST,
    LIVE,
}

/**
 * The env-resolved keys for one gateway/mode, e.g. `PLAB_RAZORPAY_TEST_KEY_ID` → `key_id`. A gateway
 * is only [enabled] once every key it declared as required actually resolved — a partial set (e.g.
 * `key_id` but no `secret`) is treated as absent so the app never launches a half-configured SDK.
 */
data class GatewayCredentials(
    val gatewayId: GatewayId,
    val mode: CredentialMode,
    val keys: Map<String, String>,
    val requiredKeyNames: List<String>,
) {
    val enabled: Boolean
        get() = requiredKeyNames.isNotEmpty() && requiredKeyNames.all { keys.containsKey(it) }
}
