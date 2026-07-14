package com.paymentslab.core.orchestration.di

import com.paymentslab.core.orchestration.PaymentOrchestrator
import com.siddharth.kmp.paymentsapi.DefaultPaymentGatewayRegistry
import com.siddharth.kmp.paymentsapi.PaymentBackend
import com.siddharth.kmp.paymentsapi.PaymentGateway
import com.siddharth.kmp.paymentsapi.PaymentGatewayRegistry
import com.siddharth.kmp.paymentsapi.PendingPaymentJournal
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Wires the orchestrator and assembles the gateway registry. The registry is built from every
 * [PaymentGateway] contributed by the `provider:*` modules via Koin's `getAll()` — so adding a
 * provider is a matter of including its module, with no edit here.
 */
val orchestrationModule: Module =
    module {
        single<PaymentGatewayRegistry> { DefaultPaymentGatewayRegistry(getAll<PaymentGateway>()) }
        single {
            PaymentOrchestrator(
                registry = get<PaymentGatewayRegistry>(),
                backend = get<PaymentBackend>(),
                journal = get<PendingPaymentJournal>(),
            )
        }
    }
