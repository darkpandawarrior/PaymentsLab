package com.paymentslab.core.network.di

import com.paymentslab.core.network.HttpClientFactory
import com.paymentslab.core.network.KtorPaymentBackend
import com.paymentslab.core.network.PaymentApiConfig
import com.paymentslab.core.network.create
import com.paymentslab.core.paymentsapi.PaymentBackend
import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin wiring for the network layer. Assembled into the app's Koin graph at the `:app` composition
 * root.
 *
 * Bindings:
 *  - [PaymentApiConfig] — default base URL; override at the composition root for other environments.
 *  - [HttpClient] — one shared client built from the platform engine via [HttpClientFactory].
 *  - [PaymentBackend] — the [KtorPaymentBackend], bound to the interface so the orchestrator depends
 *    only on `core:payments-api` and never on this Ktor implementation.
 */
val networkModule: Module =
    module {
        single { PaymentApiConfig() }
        single<HttpClient> { HttpClientFactory().create() }
        single<PaymentBackend> { KtorPaymentBackend(get(), get()) }
    }
