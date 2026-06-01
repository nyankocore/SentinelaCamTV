package com.sentinela.camtv.preferences

import com.sentinela.camtv.config.FULLSCREEN_SUBTYPE
import com.sentinela.camtv.domain.Camera
import com.sentinela.camtv.domain.DvrRtspChannel
import com.sentinela.camtv.player.AudioMode
import org.junit.Assert.assertEquals
import org.junit.Test

class FullscreenAudioPolicyTest {
    @Test
    fun fullscreenAudioIsEnabledByDefault() {
        assertEquals(
            AudioMode.Enabled,
            PlayerUiPreferences().fullscreenAudioModeFor(camera(id = "cam-1")),
        )
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
