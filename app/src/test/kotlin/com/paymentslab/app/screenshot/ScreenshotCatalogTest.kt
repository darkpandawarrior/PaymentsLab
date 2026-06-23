package com.paymentslab.app.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.paymentslab.core.designsystem.AnimatedAmount
import com.paymentslab.core.designsystem.DesignTokens
import com.paymentslab.core.designsystem.FailureShake
import com.paymentslab.core.designsystem.FlowHop
import com.paymentslab.core.designsystem.GatewayStatusBadge
import com.paymentslab.core.designsystem.GatewayStatusUi
import com.paymentslab.core.designsystem.PayloadCard
import com.paymentslab.core.designsystem.PaymentFlowDiagram
import com.paymentslab.core.designsystem.PaymentsLabTheme
import com.paymentslab.core.designsystem.PrimaryButton
import com.paymentslab.core.designsystem.RedactionReveal
import com.paymentslab.core.designsystem.SectionHeader
import com.paymentslab.core.designsystem.StepState
import com.paymentslab.core.designsystem.StepTimeline
import com.paymentslab.core.designsystem.SuccessBurst
import com.paymentslab.core.designsystem.TimelineStep
import com.paymentslab.core.paymentsapi.PaymentStatus
import com.paymentslab.feature.lab.ProviderLabScreen
import com.paymentslab.feature.lab.ProviderLabUiState
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Deterministic Compose screenshot tests on the JVM (Robolectric, no emulator). Baselines live in
 * `docs/screenshots/` and diff cleanly in PRs — they double as the README's visual reference.
 *
 * Record/refresh:  ./gradlew :app:recordRoborazziDebug
 * Verify in CI:    ./gradlew :app:verifyRoborazziDebug
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// Plain Application (not PaymentsLabApplication) — these render pure design-system composables and
// must not boot the real Koin graph (which would throw "already started" across tests).
@Config(sdk = [34], application = android.app.Application::class)
class ScreenshotCatalogTest {
    @get:Rule
    val compose = createComposeRule()

    private fun snapshot(
        name: String,
        dark: Boolean = false,
        content: @Composable () -> Unit,
    ) {
        compose.setContent {
            PaymentsLabTheme(darkTheme = dark) {
                Surface(
                    modifier = Modifier.width(360.dp),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Column(Modifier.padding(DesignTokens.Spacing.lg)) { content() }
                }
            }
        }
        compose.onRoot().captureRoboImage("../docs/screenshots/$name.png")
    }

    /** For a full screen (its own Scaffold/topbar/padding) rather than an isolated component. */
    private fun snapshotScreen(
        name: String,
        content: @Composable () -> Unit,
    ) {
        compose.setContent {
            PaymentsLabTheme {
                Surface(modifier = Modifier.width(360.dp).height(720.dp)) { content() }
            }
        }
        compose.onRoot().captureRoboImage("../docs/screenshots/$name.png")
    }

    private fun sampleTimeline() =
        persistentListOf(
            TimelineStep(
                title = "Order created",
                subtitle = "₹499 · price resolved server-side",
                state = StepState.DONE,
                payload = persistentListOf("order_id" to "order_9f3c", "amount" to "49900 INR"),
            ),
            TimelineStep(
                title = "Launching Razorpay",
                subtitle = null,
                state = StepState.DONE,
                payload = persistentListOf("key_id" to "rzp_test_ab••••yz"),
            ),
            TimelineStep(
                title = "Client returned: Success",
                subtitle = "a hint — not yet trusted",
                state = StepState.DONE,
                payload = persistentListOf("payment_id" to "pay_Kx1", "signature" to "9f••••3a"),
            ),
            TimelineStep(
                title = "Verifying with server",
                subtitle = "HMAC-SHA256 signature check",
                state = StepState.ACTIVE,
                payload = persistentListOf(),
            ),
            TimelineStep(
                title = "Settled",
                subtitle = "awaiting server verdict",
                state = StepState.PENDING,
                payload = persistentListOf(),
            ),
        )

    @Test
    fun stepTimeline_light() = snapshot("step_timeline_light") { StepTimeline(sampleTimeline()) }

    @Test
    fun stepTimeline_dark() = snapshot("step_timeline_dark", dark = true) { StepTimeline(sampleTimeline()) }

    @Test
    fun payloadCard() =
        snapshot("payload_card") {
            PayloadCard(
                title = "verify request",
                entries =
                    persistentListOf(
                        "gateway" to "razorpay",
                        "order_id" to "order_9f3c",
                        "signature" to "9f••••3a",
                    ),
            )
        }

    @Test
    fun gatewayBadges() =
        snapshot("gateway_badges") {
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm)) {
                SectionHeader("Provider status")
                GatewayStatusBadge(GatewayStatusUi.SANDBOX_READY)
                GatewayStatusBadge(GatewayStatusUi.KYC_GATED)
                GatewayStatusBadge(GatewayStatusUi.COMING_SOON)
            }
        }

    @Test
    fun primaryButton() =
        snapshot("primary_button") {
            PrimaryButton(text = "Pay ₹499 (sandbox)", onClick = {}, modifier = Modifier.fillMaxWidth())
        }

    @Test
    fun mockModeBadgeShimmer() = snapshot("mock_mode_badge_shimmer") { GatewayStatusBadge(GatewayStatusUi.MOCK_MODE) }

    @Test
    fun successBurst() = snapshot("success_burst") { SuccessBurst() }

    @Test
    fun failureShake() = snapshot("failure_shake") { FailureShake() }

    @Test
    fun animatedAmount() = snapshot("animated_amount") { AnimatedAmount(amountMinor = 49_900L, currency = "INR") }

    @Test
    fun redactionReveal() = snapshot("redaction_reveal") { RedactionReveal(value = "9f••••3a") }

    @Test
    fun paymentFlowDiagram_unverified() =
        snapshot("payment_flow_diagram_unverified") {
            PaymentFlowDiagram(activeHop = FlowHop.GATEWAY, verified = false)
        }

    @Test
    fun paymentFlowDiagram_verified() =
        snapshot("payment_flow_diagram_verified") {
            PaymentFlowDiagram(activeHop = FlowHop.WEBHOOK, verified = true)
        }

    // ── Composed ProviderLabScreen — catches layout issues the isolated component shots miss ──────
    @Test
    fun providerLabScreen_running() =
        snapshotScreen("provider_lab_screen_running") {
            ProviderLabScreen(
                state =
                    ProviderLabUiState(
                        steps = sampleTimeline(),
                        isRunning = true,
                        currentHop = FlowHop.GATEWAY,
                        verified = false,
                    ),
                providerName = "Paystack",
                priceLabel = "₹149",
                onPay = {},
                onBack = {},
            )
        }

    @Test
    fun providerLabScreen_settledSuccess() =
        snapshotScreen("provider_lab_screen_settled_success") {
            ProviderLabScreen(
                state =
                    ProviderLabUiState(
                        steps = sampleTimeline(),
                        isRunning = false,
                        hasRun = true,
                        finalStatus = PaymentStatus.SUCCESS,
                        currentHop = FlowHop.BACKEND,
                        verified = true,
                    ),
                providerName = "Paystack",
                priceLabel = "₹149",
                onPay = {},
                onBack = {},
            )
        }
}
