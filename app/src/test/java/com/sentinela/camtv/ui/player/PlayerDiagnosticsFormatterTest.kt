package com.sentinela.camtv.ui.player

import com.sentinela.camtv.player.PlayerConnectionState
import com.sentinela.camtv.player.RtspTransportMode
import com.sentinela.camtv.player.TransmissionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerDiagnosticsFormatterTest {
    @Test
    fun overlayIncludesCorePlaybackDiagnostics() {
        val lines = PlayerDiagnosticsFormatter.overlayLines(
            PlayerDiagnostics(
                cameraName = "CAM1",
                connectionState = PlayerConnectionState.Playing,
                subtype = 1,
                transmissionMode = TransmissionMode.MENOR_LATENCIA,
                initialTransportMode = RtspTransportMode.UdpFirst,
                transportMode = RtspTransportMode.UdpFirst,
                decoderFallbackEnabled = false,
                videoSize = "352x240",
                framesPerSecond = 15f,
                bandwidthEstimateBps = 512_000,
                bufferMs = 180,
                mimeType = "video/avc",
                decoderName = "OMX.amlogic.avc.decoder",
                droppedFrames = 2,
                reconnectAttempt = 1,
                readyMs = 820,
                firstFrameMs = 1_100,
                renderedFirstFrame = true,
            ),
        )

        assertTrue(lines.any { it.contains("CAM1") && it.contains("SD") && it.contains("subtype 1") })
        assertTrue(lines.any { it.contains("352x240") && it.contains("15 FPS") })
        assertTrue(lines.any { it.contains("RTSP: UDP") })
        assertTrue(lines.any { it.contains("Decoder: HW") })
        assertTrue(lines.any { it.contains("Banda 512 kb/s") && it.contains("Buffer 180 ms") })
        assertTrue(lines.any { it.contains("READY 820 ms") && it.contains("1º frame 1100 ms") })
    }

    @Test
    fun overlayShowsTcpFallbackAndMissingFirstFrame() {
        val lines = PlayerDiagnosticsFormatter.overlayLines(
            PlayerDiagnostics(
                cameraName = "CAM5",
                connectionState = PlayerConnectionState.Playing,
                subtype = 0,
                transmissionMode = TransmissionMode.MENOR_LATENCIA,
                initialTransportMode = RtspTransportMode.UdpFirst,
                transportMode = RtspTransportMode.TcpOnly,
                decoderFallbackEnabled = true,
                renderedFirstFrame = false,
                lastWatchdogReason = "black frame watchdog: sem primeiro frame",
            ),
        )

        assertTrue(lines.any { it.contains("RTSP: UDP -> TCP fallback") })
        assertTrue(lines.any { it.contains("Decoder: aguardando") })
        assertTrue(lines.any { it.contains("Fallback: permitido") })
        assertTrue(lines.any { it.contains("sem 1º frame") })
        assertTrue(lines.any { it.contains("Watchdog: black frame watchdog") })
    }

    @Test
    fun amlogicDecoderStaysHardwareEvenWithFallbackEnabled() {
        val lines = PlayerDiagnosticsFormatter.overlayLines(
            PlayerDiagnostics(
                cameraName = "CAM1",
                connectionState = PlayerConnectionState.Playing,
                subtype = 1,
                transmissionMode = TransmissionMode.MENOR_LATENCIA,
                initialTransportMode = RtspTransportMode.UdpFirst,
                transportMode = RtspTransportMode.UdpFirst,
                decoderFallbackEnabled = true,
                decoderName = "OMX.amlogic.avc.decoder.awesome2",
                renderedFirstFrame = true,
            ),
        )

        assertTrue(lines.any { it == "Decoder: HW • OMX.amlogic.avc.decoder.awesome2" })
        assertTrue(lines.any { it == "Fallback: permitido" })
        assertFalse(lines.any { it.contains("Decoder: SW") })
    }

    @Test
    fun googleDecoderIsSoftware() {
        val lines = PlayerDiagnosticsFormatter.overlayLines(
            PlayerDiagnostics(
                cameraName = "CAM1",
                connectionState = PlayerConnectionState.Playing,
                subtype = 1,
                transmissionMode = TransmissionMode.MENOR_LATENCIA,
                initialTransportMode = RtspTransportMode.UdpFirst,
                transportMode = RtspTransportMode.UdpFirst,
                decoderFallbackEnabled = false,
                decoderName = "OMX.google.h264.decoder",
                renderedFirstFrame = true,
            ),
        )

        assertTrue(lines.any { it == "Decoder: SW • OMX.google.h264.decoder" })
    }

    @Test
    fun softwareDecoderShowsFallbackInUse() {
        val lines = PlayerDiagnosticsFormatter.overlayLines(
            PlayerDiagnostics(
                cameraName = "CAM5",
                connectionState = PlayerConnectionState.Playing,
                subtype = 1,
                transmissionMode = TransmissionMode.MENOR_LATENCIA,
                initialTransportMode = RtspTransportMode.UdpFirst,
                transportMode = RtspTransportMode.UdpFirst,
                decoderFallbackEnabled = true,
                decoderName = "OMX.google.h264.decoder",
                renderedFirstFrame = true,
            ),
        )

        assertTrue(lines.any { it.contains("Decoder: SW") && it.contains("OMX.google.h264.decoder") })
        assertTrue(lines.any { it == "Fallback: em uso" })
    }

    @Test
    fun androidC2DecoderIsSoftware() {
        val lines = PlayerDiagnosticsFormatter.overlayLines(
            PlayerDiagnostics(
                cameraName = "CAM1",
                connectionState = PlayerConnectionState.Playing,
                subtype = 1,
                transmissionMode = TransmissionMode.MENOR_LATENCIA,
                initialTransportMode = RtspTransportMode.UdpFirst,
                transportMode = RtspTransportMode.UdpFirst,
                decoderFallbackEnabled = false,
                decoderName = "c2.android.avc.decoder",
                renderedFirstFrame = true,
            ),
        )

        assertTrue(lines.any { it == "Decoder: SW • c2.android.avc.decoder" })
    }

    @Test
    fun infoDisabledReturnsNoDiagnosticLines() {
        val lines = PlayerDiagnosticsFormatter.overlayLines(
            diagnostics = PlayerDiagnostics(
                cameraName = "CAM1",
                connectionState = PlayerConnectionState.Playing,
                subtype = 1,
                transmissionMode = TransmissionMode.MENOR_LATENCIA,
                initialTransportMode = RtspTransportMode.UdpFirst,
                transportMode = RtspTransportMode.UdpFirst,
                decoderFallbackEnabled = false,
                decoderName = "OMX.amlogic.avc.decoder.awesome2",
                renderedFirstFrame = true,
            ),
            showPlayerInfo = false,
        )

        assertEquals(emptyList<String>(), lines)
    }

    @Test
    fun autoDowngradedTileShowsSdAuto() {
        val lines = PlayerDiagnosticsFormatter.overlayLines(
            PlayerDiagnostics(
                cameraName = "CAM1",
                connectionState = PlayerConnectionState.Playing,
                subtype = 1,
                transmissionMode = TransmissionMode.MENOR_LATENCIA,
                initialTransportMode = RtspTransportMode.UdpFirst,
                transportMode = RtspTransportMode.UdpFirst,
                decoderFallbackEnabled = false,
                autoQualityDowngraded = true,
                decoderName = "OMX.amlogic.avc.decoder.awesome2",
                renderedFirstFrame = true,
            ),
        )

        assertTrue(lines.first().contains("SD auto"))
    }

    @Test
    fun overlayDoesNotShowLegacyTimeoutBadge() {
        val lines = PlayerDiagnosticsFormatter.overlayLines(
            PlayerDiagnostics(
                cameraName = "CAM5",
                connectionState = PlayerConnectionState.Connecting,
                subtype = 1,
                transmissionMode = TransmissionMode.MENOR_LATENCIA,
                initialTransportMode = RtspTransportMode.UdpFirst,
                transportMode = RtspTransportMode.TcpOnly,
                decoderFallbackEnabled = false,
                renderedFirstFrame = false,
            ),
        )

        assertFalse(lines.any { it.contains("Proteção: timeout") })
    }
}
