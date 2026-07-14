package com.paymentslab.feature.lab

import com.paymentslab.core.designsystem.GatewayStatusUi
import com.siddharth.kmp.paymentsapi.Capability
import com.siddharth.kmp.paymentsapi.CreatedOrder
import com.siddharth.kmp.paymentsapi.DefaultPaymentGatewayRegistry
import com.siddharth.kmp.paymentsapi.GatewayId
import com.siddharth.kmp.paymentsapi.GatewayMeta
import com.siddharth.kmp.paymentsapi.GatewayStatus
import com.siddharth.kmp.paymentsapi.PaymentGateway
import com.siddharth.kmp.paymentsapi.PaymentHost
import com.siddharth.kmp.paymentsapi.PaymentResult
import com.siddharth.kmp.paymentsapi.PreparedPayment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeGateway(
    override val id: GatewayId,
    override val meta: GatewayMeta,
) : PaymentGateway {
    override suspend fun prepare(created: CreatedOrder): PreparedPayment = error("not used in this test")

    override suspend fun pay(
        host: PaymentHost,
        prepared: PreparedPayment,
    ): PaymentResult = error("not used in this test")
}

private fun fakeGateway(
    id: String,
    displayName: String,
    status: GatewayStatus,
    region: String,
) = FakeGateway(
    id = GatewayId(id),
    meta =
        GatewayMeta(
            displayName = displayName,
            status = status,
            capabilities = setOf(Capability.ONE_TIME_PAYMENT),
            region = region,
            docsPath = "docs/providers/$id.md",
            blurb = "$displayName blurb",
        ),
)

class LabHomeViewModelTest {
    private fun viewModel(): LabHomeViewModel =
        LabHomeViewModel(
            DefaultPaymentGatewayRegistry(
                listOf(
                    fakeGateway("razorpay", "Razorpay", GatewayStatus.SANDBOX_READY, "India"),
                    fakeGateway("paystack", "Paystack", GatewayStatus.MOCK_MODE, "Africa"),
                    fakeGateway("peach", "Peach Payments", GatewayStatus.MOCK_MODE, "Africa"),
                    fakeGateway("cybersource", "Cybersource", GatewayStatus.COMING_SOON, "Global"),
                ),
            ),
        )

    @Test
    fun `region counts are computed over the full catalog`() {
        val state = viewModel().uiState.value

        val africa = state.regionCounts.first { it.region == "Africa" }
        assertEquals(2, africa.count)
    }

    @Test
    fun `sections group by status in the sandbox-mock-kyc-comingsoon order`() {
        val state = viewModel().uiState.value

        assertEquals(
            listOf(GatewayStatusUi.SANDBOX_READY, GatewayStatusUi.MOCK_MODE, GatewayStatusUi.COMING_SOON),
            state.sections.map { it.status },
        )
    }

    @Test
    fun `search query filters by display name`() {
        val vm = viewModel()

        vm.onSearchQueryChange("Paystack")

        val allRows =
            vm.uiState.value.sections
                .flatMap { it.providers }
        assertEquals(listOf("Paystack"), allRows.map { it.displayName })
    }

    @Test
    fun `toggling a status filter narrows the sections to that status only`() {
        val vm = viewModel()

        vm.onToggleStatusFilter(GatewayStatusUi.MOCK_MODE)

        assertEquals(
            listOf(GatewayStatusUi.MOCK_MODE),
            vm.uiState.value.sections
                .map { it.status },
        )
    }

    @Test
    fun `toggling a region filter twice clears it back to showing everything`() {
        val vm = viewModel()

        vm.onToggleRegionFilter("Africa")
        vm.onToggleRegionFilter("Africa")

        assertEquals(
            4,
            vm.uiState.value.sections
                .sumOf { it.providers.size },
        )
    }

    @Test
    fun `region filter narrows results to that region`() {
        val vm = viewModel()

        vm.onToggleRegionFilter("Africa")

        val allRows =
            vm.uiState.value.sections
                .flatMap { it.providers }
        assertTrue(allRows.all { it.region == "Africa" })
        assertEquals(2, allRows.size)
    }

    @Test
    fun `clear filters resets search and both filter sets`() {
        val vm = viewModel()
        vm.onSearchQueryChange("Paystack")
        vm.onToggleStatusFilter(GatewayStatusUi.MOCK_MODE)
        vm.onToggleRegionFilter("Africa")

        vm.onClearFilters()

        val state = vm.uiState.value
        assertEquals("", state.searchQuery)
        assertTrue(state.selectedStatuses.isEmpty())
        assertTrue(state.selectedRegions.isEmpty())
        assertEquals(4, state.sections.sumOf { it.providers.size })
    }
}
