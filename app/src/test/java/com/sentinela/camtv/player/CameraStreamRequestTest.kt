package com.sentinela.camtv.player

import com.sentinela.camtv.config.FULLSCREEN_SUBTYPE
import com.sentinela.camtv.config.MOSAIC_SUBTYPE
import com.sentinela.camtv.domain.Camera
import com.sentinela.camtv.domain.DvrRtspChannel
import org.junit.Assert.assertEquals
import org.junit.Test

class CameraStreamRequestTest {
    private val camera = Camera(
        id = "cam-1",
        name = "CAM1",
        source = DvrRtspChannel(
            channel = 1,
            subtype = MOSAIC_SUBTYPE,
        ),
    )

    @Test
    fun mosaicRequestUsesSubstreamAndDisablesAudio() {
        val request = camera.streamRequestFor(PlayerMode.Mosaic)

        assertEquals(MOSAIC_SUBTYPE, request.subtype)
        assertEquals(PlayerMode.Mosaic, request.mode)
        assertEquals(AudioMode.Disabled, request.audioMode)
        assertEquals(TransmissionMode.MENOR_LATENCIA, request.transmissionMode)
    }

    @Test
    fun fullscreenRequestUsesMainStreamAndEnablesAudio() {
        val request = camera.streamRequestFor(PlayerMode.Fullscreen)

        assertEquals(FULLSCREEN_SUBTYPE, request.subtype)
        assertEquals(PlayerMode.Fullscreen, request.mode)
        assertEquals(AudioMode.Enabled, request.audioMode)
        assertEquals(TransmissionMode.MENOR_LATENCIA, request.transmissionMode)
    }
}
