package com.paymentslab.ios.shared

import com.paymentslab.core.data.di.dataModule
import com.paymentslab.core.network.PaymentApiConfig
import com.paymentslab.core.network.di.networkModule
import com.paymentslab.core.orchestration.di.orchestrationModule
import com.paymentslab.feature.lab.di.labModule
import com.paymentslab.provider.hostedwebview.di.hostedWebViewModule
import com.paymentslab.provider.mobilemoney.di.mobileMoneyModule
import org.koin.core.context.startKoin

/**
 * B8's composition root — the iOS counterpart to `app/PaymentsLabApplication.kt`. Deliberately
 * narrower: only the KMP-safe archetype C/D providers are registered (see `IosGatewayConfigs.kt`),
 * so `core:security` (Android-only VAPT suite) and every native-SDK archetype-A provider module
 * are correctly absent here, not silently missing.
 *
 * Called once from Swift (`KoinInitKt.doInitKoin()`) before `MainViewController()` is presented.
 */
fun doInitKoin() {
    startKoin {
        modules(
            dataModule,
            networkModule(PaymentApiConfig(baseUrl = IosBackendConfig.BASE_URL)),
            orchestrationModule,
            hostedWebViewModule(iosHostedGatewayConfigs),
            mobileMoneyModule(iosMobileMoneyConfigs),
            labModule,
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
