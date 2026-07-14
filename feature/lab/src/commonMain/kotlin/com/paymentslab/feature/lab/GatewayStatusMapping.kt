package com.paymentslab.feature.lab

import com.paymentslab.core.designsystem.GatewayStatusUi
import com.siddharth.kmp.paymentsapi.GatewayStatus

/** 1:1 mapping of the domain [GatewayStatus] onto the design-system [GatewayStatusUi]. */
internal fun GatewayStatus.toUi(): GatewayStatusUi =
    when (this) {
        GatewayStatus.SANDBOX_READY -> GatewayStatusUi.SANDBOX_READY
        GatewayStatus.MOCK_MODE -> GatewayStatusUi.MOCK_MODE
        GatewayStatus.KYC_GATED -> GatewayStatusUi.KYC_GATED
        GatewayStatus.COMING_SOON -> GatewayStatusUi.COMING_SOON
    }
