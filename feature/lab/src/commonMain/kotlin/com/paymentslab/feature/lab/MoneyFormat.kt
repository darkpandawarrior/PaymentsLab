package com.paymentslab.feature.lab

import com.paymentslab.core.paymentsapi.Money

/**
 * Presentation-only formatting of [Money] into a human string. Formatting lives at the UI edge —
 * never in the [Money] value type — per the money-handling rule the whole app teaches.
 */
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
