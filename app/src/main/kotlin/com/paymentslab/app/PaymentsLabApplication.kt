package com.paymentslab.app

import android.app.Application
import com.paymentslab.core.data.di.dataModule
import com.paymentslab.core.network.di.networkModule
import com.paymentslab.core.orchestration.di.orchestrationModule
import com.paymentslab.feature.checkoutdemo.di.checkoutDemoModule
import com.paymentslab.feature.history.di.historyModule
import com.paymentslab.feature.lab.di.labModule
import com.paymentslab.provider.cashfree.di.cashfreeModule
import com.paymentslab.provider.razorpay.di.razorpayModule
import com.paymentslab.provider.stripe.di.stripeModule
import com.paymentslab.provider.upiintent.di.upiIntentModule
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * Composition root. Assembles every module's Koin definitions in one place — the ONLY place that
 * knows the full graph. The gateway registry is built by `orchestrationModule` from the providers
 * contributed here via `getAll<PaymentGateway>()`, so enabling a provider is a one-line change to
 * this module list.
 */
class PaymentsLabApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Napier.base(DebugAntilog())
        startKoin {
            androidContext(this@PaymentsLabApplication)
            modules(
                // core
                dataModule,
                networkModule,
                orchestrationModule,
                // providers (each contributes a PaymentGateway into the registry)
                upiIntentModule,
                razorpayModule,
                cashfreeModule,
                stripeModule,
                // features
                labModule,
                historyModule,
                checkoutDemoModule,
            )
        }
    }
}
