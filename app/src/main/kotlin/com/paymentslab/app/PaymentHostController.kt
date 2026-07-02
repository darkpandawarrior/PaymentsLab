package com.paymentslab.app

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import com.paymentslab.core.paymentsapi.AndroidPaymentHost
import java.util.concurrent.atomic.AtomicInteger

/**
 * The app's single [AndroidPaymentHost]. Backed by the launcher Activity, it hands gateways an
 * `Activity` when their SDK needs one and registers ActivityResult contracts on demand through the
 * activity's [androidx.activity.result.ActivityResultRegistry] — which, unlike
 * `registerForActivityResult`, is safe to call after the Activity has started (exactly when a
 * provider's `pay()` runs). Each registration is unregistered as soon as it fires, so nothing leaks
 * across configuration change.
 */
class PaymentHostController(
    override val activity: ComponentActivity,
) : AndroidPaymentHost {
    private val counter = AtomicInteger(0)

    override fun <I, O> registerForResult(
        contract: ActivityResultContract<I, O>,
        onResult: (O) -> Unit,
    ): ActivityResultLauncher<I> {
        val key = "paymentslab_result_${counter.incrementAndGet()}"
        lateinit var launcher: ActivityResultLauncher<I>
        launcher =
            activity.activityResultRegistry.register(key, contract) { result ->
                onResult(result)
                launcher.unregister()
            }
        return launcher
    }
}
