package com.sentinela.camtv.recording

import android.net.Uri

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

fun RecordingProbeResult.userMessage(): String = when (this) {
    is RecordingProbeResult.Success -> warning?.let { "Gravação salva: $locationLabel. $it" }
        ?: "Gravação salva: $locationLabel"
    is RecordingProbeResult.Failure -> error.userMessage()
}

fun RecordingProbeError.userMessage(): String = when (this) {
    RecordingProbeError.UnsupportedAndroid -> "Gravação indisponível neste Android."
    RecordingProbeError.UnsupportedRtspSource -> "Gravação não suportada neste aparelho."
    RecordingProbeError.UnsupportedVideoCodec -> "Este teste grava apenas vídeo H.264."
    RecordingProbeError.NoVideoTrack -> "Não foi possível encontrar vídeo neste stream."
    RecordingProbeError.NoSamples,
    RecordingProbeError.WriteFailed,
    -> "Falha ao gravar vídeo."
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
