package com.paymentslab.feature.lab.di

import com.paymentslab.core.orchestration.OrchestratorFlowRunner
import com.paymentslab.core.orchestration.PaymentFlowRunner
import com.paymentslab.feature.lab.LabHomeViewModel
import com.paymentslab.feature.lab.ProviderLabViewModel
import com.paymentslab.feature.lab.ai.AiSettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module for the Integration Lab feature. Provides all three ViewModels (catalog, provider
 * detail, AI settings) and binds the production [PaymentFlowRunner] over the orchestrator
 * (resolved from `orchestrationModule`). [AiSettingsViewModel]'s own dependencies
 * (ModelManager/OnDeviceLlm/ModelManifestEntry/SecureKeyStore) come from `:app`'s `aiModule`.
 */
val labModule =
    module {
        single<PaymentFlowRunner> { OrchestratorFlowRunner(get()) }
        viewModel { LabHomeViewModel(get()) }
        viewModel { ProviderLabViewModel(get()) }
        viewModel { AiSettingsViewModel(get(), get(), get(), get()) }
    }
