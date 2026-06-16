package com.paymentslab.app.work

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.paymentslab.core.orchestration.PaymentOrchestrator

/**
 * Constructs [PaymentReconciliationWorker] with its Koin-provided [PaymentOrchestrator] dependency.
 * Wired into WorkManager via the Application's `Configuration.Provider`, so WorkManager can inject a
 * worker that needs more than a no-arg constructor without pulling in a DI-specific WorkManager add-on.
 */
class PaymentWorkerFactory(
    private val orchestrator: PaymentOrchestrator,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? =
        when (workerClassName) {
            PaymentReconciliationWorker::class.java.name ->
                PaymentReconciliationWorker(appContext, workerParameters, orchestrator)
            else -> null // fall through to the default factory for any other worker
        }
}
