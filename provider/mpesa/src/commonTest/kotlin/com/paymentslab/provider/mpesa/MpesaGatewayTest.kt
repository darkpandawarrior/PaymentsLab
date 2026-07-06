package com.paymentslab.provider.mpesa

import com.paymentslab.core.network.HttpClientFactory
import com.paymentslab.core.network.PaymentApiConfig
import com.paymentslab.core.network.create
import com.paymentslab.core.paymentsapi.GatewayId
import com.paymentslab.core.paymentsapi.GatewayStatus
import com.paymentslab.core.paymentsapi.Money
import com.paymentslab.core.paymentsapi.PaymentResult
import com.paymentslab.core.paymentsapi.PendingReason
import com.paymentslab.core.paymentsapi.PreparedPayment
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MpesaGatewayTest {
    private val config = MpesaConfig()
    private val prepared =
        PreparedPayment(
            gatewayId = GatewayId("mpesa"),
            orderId = "order_1",
            amount = Money.inr(100),
            params = emptyMap(),
        )

    /** Mirrors MobileMoneyGatewayTest: unreachable host so the mock-flip POST fails fast. */
    @Test
    fun `pay returns Pending even if the mock mpesa trigger request fails`() =
        runTest {
            val httpClient = HttpClientFactory().create()
            val gateway = MpesaGateway(config, httpClient, PaymentApiConfig(baseUrl = "http://127.0.0.1:1"))

            val result = gateway.pay(host = FakeHost, prepared = prepared)

            val pending = assertIs<PaymentResult.Pending>(result)
            assertEquals(PendingReason.AWAITING_WEBHOOK, pending.reason)
        }

    @Test
    fun `meta is built from config`() {
        val httpClient = HttpClientFactory().create()
        val gateway = MpesaGateway(config, httpClient, PaymentApiConfig())

        assertEquals("M-Pesa", gateway.meta.displayName)
        assertEquals(GatewayStatus.MOCK_MODE, gateway.meta.status)
        assertEquals(GatewayId("mpesa"), gateway.id)
    }

    private object FakeHost : com.paymentslab.core.paymentsapi.PaymentHost
}
