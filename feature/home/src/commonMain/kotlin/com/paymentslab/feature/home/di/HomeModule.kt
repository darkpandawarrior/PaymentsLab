package com.paymentslab.feature.home.di

import com.paymentslab.feature.home.HomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Koin module for the Home dashboard. Registry and journal are resolved from existing modules. */
val homeModule =
    module {
        viewModel { HomeViewModel(get(), get()) }
    }
