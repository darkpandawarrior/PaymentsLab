package com.paymentslab.feature.history.di

import com.paymentslab.feature.history.HistoryViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Koin module for the History feature. The journal is resolved from `core:data`'s module. */
val historyModule =
    module {
        viewModel { HistoryViewModel(get()) }
    }
