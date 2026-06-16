package com.paymentslab.app

import android.app.Application
import androidx.work.Configuration
import com.paymentslab.app.work.PaymentReconciliationWorker
import com.paymentslab.app.work.PaymentWorkerFactory
import com.paymentslab.core.data.di.dataModule
import com.paymentslab.core.network.di.networkModule
import com.paymentslab.core.orchestration.di.orchestrationModule
import com.paymentslab.core.security.DeviceIntegrity
import com.paymentslab.core.security.di.securityModule
import com.paymentslab.feature.checkoutdemo.di.checkoutDemoModule
import com.paymentslab.feature.history.di.historyModule
import com.paymentslab.feature.lab.di.labModule
import com.paymentslab.provider.cashfree.di.cashfreeModule
import com.paymentslab.provider.razorpay.di.razorpayModule
import com.paymentslab.provider.stripe.di.stripeModule
import com.paymentslab.provider.upiintent.di.upiIntentModule
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.startKoin

/**
 * Composition root. Assembles every module's Koin definitions in one place — the ONLY place that
 * knows the full graph. The gateway registry is built by `orchestrationModule` from the providers
 * contributed here via `getAll<PaymentGateway>()`, so enabling a provider is a one-line change here.
 *
 * Also the WorkManager [Configuration.Provider]: WorkManager initializes on demand with a factory
 * that can build the reconciliation worker from Koin (see [PaymentWorkerFactory]).
 */
class PaymentsLabApplication :
    Application(),
    Configuration.Provider,
    KoinComponent {

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
                securityModule,
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

        // VAPT: inspect device integrity on launch (log-only here; a real app can gate sensitive
        // flows on a compromised device per its risk policy). Off the main thread — inspect() does
        // file I/O and spawns a `which su` process.
        CoroutineScope(Dispatchers.IO).launch {
            val report = get<DeviceIntegrity>().inspect()
            if (report.isCompromised) {
                Napier.w("Device integrity signals: ${report.signals}", tag = "Security")
            }
        }

        // Process-death recovery safety net for any payment left pending.
        PaymentReconciliationWorker.enqueue(this)
    }

    // WorkManager on-demand init uses this — set after startKoin so the Koin graph is ready.
    override val workManagerConfiguration: Configuration
        get() =
            Configuration.Builder()
                .setWorkerFactory(PaymentWorkerFactory(get()))
                .build()
}
