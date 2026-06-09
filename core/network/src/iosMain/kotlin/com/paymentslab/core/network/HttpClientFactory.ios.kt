package com.paymentslab.core.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

/** iOS engine: Darwin — wraps `NSURLSession`, the native Apple networking stack. */
actual class HttpClientFactory actual constructor() {
    actual fun engine(): HttpClientEngine = Darwin.create()
}
