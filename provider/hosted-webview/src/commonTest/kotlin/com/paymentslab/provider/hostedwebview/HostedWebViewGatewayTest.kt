package com.paymentslab.provider.hostedwebview

import com.paymentslab.core.paymentsapi.Capability
import com.paymentslab.core.paymentsapi.GatewayId
import com.paymentslab.core.paymentsapi.GatewayStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class HostedWebViewGatewayTest {
    private val config =
        HostedGatewayConfig(
            gatewayId = GatewayId("mock_hosted"),
            displayName = "Mock Hosted",
            region = "Global",
            docsPath = "docs/providers/mock-hosted.md",
            blurb = "test",
            capabilities = setOf(Capability.CARDS),
            status = GatewayStatus.MOCK_MODE,
            buildCheckoutUrl = { "https://checkout.example/pay" },
            matchReturn = { null },
        )
    private val gateway = HostedWebViewGateway(config, HostedCheckoutRelay())

    @Test
    fun `success outcome maps to PaymentResult Success with payment id`() {
        val result = gateway.mapOutcome(HostedReturnOutcome.Success(paymentId = "pay_1"))

        assertEquals("pay_1", assertIs<com.paymentslab.core.paymentsapi.PaymentResult.Success>(result).paymentId)
    }

    @Test
    fun `failure outcome maps to PaymentResult Failure with reason as message`() {
        val result = gateway.mapOutcome(HostedReturnOutcome.Failure(reason = "card_declined"))

        val failure = assertIs<com.paymentslab.core.paymentsapi.PaymentResult.Failure>(result)
        assertEquals(com.paymentslab.core.paymentsapi.FailureCode.GATEWAY_DECLINED, failure.code)
    }

    @Test
    fun `cancelled outcome maps to PaymentResult Cancelled`() {
        val result = gateway.mapOutcome(HostedReturnOutcome.Cancelled)

        assertIs<com.paymentslab.core.paymentsapi.PaymentResult.Cancelled>(result)
    }
}
