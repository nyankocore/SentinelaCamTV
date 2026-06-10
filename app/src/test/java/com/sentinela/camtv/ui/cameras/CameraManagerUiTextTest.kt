package com.sentinela.camtv.ui.cameras

import com.sentinela.camtv.data.mosaic.MosaicSlot
import com.sentinela.camtv.domain.Camera
import com.sentinela.camtv.domain.RtspCameraSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraManagerUiTextTest {
    @Test
    fun connectedTabMentionsMosaicsTab() {
        assertEquals(
            "Confira as câmeras conectadas. Para organizar posições, use a aba Mosaicos.",
            CONNECTED_TAB_DESCRIPTION,
        )
    }

    @Test
    fun connectedCameraMessageUsesPluralSuccessTitle() {
        assertEquals(
            "Câmera(s) conectada(s)",
            cameraDialogTitle(cameraConnectedMessage(isFirstRegistration = false)),
        )
    }

    @Test
    fun firstRegistrationMessageGuidesToMosaicsTab() {
        assertEquals(
            "Câmera(s) conectada(s). Organize seu mosaico na aba Mosaicos antes de visualizar a(s) câmera(s).",
            cameraConnectedMessage(isFirstRegistration = true),
        )
        assertEquals(
            CameraManagerDialogAction.ORGANIZE_MOSAIC,
            cameraConnectedAction(isFirstRegistration = true),
        )
    }

    @Test
    fun laterRegistrationMessageIsShort() {
        assertEquals(
            "Câmera(s) conectada(s).",
            cameraConnectedMessage(isFirstRegistration = false),
        )
        assertEquals(null, cameraConnectedAction(isFirstRegistration = false))
    }

    @Test
    fun connectedCamerasDisplayDoesNotLimitToFive() {
        val cameras = (1..7).map { index ->
            Camera(
                id = "cam-$index",
                name = "CAM$index",
                source = RtspCameraSource("rtsp://camera/$index", null),
                position = 8 - index,
            )
        }

        val result = connectedCamerasForDisplay(cameras)

        assertEquals(7, result.size)
        assertEquals("cam-7", result.first().id)
    }

    @Test
    fun mosaicSummaryUsesOutsideMosaicText() {
        val state = CameraManagerUiState(
            cameras = listOf(
                Camera("cam-1", "CAM1", RtspCameraSource("rtsp://camera/1", null), position = 0),
                Camera("cam-2", "CAM2", RtspCameraSource("rtsp://camera/2", null), position = 1),
            ),
            mosaicSlots = listOf(MosaicSlot(mosaicIndex = 0, slotIndex = 0, cameraId = "cam-1")),
        )

        val summary = mosaicSummaryText(state)

        assertEquals("Fora do mosaico", OUTSIDE_MOSAIC_TITLE)
        assertFalse(summary.contains("Sem mosaico"))
        assertTrue(summary.contains("Fora do mosaico: 1 câmera"))
    }

    @Test
    fun emptyMosaicSlotIsNotSelectedWhenNoCameraIsSelected() {
        assertFalse(isMosaicSlotSelected(cameraId = null, selectedCameraId = null))
    }

    @Test
    fun occupiedMosaicSlotIsSelectedOnlyForMatchingCamera() {
        assertTrue(isMosaicSlotSelected(cameraId = "cam-1", selectedCameraId = "cam-1"))
        assertFalse(isMosaicSlotSelected(cameraId = "cam-1", selectedCameraId = "cam-2"))
    }
}
