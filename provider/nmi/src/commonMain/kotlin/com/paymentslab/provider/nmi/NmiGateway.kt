package com.paymentslab.provider.nmi

import com.paymentslab.core.common.UiText
import com.paymentslab.core.paymentsapi.Capability
import com.paymentslab.core.paymentsapi.CreatedOrder
import com.paymentslab.core.paymentsapi.FailureCode
import com.paymentslab.core.paymentsapi.GatewayId
import com.paymentslab.core.paymentsapi.GatewayMeta
import com.paymentslab.core.paymentsapi.GatewayStatus
import com.paymentslab.core.paymentsapi.PaymentGateway
import com.paymentslab.core.paymentsapi.PaymentHost
import com.paymentslab.core.paymentsapi.PaymentResult
import com.paymentslab.core.paymentsapi.PreparedPayment
import com.paymentslab.core.paymentsapi.Redactor
import com.paymentslab.core.paymentsapi.VaultBackend
import kotlinx.coroutines.CancellationException

/**
 * NMI as a third vault-pattern target (roadmap #12) — same demonstration as `provider:peach`: the
 * stored-instrument `card_id` vault (`VaultStore`/`VaultRoutes`/`VaultBackend`, roadmap #7) is
 * processor-agnostic, so a second/third real-world processor (NMI's own `card_id`/`customer_vault_id`
 * terminology is where this pattern's name comes from) can reuse it unchanged. Supersedes the
 * `nmiStubConfig` docs-only entry — `:provider:nmi` is now a working (mock) `PaymentGateway`, not
 * just a catalog stub. Mirrors `provider:cash`'s lightest-module shape: no Compose, no hosted-webview.
 */
class NmiGateway(
    private val vault: VaultBackend,
) : PaymentGateway {
    override val id: GatewayId = GatewayId("nmi")

    override val meta: GatewayMeta =
        GatewayMeta(
            displayName = "NMI",
            status = GatewayStatus.MOCK_MODE,
            capabilities = setOf(Capability.ONE_TIME_PAYMENT, Capability.CARDS),
            region = "Global",
            docsPath = "docs/providers/nmi.md",
            blurb = "Vault-pattern target #3 — charges a saved instrument via the shared stored-card vault.",
        )

    override suspend fun prepare(created: CreatedOrder): PreparedPayment =
        PreparedPayment(
            gatewayId = id,
            orderId = created.order.orderId,
            amount = created.order.amount,
            params = created.providerParams,
        )

    override suspend fun pay(
        host: PaymentHost,
        prepared: PreparedPayment,
    ): PaymentResult {
        val customerId = prepared.params["customerId"]
        val instrumentId = prepared.params["instrumentId"]
        val catalogItemId = prepared.params["catalogItemId"]
        if (customerId == null || instrumentId == null || catalogItemId == null) {
            return failure(FailureCode.CONFIG_MISSING, "Missing saved-instrument params", "no_instrument")
        }

        return try {
            val charge =
                vault.charge(
                    customerId = customerId,
                    instrumentId = instrumentId,
                    catalogItemId = catalogItemId,
                    idempotencyKey = "nmi_${prepared.orderId}",
                )
            PaymentResult.Success(
                paymentId = charge.chargeId,
                verification = mapOf("charge_id" to charge.chargeId),
                raw = Redactor.redact("nmi_success", mapOf("charge_id" to charge.chargeId)),
            )
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            failure(FailureCode.NETWORK_ERROR, "Vault charge failed: ${t.message}", "vault_error")
        }
    }

    private fun failure(
        code: FailureCode,
        message: String,
        rawReason: String,
    ) = PaymentResult.Failure(
        code = code,
        message = UiText.of(message),
        raw = Redactor.redact("nmi_failure", mapOf("error" to rawReason)),
    )
}
