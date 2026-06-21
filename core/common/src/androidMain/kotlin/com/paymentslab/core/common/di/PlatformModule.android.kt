package com.paymentslab.core.common.di

import org.koin.core.module.Module
import org.koin.dsl.module

/** No Android-specific bindings yet — see [platformModule] doc. */
actual fun platformModule(): Module = module {}
