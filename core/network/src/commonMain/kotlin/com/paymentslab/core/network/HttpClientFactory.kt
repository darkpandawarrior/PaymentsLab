package com.paymentslab.core.network

import com.paymentslab.core.common.AppLog
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Builds the single shared Ktor [HttpClient] for the app.
 *
 * The engine is the only platform-specific piece — OkHttp on Android, Darwin on iOS — so it is the
 * `expect`/`actual` seam. Everything else (JSON content negotiation, logging) is configured once in
 * common code against that engine, keeping the client setup identical across targets.
 */
expect class HttpClientFactory() {
    /** Provides the platform engine: OkHttp (androidMain) / Darwin (iosMain). */
    fun engine(): HttpClientEngine
}

private const val TAG = "PaymentsHttp"

/**
 * Assembles the [HttpClient] from the platform [HttpClientFactory.engine].
 *
 * - `ContentNegotiation(Json { ignoreUnknownKeys = true })` — server-added fields never break the
 *   client (forward compatibility), matching the DTOs in `core:protocol`.
 * - `Logging` routes through [AppLog] so request/response lines land in the same KMP log facade as
 *   the rest of the app instead of Ktor's default stdout.
 */
fun HttpClientFactory.create(): HttpClient =
    HttpClient(engine()) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                },
            )
        }
        install(Logging) {
            level = LogLevel.INFO
            logger =
                object : io.ktor.client.plugins.logging.Logger {
                    override fun log(message: String) = AppLog.d(TAG, message)
                }
        }
    }
