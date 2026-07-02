package com.paymentslab.core.common

/**
 * Crash / non-fatal reporting behind an interface, so the app depends on the *capability* — not on
 * Firebase Crashlytics or Sentry. A public sandbox repo can't ship a real crash backend (needs a
 * project + `google-services.json`), so the default is [NapierCrashReporter] (logs only). Swapping in
 * a `CrashlyticsCrashReporter` / `SentryCrashReporter` is a one-line DI change with no call-site edits.
 *
 * The methods mirror what production crash tools expose: a non-fatal exception, a breadcrumb trail,
 * searchable custom keys, and a user identifier — exactly what you need to debug a payment
 * reconciliation break or a token-refresh failure after the fact.
 */
interface CrashReporter {
    /** Report a caught, non-crashing error (e.g. a payment flow that failed and was handled). */
    fun recordException(
        throwable: Throwable,
        message: String? = null,
    )

    /** Leave a breadcrumb — the last N are attached to the next report. */
    fun log(breadcrumb: String)

    /** Attach a searchable key/value to subsequent reports (build variant, gateway, order id …). */
    fun setCustomKey(
        key: String,
        value: String,
    )

    /** Associate reports with a (non-PII) user/session identifier, or clear it with `null`. */
    fun setUserId(id: String?)
}

/**
 * Default, dependency-free [CrashReporter] that routes everything through [AppLog] (Napier). It gives
 * the same call sites and breadcrumb discipline as a real backend, minus the upload — so wiring a
 * Crashlytics/Sentry implementation later changes nothing but the DI binding.
 */
class NapierCrashReporter : CrashReporter {
    override fun recordException(
        throwable: Throwable,
        message: String?,
    ) = AppLog.e(TAG, message ?: "non-fatal", throwable)

    override fun log(breadcrumb: String) = AppLog.d(TAG, "breadcrumb: $breadcrumb")

    override fun setCustomKey(
        key: String,
        value: String,
    ) = AppLog.d(TAG, "key $key=$value")

    override fun setUserId(id: String?) = AppLog.d(TAG, "userId=$id")

    private companion object {
        const val TAG = "CrashReporter"
    }
}
