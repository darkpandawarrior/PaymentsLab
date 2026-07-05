package com.paymentslab.feature.checkoutdemo.di

import com.paymentslab.core.orchestration.OrchestratorFlowRunner
import com.paymentslab.core.orchestration.PaymentFlowRunner
import com.paymentslab.feature.checkoutdemo.CheckoutViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module for the explained-checkout demo. Binds the production [PaymentFlowRunner] over the
 * orchestrator; the registry is resolved from `orchestrationModule`.
 */
val checkoutDemoModule =
    module {
        single<PaymentFlowRunner> { OrchestratorFlowRunner(get()) }
        viewModel { CheckoutViewModel(get(), get()) }
    }
