package com.sentinela.camtv.capture

import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureFileNameFormatterTest {
    @Test
    fun fileNameUsesSanitizedCameraNameAndMilliseconds() {
        val formatter = CaptureFileNameFormatter(now = { Date(1_700_000_000_123L) })

        val fileName = formatter.photoFileName("CAM 1 / Rua")

        assertTrue(fileName.startsWith("SentinelaCamTV_cam_1_rua_"))
        assertTrue(fileName.endsWith("_123.jpg"))
    }

    @Test
    fun emptyCameraNameFallsBackToCamera() {
        val formatter = CaptureFileNameFormatter(now = { Date(1_700_000_000_123L) })

        val fileName = formatter.photoFileName("  !!  ")

        assertTrue(fileName.startsWith("SentinelaCamTV_camera_"))
    }

    @Test
    fun videoProbeFileNameUsesMp4AndMilliseconds() {
        val formatter = CaptureFileNameFormatter(now = { Date(1_700_000_000_123L) })

        val fileName = formatter.videoProbeFileName("CAM 1 / Rua")

        assertTrue(fileName.startsWith("SentinelaCamTV_cam_1_rua_"))
        assertTrue(fileName.endsWith("_123.mp4"))
    }

    @Test
    fun sanitizedNameKeepsReasonableLength() {
        val formatter = CaptureFileNameFormatter()

        val name = formatter.sanitizeCameraName("CAMERA_COM_NOME_MUITO_LONGO_PARA_ARQUIVO_LOCAL")

        assertEquals(36, name.length)
    }
}
