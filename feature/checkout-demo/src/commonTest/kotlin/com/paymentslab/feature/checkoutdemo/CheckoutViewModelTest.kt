package com.paymentslab.feature.checkoutdemo

import com.siddharth.kmp.common.UiText
import com.paymentslab.core.designsystem.StepState
import com.paymentslab.core.paymentsapi.GatewayId
import com.paymentslab.core.paymentsapi.Money
import com.paymentslab.core.paymentsapi.PaymentResult
import com.paymentslab.core.paymentsapi.PaymentSnapshot
import com.paymentslab.core.paymentsapi.PaymentStatus
import com.paymentslab.core.paymentsapi.PaymentStep
import com.paymentslab.core.paymentsapi.RedactedPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CheckoutViewModelTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun successScript(): List<PaymentStep> =
        listOf(
            PaymentStep.OrderCreated(
                orderId = "order_1",
                amount = Money.inr(499),
                payload = RedactedPayload.of("order", "order_id" to "order_1"),
            ),
            PaymentStep.Launching(GatewayId("razorpay")),
            PaymentStep.ClientResult(
                result = PaymentResult.Success("pay_1", mapOf("signature" to "abc"), RedactedPayload.EMPTY),
                payload = RedactedPayload.of("client", "payment_id" to "pay_1"),
            ),
            PaymentStep.Verifying(),
            PaymentStep.Settled(
                status = PaymentStatus.SUCCESS,
                snapshot = PaymentSnapshot("order_1", "pay_1", PaymentStatus.SUCCESS),
                payload = RedactedPayload.of("settled", "status" to "SUCCESS"),
            ),
        )

    private fun failScript(): List<PaymentStep> = listOf(PaymentStep.Errored(UiText.of("boom")))

    // ── (a) re-press after a failure reuses the SAME key (server dedups the retried order) ─────────
    @Test
    fun rePressingPayAfterFailure_reusesTheSameIdempotencyKey() =
        runTest {
            val runner = FakePaymentFlowRunner { _ -> failScript() }
            val vm = CheckoutViewModel(runner, sandboxAndGatedRegistry())
            vm.selectProduct(
                vm.uiState.value.products
                    .first { it.catalogItemId == "book_499" },
            )
            vm.selectGateway(GatewayId("razorpay"))

            vm.pay(TestHost) // fails
            vm.pay(TestHost) // re-press, same selection

            assertEquals(2, runner.keysReceived.size)
            assertEquals(runner.keysReceived[0], runner.keysReceived[1])
        }

    // ── (b) success then pay again → a DIFFERENT key (genuinely new order) ────────────────────────
    @Test
    fun payingAgainAfterSuccess_usesAFreshIdempotencyKey() =
        runTest {
            // Run 0 succeeds, run 1 also succeeds; keys must differ.
            val runner = FakePaymentFlowRunner { _ -> successScript() }
            val vm = CheckoutViewModel(runner, sandboxAndGatedRegistry())
            vm.selectProduct(
                vm.uiState.value.products
                    .first { it.catalogItemId == "book_499" },
            )
            vm.selectGateway(GatewayId("razorpay"))

            vm.pay(TestHost) // succeeds → key cleared
            vm.pay(TestHost) // new order

            assertEquals(2, runner.keysReceived.size)
            assertTrue(runner.keysReceived[0] != runner.keysReceived[1])
        }

    // ── (c) changing selection between presses → a DIFFERENT key ──────────────────────────────────
    @Test
    fun changingSelectionBetweenPresses_usesAFreshIdempotencyKey() =
        runTest {
            val runner = FakePaymentFlowRunner { _ -> failScript() }
            val vm = CheckoutViewModel(runner, sandboxAndGatedRegistry())
            vm.selectProduct(
                vm.uiState.value.products
                    .first { it.catalogItemId == "book_499" },
            )
            vm.selectGateway(GatewayId("razorpay"))

            vm.pay(TestHost) // fails
            vm.selectProduct(
                vm.uiState.value.products
                    .first { it.catalogItemId == "coffee_149" },
            )
            vm.pay(TestHost) // different product

            assertEquals(2, runner.keysReceived.size)
            assertTrue(runner.keysReceived[0] != runner.keysReceived[1])
        }

    @Test
    fun only_sandbox_ready_gateways_are_offered() {
        val vm = CheckoutViewModel(FakePaymentFlowRunner(emptyList()), sandboxAndGatedRegistry())
        val ids =
            vm.uiState.value.gateways
                .map { it.id.value }
        assertEquals(listOf("upi_intent", "razorpay"), ids)
    }

    @Test
    fun demo_catalog_ids_match_the_agreed_contract() {
        val vm = CheckoutViewModel(FakePaymentFlowRunner(emptyList()), sandboxAndGatedRegistry())
        assertEquals(
            listOf("coffee_149", "book_499", "headphones_2499", "course_9999", "ebook_usd_9"),
            vm.uiState.value.products
                .map { it.catalogItemId },
        )
    }

    @Test
    fun happy_path_runs_flow_and_builds_success_timeline() =
        runTest {
            val runner = FakePaymentFlowRunner(successScript())
            val vm = CheckoutViewModel(runner, sandboxAndGatedRegistry())

            val product =
                vm.uiState.value.products
                    .first { it.catalogItemId == "book_499" }
            vm.selectProduct(product)
            vm.selectGateway(GatewayId("razorpay"))
            assertTrue(vm.uiState.value.canPay)

            vm.pay(TestHost)

            val state = vm.uiState.value
            assertFalse(state.isRunning)
            assertEquals(PaymentStatus.SUCCESS, state.finalStatus)
            assertEquals(5, state.steps.size)
            assertEquals(StepState.DONE, state.steps.last().state)
            // The selected product's id was forwarded to the runner (the server resolves the price).
            assertEquals("book_499", runner.lastCatalogItemId)
            assertEquals(GatewayId("razorpay"), runner.lastGatewayId)
        }

    @Test
    fun pay_is_a_noop_without_product_and_gateway() =
        runTest {
            val runner = FakePaymentFlowRunner(successScript())
            val vm = CheckoutViewModel(runner, sandboxAndGatedRegistry())

            vm.pay(TestHost)

            assertEquals(0, vm.uiState.value.steps.size)
            assertEquals(null, runner.lastCatalogItemId)
        }
}
