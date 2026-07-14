package com.paymentslab.provider.stripeconnect

import com.paymentslab.core.common.AppLog
import com.siddharth.kmp.paymentsapi.ConnectAccount
import com.siddharth.kmp.paymentsapi.ConnectAccountStatus
import com.siddharth.kmp.paymentsapi.ConnectBackend
import com.siddharth.kmp.paymentsapi.GatewayId
import com.siddharth.kmp.paymentsapi.Money
import com.siddharth.kmp.paymentsapi.PayoutSnapshot
import com.paymentslab.provider.hostedwebview.HostedCheckoutRelay
import com.paymentslab.provider.hostedwebview.HostedReturnOutcome
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Not a [com.siddharth.kmp.paymentsapi.PaymentGateway] — Connect onboards a payout destination,
 * it doesn't pay an order. Shared with [com.paymentslab.provider.stripeconnect.StripeConnectCheckoutHost]
 * so both sides of the relay key on the same id.
 */
val STRIPE_CONNECT_GATEWAY_ID = GatewayId("stripe_connect")

/**
 * Client-side Stripe Connect payout onboarding (roadmap #11): mock hosted OAuth (via the shared
 * [HostedCheckoutRelay] + `HostedCheckoutHost` composable already mounted at `:app`'s nav host) to
 * connect a payout account, then a payout to that account over the existing payout rail
 * ([ConnectBackend.payout], which server-side just reuses `PayoutStore`).
 *
 * This is deliberately NOT a `PaymentGateway` — [PaymentGateway.pay] models paying FOR an order;
 * Connect onboarding has no order at all, it's a one-time destination setup. Modeled as its own
 * service so `:app` calls it directly rather than forcing it through the order/pay contract.
 *
 * `status = MOCK_MODE`: a real Connect OAuth/KYC flow is partner-gated (see [ConnectBackend]'s own
 * doc comment) — this always resolves through the mock hosted-OAuth callback, never a live redirect.
 */
class StripeConnectOnboarding(
    private val backend: ConnectBackend,
    private val relay: HostedCheckoutRelay,
) {
    /**
     * Starts onboarding, opens the mock hosted OAuth in the shared WebView relay, and suspends until
     * the return-URL fires. The mock consent page's "Authorize" link is itself the OAuth callback
     * (a plain `GET /mock/connect/{id}/complete` — see `ConnectRoutes`), so by the time the WebView
     * lands there the account is already CONNECTED server-side; this just polls [ConnectBackend.status]
     * to read that back, same "poll server-authoritative state" idiom [ConnectBackend.status] itself
     * follows for payouts.
     */
    suspend fun onboard(): ConnectAccount {
        val onboarding = backend.onboard()
        AppLog.d(TAG, "onboarding started id=${onboarding.onboardingId}")

        val outcome =
            suspendCancellableCoroutine { cont ->
                relay.register(STRIPE_CONNECT_GATEWAY_ID) { result ->
                    if (cont.isActive) cont.resume(result) { _, _, _ -> }
                }
                cont.invokeOnCancellation { relay.clear(STRIPE_CONNECT_GATEWAY_ID) }
                relay.launch(STRIPE_CONNECT_GATEWAY_ID, onboarding.hostedOAuthUrl)
            }

        return when (outcome) {
            is HostedReturnOutcome.Success -> backend.status(onboarding.accountId)
            is HostedReturnOutcome.Failure, HostedReturnOutcome.Cancelled ->
                ConnectAccount(accountId = onboarding.accountId, status = ConnectAccountStatus.ONBOARDING_PENDING)
        }
    }

    /** Pays out to a connected account. Throws (via [ConnectBackend]) if the account isn't CONNECTED. */
    suspend fun payout(
        accountId: String,
        amount: Money,
        idempotencyKey: String,
    ): PayoutSnapshot = backend.payout(accountId, amount, idempotencyKey)

    /** `GET /connect/{accountId}` — poll onboarding status. */
    suspend fun status(accountId: String): ConnectAccount = backend.status(accountId)

    private companion object {
        const val TAG = "StripeConnectOnboarding"
    }
}
