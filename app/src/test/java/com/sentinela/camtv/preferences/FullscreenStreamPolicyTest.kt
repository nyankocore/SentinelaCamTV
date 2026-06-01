package com.sentinela.camtv.preferences

import com.sentinela.camtv.config.FULLSCREEN_SUBTYPE
import com.sentinela.camtv.domain.Camera
import com.sentinela.camtv.domain.DvrRtspChannel
import com.sentinela.camtv.player.TransmissionMode
import org.junit.Assert.assertEquals
import org.junit.Test

class FullscreenStreamPolicyTest {
    @Test
    fun fullscreenUsesMainStreamByDefault() {
        val request = PlayerUiPreferences().fullscreenStreamRequestFor(camera(id = "cam-5"))

        assertEquals(FULLSCREEN_SUBTYPE, request.subtype)
    }

    @Test
    fun fullscreenRequestUsesEnabledAudioAndGlobalTransmissionMode() {
        val request = PlayerUiPreferences(
            globalTransmissionMode = TransmissionMode.QUALIDADE,
        ).fullscreenStreamRequestFor(camera(id = "cam-5"))

        assertEquals(FULLSCREEN_SUBTYPE, request.subtype)
        assertEquals(TransmissionMode.QUALIDADE, request.transmissionMode)
    }

    private fun camera(id: String): Camera = Camera(
        id = id,
        name = id.uppercase(),
        source = DvrRtspChannel(
            channel = 1,
            subtype = FULLSCREEN_SUBTYPE,
        ),
    )
}
