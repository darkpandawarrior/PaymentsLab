package com.paymentslab.provider.omise.di

import com.siddharth.kmp.paymentsapi.PaymentGateway
import com.paymentslab.provider.omise.OmiseGateway
import org.koin.dsl.module

val omiseModule =
    module {
        single<PaymentGateway> { OmiseGateway() }
    }
