package com.paymentslab.feature.history

import app.cash.turbine.test
import com.siddharth.kmp.paymentsapi.GatewayId
import com.siddharth.kmp.paymentsapi.Money
import com.siddharth.kmp.paymentsapi.PaymentStatus
import com.siddharth.kmp.paymentsapi.PendingPayment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HistoryViewModelTest {
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
        item: String,
        gateway: String,
        rupees: Long,
        status: PaymentStatus,
        createdAt: Long,
    ) = PendingPayment(
        orderId = orderId,
        catalogItemId = item,
        gatewayId = GatewayId(gateway),
        amount = Money.inr(rupees),
        createdAtEpochMs = createdAt,
        status = status,
    )

    @Test
    fun maps_journal_stream_newest_first_and_formats_amount() =
        runTest {
            val journal =
                FakePendingPaymentJournal(
                    listOf(
                        payment("order_a", "coffee", "razorpay", 149, PaymentStatus.SUCCESS, createdAt = 100),
                        payment("order_b", "book", "upi_intent", 499, PaymentStatus.FAILED, createdAt = 200),
                    ),
                )
            val vm = HistoryViewModel(journal)

            vm.uiState.test {
                val state = awaitItem()
                assertEquals(false, state.isLoading)
                assertEquals(2, state.rows.size)
                // Newest (createdAt=200) first.
                assertEquals("order_b", state.rows[0].orderId)
                assertEquals("book", state.rows[0].catalogItemId)
                assertEquals("upi_intent", state.rows[0].gatewayId)
                assertEquals("₹499.00", state.rows[0].amount)
                assertEquals(PaymentStatus.FAILED, state.rows[0].status)
                assertEquals("order_a", state.rows[1].orderId)
                assertEquals("₹149.00", state.rows[1].amount)
            }
        }

    @Test
    fun reflects_live_updates_from_the_journal() =
        runTest {
            val journal = FakePendingPaymentJournal(emptyList())
            val vm = HistoryViewModel(journal)

            vm.uiState.test {
                val empty = awaitItem()
                assertEquals(0, empty.rows.size)

                journal.emit(
                    listOf(payment("order_c", "course", "stripe", 9999, PaymentStatus.CREATED, createdAt = 300)),
                )
                val updated = awaitItem()
                assertEquals(1, updated.rows.size)
                assertEquals("order_c", updated.rows[0].orderId)
                assertEquals(PaymentStatus.CREATED, updated.rows[0].status)
            }
        }

    @Test
    fun toggling_a_status_filter_narrows_the_visible_rows() =
        runTest {
            val journal =
                FakePendingPaymentJournal(
                    listOf(
                        payment("order_a", "coffee", "razorpay", 149, PaymentStatus.SUCCESS, createdAt = 100),
                        payment("order_b", "book", "upi_intent", 499, PaymentStatus.FAILED, createdAt = 200),
                    ),
                )
            val vm = HistoryViewModel(journal)

            vm.uiState.test {
                awaitItem() // initial, unfiltered
                vm.onToggleStatusFilter(PaymentStatus.SUCCESS)
                val filtered = awaitItem()
                assertEquals(1, filtered.rows.size)
                assertEquals("order_a", filtered.rows[0].orderId)

                vm.onToggleStatusFilter(PaymentStatus.SUCCESS) // toggle back off
                val cleared = awaitItem()
                assertEquals(2, cleared.rows.size)
            }
        }
}
