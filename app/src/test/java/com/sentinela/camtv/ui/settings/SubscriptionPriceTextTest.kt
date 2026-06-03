package com.sentinela.camtv.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubscriptionPriceTextTest {
    @Test
    fun annualSavingsUsesBrazilianPrices() {
        assertEquals(
            "Economia aproximada: 16% (R$ 18,90 por ano).",
            annualSavingsText("R$ 9,90", "R$ 99,90"),
        )
    }

    @Test
    fun annualSavingsReturnsNullWhenAnnualIsNotCheaper() {
        assertNull(annualSavingsText("R$ 9,90", "R$ 120,00"))
    }
}
