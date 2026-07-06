package com.paymentslab.provider.flutterwave.di

import com.paymentslab.core.paymentsapi.PaymentGateway
import com.paymentslab.provider.flutterwave.FlutterwaveGateway
import com.paymentslab.provider.hostedwebview.HostedCheckoutRelay
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin module for the native `provider:flutterwave` gateway.
 *
 * [HostedCheckoutRelay] is already a Koin single contributed by `hostedWebViewModule` — this module
 * resolves the same instance via `get()` rather than creating a second relay, so Flutterwave's
 * checkout shares the one `HostedCheckoutHost` composable mounted at `:app`'s nav host with every
 * other hosted-checkout gateway.
 */
val flutterwaveModule: Module =
    module {
        single { FlutterwaveGateway(get()) } bind PaymentGateway::class
    }
