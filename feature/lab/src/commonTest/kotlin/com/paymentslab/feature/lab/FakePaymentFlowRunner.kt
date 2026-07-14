package com.paymentslab.feature.lab

import com.paymentslab.core.orchestration.PaymentFlowRunner
import com.siddharth.kmp.paymentsapi.GatewayId
import com.siddharth.kmp.paymentsapi.PaymentHost
import com.siddharth.kmp.paymentsapi.PaymentStep
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

object TestHost : PaymentHost

/**
 * A [PaymentFlowRunner] that replays a scripted sequence of steps — no real orchestrator. [scriptFor]
 * picks the script by zero-based call index (e.g. fail first run, succeed next); every idempotency
 * key it receives is captured in [keysReceived] for assertions.
 */
class FakePaymentFlowRunner(
    private val scriptFor: (callIndex: Int) -> List<PaymentStep>,
) : PaymentFlowRunner {
    constructor(script: List<PaymentStep>) : this({ script })

    var runCount = 0
        private set
    val keysReceived = mutableListOf<String>()

    override fun run(
        host: PaymentHost,
        gatewayId: GatewayId,
        catalogItemId: String,
        idempotencyKey: String,
    ): Flow<PaymentStep> {
        val callIndex = runCount
        runCount++
        keysReceived += idempotencyKey
        return scriptFor(callIndex).asFlow()
    }
}
