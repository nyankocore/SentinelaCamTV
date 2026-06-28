package com.sentinela.camtv.recording

import android.net.Uri
import com.sentinela.camtv.R
import com.sentinela.camtv.ui.text.UiText

data class RecordingProbeRequest(
    val cameraName: String,
    val rtspUrl: String,
    val maxDurationMs: Long = RECORDING_UNLIMITED_DURATION_MS,
)

sealed interface RecordingProbeResult {
    data class Success(
        val fileName: String,
        val locationLabel: String,
        val uri: Uri?,
        val warning: String? = null,
    ) : RecordingProbeResult

    data class Failure(
        val error: RecordingProbeError,
    ) : RecordingProbeResult
}

enum class RecordingProbeError {
    UnsupportedAndroid,
    UnsupportedRtspSource,
    UnsupportedVideoCodec,
    NoVideoTrack,
    NoSamples,
    WriteFailed,
}

fun RecordingProbeResult.userMessage(): UiText = when (this) {
    is RecordingProbeResult.Success -> warning?.let {
        UiText.Resource(R.string.recording_saved_with_warning, listOf(locationLabel, it))
    } ?: UiText.Resource(R.string.recording_saved, listOf(locationLabel))
    is RecordingProbeResult.Failure -> error.userMessage()
}

fun RecordingProbeError.userMessage(): UiText = when (this) {
    RecordingProbeError.UnsupportedAndroid -> UiText.Resource(R.string.recording_unsupported_android)
    RecordingProbeError.UnsupportedRtspSource -> UiText.Resource(R.string.recording_unsupported_rtsp)
    RecordingProbeError.UnsupportedVideoCodec -> UiText.Resource(R.string.recording_unsupported_video_codec)
    RecordingProbeError.NoVideoTrack -> UiText.Resource(R.string.recording_no_video_track)
    RecordingProbeError.NoSamples,
    RecordingProbeError.WriteFailed,
    -> UiText.Resource(R.string.recording_failed)
}

class RecordingStopSignal {
    @Volatile
    private var stopped = false

    fun stop() {
        stopped = true
    }

    fun isStopped(): Boolean = stopped
}

object RecordingLocationLabels {
    const val STANDARD_VIDEOS = "Vídeos/Sentinela Cam TV"
    const val APP_EXTERNAL_VIDEOS = "Vídeos do app/Sentinela Cam TV"
}

const val RECORDING_WITHOUT_AUDIO_WARNING = "Vídeo gravado sem áudio: áudio não encontrado ou não suportado."
const val RECORDING_UNLIMITED_DURATION_MS = Long.MAX_VALUE
