package com.paymentslab.backend

import com.paymentslab.core.config.CredentialMode
import com.paymentslab.core.config.EnvCredentialStore
import com.paymentslab.core.config.GatewayCredentials
import com.paymentslab.core.paymentsapi.GatewayId

/**
 * Server configuration + secrets, read from the environment with safe TEST defaults.
 *
 * NEVER hardcode a real secret here. The defaults below are obvious placeholders used for local dev
 * and the test suite only; a real deployment injects the values via the environment (see `.env.example`).
 */
data class ServerConfig(
    val port: Int,
    /** Externally-reachable base URL the WebView/hosted-checkout redirects target (emulator loopback locally). */
    val publicBaseUrl: String,
    val razorpayKeyId: String,
    val razorpaySecret: String,
    val razorpayWebhookSecret: String,
    val stripePublishableKey: String,
    val stripeSecret: String,
    val cashfreeAppId: String,
    val cashfreeSecret: String,
    /** PLAB_PAYSTACK_TEST_* — [GatewayCredentials.enabled] is false when unset; PaystackAdapter mock-falls-back. */
    val paystackCredentials: GatewayCredentials,
    /** PLAB_PAYPAL_TEST_* — same auto-degrade pattern as [paystackCredentials]. */
    val paypalCredentials: GatewayCredentials,
    /** PLAB_SQUARE_TEST_* (application_id/access_token/location_id) — same auto-degrade pattern. */
    val squareCredentials: GatewayCredentials,
) {
    companion object {
        private fun env(
            name: String,
            default: String,
        ): String = System.getenv(name)?.takeIf { it.isNotBlank() } ?: default

        fun fromEnv(): ServerConfig {
            val credentialStore = EnvCredentialStore(System.getenv())
            return ServerConfig(
                port = System.getenv("PORT")?.toIntOrNull() ?: 8080,
                publicBaseUrl = env("PLAB_PUBLIC_BASE_URL", "http://10.0.2.2:${System.getenv("PORT") ?: "8080"}"),
                // Test defaults — real values come from the environment in production.
                razorpayKeyId = env("RAZORPAY_KEY_ID", "rzp_test_1234567890"),
                razorpaySecret = env("RAZORPAY_SECRET", "test_razorpay_secret"),
                razorpayWebhookSecret = env("RAZORPAY_WEBHOOK_SECRET", "test_razorpay_webhook_secret"),
                stripePublishableKey = env("STRIPE_PUBLISHABLE_KEY", "pk_test_1234567890"),
                stripeSecret = env("STRIPE_SECRET", "sk_test_1234567890"),
                cashfreeAppId = env("CASHFREE_APP_ID", "test_cashfree_app_id"),
                cashfreeSecret = env("CASHFREE_SECRET", "test_cashfree_secret"),
                paystackCredentials =
                    credentialStore.credentialsFor(
                        gatewayId = GatewayId("paystack"),
                        mode = CredentialMode.TEST,
                        requiredKeyNames = listOf("secret_key"),
                    ),
                paypalCredentials =
                    credentialStore.credentialsFor(
                        gatewayId = GatewayId("paypal"),
                        mode = CredentialMode.TEST,
                        requiredKeyNames = listOf("client_id", "client_secret"),
                    ),
                squareCredentials =
                    credentialStore.credentialsFor(
                        gatewayId = GatewayId("square"),
                        mode = CredentialMode.TEST,
                        requiredKeyNames = listOf("application_id", "access_token", "location_id"),
                    ),
            )
        }
    }
}
