package com.paymentslab.core.config

import com.siddharth.kmp.paymentsapi.GatewayId
import com.siddharth.kmp.paymentsapi.GatewayStatus

/** Resolves a gateway's env-backed credentials on demand. Backed by `System.getenv`/`BuildConfig`. */
interface CredentialStore {
    fun credentialsFor(
        gatewayId: GatewayId,
        mode: CredentialMode,
        requiredKeyNames: List<String>,
    ): GatewayCredentials
}

/**
 * The naming convention every gateway's env keys follow: `PLAB_<GATEWAY>_<MODE>_<KEY>`, e.g.
 * `PLAB_RAZORPAY_TEST_KEY_ID`. Platform-agnostic: given a flat env map (backend's `System.getenv()`,
 * or Android's `BuildConfig` fields collected into a map by the app module), resolution logic is
 * identical everywhere — the platform only differs in *where the map comes from*.
 */
class EnvCredentialStore(
    private val env: Map<String, String>,
) : CredentialStore {
    override fun credentialsFor(
        gatewayId: GatewayId,
        mode: CredentialMode,
        requiredKeyNames: List<String>,
    ): GatewayCredentials {
        val gatewayToken = gatewayId.value.uppercase()
        val keys =
            requiredKeyNames
                .mapNotNull { keyName ->
                    val envKey = "PLAB_${gatewayToken}_${mode.name}_${keyName.uppercase()}"
                    env[envKey]?.takeIf { it.isNotBlank() }?.let { keyName to it }
                }.toMap()
        return GatewayCredentials(gatewayId, mode, keys, requiredKeyNames)
    }
}

/**
 * A gateway declared [GatewayStatus.SANDBOX_READY] but with no resolved credentials can't actually run
 * against the real provider — it auto-degrades to [GatewayStatus.MOCK_MODE] so the Lab stays honest
 * and still demoable. Every other declared status passes through unchanged (a `KYC_GATED` gateway
 * doesn't become "sandbox ready" just because someone set an env var).
 */
fun resolveEffectiveStatus(
    declared: GatewayStatus,
    credentials: GatewayCredentials,
): GatewayStatus =
    if (declared == GatewayStatus.SANDBOX_READY && !credentials.enabled) {
        GatewayStatus.MOCK_MODE
    } else {
        declared
    }
