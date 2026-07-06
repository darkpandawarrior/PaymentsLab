package com.paymentslab.provider.hostedwebview

import android.webkit.SslErrorHandler
import android.webkit.WebView
import com.multiplatform.webview.web.AccompanistWebViewClient

/**
 * SECURITY: SSL/certificate errors on a hosted checkout page fail closed. [compose-webview-multiplatform]'s
 * `AccompanistWebViewClient` does not override `onReceivedSslError` at all, so without this subclass the
 * platform default (`handler.cancel()`) silently cancels the load with no signal to the payment flow — the
 * WebView would just sit blank. This override makes the failure explicit and reports it through [onSslError]
 * so the orchestrator can settle the payment as [com.paymentslab.core.paymentsapi.PaymentResult.Failure].
 *
 * NEVER call `handler.proceed()` here — that is the exact "flaky regional cert" hack Play policy rejects.
 */
class SslFailClosedWebViewClient(
    private val onSslError: () -> Unit,
) : AccompanistWebViewClient() {
    override fun onReceivedSslError(
        view: WebView,
        handler: SslErrorHandler,
        error: android.net.http.SslError,
    ) {
        handler.cancel() // fail closed — never handler.proceed()
        onSslError()
    }
}
