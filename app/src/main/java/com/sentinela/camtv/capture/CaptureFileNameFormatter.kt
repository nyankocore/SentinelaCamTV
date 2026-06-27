package com.sentinela.camtv.capture

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CaptureFileNameFormatter(
    private val now: () -> Date = { Date() },
) {
    fun photoFileName(cameraName: String): String {
        val safeCameraName = sanitizeCameraName(cameraName)
        val timestamp = SimpleDateFormat(PHOTO_TIMESTAMP_PATTERN, Locale.US).format(now())
        return "SentinelaCamTV_${safeCameraName}_$timestamp.jpg"
    }

    fun videoProbeFileName(cameraName: String): String {
        val safeCameraName = sanitizeCameraName(cameraName)
        val timestamp = SimpleDateFormat(PHOTO_TIMESTAMP_PATTERN, Locale.US).format(now())
        return "SentinelaCamTV_${safeCameraName}_$timestamp.mp4"
    }

    fun sanitizeCameraName(cameraName: String): String {
        val cleaned = cameraName
            .trim()
            .replace(Regex("[^A-Za-z0-9_-]+"), "_")
            .trim('_')
            .take(MAX_CAMERA_NAME_LENGTH)
        return cleaned.ifBlank { "camera" }.lowercase(Locale.US)
    }

    private companion object {
        const val MAX_CAMERA_NAME_LENGTH = 36
        const val PHOTO_TIMESTAMP_PATTERN = "yyyyMMdd_HHmmss_SSS"
    }
}
