package com.sentinela.camtv.player

enum class PlayerMode {
    Mosaic,
    Fullscreen,
}

enum class AudioMode {
    Disabled,
    Enabled,
}

enum class RtspTransportMode {
    UdpFirst,
    TcpOnly,
}

enum class TransmissionMode {
    MENOR_LATENCIA,
    QUALIDADE,
}

data class PlayerBufferPreset(
    val minBufferMs: Int,
    val maxBufferMs: Int,
    val bufferForPlaybackMs: Int,
    val bufferAfterRebufferMs: Int,
) {
    init {
        require(maxBufferMs >= minBufferMs) {
            "maxBufferMs must be greater than or equal to minBufferMs"
        }
        require(minBufferMs >= bufferForPlaybackMs) {
            "minBufferMs must be greater than or equal to bufferForPlaybackMs"
        }
        require(minBufferMs >= bufferAfterRebufferMs) {
            "minBufferMs must be greater than or equal to bufferAfterRebufferMs"
        }
    }
}

object PlayerBufferPresets {
    val LowLatency = PlayerBufferPreset(
        minBufferMs = 75,
        maxBufferMs = 125,
        bufferForPlaybackMs = 50,
        bufferAfterRebufferMs = 75,
    )

    val Quality = PlayerBufferPreset(
        minBufferMs = 750,
        maxBufferMs = 2_000,
        bufferForPlaybackMs = 350,
        bufferAfterRebufferMs = 750,
    )
}

data class PlayerStreamConfig(
    val mode: PlayerMode,
    val audioMode: AudioMode,
    val bufferPreset: PlayerBufferPreset,
    val rtspTimeoutMs: Long,
    val transportMode: RtspTransportMode,
    val transmissionMode: TransmissionMode,
    val enableDecoderFallback: Boolean = false,
)

fun defaultPlayerStreamConfig(
    mode: PlayerMode,
    audioMode: AudioMode,
    transmissionMode: TransmissionMode = TransmissionMode.MENOR_LATENCIA,
    transportMode: RtspTransportMode = transmissionMode.defaultTransportMode(),
    enableDecoderFallback: Boolean = false,
): PlayerStreamConfig = PlayerStreamConfig(
    mode = mode,
    audioMode = audioMode,
    bufferPreset = transmissionMode.bufferPreset(),
    rtspTimeoutMs = transmissionMode.rtspTimeoutMs(),
    transportMode = transportMode,
    transmissionMode = transmissionMode,
    enableDecoderFallback = enableDecoderFallback,
)

fun TransmissionMode.defaultTransportMode(): RtspTransportMode = when (this) {
    TransmissionMode.MENOR_LATENCIA -> RtspTransportMode.UdpFirst
    TransmissionMode.QUALIDADE -> RtspTransportMode.TcpOnly
}

fun TransmissionMode.bufferPreset(): PlayerBufferPreset = when (this) {
    TransmissionMode.MENOR_LATENCIA -> PlayerBufferPresets.LowLatency
    TransmissionMode.QUALIDADE -> PlayerBufferPresets.Quality
}

fun TransmissionMode.rtspTimeoutMs(): Long = when (this) {
    TransmissionMode.MENOR_LATENCIA -> 3_000L
    TransmissionMode.QUALIDADE -> 3_000L
}

fun TransmissionMode.next(): TransmissionMode = when (this) {
    TransmissionMode.MENOR_LATENCIA -> TransmissionMode.QUALIDADE
    TransmissionMode.QUALIDADE -> TransmissionMode.MENOR_LATENCIA
}
