package com.paymentslab.core.network.di

import com.paymentslab.core.common.AppLog
import com.paymentslab.core.network.HttpClientFactory
import com.paymentslab.core.network.KtorConnectBackend
import com.paymentslab.core.network.KtorPaymentBackend
import com.paymentslab.core.network.KtorPayoutBackend
import com.paymentslab.core.network.KtorVaultBackend
import com.paymentslab.core.network.create
import com.siddharth.kmp.network.createHttpClient
import com.siddharth.kmp.paymentsapi.ConnectBackend
import com.siddharth.kmp.paymentsapi.PaymentApiConfig
import com.siddharth.kmp.paymentsapi.PaymentBackend
import com.siddharth.kmp.paymentsapi.PayoutBackend
import com.siddharth.kmp.paymentsapi.VaultBackend
import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.Logger
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Qualifier for the legacy [HttpClient] built by the in-app [HttpClientFactory] — kept only for the
 * [KtorPaymentBackend]-family orchestration classes below (minimize churn: their behavior is
 * unchanged by this module's extraction). Every provider gateway (mobile-money/mpesa/wallet/xendit,
 * plus any future one) resolves the DEFAULT, unqualified [HttpClient] instead — see [networkModule].
 */
private val LEGACY_HTTP_CLIENT = named("legacyPaymentsHttp")

private const val TAG = "PaymentsHttp"

/**
 * Koin wiring for the network layer. Assembled into the app's Koin graph at the `:app` composition
 * root.
 *
 * Bindings:
 *  - [PaymentApiConfig] — passed in ([config]); the app supplies the per-environment base URL from
 *    `BuildConfig.BACKEND_URL` (localhost for debug/vapt, the real host for release).
 *  - [HttpClient] (default/unqualified) — built via kmp-toolkit's `:network` `createHttpClient`
 *    (retry/timeout seams, superset target set). Every provider module's Koin definition resolves
 *    this one with a plain `get()`. Its Ktor logging is routed through [AppLog] so it lands in the
 *    same log pipeline as the rest of the app instead of println/Logcat directly.
 *  - [LEGACY_HTTP_CLIENT]-qualified [HttpClient] — the original in-app client from
 *    [HttpClientFactory], kept only for the [KtorPaymentBackend]-family orchestration classes so
 *    their behavior doesn't shift as part of this extraction.
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
                        override fun log(message: String) = AppLog.d(TAG, message)
                    },
            )
        }
        single<HttpClient>(LEGACY_HTTP_CLIENT) { HttpClientFactory().create() }
        single<PaymentBackend> { KtorPaymentBackend(get(LEGACY_HTTP_CLIENT), get()) }
        single<PayoutBackend> { KtorPayoutBackend(get(LEGACY_HTTP_CLIENT), get()) }
        single<VaultBackend> { KtorVaultBackend(get(LEGACY_HTTP_CLIENT), get()) }
        single<ConnectBackend> { KtorConnectBackend(get(LEGACY_HTTP_CLIENT), get()) }
    }
