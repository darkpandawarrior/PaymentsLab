package com.paymentslab.provider.mpesa

import com.siddharth.kmp.paymentsapi.Capability
import com.siddharth.kmp.paymentsapi.GatewayId
import com.siddharth.kmp.paymentsapi.GatewayStatus

/**
 * M-Pesa Daraja (STK push) — moved out of the generic [com.paymentslab.provider.mobilemoney]
 * fan-out into its own module so it gets its own mock webhook settle route
 * (`POST /mock/mpesa/{orderId}/settle`) instead of the shared `/mock/momo/{provider}` delayed-flip,
 * matching the reference archetype-D shape 1:1 per roadmap #10. Same PENDING/AWAITING_WEBHOOK
 * result — only the settle route ownership changes.
 */
data class MpesaConfig(
    val gatewayId: GatewayId = GatewayId("mpesa"),
    val displayName: String = "M-Pesa",
    val region: String = "Kenya/Tanzania",
    val docsPath: String = "docs/providers/mpesa.md",
    val blurb: String = "Daraja STK push — confirmation happens on the payer's phone, no in-app SDK/UI.",
    val capabilities: Set<Capability> = setOf(Capability.ONE_TIME_PAYMENT, Capability.WALLET),
    val status: GatewayStatus = GatewayStatus.MOCK_MODE,
)
