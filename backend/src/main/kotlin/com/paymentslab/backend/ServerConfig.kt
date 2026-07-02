package com.paymentslab.backend

/**
 * Server configuration + secrets, read from the environment with safe TEST defaults.
 *
 * NEVER hardcode a real secret here. The defaults below are obvious placeholders used for local dev
 * and the test suite only; a real deployment injects the values via the environment (see `.env.example`).
 */
data class ServerConfig(
    val port: Int,
    val razorpayKeyId: String,
    val razorpaySecret: String,
    val razorpayWebhookSecret: String,
    val stripePublishableKey: String,
    val stripeSecret: String,
    val cashfreeAppId: String,
    val cashfreeSecret: String,
) {
    companion object {
        private fun env(
            name: String,
            default: String,
        ): String = System.getenv(name)?.takeIf { it.isNotBlank() } ?: default

        fun fromEnv(): ServerConfig =
            ServerConfig(
                port = System.getenv("PORT")?.toIntOrNull() ?: 8080,
                // Test defaults — real values come from the environment in production.
                razorpayKeyId = env("RAZORPAY_KEY_ID", "rzp_test_1234567890"),
                razorpaySecret = env("RAZORPAY_SECRET", "test_razorpay_secret"),
                razorpayWebhookSecret = env("RAZORPAY_WEBHOOK_SECRET", "test_razorpay_webhook_secret"),
                stripePublishableKey = env("STRIPE_PUBLISHABLE_KEY", "pk_test_1234567890"),
                stripeSecret = env("STRIPE_SECRET", "sk_test_1234567890"),
                cashfreeAppId = env("CASHFREE_APP_ID", "test_cashfree_app_id"),
                cashfreeSecret = env("CASHFREE_SECRET", "test_cashfree_secret"),
            )
    }
}
