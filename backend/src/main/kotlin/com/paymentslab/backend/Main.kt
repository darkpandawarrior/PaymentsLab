package com.paymentslab.backend

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

/**
 * Entrypoint. Boots Ktor on Netty at PORT (default 8080). Secrets are read from the environment with
 * safe test defaults (see [ServerConfig] and `.env.example`). Not exercised by tests — `testApplication`
 * bypasses Netty entirely.
 */
fun main() {
    val config = ServerConfig.fromEnv()
    embeddedServer(Netty, port = config.port) { module(config) }.start(wait = true)
}
