package com.paymentslab.backend

import com.paymentslab.core.protocol.ConnectAccountStatusDto
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory, thread-safe store for Stripe Connect payout onboarding (roadmap #11) — mirrors
 * [PayoutStore]'s shape (same [ConcurrentHashMap] + idempotency-key dedup pattern).
 *
 * Real Connect onboarding is a KYC/OAuth-gated flow a solo developer can't complete against a live
 * provider sandbox, so every account starts `ONBOARDING_PENDING` and only becomes `CONNECTED` once
 * the mock hosted-OAuth callback ([ConnectRoutes]'s `/mock/connect/{onboardingId}/complete`) fires —
 * same honest initiate → pending → resolved-by-callback shape [PayoutStore] already uses.
 */
class ConnectStore {
    data class OnboardingRecord(
        val onboardingId: String,
        val accountId: String,
        val status: ConnectAccountStatusDto,
        val updatedAtEpochMs: Long,
    )

    private val onboardingIdToAccountId = ConcurrentHashMap<String, String>()
    private val accounts = ConcurrentHashMap<String, OnboardingRecord>()

    /** Starts (or, if [onboardingId] was already used, returns) one onboarding attempt. Idempotent. */
    fun startOnboarding(onboardingId: String): OnboardingRecord {
        val accountId = onboardingIdToAccountId.computeIfAbsent(onboardingId) { "acct_$onboardingId" }
        return accounts.computeIfAbsent(accountId) {
            OnboardingRecord(
                onboardingId = onboardingId,
                accountId = accountId,
                status = ConnectAccountStatusDto.ONBOARDING_PENDING,
                updatedAtEpochMs = System.currentTimeMillis(),
            )
        }
    }

    /**
     * The mock OAuth callback — flips the account to CONNECTED. Idempotent: calling this twice for
     * the same [onboardingId] just returns the already-CONNECTED record rather than re-transitioning.
     */
    fun completeOnboarding(onboardingId: String): OnboardingRecord? {
        val accountId = onboardingIdToAccountId[onboardingId] ?: return null
        return accounts.computeIfPresent(accountId) { _, existing ->
            if (existing.status == ConnectAccountStatusDto.CONNECTED) {
                existing
            } else {
                existing.copy(status = ConnectAccountStatusDto.CONNECTED, updatedAtEpochMs = System.currentTimeMillis())
            }
        }
    }

    fun get(accountId: String): OnboardingRecord? = accounts[accountId]
}
