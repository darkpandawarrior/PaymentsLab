package com.paymentslab.core.designsystem

import com.siddharth.kmp.common.minorToDecimalString
import com.siddharth.kmp.paymentsapi.Money

/**
 * Presentation-only formatting of [Money] into a human string. Formatting lives at the UI edge —
 * never in the [Money] value type — per the money-handling rule the whole app teaches.
 */
fun Money.format(): String {
    val symbol =
        when (currency) {
            "INR" -> "₹"
            "USD" -> "$"
            else -> "$currency "
        }
    return "$symbol${amountMinor.minorToDecimalString()}"
}
