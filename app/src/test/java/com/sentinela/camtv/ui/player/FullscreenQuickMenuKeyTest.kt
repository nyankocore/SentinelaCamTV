package com.sentinela.camtv.ui.player

import androidx.compose.ui.input.key.Key
import com.sentinela.camtv.ui.mosaic.MosaicNavigationDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FullscreenQuickMenuKeyTest {
    @Test
    fun okAndEnterOpenQuickMenu() {
        assertTrue(Key.DirectionCenter.opensFullscreenQuickMenu())
        assertTrue(Key.Enter.opensFullscreenQuickMenu())
        assertTrue(Key.NumPadEnter.opensFullscreenQuickMenu())
    }

    @Test
    fun directionDownDoesNotOpenQuickMenu() {
        assertFalse(Key.DirectionDown.opensFullscreenQuickMenu())
    }

    @Test
    fun directionKeysMapToFullscreenNavigation() {
        assertEquals(MosaicNavigationDirection.Up, Key.DirectionUp.fullscreenNavigationDirection())
        assertEquals(MosaicNavigationDirection.Down, Key.DirectionDown.fullscreenNavigationDirection())
        assertEquals(MosaicNavigationDirection.Left, Key.DirectionLeft.fullscreenNavigationDirection())
        assertEquals(MosaicNavigationDirection.Right, Key.DirectionRight.fullscreenNavigationDirection())
        assertNull(Key.DirectionCenter.fullscreenNavigationDirection())
    }

    @Test
    fun recordingLabelUsesManualRecordingState() {
        assertEquals(
            "Gravar",
            fullscreenRecordingMenuLabel(recordingProbeActive = false),
        )
        assertEquals(
            "Parar",
            fullscreenRecordingMenuLabel(recordingProbeActive = true),
        )
    }
}
