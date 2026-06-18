package com.paymentslab.app

import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.cashfree.pg.api.CFPaymentGatewayService
import com.cashfree.pg.core.api.callback.CFCheckoutResponseCallback
import com.cashfree.pg.core.api.utils.CFErrorResponse
import com.paymentslab.core.common.AppLog
import com.paymentslab.core.designsystem.PaymentsLabTheme
import com.paymentslab.core.security.AppSecurityManager
import com.paymentslab.provider.cashfree.CashfreeCheckoutRelay
import com.paymentslab.provider.razorpay.RazorpayCallbackRelay
import com.paymentslab.provider.razorpay.RazorpayCallbackResult
import com.paymentslab.provider.stripe.StripePaymentLauncherHost
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import com.stripe.android.paymentsheet.rememberPaymentSheet
import org.koin.android.ext.android.inject

/**
 * The single launcher Activity — and the one place the Activity-callback-era gateway SDKs are
 * allowed to touch. It owns nothing about payment logic; it only forwards each SDK's Activity-level
 * callback into that provider's coroutine bridge:
 *
 *  - **Razorpay** requires the Activity itself to implement [PaymentResultWithDataListener]; results
 *    are flattened (no Razorpay types escape) and pushed to [RazorpayCallbackRelay].
 *  - **Cashfree** requires [CFCheckoutResponseCallback] set on the gateway service in `onCreate`;
 *    results go to the injected [CashfreeCheckoutRelay].
 *  - **Stripe** needs its `PaymentSheet` built in Compose scope; the sheet's result callback is
 *    routed to the injected [StripePaymentLauncherHost].
 *
 * Gateways reach this Activity only through the opaque [com.paymentslab.core.paymentsapi.PaymentHost]
 * ([PaymentHostController]); they never hold a reference to it.
 */
class MainActivity :
    ComponentActivity(),
    PaymentResultWithDataListener,
    CFCheckoutResponseCallback {

    private val stripeHost: StripePaymentLauncherHost by inject()
    private val cashfreeRelay: CashfreeCheckoutRelay by inject()
    private val appSecurity: AppSecurityManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // VAPT screen defenses: FLAG_SECURE (no screenshots/recording) + tapjacking (drop obscured
        // touches). Whole-activity here; SecureScreen additionally guards the payment routes.
        appSecurity.applySecurityToActivity(this)

        // Cashfree re-attaches its callback on recreation, so this must run in onCreate.
        runCatching { CFPaymentGatewayService.getInstance().setCheckoutCallback(this) }
            .onFailure { AppLog.w(TAG, "Cashfree callback wiring failed", it) }

        val host = PaymentHostController(this)

        setContent {
            PaymentsLabTheme {
                // Stripe's PaymentSheet must be created in Compose scope (it registers an
                // ActivityResultLauncher before STARTED); route its result into the provider bridge.
                val paymentSheet = rememberPaymentSheet { result -> stripeHost.onResult(result) }
                LaunchedEffect(paymentSheet) {
                    stripeHost.attach { clientSecret, configuration ->
                        paymentSheet.presentWithPaymentIntent(clientSecret, configuration)
                    }
                }
                DisposableEffect(Unit) { onDispose { stripeHost.detach() } }

                AppNavHost(paymentHost = host)
            }
        }
    }

    // Belt-and-suspenders tapjacking guard: swallow touches delivered while an overlay is on top,
    // in addition to the view-level filterTouchesWhenObscured set by applySecurityToActivity.
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (appSecurity.shouldBlockObscuredTouch(ev)) return false
        return super.dispatchTouchEvent(ev)
    }

    // ── Razorpay (PaymentResultWithDataListener) ────────────────────────────
    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        val data = paymentData?.data
        RazorpayCallbackRelay.emit(
            RazorpayCallbackResult.Success(
                razorpayPaymentId = razorpayPaymentId ?: paymentData?.paymentId,
                razorpayOrderId = data?.optString("razorpay_order_id")?.ifBlank { null },
                razorpaySignature = paymentData?.signature ?: data?.optString("razorpay_signature")?.ifBlank { null },
            ),
        )
    }

    override fun onPaymentError(code: Int, response: String?, paymentData: PaymentData?) {
        RazorpayCallbackRelay.emit(RazorpayCallbackResult.Error(code = code, description = response))
    }

    // ── Cashfree (CFCheckoutResponseCallback) ───────────────────────────────
    override fun onPaymentVerify(orderId: String) {
        cashfreeRelay.onPaymentVerify(orderId)
    }

    override fun onPaymentFailure(errorResponse: CFErrorResponse, orderId: String) {
        cashfreeRelay.onPaymentFailure(
            orderId = orderId,
            errorMessage = errorResponse.message,
            errorCode = errorResponse.code,
        )
    }

    private companion object {
        const val TAG = "MainActivity"
    }
}
