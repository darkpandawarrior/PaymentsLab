package com.paymentslab.feature.lab

import com.paymentslab.core.orchestration.PaymentFlowRunner
import com.paymentslab.core.paymentsapi.GatewayId
import com.paymentslab.core.paymentsapi.PaymentHost
import com.paymentslab.core.paymentsapi.PaymentStep
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

object TestHost : PaymentHost

/** A [PaymentFlowRunner] that replays a fixed, scripted sequence of steps — no real orchestrator. */
class FakePaymentFlowRunner(
    private val script: List<PaymentStep>,
) : PaymentFlowRunner {
    var runCount = 0
        private set

    override fun run(
        host: PaymentHost,
        gatewayId: GatewayId,
        catalogItemId: String,
    ): Flow<PaymentStep> {
        runCount++
        return script.asFlow()
    }
}
