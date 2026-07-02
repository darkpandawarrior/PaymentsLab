package com.paymentslab.core.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

/** Android engine: OkHttp — the mature, connection-pooling HTTP stack already familiar on Android. */
actual class HttpClientFactory actual constructor() {
    actual fun engine(): HttpClientEngine = OkHttp.create()
}
