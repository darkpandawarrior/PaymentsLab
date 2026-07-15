package com.paymentslab.backend

import com.paymentslab.core.config.GatewayCredentials
import com.paymentslab.core.protocol.CatalogItemDto
import com.paymentslab.core.protocol.PaymentStatusDto
import com.paymentslab.core.protocol.VerifyRequest
import com.siddharth.kmp.common.minorToDecimalString
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * PayPal — Tier-1, real Orders v2 REST API (no Android SDK needed; the approval step is a hosted
 * redirect, so this rides `provider:hosted-webview` client-side exactly like Paystack, no new client
 * module). `credentials.enabled` (from `core:config`, `PLAB_PAYPAL_TEST_CLIENT_ID`/`_CLIENT_SECRET`)
 * decides real vs mock, same pattern as [PaystackAdapter].
 *
 * **Real** (keys configured): fetches an OAuth token (`POST /v1/oauth2/token`, HTTP Basic
 * `client_id:client_secret`), then `POST /v2/checkout/orders` and returns the `approve` link from
 * the response's `links` array as `checkout_url`. `verify` captures the order
 * (`POST /v2/checkout/orders/{id}/capture`) using `req.paymentId` as PayPal's order id (the client
 * echoes it back — see `docs/providers/paypal.md`) and maps `COMPLETED` → SUCCESS.
 *
 * **Mock** (default): falls back to the generic `/mock/checkout/paypal` path.
 *
 * Not exercised against the live sandbox in this repo (no test credentials available) — the
 * request/response mapping is verified with Ktor `MockEngine` instead
 * (`PayPalAdapterTest.kt`).
 * `docs: https://developer.paypal.com/docs/api/orders/v2/`
 */
class PayPalAdapter(
    private val credentials: GatewayCredentials,
    private val publicBaseUrl: String,
    private val httpClient: HttpClient,
) : GatewayAdapter {
    override val gatewayId: String = "paypal"

    override suspend fun createProviderOrder(
        orderId: String,
        item: CatalogItemDto,
    ): Map<String, String> {
        if (!credentials.enabled) {
            return mapOf("checkout_url" to "$publicBaseUrl/mock/checkout/paypal?orderId=$orderId")
        }
        val accessToken = fetchAccessToken()
        val response: PayPalOrderResponse =
            httpClient
                .post("$PAYPAL_SANDBOX_BASE/v2/checkout/orders") {
                    header("Authorization", "Bearer $accessToken")
                    contentType(ContentType.Application.Json)
                    setBody(
                        PayPalOrderRequest(
                            intent = "CAPTURE",
                            purchaseUnits =
                                listOf(
                                    PayPalPurchaseUnit(
                                        amount =
                                            PayPalAmount(
                                                currencyCode = item.currency,
                                                value = item.amountMinor.minorToDecimalString(),
                                            ),
                                    ),
                                ),
                            applicationContext =
                                PayPalApplicationContext(returnUrl = "$publicBaseUrl/mock/return/success"),
                        ),
                    )
                }.body()
        val approveLink =
            response.links.firstOrNull { it.rel == "approve" }?.href
                ?: "$publicBaseUrl/mock/checkout/paypal?orderId=$orderId"
        return mapOf("checkout_url" to approveLink, "paypal_order_id" to response.id)
    }

    override suspend fun verify(req: VerifyRequest): PaymentStatusDto {
        if (!credentials.enabled) return PaymentStatusDto.PENDING
        val paypalOrderId = req.paymentId ?: return PaymentStatusDto.FAILED
        val accessToken = fetchAccessToken()
        val response: PayPalCaptureResponse =
            httpClient
                .post("$PAYPAL_SANDBOX_BASE/v2/checkout/orders/$paypalOrderId/capture") {
                    header("Authorization", "Bearer $accessToken")
                    contentType(ContentType.Application.Json)
                }.body()
        return when (response.status) {
            "COMPLETED" -> PaymentStatusDto.SUCCESS
            "VOIDED", "DECLINED" -> PaymentStatusDto.FAILED
            else -> PaymentStatusDto.PENDING
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private suspend fun fetchAccessToken(): String {
        val secretKey = credentials.keys.getValue("client_secret")
        val clientId = credentials.keys.getValue("client_id")
        val basicAuth = Base64.encode("$clientId:$secretKey".encodeToByteArray())
        val response: PayPalTokenResponse =
            httpClient
                .submitForm(
                    url = "$PAYPAL_SANDBOX_BASE/v1/oauth2/token",
                    formParameters = Parameters.build { append("grant_type", "client_credentials") },
                ) {
                    header("Authorization", "Basic $basicAuth")
                }.body()
        return response.accessToken
    }

    private companion object {
        const val PAYPAL_SANDBOX_BASE = "https://api-m.sandbox.paypal.com"
    }
}

@Serializable
data class PayPalOrderRequest(
    val intent: String,
    @SerialName("purchase_units") val purchaseUnits: List<PayPalPurchaseUnit>,
    @SerialName("application_context") val applicationContext: PayPalApplicationContext,
)

@Serializable
data class PayPalPurchaseUnit(
    val amount: PayPalAmount,
)

@Serializable
data class PayPalAmount(
    @SerialName("currency_code") val currencyCode: String,
    val value: String,
)

@Serializable
data class PayPalApplicationContext(
    @SerialName("return_url") val returnUrl: String,
)

@Serializable
data class PayPalOrderResponse(
    val id: String,
    val status: String,
    val links: List<PayPalLink>,
)

@Serializable
data class PayPalLink(
    val href: String,
    val rel: String,
)

@Serializable
data class PayPalCaptureResponse(
    val id: String,
    val status: String,
)

@Serializable
data class PayPalTokenResponse(
    @SerialName("access_token") val accessToken: String,
)
