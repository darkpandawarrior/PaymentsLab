package com.paymentslab.provider.stripeconnect.di

import com.paymentslab.provider.stripeconnect.StripeConnectOnboarding
import org.koin.dsl.module

/**
 * Koin module for `provider:stripe-connect`. [com.paymentslab.provider.hostedwebview.HostedCheckoutRelay]
 * is already a Koin single contributed by `hostedWebViewModule` — this resolves the same instance via
 * `get()` (same reuse [com.paymentslab.provider.paystack.di.paystackModule] does) rather than a second
 * relay instance.
 */
val stripeConnectModule =
    module {
        single { StripeConnectOnboarding(get(), get()) }
    }
