package com.paymentslab.core.network.di

import com.paymentslab.core.network.HttpClientFactory
import com.paymentslab.core.network.KtorConnectBackend
import com.paymentslab.core.network.KtorPaymentBackend
import com.paymentslab.core.network.KtorPayoutBackend
import com.paymentslab.core.network.KtorVaultBackend
import com.paymentslab.core.network.PaymentApiConfig
import com.paymentslab.core.network.create
import com.paymentslab.core.paymentsapi.ConnectBackend
import com.paymentslab.core.paymentsapi.PaymentBackend
import com.paymentslab.core.paymentsapi.PayoutBackend
import com.paymentslab.core.paymentsapi.VaultBackend
import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin wiring for the network layer. Assembled into the app's Koin graph at the `:app` composition
 * root.
 *
 * Bindings:
 *  - [PaymentApiConfig] — passed in ([config]); the app supplies the per-environment base URL from
 *    `BuildConfig.BACKEND_URL` (localhost for debug/vapt, the real host for release).
 *  - [HttpClient] — one shared client built from the platform engine via [HttpClientFactory].
 *  - [PaymentBackend] — the [KtorPaymentBackend], bound to the interface so the orchestrator depends
 *    only on `core:payments-api` and never on this Ktor implementation.
 */
fun networkModule(config: PaymentApiConfig = PaymentApiConfig()): Module =
    module {
        single { config }
        single<HttpClient> { HttpClientFactory().create() }
        single<PaymentBackend> { KtorPaymentBackend(get(), get()) }
        single<PayoutBackend> { KtorPayoutBackend(get(), get()) }
        single<VaultBackend> { KtorVaultBackend(get(), get()) }
        single<ConnectBackend> { KtorConnectBackend(get(), get()) }
    }
