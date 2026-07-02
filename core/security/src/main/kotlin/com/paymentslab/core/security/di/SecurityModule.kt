package com.paymentslab.core.security.di

import com.paymentslab.core.security.AndroidDeviceIntegrity
import com.paymentslab.core.security.DeviceIntegrity
import com.paymentslab.core.security.KeystoreSecureStore
import com.paymentslab.core.security.SecureStore
import com.paymentslab.core.security.SecurityConfig
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin wiring for `core:security`.
 *
 * - [SecureStore] is bound to the Keystore-backed implementation — the only production-safe one.
 * - [SecurityConfig] is provided with defaults; an app can override this definition to tighten the
 *   posture (e.g. `SecurityConfig(allowEmulator = false)` for a release-only module).
 * - [DeviceIntegrity] resolves the config via `get()`, so the two stay consistent.
 *
 * Assemble into the app graph alongside the other module definitions in the `startKoin { }` block.
 */
val securityModule: Module = module {
    single<SecureStore> { KeystoreSecureStore(androidContext()) }
    single { SecurityConfig() }
    single<DeviceIntegrity> { AndroidDeviceIntegrity(androidContext(), get()) }
}
