package com.paymentslab.core.orchestration

import com.siddharth.kmp.paymentsapi.GatewayId
import com.siddharth.kmp.paymentsapi.PaymentHost
import com.siddharth.kmp.paymentsapi.PaymentStep
import kotlinx.coroutines.flow.Flow

/**
 * A one-method seam over [PaymentOrchestrator.pay] so feature ViewModels can be exercised in
 * `commonTest` with a scripted [Flow] of [PaymentStep]s — no backend, no journal, no real SDK.
 * The production binding delegates straight to the orchestrator.
 */
fun interface PaymentFlowRunner {
    fun run(
        host: PaymentHost,
        gatewayId: GatewayId,
        catalogItemId: String,
        idempotencyKey: String,
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
        idempotencyKey: String,
    ): Flow<PaymentStep> = orchestrator.pay(host, gatewayId, catalogItemId, idempotencyKey)
}
