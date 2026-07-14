package com.paymentslab.ios.shared

import com.paymentslab.core.data.di.dataModule
import com.paymentslab.core.network.PaymentApiConfig
import com.paymentslab.core.network.di.networkModule
import com.paymentslab.core.orchestration.di.orchestrationModule
import com.siddharth.kmp.paymentsapi.PaymentGateway
import com.paymentslab.feature.checkoutdemo.di.checkoutDemoModule
import com.paymentslab.feature.history.di.historyModule
import com.paymentslab.feature.home.di.homeModule
import com.paymentslab.feature.lab.di.labModule
import com.paymentslab.provider.hostedwebview.di.hostedWebViewModule
import com.paymentslab.provider.mobilemoney.di.mobileMoneyModule
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * B8's composition root — the iOS counterpart to `app/PaymentsLabApplication.kt`. Registers the
 * KMP-safe archetype C/D providers (see `IosGatewayConfigs.kt`) plus every native-SDK gateway that
 * has a real iOS SDK and a Swift-side [*CheckoutHost] implementation: Stripe, Razorpay, Cashfree,
 * Omise, Square. `core:security` (Android-only VAPT suite) is correctly absent. Google Pay has no
 * iOS equivalent at all (Apple Pay is a separate Apple product, not a Google Pay port) — also
 * correctly absent.
 *
 * Called once from Swift (`KoinInitKt.doInitKoin(...)`) before `MainViewController()` is presented.
 * Every `*CheckoutHost` parameter is Swift's real SDK implementation, constructed in Swift (where
 * each vendor's SDK lives) and handed down — Kotlin never reaches up into a framework it can't
 * cinterop against.
 */
fun doInitKoin(
    stripeCheckoutHost: StripeCheckoutHost,
    razorpayCheckoutHost: RazorpayCheckoutHost,
    cashfreeCheckoutHost: CashfreeCheckoutHost,
    omiseCheckoutHost: OmiseCheckoutHost,
    squareCheckoutHost: SquareCheckoutHost,
) {
    startKoin {
        modules(
            dataModule,
            networkModule(PaymentApiConfig(baseUrl = IosBackendConfig.BASE_URL)),
            orchestrationModule,
            hostedWebViewModule(iosHostedGatewayConfigs),
            mobileMoneyModule(iosMobileMoneyConfigs),
            labModule,
            checkoutDemoModule,
            historyModule,
            homeModule,
            module {
                single { stripeCheckoutHost }
                single<PaymentGateway>(qualifier = named("stripe")) { StripeIosGateway(get()) }

                single { razorpayCheckoutHost }
                single<PaymentGateway>(qualifier = named("razorpay")) { RazorpayIosGateway(get()) }

                single { cashfreeCheckoutHost }
                single<PaymentGateway>(qualifier = named("cashfree")) { CashfreeIosGateway(get()) }

                single { omiseCheckoutHost }
                single<PaymentGateway>(qualifier = named("omise")) { OmiseIosGateway(get()) }

                single { squareCheckoutHost }
                single<PaymentGateway>(qualifier = named("square")) { SquareIosGateway(get()) }
            },
        )
    }
}

/**
 * The iOS Simulator reaches the host machine via `localhost`, unlike the Android emulator's
 * `10.2.2.2` loopback alias — the one genuine platform difference in reaching the same local
 * backend. A real device would need the host machine's LAN IP instead; not handled here since this
 * module targets Simulator-based verification only.
 */
private object IosBackendConfig {
    const val BASE_URL = "http://localhost:8080"
}
