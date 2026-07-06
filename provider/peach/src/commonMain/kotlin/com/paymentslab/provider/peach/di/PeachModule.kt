package com.paymentslab.provider.peach.di

import com.paymentslab.core.paymentsapi.PaymentGateway
import com.paymentslab.provider.peach.PeachGateway
import org.koin.dsl.bind
import org.koin.dsl.module

/** Single [PaymentGateway], same no-fan-out shape as [com.paymentslab.provider.cash.di.cashModule]. */
val peachModule =
    module {
        single { PeachGateway(get()) } bind PaymentGateway::class
    }
