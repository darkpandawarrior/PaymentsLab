package com.paymentslab.core.network

/**
 * Where the app finds the `backend/` Ktor server.
 *
 * The default [baseUrl] is `http://10.0.2.2:8080` — the Android emulator's alias for the host
 * machine's loopback (`127.0.0.1`). Inside the emulator, `localhost` resolves to the emulated
 * device itself, so the special `10.0.2.2` address is required to reach a server running on the
 * developer's laptop. `8080` is the port the `backend/` module listens on.
 *
 * Override for other environments:
 *  - Physical device on the same LAN: `PaymentApiConfig("http://<laptop-lan-ip>:8080")`.
 *  - iOS simulator (shares the host network stack): `PaymentApiConfig("http://localhost:8080")`.
 *  - Staging/prod: pass the real HTTPS URL.
 *
 * The value is injected as a Koin `single` (see [com.paymentslab.core.network.di.networkModule]),
 * so an environment override is a one-line change at the composition root, not a code edit here.
 */
data class PaymentApiConfig(
    val baseUrl: String = DEFAULT_BASE_URL,
) {
    companion object {
        /** Android emulator → host-machine loopback. See the class doc for other environments. */
        const val DEFAULT_BASE_URL: String = "http://10.0.2.2:8080"
    }
}
