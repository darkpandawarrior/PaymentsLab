package com.paymentslab.provider.wallet.di

import com.paymentslab.core.paymentsapi.PaymentGateway
import com.paymentslab.provider.wallet.WalletConfig
import com.paymentslab.provider.wallet.WalletGateway
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

/** One [PaymentGateway] per [WalletConfig], sharing the app's existing `HttpClient`/`PaymentApiConfig`. */
fun walletModule(configs: List<WalletConfig>) =
    module {
        configs.forEach { config ->
            single(qualifier = named(config.gatewayId.value)) {
                WalletGateway(config, get(), get())
            } bind PaymentGateway::class
        }
    }
