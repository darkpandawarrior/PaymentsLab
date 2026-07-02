package com.paymentslab.core.common

import io.github.aakira.napier.Napier

/**
 * Thin KMP logging facade over Napier so commonMain code can log without touching `android.util.Log`
 * (unavailable in shared code). Every call carries an explicit tag — no inline tag strings.
 */
object AppLog {
    fun d(tag: String, message: String) = Napier.d(message, tag = tag)

    fun i(tag: String, message: String) = Napier.i(message, tag = tag)

    fun w(tag: String, message: String, error: Throwable? = null) = Napier.w(message, error, tag = tag)

    fun e(tag: String, message: String, error: Throwable? = null) = Napier.e(message, error, tag = tag)
}
