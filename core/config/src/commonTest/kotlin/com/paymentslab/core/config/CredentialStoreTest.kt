package com.paymentslab.core.config

import com.siddharth.kmp.paymentsapi.GatewayId
import com.siddharth.kmp.paymentsapi.GatewayStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CredentialStoreTest {
    private val gatewayId = GatewayId("razorpay")

    @Test
    fun `resolves keys present in the env map under the PLAB naming convention`() {
        val store =
            EnvCredentialStore(
                mapOf("PLAB_RAZORPAY_TEST_KEY_ID" to "rzp_test_123", "PLAB_RAZORPAY_TEST_SECRET" to "s3cr3t"),
            )

        val creds = store.credentialsFor(gatewayId, CredentialMode.TEST, listOf("key_id", "secret"))

        assertEquals("rzp_test_123", creds.keys["key_id"])
        assertTrue(creds.enabled)
    }

    @Test
    fun `a partially resolved key set is not enabled`() {
        val store = EnvCredentialStore(mapOf("PLAB_RAZORPAY_TEST_KEY_ID" to "rzp_test_123"))

        val creds = store.credentialsFor(gatewayId, CredentialMode.TEST, listOf("key_id", "secret"))

        assertFalse(creds.enabled)
    }

    @Test
    fun `blank env values are treated as absent`() {
        val store = EnvCredentialStore(mapOf("PLAB_RAZORPAY_TEST_KEY_ID" to "  "))

        val creds = store.credentialsFor(gatewayId, CredentialMode.TEST, listOf("key_id"))

        assertFalse(creds.enabled)
    }

    @Test
    fun `sandbox ready with no credentials degrades to mock mode`() {
        val creds = GatewayCredentials(gatewayId, CredentialMode.TEST, emptyMap(), requiredKeyNames = listOf("key_id"))

        assertEquals(GatewayStatus.MOCK_MODE, resolveEffectiveStatus(GatewayStatus.SANDBOX_READY, creds))
    }

    @Test
    fun `sandbox ready with resolved credentials stays sandbox ready`() {
        val creds =
            GatewayCredentials(
                gatewayId,
                CredentialMode.TEST,
                mapOf("key_id" to "rzp_test_123"),
                requiredKeyNames = listOf("key_id"),
            )

        assertEquals(GatewayStatus.SANDBOX_READY, resolveEffectiveStatus(GatewayStatus.SANDBOX_READY, creds))
    }

    @Test
    fun `non sandbox statuses pass through regardless of credentials`() {
        val creds = GatewayCredentials(gatewayId, CredentialMode.TEST, emptyMap(), requiredKeyNames = emptyList())

        assertEquals(GatewayStatus.KYC_GATED, resolveEffectiveStatus(GatewayStatus.KYC_GATED, creds))
    }
}
