package com.paymentslab.core.orchestration

import com.paymentslab.core.paymentsapi.DefaultPaymentGatewayRegistry
import com.paymentslab.core.paymentsapi.GatewayId
import com.paymentslab.core.paymentsapi.Money
import com.paymentslab.core.paymentsapi.PaymentStatus
import com.paymentslab.core.paymentsapi.PaymentStep
import com.paymentslab.core.paymentsapi.SplitLeg
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Split payments: one logical order paid as a wallet leg + a gateway leg
 * ([PaymentOrchestrator.paySplit]). Covers the happy path, leg-2-failure compensation, the
 * insufficient-balance guard, and idempotent replay — the correctness properties from the split-
 * payment design (wallet leg first, gateway leg second, compensating credit if leg 2 fails).
 */
class PaymentOrchestratorSplitTest {
    private val walletGid = GatewayId("wallet")
    private val gatewayGid = GatewayId("gw")
    private val walletAccountId = "wallet_user1"
    private val total = Money.inr(500)
    private val walletAmount = Money.inr(200) // remainder = 300, charged to the gateway leg

    private fun setup(
        walletBalanceMinor: Long = total.amountMinor,
        gatewayResult: com.paymentslab.core.paymentsapi.PaymentResult = success(),
        gatewayVerifyStatus: PaymentStatus = PaymentStatus.SUCCESS,
    ): Triple<PaymentOrchestrator, FakeLedger, FakeWalletLedgerPort> {
        val ledger = FakeLedger(walletBalanceMinor)
        val walletGateway = FakeWalletGateway(walletGid, ledger)
        val gateway = FakeGateway(gatewayGid, result = gatewayResult)
        val ledgerPort = FakeWalletLedgerPort(ledger)
        // Wallet leg verifies SUCCESS (default); only the gateway leg carries [gatewayVerifyStatus],
        // so a "gateway leg fails" test doesn't also sink the wallet leg's own server verify.
        val backend = FakeSplitBackend(total, statusByGateway = mapOf(gatewayGid to gatewayVerifyStatus))
        val orchestrator =
            PaymentOrchestrator(
                registry = DefaultPaymentGatewayRegistry(listOf(walletGateway, gateway)),
                backend = backend,
                journal = FakeJournal(),
                pollConfig = PaymentOrchestrator.PollConfig(initialDelayMs = 10, maxDelayMs = 40, maxAttempts = 5),
                now = { 1_000L },
            )
        return Triple(orchestrator, ledger, ledgerPort)
    }

    @Test
    fun happyPath_bothLegsSucceed_walletDebitedOnce_gatewayChargedRemainder() =
        runTest {
            val (orchestrator, ledger, ledgerPort) = setup()
            val steps =
                orchestrator
                    .paySplit(
                        NoopHost,
                        walletGid,
                        walletAccountId,
                        walletAmount,
                        ledgerPort,
                        gatewayGid,
                        "item_1",
                        "idem_split",
                    ).toList()

            val legSettled = steps.filterIsInstance<PaymentStep.LegSettled>()
            assertEquals(2, legSettled.size)
            assertEquals(SplitLeg.WALLET, legSettled[0].leg)
            assertEquals(PaymentStatus.SUCCESS, legSettled[0].settled.status)
            assertEquals(SplitLeg.GATEWAY, legSettled[1].leg)
            assertEquals(PaymentStatus.SUCCESS, legSettled[1].settled.status)

            // Wallet debited exactly once, for exactly the wallet-leg amount.
            assertEquals(total.amountMinor - walletAmount.amountMinor, ledger.balanceMinor)
            assertTrue(steps.none { it is PaymentStep.Compensated }, "no compensation on the happy path")
        }

    @Test
    fun leg2Failure_compensatesWalletLeg_netWalletMovementZero_overallFailure() =
        runTest {
            val failure =
                com.paymentslab.core.paymentsapi.PaymentResult.Failure(
                    com.paymentslab.core.paymentsapi.FailureCode.GATEWAY_DECLINED,
                    com.paymentslab.core.common.UiText.Empty,
                    com.paymentslab.core.paymentsapi.RedactedPayload.EMPTY,
                )
            val (orchestrator, ledger, ledgerPort) =
                setup(gatewayResult = failure, gatewayVerifyStatus = PaymentStatus.FAILED)
            val startingBalance = ledger.balanceMinor

            val steps =
                orchestrator
                    .paySplit(
                        NoopHost,
                        walletGid,
                        walletAccountId,
                        walletAmount,
                        ledgerPort,
                        gatewayGid,
                        "item_1",
                        "idem_split",
                    ).toList()

            val legSettled = steps.filterIsInstance<PaymentStep.LegSettled>()
            assertEquals(PaymentStatus.SUCCESS, legSettled[0].settled.status) // wallet leg ok
            assertEquals(PaymentStatus.FAILED, legSettled[1].settled.status) // gateway leg failed

            val compensated = steps.filterIsInstance<PaymentStep.Compensated>().single()
            assertEquals(walletAmount, compensated.walletAmount)

            // Exactly one refund, for exactly the wallet-leg amount -> net wallet movement zero.
            assertEquals(1, ledgerPort.refundCalls.size)
            assertEquals(walletAmount.amountMinor, ledgerPort.refundCalls.single().second)
            assertEquals(startingBalance, ledger.balanceMinor)
        }

    @Test
    fun insufficientWalletBalance_failsBeforeGatewayLeg_noGatewayCharge() =
        runTest {
            val gateway = FakeGateway(gatewayGid, result = success())
            val ledger = FakeLedger(initialBalanceMinor = 50) // less than walletAmount (200)
            val walletGateway = FakeWalletGateway(walletGid, ledger)
            val ledgerPort = FakeWalletLedgerPort(ledger)
            val backend = FakeSplitBackend(total)
            val orchestrator =
                PaymentOrchestrator(
                    registry = DefaultPaymentGatewayRegistry(listOf(walletGateway, gateway)),
                    backend = backend,
                    journal = FakeJournal(),
                    now = { 1_000L },
                )

            val steps =
                orchestrator
                    .paySplit(
                        NoopHost,
                        walletGid,
                        walletAccountId,
                        walletAmount,
                        ledgerPort,
                        gatewayGid,
                        "item_1",
                        "idem_split",
                    ).toList()

            // The wallet leg fails at prepare (insufficient balance) — same as the real WalletGateway,
            // which throws PaymentPreparationException — so the leg surfaces as Errored and the guard
            // stops the split before the gateway leg. The money guarantee: gateway never runs.
            assertTrue(steps.any { it is PaymentStep.Errored }, "wallet leg must surface a failure: $steps")
            assertTrue(
                steps.filterIsInstance<PaymentStep.LegSettled>().none { it.leg == SplitLeg.GATEWAY },
                "gateway leg must never settle: $steps",
            )
            assertTrue(
                steps.none { it is PaymentStep.OrderCreated && it.orderId.contains("gateway") },
                "gateway leg's order must never be created: $steps",
            )
            assertTrue(
                steps.none { it is PaymentStep.Compensated },
                "nothing to compensate — wallet was never debited",
            )
            assertEquals(50, ledger.balanceMinor, "balance untouched — no debit, no charge")
        }

    @Test
    fun replayingSplit_withSameKey_doesNotDoubleDebitWallet() =
        runTest {
            val (orchestrator, ledger, ledgerPort) = setup()

            repeat(2) {
                orchestrator
                    .paySplit(
                        NoopHost,
                        walletGid,
                        walletAccountId,
                        walletAmount,
                        ledgerPort,
                        gatewayGid,
                        "item_1",
                        "idem_split", // SAME top-level key both times
                    ).toList()
            }

            // Debited once total, not twice, despite two full paySplit runs with the same key.
            assertEquals(total.amountMinor - walletAmount.amountMinor, ledger.balanceMinor)
        }
}
