package com.paymentslab.backend

import com.paymentslab.core.protocol.CatalogItemDto

/**
 * Fixed server-side catalog. Price lives HERE, on the server — order creation resolves the amount
 * from the catalog and the client-sent amount (there is none in [com.paymentslab.core.protocol.CreateOrderRequest],
 * by design) is never trusted. This is the trust boundary the whole showcase is built around.
 */
class CatalogService {
    private val items: List<CatalogItemDto> =
        listOf(
            // IDs align with the checkout-demo feature's product list.
            CatalogItemDto(
                id = "coffee_149",
                title = "Coffee",
                description = "A hot cup of filter coffee.",
                amountMinor = 14_900L,
                currency = "INR",
            ),
            CatalogItemDto(
                id = "book_499",
                title = "Paperback Book",
                description = "A well-worn paperback classic.",
                amountMinor = 49_900L,
                currency = "INR",
            ),
            CatalogItemDto(
                id = "headphones_2499",
                title = "Wireless Headphones",
                description = "Over-ear noise-cancelling headphones.",
                amountMinor = 249_900L,
                currency = "INR",
            ),
            CatalogItemDto(
                id = "course_9999",
                title = "Online Course",
                description = "Lifetime access to the KMP payments course.",
                amountMinor = 999_900L,
                currency = "INR",
            ),
            CatalogItemDto(
                id = "ebook_usd_9",
                title = "E-book (USD)",
                description = "A DRM-free e-book, priced in USD.",
                amountMinor = 999L,
                currency = "USD",
            ),
        )

    fun all(): List<CatalogItemDto> = items

    fun find(id: String): CatalogItemDto? = items.firstOrNull { it.id == id }
}
