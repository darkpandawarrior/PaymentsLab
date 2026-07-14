package com.paymentslab.feature.lab

import com.paymentslab.core.designsystem.FlowHop
import com.siddharth.kmp.paymentsapi.PaymentStep

/**
 * Where this step sits on the [PaymentFlowDiagram]'s spine. [PaymentStep.Errored] stays at APP — the
 * flow broke before reaching anywhere authoritative, so there's nothing to point at further along.
 */
internal fun PaymentStep.toFlowHop(): FlowHop =
    when (this) {
        is PaymentStep.OrderCreated -> FlowHop.APP
        is PaymentStep.Launching -> FlowHop.GATEWAY
        is PaymentStep.ClientResult -> FlowHop.GATEWAY
        is PaymentStep.Verifying -> FlowHop.BACKEND
        is PaymentStep.Settled -> FlowHop.BACKEND
        is PaymentStep.Errored -> FlowHop.APP
        is PaymentStep.LegSettled -> FlowHop.BACKEND
        is PaymentStep.Compensated -> FlowHop.BACKEND
    }

/** Only [PaymentStep.Settled] means the backend has actually spoken — everything before is a hint. */
internal fun PaymentStep.isVerified(): Boolean = this is PaymentStep.Settled || this is PaymentStep.LegSettled
