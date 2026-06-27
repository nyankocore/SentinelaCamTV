package com.sentinela.camtv.recording

import com.sentinela.camtv.diagnostics.DiagnosticsSanitizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingProbeModelsTest {
    @Test
    fun successMessageMentionsSavedLocation() {
        val message = RecordingProbeResult.Success(
            fileName = "video.mp4",
            locationLabel = RecordingLocationLabels.STANDARD_VIDEOS,
            uri = null,
        ).userMessage()

        assertEquals("Gravação salva: Vídeos/Sentinela Cam TV", message)
    }

    @Test
    fun requestUsesUnlimitedDurationByDefault() {
        val request = RecordingProbeRequest(
            cameraName = "CAM1",
            rtspUrl = "rtsp://example.test/live",
        )

        assertEquals(RECORDING_UNLIMITED_DURATION_MS, request.maxDurationMs)
    }

    @Test
    fun successMessageMentionsMissingAudioWarning() {
        val message = RecordingProbeResult.Success(
            fileName = "video.mp4",
            locationLabel = RecordingLocationLabels.STANDARD_VIDEOS,
            uri = null,
            warning = RECORDING_WITHOUT_AUDIO_WARNING,
        ).userMessage()

        assertEquals(
            "Gravação salva: Vídeos/Sentinela Cam TV. $RECORDING_WITHOUT_AUDIO_WARNING",
            message,
        )
    }

    @Test
    fun unsupportedRtspSourceUsesFriendlyMessage() {
        assertEquals(
            "Gravação não suportada neste aparelho.",
            RecordingProbeError.UnsupportedRtspSource.userMessage(),
        )
    }

    @Test
    fun stopSignalStartsActiveAndCanBeStopped() {
        val stopSignal = RecordingStopSignal()

        assertFalse(stopSignal.isStopped())

        stopSignal.stop()

        assertTrue(stopSignal.isStopped())
    }

    @Test
    fun recordingLogsCanSanitizeRtspUrl() {
        val sanitized = DiagnosticsSanitizer.sanitize(
            "Falha rtsp://user:pass" +
                "@198.51.100.10:554/cam/realmonitor?channel=1&subtype=0",
        )

        assertTrue(sanitized.contains("rtsp://198.51.100.10:554/<caminho-removido>"))
        assertFalse(sanitized.contains("user:pass"))
        assertFalse(sanitized.contains("realmonitor"))
    }
}
