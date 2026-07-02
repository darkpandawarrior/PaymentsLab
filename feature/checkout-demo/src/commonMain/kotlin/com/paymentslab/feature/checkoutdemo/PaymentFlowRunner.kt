package com.paymentslab.feature.checkoutdemo

import com.paymentslab.core.orchestration.PaymentOrchestrator
import com.paymentslab.core.paymentsapi.GatewayId
import com.paymentslab.core.paymentsapi.PaymentHost
import com.paymentslab.core.paymentsapi.PaymentStep
import kotlinx.coroutines.flow.Flow

/**
 * A one-method seam over [PaymentOrchestrator.pay] so [CheckoutViewModel] can be exercised in
 * `commonTest` with a scripted [Flow] of [PaymentStep]s. The production binding delegates straight
 * to the orchestrator.
 */
fun interface PaymentFlowRunner {
    fun run(
        host: PaymentHost,
        gatewayId: GatewayId,
        catalogItemId: String,
    ): Flow<PaymentStep>
}

/** Production [PaymentFlowRunner] — forwards to the real [PaymentOrchestrator]. */
class OrchestratorFlowRunner(
    private val orchestrator: PaymentOrchestrator,
) : PaymentFlowRunner {
    override fun run(
        host: PaymentHost,
        gatewayId: GatewayId,
        catalogItemId: String,
    ): Flow<PaymentStep> = orchestrator.pay(host, gatewayId, catalogItemId)
}
