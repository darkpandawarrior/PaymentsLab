package com.paymentslab.provider.googlepay.di

import com.paymentslab.core.paymentsapi.PaymentGateway
import com.paymentslab.provider.googlepay.GooglePayGateway
import org.koin.dsl.module

val googlePayModule =
    module {
        single<PaymentGateway> { GooglePayGateway() }
    }
