package com.sentinela.camtv.localization

import android.content.res.Configuration
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppLanguageTest {
    @Test
    fun blankAndSystemTagsResolveToSystem() {
        assertEquals(AppLanguage.System, AppLanguage.fromTag(null))
        assertEquals(AppLanguage.System, AppLanguage.fromTag(""))
        assertEquals(AppLanguage.System, AppLanguage.fromTag("system"))
    }

    @Test
    fun validTagsResolveToLanguage() {
        assertEquals(AppLanguage.PortugueseBrazil, AppLanguage.fromTag("pt-BR"))
        assertEquals(AppLanguage.English, AppLanguage.fromTag("en"))
        assertEquals(AppLanguage.Spanish, AppLanguage.fromTag("es"))
        assertEquals(AppLanguage.Turkish, AppLanguage.fromTag("tr"))
        assertEquals(AppLanguage.Arabic, AppLanguage.fromTag("ar"))
        assertEquals(AppLanguage.Russian, AppLanguage.fromTag("ru"))
    }

    @Test
    fun invalidTagFallsBackToSystem() {
        assertEquals(AppLanguage.System, AppLanguage.fromTag("xx"))
    }

    @Test
    fun systemLanguageUsesNullStorageTag() {
        assertNull(AppLanguage.System.storageTag)
    }

    @Test
    fun arabicUsesRtlAndExplicitLanguagesUseLtr() {
        assertEquals(LayoutDirection.Rtl, Configuration().appLayoutDirection(AppLanguage.Arabic))
        assertEquals(LayoutDirection.Ltr, Configuration().appLayoutDirection(AppLanguage.English))
        assertEquals(LayoutDirection.Ltr, Configuration().appLayoutDirection(AppLanguage.PortugueseBrazil))
    }
}
