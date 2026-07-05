package com.paymentslab.feature.checkoutdemo

import androidx.compose.runtime.Immutable
import com.paymentslab.core.designsystem.format
import com.paymentslab.core.paymentsapi.Money
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** A demo product shown in the explained-checkout flow. */
@Immutable
data class DemoProduct(
    val catalogItemId: String,
    val title: String,
    val price: Money,
) {
    val priceLabel: String get() = price.format()
}

/**
 * The hardcoded demo catalog. The [catalogItemId]s are the contract with the backend catalog — the
 * server resolves the authoritative price from these ids; the client never sends an amount.
 */
val DEMO_PRODUCTS: ImmutableList<DemoProduct> =
    persistentListOf(
        DemoProduct("coffee_149", "Filter Coffee", Money.inr(149)),
        DemoProduct("book_499", "Paperback Book", Money.inr(499)),
        DemoProduct("headphones_2499", "Wireless Headphones", Money.inr(2499)),
        DemoProduct("course_9999", "KMP Payments Course", Money.inr(9999)),
        DemoProduct("ebook_usd_9", "E-book (USD)", Money.usd(9)),
    )
