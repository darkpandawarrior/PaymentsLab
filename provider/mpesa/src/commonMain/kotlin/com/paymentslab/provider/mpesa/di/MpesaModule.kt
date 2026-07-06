package com.paymentslab.provider.mpesa.di

import com.paymentslab.core.paymentsapi.PaymentGateway
import com.paymentslab.provider.mpesa.MpesaConfig
import com.paymentslab.provider.mpesa.MpesaGateway
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** One [PaymentGateway] for M-Pesa, sharing the app's existing `HttpClient`/`PaymentApiConfig`. */
fun mpesaModule(config: MpesaConfig = MpesaConfig()) =
    module {
        single<PaymentGateway>(qualifier = named(config.gatewayId.value)) {
            MpesaGateway(config, get(), get())
        }
    }
