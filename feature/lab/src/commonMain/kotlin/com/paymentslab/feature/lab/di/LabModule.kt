package com.paymentslab.feature.lab.di

import com.paymentslab.feature.lab.LabHomeViewModel
import com.paymentslab.feature.lab.OrchestratorFlowRunner
import com.paymentslab.feature.lab.PaymentFlowRunner
import com.paymentslab.feature.lab.ProviderLabViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module for the Integration Lab feature. Provides both ViewModels and binds the production
 * [PaymentFlowRunner] over the orchestrator (resolved from `orchestrationModule`).
 */
val labModule =
    module {
        single<PaymentFlowRunner> { OrchestratorFlowRunner(get()) }
        viewModel { LabHomeViewModel(get()) }
        viewModel { ProviderLabViewModel(get()) }
    }
