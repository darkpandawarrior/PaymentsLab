package com.paymentslab.provider.hostedwebview.di

import com.paymentslab.core.paymentsapi.PaymentGateway
import com.paymentslab.provider.hostedwebview.HostedCheckoutRelay
import com.paymentslab.provider.hostedwebview.HostedGatewayConfig
import com.paymentslab.provider.hostedwebview.HostedWebViewGateway
import org.koin.dsl.module

/**
 * One [HostedCheckoutRelay] shared by every hosted-webview gateway, plus one [PaymentGateway] per
 * [HostedGatewayConfig]. Empty at B0 by design — the first real config (Paystack) lands with the B1
 * vertical slice; the fan-out batches (B2) then just append to [configs].
 */
fun hostedWebViewModule(configs: List<HostedGatewayConfig>) =
    module {
        single { HostedCheckoutRelay() }
        // Exposed so the app can mount one HostedCheckoutHost that knows every configured gateway.
        single { configs }
        configs.forEach { config ->
            single<PaymentGateway>(
                qualifier =
                    org.koin.core.qualifier
                        .named(config.gatewayId.value),
            ) {
                HostedWebViewGateway(config, get())
            }
        }
    }
