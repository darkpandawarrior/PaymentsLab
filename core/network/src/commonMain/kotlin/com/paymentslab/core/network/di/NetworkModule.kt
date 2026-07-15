package com.paymentslab.core.network.di

import com.paymentslab.core.network.KtorConnectBackend
import com.paymentslab.core.network.KtorPaymentBackend
import com.paymentslab.core.network.KtorPayoutBackend
import com.paymentslab.core.network.KtorVaultBackend
import com.siddharth.kmp.common.AppLog
import com.siddharth.kmp.network.createHttpClient
import com.siddharth.kmp.paymentsapi.ConnectBackend
import com.siddharth.kmp.paymentsapi.PaymentApiConfig
import com.siddharth.kmp.paymentsapi.PaymentBackend
import com.siddharth.kmp.paymentsapi.PayoutBackend
import com.siddharth.kmp.paymentsapi.VaultBackend
import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.Logger
import org.koin.core.module.Module
import org.koin.dsl.module

private const val TAG = "PaymentsHttp"

/**
 * Koin wiring for the network layer. Assembled into the app's Koin graph at the `:app` composition
 * root.
 *
 * Bindings:
 *  - [PaymentApiConfig] — passed in ([config]); the app supplies the per-environment base URL from
 *    `BuildConfig.BACKEND_URL` (localhost for debug/vapt, the real host for release).
 *  - [HttpClient] (default/unqualified) — built via kmp-toolkit's `:network` `createHttpClient`
 *    (retry/timeout seams, superset target set). Every consumer — every provider gateway plus the
 *    [KtorPaymentBackend] family below — resolves this one with a plain `get()`. Its Ktor logging is
 *    routed through [AppLog] so it lands in the same log pipeline as the rest of the app instead of
 *    println/Logcat directly.
 *  - [PaymentBackend] — the [KtorPaymentBackend], bound to the interface so the orchestrator depends
 *    only on `core:payments-api` and never on this Ktor implementation.
 */
fun networkModule(config: PaymentApiConfig = PaymentApiConfig()): Module =
    module {
        single { config }
        single<HttpClient> {
            createHttpClient(
                logger =
                    object : Logger {
                        override fun log(message: String) = AppLog.d(message, tag = TAG)
                    },
            )
        }
        single<PaymentBackend> { KtorPaymentBackend(get(), get()) }
        single<PayoutBackend> { KtorPayoutBackend(get(), get()) }
        single<VaultBackend> { KtorVaultBackend(get(), get()) }
        single<ConnectBackend> { KtorConnectBackend(get(), get()) }
    }
