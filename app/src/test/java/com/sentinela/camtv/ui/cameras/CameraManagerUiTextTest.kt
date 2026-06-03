package com.sentinela.camtv.ui.cameras

import com.sentinela.camtv.domain.Camera
import com.sentinela.camtv.domain.RtspCameraSource
import org.junit.Assert.assertEquals
import org.junit.Test

class CameraManagerUiTextTest {
    @Test
    fun connectedTabMentionsEditMosaic() {
        assertEquals(
            "Confira as câmeras conectadas. Para trocar posições ou excluir, use Editar mosaico no menu rápido do mosaico.",
            CONNECTED_TAB_DESCRIPTION,
        )
    }

    @Test
    fun freeActiveCameraMessageUsesSuccessTitle() {
        assertEquals(
            "Câmera ativa atualizada",
            cameraDialogTitle("Câmera ativa no modo grátis atualizada."),
        )
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
}
