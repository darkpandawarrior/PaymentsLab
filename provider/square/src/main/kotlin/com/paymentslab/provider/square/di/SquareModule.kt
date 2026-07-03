package com.paymentslab.provider.square.di

import com.paymentslab.core.paymentsapi.PaymentGateway
import com.paymentslab.provider.square.SquareGateway
import org.koin.dsl.module

val squareModule =
    module {
        single<PaymentGateway> { SquareGateway() }
    }
