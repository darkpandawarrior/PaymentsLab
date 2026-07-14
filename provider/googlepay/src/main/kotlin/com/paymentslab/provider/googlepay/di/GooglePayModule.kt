package com.paymentslab.provider.googlepay.di

import com.siddharth.kmp.paymentsapi.PaymentGateway
import com.paymentslab.provider.googlepay.GooglePayGateway
import org.koin.dsl.module

val googlePayModule =
    module {
        single<PaymentGateway> { GooglePayGateway() }
    }
