package com.paymentslab.core.designsystem

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GatewayBrandingTest {
    @Test
    fun known_gateway_returns_its_curated_logo() {
        val asset = GatewayBranding.forId("stripe")
        assertIs<GatewayBrandAsset.Logo>(asset)
    }

    @Test
    fun unknown_gateway_falls_back_to_a_deterministic_monogram() {
        val first = GatewayBranding.forId("some_future_gateway_not_in_the_catalog")
        val second = GatewayBranding.forId("some_future_gateway_not_in_the_catalog")
        assertIs<GatewayBrandAsset.Monogram>(first)
        // Same ID -> same color every time (deterministic, not random per composition).
        assertEquals((first as GatewayBrandAsset.Monogram).color, (second as GatewayBrandAsset.Monogram).color)
    }

    @Test
    fun monogram_letter_is_the_first_letter_of_the_display_name_uppercased() {
        val asset = GatewayBranding.forId("wipay", displayName = "WiPay")
        assertEquals('W', (asset as GatewayBrandAsset.Monogram).letter)
    }

    @Test
    fun different_ids_are_spread_across_the_palette_not_collapsed_to_one_color() {
        val ids = listOf("araka", "kanoo", "thiwani", "vukapay", "moncash", "jcc", "truevo", "dotlines")
        val colors = ids.map { (GatewayBranding.forId(it) as GatewayBrandAsset.Monogram).color }
        assertTrue(colors.toSet().size > 1, "expected the hash to spread ids across more than one color")
    }
}
