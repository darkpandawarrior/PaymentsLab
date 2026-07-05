package com.paymentslab.feature.checkoutdemo

import com.paymentslab.core.orchestration.PaymentFlowRunner
import com.paymentslab.core.paymentsapi.Capability
import com.paymentslab.core.paymentsapi.CreatedOrder
import com.paymentslab.core.paymentsapi.GatewayId
import com.paymentslab.core.paymentsapi.GatewayMeta
import com.paymentslab.core.paymentsapi.GatewayStatus
import com.paymentslab.core.paymentsapi.PaymentGateway
import com.paymentslab.core.paymentsapi.PaymentGatewayRegistry
import com.paymentslab.core.paymentsapi.PaymentHost
import com.paymentslab.core.paymentsapi.PaymentResult
import com.paymentslab.core.paymentsapi.PaymentStep
import com.paymentslab.core.paymentsapi.PreparedPayment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

object TestHost : PaymentHost

/** A [PaymentFlowRunner] that replays a fixed, scripted sequence of steps. */
class FakePaymentFlowRunner(
    private val script: List<PaymentStep>,
) : PaymentFlowRunner {
    var lastCatalogItemId: String? = null
        private set
    var lastGatewayId: GatewayId? = null
        private set

    override fun run(
        host: PaymentHost,
        gatewayId: GatewayId,
        catalogItemId: String,
    ): Flow<PaymentStep> {
        lastCatalogItemId = catalogItemId
        lastGatewayId = gatewayId
        return script.asFlow()
    }
}

/** A gateway that only carries catalog metadata — never exercised by the ViewModel. */
private class MetaOnlyGateway(
    override val id: GatewayId,
    status: GatewayStatus,
    name: String,
) : PaymentGateway {
    override val meta =
        GatewayMeta(
            displayName = name,
            status = status,
            capabilities = setOf(Capability.ONE_TIME_PAYMENT),
            region = "IN",
            docsPath = "docs/${id.value}.md",
            blurb = "fake",
        )

    override suspend fun prepare(created: CreatedOrder): PreparedPayment = error("unused")

    override suspend fun pay(
        host: PaymentHost,
        prepared: PreparedPayment,
    ): PaymentResult = error("unused")
}

/** In-memory registry seeded with a mix of statuses so SANDBOX_READY filtering can be asserted. */
class FakeRegistry(
    override val gateways: List<PaymentGateway>,
) : PaymentGatewayRegistry {
    override fun byId(id: GatewayId): PaymentGateway? = gateways.firstOrNull { it.id == id }

    override fun withCapability(capability: Capability): List<PaymentGateway> =
        gateways.filter { capability in it.meta.capabilities }
}

fun sandboxAndGatedRegistry(): PaymentGatewayRegistry =
    FakeRegistry(
        listOf(
            MetaOnlyGateway(GatewayId("upi_intent"), GatewayStatus.SANDBOX_READY, "UPI Intent"),
            MetaOnlyGateway(GatewayId("razorpay"), GatewayStatus.SANDBOX_READY, "Razorpay"),
            MetaOnlyGateway(GatewayId("stripe"), GatewayStatus.KYC_GATED, "Stripe"),
            MetaOnlyGateway(GatewayId("cashfree"), GatewayStatus.COMING_SOON, "Cashfree"),
        ),
    )
