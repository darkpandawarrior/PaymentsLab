package com.paymentslab.core.common

/**
 * A string destined for the UI, kept out of the domain/data layers. The presentation layer resolves
 * it to an actual `String`. Errors are mapped to [UiText] at the point they're produced, so
 * ViewModels and gateways never hold pre-formatted, locale-bound strings.
 *
 * Kept intentionally small for the showcase: a literal [Dynamic] value covers every current need;
 * a resource-key variant can be added when localization lands without touching call sites that
 * already return [UiText].
 */
sealed interface UiText {
    data class Dynamic(
        val value: String,
    ) : UiText

    data object Empty : UiText

    companion object {
        fun of(value: String?): UiText = if (value.isNullOrBlank()) Empty else Dynamic(value)
    }
}
