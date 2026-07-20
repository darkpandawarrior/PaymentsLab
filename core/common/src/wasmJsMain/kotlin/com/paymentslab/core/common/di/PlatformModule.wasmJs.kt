package com.paymentslab.core.common.di

import org.koin.core.module.Module
import org.koin.dsl.module

/** No wasm-specific bindings — the web preview wires its fakes in `:web`'s own module. */
actual fun platformModule(): Module = module {}
