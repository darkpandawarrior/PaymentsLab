package com.paymentslab.core.common.di

import org.koin.core.module.Module
import org.koin.dsl.module

/** No iOS-specific bindings yet — no `iosApp` entry point exists (B8 milestone). See [platformModule] doc. */
actual fun platformModule(): Module = module {}
