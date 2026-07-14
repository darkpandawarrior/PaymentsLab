package com.paymentslab.provider.cash.di

import com.siddharth.kmp.paymentsapi.PaymentGateway
import com.paymentslab.provider.cash.CashGateway
import org.koin.dsl.bind
import org.koin.dsl.module

/** Single [PaymentGateway] — cash has no per-region fan-out, unlike mobile-money/hosted-webview. */
val cashModule =
    module {
        single { CashGateway() } bind PaymentGateway::class
    }
