package com.paymentslab.feature.history

import com.paymentslab.core.paymentsapi.Money

/** Presentation-only formatting of [Money] — formatting lives at the UI edge, never in the value type. */
internal fun Money.format(): String {
    val symbol =
        when (currency) {
            "INR" -> "₹"
            "USD" -> "$"
            else -> "$currency "
        }
    val whole = amountMinor / 100
    val frac = (amountMinor % 100).toInt()
    val fracStr = if (frac < 10) "0$frac" else "$frac"
    return "$symbol$whole.$fracStr"
}
