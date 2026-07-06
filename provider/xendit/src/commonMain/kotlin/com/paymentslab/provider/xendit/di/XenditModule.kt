package com.paymentslab.provider.xendit.di

import com.paymentslab.core.paymentsapi.PaymentGateway
import com.paymentslab.provider.xendit.XenditConfig
import com.paymentslab.provider.xendit.XenditGateway
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** One [PaymentGateway] for Xendit, sharing the app's existing `HttpClient`/`PaymentApiConfig`. */
fun xenditModule(config: XenditConfig = XenditConfig()) =
    module {
        single<PaymentGateway>(qualifier = named(config.gatewayId.value)) {
            XenditGateway(config, get(), get())
        }
    }
