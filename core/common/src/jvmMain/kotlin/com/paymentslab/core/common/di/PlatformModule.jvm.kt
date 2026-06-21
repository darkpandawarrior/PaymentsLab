package com.paymentslab.core.common.di

import org.koin.core.module.Module
import org.koin.dsl.module

/** No JVM-specific bindings — the backend doesn't use Koin. See [platformModule] doc. */
actual fun platformModule(): Module = module {}
