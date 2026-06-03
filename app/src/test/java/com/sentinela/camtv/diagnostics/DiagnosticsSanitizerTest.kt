package com.sentinela.camtv.diagnostics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsSanitizerTest {
    @Test
    fun removesRtspPathAndCredentialsFromText() {
        val sanitized = DiagnosticsSanitizer.sanitize(
            "Erro rtsp://bruno:senha@192.168.100.31:554/cam/realmonitor?channel=1&subtype=0 username=bruno senha=bruno2077",
        )

        assertTrue(sanitized.contains("rtsp://192.168.100.31:554/<caminho-removido>"))
        assertTrue(sanitized.contains("username=<removido>"))
        assertTrue(sanitized.contains("senha=<removido>"))
        assertFalse(sanitized.contains("bruno2077"))
        assertFalse(sanitized.contains("realmonitor"))
    }

    @Test
    fun sanitizedThrowableKeepsOriginalClassButRemovesSensitiveMessage() {
        val throwable = IllegalStateException("Falha em rtsp://user:pass@10.0.0.5/live senha=123")

        val sanitized = DiagnosticsSanitizer.sanitizeThrowable(throwable)

        assertTrue(sanitized.message.orEmpty().contains("IllegalStateException"))
        assertFalse(sanitized.message.orEmpty().contains("pass"))
        assertFalse(sanitized.message.orEmpty().contains("123"))
    }
}
