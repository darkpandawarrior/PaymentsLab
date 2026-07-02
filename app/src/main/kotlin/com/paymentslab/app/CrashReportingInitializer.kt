package com.paymentslab.app

import com.paymentslab.core.common.CrashReporter

/**
 * Installs process-wide crash reporting: seeds searchable custom keys (build variant, version) and
 * chains a global uncaught-exception handler that reports the crash through the [CrashReporter] before
 * delegating to the platform's default handler. Backend-agnostic — the reporter is whatever DI binds
 * (Napier by default, Crashlytics/Sentry in a real deployment).
 */
object CrashReportingInitializer {
    fun install(reporter: CrashReporter, customKeys: Map<String, String>) {
        customKeys.forEach { (key, value) -> reporter.setCustomKey(key, value) }

        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { reporter.recordException(throwable, "uncaught on ${thread.name}") }
            previous?.uncaughtException(thread, throwable)
        }
    }
}
