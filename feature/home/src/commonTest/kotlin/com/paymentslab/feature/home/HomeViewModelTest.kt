package com.paymentslab.feature.home

import app.cash.turbine.test
import com.paymentslab.core.paymentsapi.GatewayId
import com.paymentslab.core.paymentsapi.Money
import com.paymentslab.core.paymentsapi.PaymentStatus
import com.paymentslab.core.paymentsapi.PendingPayment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeViewModelTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun payment(
        orderId: String,
        gateway: String,
        status: PaymentStatus,
        createdAt: Long,
    ) = PendingPayment(
        orderId = orderId,
        catalogItemId = "book_499",
        gatewayId = GatewayId(gateway),
        amount = Money.inr(499),
        createdAtEpochMs = createdAt,
        status = status,
    )

    @Test
    fun gateway_count_reflects_the_full_registry_not_just_sandbox_ready() =
        runTest {
            val vm = HomeViewModel(mixedStatusRegistry(), FakePendingPaymentJournal())
            assertEquals(5, vm.uiState.value.gatewayCount)
        }

    @Test
    fun success_rate_is_the_percentage_of_terminal_successes_among_resolved_payments() =
        runTest {
            val journal =
                FakePendingPaymentJournal(
                    listOf(
                        payment("o1", "razorpay", PaymentStatus.SUCCESS, createdAt = 100),
                        payment("o2", "razorpay", PaymentStatus.SUCCESS, createdAt = 200),
                        payment("o3", "razorpay", PaymentStatus.FAILED, createdAt = 300),
                        payment("o4", "razorpay", PaymentStatus.CREATED, createdAt = 400), // not yet resolved
                    ),
                )
            val vm = HomeViewModel(mixedStatusRegistry(), journal)

            vm.uiState.test {
                val state = awaitItem()
                // 2 success out of 3 resolved (CREATED is excluded — it's not a terminal outcome).
                assertEquals(67, state.successRatePercent)
            }
        }

    @Test
    fun success_rate_is_zero_with_no_resolved_payments_not_a_divide_by_zero_crash() =
        runTest {
            val vm = HomeViewModel(mixedStatusRegistry(), FakePendingPaymentJournal())
            vm.uiState.test {
                assertEquals(0, awaitItem().successRatePercent)
            }
        }

    @Test
    fun recent_activity_is_the_three_newest_payments() =
        runTest {
            val journal =
                FakePendingPaymentJournal(
                    listOf(
                        payment("o1", "razorpay", PaymentStatus.SUCCESS, createdAt = 100),
                        payment("o2", "razorpay", PaymentStatus.SUCCESS, createdAt = 400),
                        payment("o3", "razorpay", PaymentStatus.FAILED, createdAt = 200),
                        payment("o4", "razorpay", PaymentStatus.SUCCESS, createdAt = 300),
                    ),
                )
            val vm = HomeViewModel(mixedStatusRegistry(), journal)

            vm.uiState.test {
                val state = awaitItem()
                assertEquals(3, state.recentActivity.size)
                assertEquals(listOf("o2", "o4", "o3"), state.recentActivity.map { it.orderId })
            }
        }
}
