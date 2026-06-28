package com.sentinela.camtv.preferences

import com.sentinela.camtv.player.StreamQuality
import com.sentinela.camtv.player.TransmissionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerUiPreferencesTest {
    @Test
    fun showPlayerInfoDefaultsToTrue() {
        assertTrue(PlayerUiPreferences().showPlayerInfo)
    }

    @Test
    fun mosaicAndFullscreenInfoDefaultToTrue() {
        val preferences = PlayerUiPreferences()

        assertTrue(preferences.showMosaicInfo)
        assertTrue(preferences.showFullscreenInfo)
    }

    @Test
    fun globalTransmissionModeDefaultsToLowerLatency() {
        assertEquals(TransmissionMode.MENOR_LATENCIA, PlayerUiPreferences().globalTransmissionMode)
    }

    @Test
    fun fullscreenQuickMenuHintStartsUnseen() {
        assertFalse(PlayerUiPreferences().fullscreenQuickMenuHintSeen)
    }

    @Test
    fun mosaicStreamQualityDefaultsToSd() {
        assertEquals(StreamQuality.SD, PlayerUiPreferences().mosaicStreamQuality)
    }

    @Test
    fun appLanguageDefaultsToSystem() {
        assertEquals(null, PlayerUiPreferences().appLanguageTag)
    }
}
