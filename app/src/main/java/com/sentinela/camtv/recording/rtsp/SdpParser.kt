package com.sentinela.camtv.recording.rtsp

import java.util.Locale

data class SdpSession(
    val sessionControl: String?,
    val tracks: List<SdpTrack>,
) {
    val videoTrack: SdpTrack? =
        tracks.firstOrNull { it.kind == SdpTrackKind.Video && it.encoding.isH264 }

    val audioTrack: SdpTrack? =
        tracks.firstOrNull { it.kind == SdpTrackKind.Audio && it.encoding.isSupportedAudio }
}

data class SdpTrack(
    val kind: SdpTrackKind,
    val payloadType: Int,
    val encoding: SdpEncoding,
    val clockRate: Int,
    val channels: Int,
    val control: String?,
    val fmtp: Map<String, String>,
) {
    val sps: ByteArray? = fmtp["sprop-parameter-sets"]
        ?.split(',')
        ?.firstOrNull()
        ?.decodeBase64OrNull()

    val pps: ByteArray? = fmtp["sprop-parameter-sets"]
        ?.split(',')
        ?.getOrNull(1)
        ?.decodeBase64OrNull()

    val aacConfig: ByteArray? = fmtp["config"]?.hexToBytesOrNull()

    val aacSizeLength: Int = fmtp["sizelength"]?.toIntOrNull() ?: 13
    val aacIndexLength: Int = fmtp["indexlength"]?.toIntOrNull() ?: 3
    val aacIndexDeltaLength: Int = fmtp["indexdeltalength"]?.toIntOrNull() ?: 3
}

enum class SdpTrackKind {
    Video,
    Audio,
    Other,
}

enum class SdpEncoding {
    H264,
    H264B,
    H264H,
    AAC,
    PCMU,
    PCMA,
    Unknown;

    val isH264: Boolean
        get() = this == H264 || this == H264B || this == H264H

    val isSupportedAudio: Boolean
        get() = this == AAC || this == PCMU || this == PCMA
}

object SdpParser {
    fun parse(rawSdp: String): SdpSession {
        val lines = rawSdp
            .replace("\r\n", "\n")
            .split('\n')
            .map { it.trim() }
            .filter { it.isNotBlank() }

        var sessionControl: String? = null
        val tracks = mutableListOf<MutableSdpTrack>()
        var current: MutableSdpTrack? = null

        lines.forEach { line ->
            when {
                line.startsWith("m=", ignoreCase = true) -> {
                    current = parseMediaLine(line)?.also { tracks += it }
                }
                line.startsWith("a=control:", ignoreCase = true) -> {
                    val control = line.substringAfter(':').trim()
                    if (current == null) {
                        sessionControl = control
                    } else {
                        current?.control = control
                    }
                }
                line.startsWith("a=rtpmap:", ignoreCase = true) -> {
                    parseRtpMap(line)?.let { rtpMap ->
                        tracks.firstOrNull { it.payloadType == rtpMap.payloadType }?.apply {
                            encoding = rtpMap.encoding
                            clockRate = rtpMap.clockRate
                            channels = rtpMap.channels
                        }
                    }
                }
                line.startsWith("a=fmtp:", ignoreCase = true) -> {
                    parseFmtp(line)?.let { fmtp ->
                        tracks.firstOrNull { it.payloadType == fmtp.payloadType }
                            ?.fmtp
                            ?.putAll(fmtp.parameters)
                    }
                }
            }
        }

        return SdpSession(
            sessionControl = sessionControl,
            tracks = tracks.map {
                SdpTrack(
                    kind = it.kind,
                    payloadType = it.payloadType,
                    encoding = it.encoding,
                    clockRate = it.clockRate,
                    channels = it.channels,
                    control = it.control,
                    fmtp = it.fmtp.toMap(),
                )
            },
        )
    }

    private fun parseMediaLine(line: String): MutableSdpTrack? {
        val parts = line.substringAfter("m=").trim().split(Regex("\\s+"))
        val kind = when (parts.getOrNull(0)?.lowercase(Locale.US)) {
            "video" -> SdpTrackKind.Video
            "audio" -> SdpTrackKind.Audio
            else -> SdpTrackKind.Other
        }
        val payloadType = parts.lastOrNull()?.toIntOrNull() ?: return null
        return MutableSdpTrack(
            kind = kind,
            payloadType = payloadType,
        )
    }

    private fun parseRtpMap(line: String): RtpMap? {
        val value = line.substringAfter("a=rtpmap:", "").trim()
        val payloadType = value.substringBefore(' ').toIntOrNull() ?: return null
        val encodingParts = value.substringAfter(' ', "").split('/')
        val encoding = when (encodingParts.getOrNull(0)?.uppercase(Locale.US)) {
            "H264" -> SdpEncoding.H264
            "H264B" -> SdpEncoding.H264B
            "H264H" -> SdpEncoding.H264H
            "MPEG4-GENERIC" -> SdpEncoding.AAC
            "PCMU" -> SdpEncoding.PCMU
            "PCMA" -> SdpEncoding.PCMA
            else -> SdpEncoding.Unknown
        }
        return RtpMap(
            payloadType = payloadType,
            encoding = encoding,
            clockRate = encodingParts.getOrNull(1)?.toIntOrNull() ?: DEFAULT_VIDEO_CLOCK,
            channels = encodingParts.getOrNull(2)?.toIntOrNull() ?: 1,
        )
    }

    private fun parseFmtp(line: String): Fmtp? {
        val value = line.substringAfter("a=fmtp:", "").trim()
        val payloadType = value.substringBefore(' ').toIntOrNull() ?: return null
        val rawParameters = value.substringAfter(' ', "")
        val parameters = rawParameters
            .split(';')
            .mapNotNull { parameter ->
                val key = parameter.substringBefore('=', "").trim().lowercase(Locale.US)
                val raw = parameter.substringAfter('=', "").trim()
                if (key.isBlank()) null else key to raw
            }
            .toMap()
        return Fmtp(payloadType, parameters)
    }

    private data class MutableSdpTrack(
        val kind: SdpTrackKind,
        val payloadType: Int,
        var encoding: SdpEncoding = SdpEncoding.Unknown,
        var clockRate: Int = DEFAULT_VIDEO_CLOCK,
        var channels: Int = 1,
        var control: String? = null,
        val fmtp: MutableMap<String, String> = linkedMapOf(),
    )

    private data class RtpMap(
        val payloadType: Int,
        val encoding: SdpEncoding,
        val clockRate: Int,
        val channels: Int,
    )

    private data class Fmtp(
        val payloadType: Int,
        val parameters: Map<String, String>,
    )

    private const val DEFAULT_VIDEO_CLOCK = 90_000
}

fun SdpTrack.resolveControlUris(baseUri: String): List<String> {
    val controlValue = control?.takeIf { it.isNotBlank() } ?: return listOf(baseUri)
    if (controlValue.equals("*", ignoreCase = true)) {
        return listOf(baseUri)
    }
    if (controlValue.startsWith("rtsp://", ignoreCase = true)) {
        return listOf(controlValue)
    }

    val query = baseUri.substringAfter('?', missingDelimiterValue = "")
    val baseWithoutQuery = baseUri.substringBefore('?').trimEnd('/')
    return buildList {
        add("$baseWithoutQuery/$controlValue${if (query.isBlank()) "" else "?$query"}")
        add("${baseUri.trimEnd('/')}/$controlValue")
    }.distinct()
}

private fun String.decodeBase64OrNull(): ByteArray? =
    runCatching { RtspBase64.decode(this) }.getOrNull()

private fun String.hexToBytesOrNull(): ByteArray? {
    val clean = trim()
    if (clean.length % 2 != 0) return null
    return runCatching {
        ByteArray(clean.length / 2) { index ->
            clean.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
    }.getOrNull()
}
