package com.paymentslab.feature.checkoutdemo.di

import com.paymentslab.feature.checkoutdemo.CheckoutViewModel
import com.paymentslab.feature.checkoutdemo.OrchestratorFlowRunner
import com.paymentslab.feature.checkoutdemo.PaymentFlowRunner
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
