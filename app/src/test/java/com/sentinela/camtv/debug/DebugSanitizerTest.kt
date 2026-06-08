package com.sentinela.camtv.debug

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugSanitizerTest {
    @Test
    fun sanitizerRemovesSensitiveCameraData() {
        val raw = "rtsp://usuario:segredo@192.0.2.10:554/cam?token=abc&usuario=usuario senha=123"

        val sanitized = sanitizeDebugText(raw)

        assertFalse(sanitized.contains("usuario:segredo"))
        assertFalse(sanitized.contains("192.0.2.10"))
        assertFalse(sanitized.contains("token=abc"))
        assertFalse(sanitized.contains("senha=123"))
        assertTrue(sanitized.contains("rtsp://<oculto>"))
        assertTrue(sanitized.contains("<ip>") || !sanitized.contains("192.0.2"))
    }
}
