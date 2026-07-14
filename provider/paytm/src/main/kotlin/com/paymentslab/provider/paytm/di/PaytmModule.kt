package com.paymentslab.provider.paytm.di

import com.siddharth.kmp.paymentsapi.PaymentGateway
import com.paymentslab.provider.hostedwebview.HostedCheckoutRelay
import com.paymentslab.provider.paytm.PaytmGateway
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin module for the native `provider:paytm` gateway.
 *
 * [HostedCheckoutRelay] is already a Koin single contributed by `hostedWebViewModule` — this module
 * resolves the same instance via `get()` rather than creating a second relay, so Paytm's checkout
 * shares the one `HostedCheckoutHost` composable mounted at `:app`'s nav host with every other
 * hosted-checkout gateway.
 */
val paytmModule: Module =
    module {
        single { PaytmGateway(get()) } bind PaymentGateway::class
    }
