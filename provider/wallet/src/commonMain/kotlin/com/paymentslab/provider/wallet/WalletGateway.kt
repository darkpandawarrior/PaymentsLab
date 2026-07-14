package com.paymentslab.provider.wallet

import com.siddharth.kmp.common.UiText
import com.paymentslab.core.network.PaymentApiConfig
import com.paymentslab.core.paymentsapi.CreatedOrder
import com.paymentslab.core.paymentsapi.FailureCode
import com.paymentslab.core.paymentsapi.GatewayId
import com.paymentslab.core.paymentsapi.GatewayMeta
import com.paymentslab.core.paymentsapi.PaymentGateway
import com.paymentslab.core.paymentsapi.PaymentHost
import com.paymentslab.core.paymentsapi.PaymentPreparationException
import com.paymentslab.core.paymentsapi.PaymentResult
import com.paymentslab.core.paymentsapi.PreparedPayment
import com.paymentslab.core.paymentsapi.RedactedPayload
import com.paymentslab.core.paymentsapi.Redactor
import com.paymentslab.core.protocol.WalletBalanceResponse
import com.paymentslab.core.protocol.WalletDebitRequest
import com.paymentslab.core.protocol.WalletTransactionResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException

/**
 * Archetype-E ("internal rail"): a `PaymentGateway` backed by the backend's double-entry
 * [com.paymentslab.backend.LedgerStore] — no external SDK, no WebView, no 3DS/webhook. `prepare`
 * pre-flight-checks the wallet balance (server round-trip, per the interface contract); `pay` posts
 * the debit idempotently and reports the ledger txn as the payment id.
 *
 * Refund is deliberately NOT on this class or on [PaymentGateway] — the base interface has no
 * refund method by design (see `Gateway.kt`), so a reversing credit is exposed only at the ledger
 * layer (`POST /wallet/{accountId}/refund`, exercised directly against [LedgerStore] in tests), not
 * bolted onto this gateway's shape.
 */
class WalletGateway(
    private val config: WalletConfig,
    private val httpClient: HttpClient,
    private val apiConfig: PaymentApiConfig,
) : PaymentGateway {
    override val id: GatewayId = config.gatewayId

    override val meta: GatewayMeta =
        GatewayMeta(
            displayName = config.displayName,
            status = config.status,
            capabilities = config.capabilities,
            region = config.region,
            docsPath = config.docsPath,
            blurb = config.blurb,
        )

    private val base: String get() = apiConfig.baseUrl.trimEnd('/')

    /** Pre-flight balance check — the showcase's "balance race" story starts here. */
    override suspend fun prepare(created: CreatedOrder): PreparedPayment {
        val balance = fetchBalanceOrThrow()

        if (balance < created.order.amount.amountMinor) {
            throw PaymentPreparationException(
                "Insufficient wallet balance: have $balance, need ${created.order.amount.amountMinor}",
            )
        }

        return PreparedPayment(
            gatewayId = id,
            orderId = created.order.orderId,
            amount = created.order.amount,
            params = created.providerParams,
        )
    }

    private suspend fun fetchBalanceOrThrow(): Long =
        try {
            val response: WalletBalanceResponse =
                httpClient.get("$base/wallet/${config.walletAccountId}/balance").body()
            response.balanceMinor
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            throw PaymentPreparationException("Could not read wallet balance", t)
        }

    /** Posts the debit idempotently, keyed off the order id — replaying never double-charges. */
    override suspend fun pay(
        host: PaymentHost,
        prepared: PreparedPayment,
    ): PaymentResult {
        val idempotencyKey = "pay_${prepared.orderId}"
        return try {
            val response: WalletTransactionResponse =
                httpClient
                    .post("$base/wallet/${config.walletAccountId}/debit") {
                        contentType(ContentType.Application.Json)
                        setBody(WalletDebitRequest(idempotencyKey, prepared.amount.amountMinor))
                    }.body()

            PaymentResult.Success(
                paymentId = response.txnId,
                verification = mapOf("txn_id" to response.txnId, "account_id" to response.accountId),
                raw =
                    Redactor.redact(
                        "${id.value}_debit",
                        mapOf("txn_id" to response.txnId, "balance_after" to response.balanceMinor.toString()),
                    ),
            )
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            PaymentResult.Failure(
                code = FailureCode.GATEWAY_DECLINED,
                message = UiText.Dynamic(t.message ?: "Wallet debit failed"),
                raw = RedactedPayload.of("${id.value}_debit_failed", "error" to (t.message ?: "unknown")),
            )
        }
    }
}
