package com.sentinela.camtv.recording.rtsp

import com.sentinela.camtv.diagnostics.DiagnosticsSanitizer
import com.sentinela.camtv.recording.RecordingProbeError
import com.sentinela.camtv.recording.RecordingStopSignal
import com.sentinela.camtv.recording.RECORDING_WITHOUT_AUDIO_WARNING
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.coroutines.coroutineContext

class RtspRecordingEngine(
    private val timeoutMs: Int = DEFAULT_TIMEOUT_MS,
) {
    suspend fun record(
        rtspUrl: String,
        outputFile: File,
        maxDurationMs: Long,
        stopSignal: RecordingStopSignal,
    ): RtspRecordingResult {
        val endpoint = RtspEndpoint.parse(rtspUrl)
            ?: return RtspRecordingResult.Failure(RecordingProbeError.UnsupportedRtspSource)
        val client = RtspTcpClient(endpoint, timeoutMs)
        return client.use {
            client.record(outputFile, maxDurationMs, stopSignal)
        }
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 3_000
    }
}

sealed interface RtspRecordingResult {
    data class Success(val warning: String?) : RtspRecordingResult
    data class Failure(val error: RecordingProbeError) : RtspRecordingResult
}

private class RtspTcpClient(
    private val endpoint: RtspEndpoint,
    private val timeoutMs: Int,
) : AutoCloseable {
    private val authenticator = RtspAuthenticator()
    private var socket: Socket? = null
    private var input: BufferedInputStream? = null
    private var output: OutputStream? = null
    private var cseq = 1
    private var sessionHeader: String? = null
    private var challenge: RtspAuthChallenge? = null

    suspend fun record(
        outputFile: File,
        maxDurationMs: Long,
        stopSignal: RecordingStopSignal,
    ): RtspRecordingResult {
        return try {
            connect()
            request("OPTIONS", endpoint.requestUri)
            val describe = request(
                method = "DESCRIBE",
                uri = endpoint.requestUri,
                extraHeaders = mapOf("Accept" to "application/sdp"),
            )
            if (!describe.isSuccess()) {
                return RtspRecordingResult.Failure(RecordingProbeError.UnsupportedRtspSource)
            }

            val sdp = SdpParser.parse(String(describe.body, Charsets.UTF_8))
            val videoTrack = sdp.videoTrack
                ?: return RtspRecordingResult.Failure(RecordingProbeError.UnsupportedVideoCodec)
            val audioTrack = sdp.audioTrack
            val baseUri = describe.header("Content-Base")
                ?: describe.header("Content-Location")
                ?: endpoint.requestUri
            val trackChannels = setupTracks(baseUri, videoTrack, audioTrack)
            if (trackChannels.videoChannel < 0) {
                return RtspRecordingResult.Failure(RecordingProbeError.NoVideoTrack)
            }
            Timber.tag(LOG_TAG).i(
                "gravacao RTSP: audio=${audioTrack?.encoding ?: "nenhum"} " +
                    "audioChannel=${trackChannels.audioChannel} " +
                    "aacConfig=${audioTrack?.aacConfig != null}",
            )

            request("PLAY", endpoint.requestUri)
            receiveAndWrite(
                outputFile = outputFile,
                maxDurationMs = maxDurationMs,
                stopSignal = stopSignal,
                videoTrack = videoTrack,
                audioTrack = audioTrack,
                trackChannels = trackChannels,
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (throwable: Throwable) {
            Timber.tag(LOG_TAG).w(
                throwable,
                "falha no nucleo RTSP: ${DiagnosticsSanitizer.sanitize(throwable.message.orEmpty())}",
            )
            RtspRecordingResult.Failure(RecordingProbeError.UnsupportedRtspSource)
        } finally {
            runCatching { request("TEARDOWN", endpoint.requestUri) }
        }
    }

    private fun connect() {
        val newSocket = Socket()
        newSocket.connect(InetSocketAddress(endpoint.host, endpoint.port), timeoutMs)
        newSocket.soTimeout = timeoutMs
        socket = newSocket
        input = BufferedInputStream(newSocket.getInputStream())
        output = newSocket.getOutputStream()
    }

    private fun setupTracks(
        baseUri: String,
        videoTrack: SdpTrack,
        audioTrack: SdpTrack?,
    ): TrackChannels {
        var nextChannel = 0
        val videoChannel = setupTrack(baseUri, videoTrack, nextChannel)
        nextChannel += CHANNELS_PER_TRACK
        val audioChannel = audioTrack?.let { setupTrack(baseUri, it, nextChannel) } ?: -1
        return TrackChannels(videoChannel = videoChannel, audioChannel = audioChannel)
    }

    private fun setupTrack(baseUri: String, track: SdpTrack, rtpChannel: Int): Int {
        for (controlUri in track.resolveControlUris(baseUri)) {
            val response = runCatching {
                request(
                    method = "SETUP",
                    uri = controlUri,
                    extraHeaders = mapOf(
                        "Transport" to "RTP/AVP/TCP;unicast;interleaved=$rtpChannel-${rtpChannel + 1}",
                    ),
                )
            }.getOrNull()
            if (response?.isSuccess() == true) {
                return parseInterleavedRtpChannel(response.header("Transport")) ?: rtpChannel
            }
        }
        return -1
    }

    private suspend fun receiveAndWrite(
        outputFile: File,
        maxDurationMs: Long,
        stopSignal: RecordingStopSignal,
        videoTrack: SdpTrack,
        audioTrack: SdpTrack?,
        trackChannels: TrackChannels,
    ): RtspRecordingResult {
        val currentInput = input ?: return RtspRecordingResult.Failure(RecordingProbeError.UnsupportedRtspSource)
        val h264Assembler = H264RtpAssembler(videoTrack.sps, videoTrack.pps)
        val activeAudioTrack = audioTrack.takeIf { trackChannels.audioChannel >= 0 }
        val aacAssembler = activeAudioTrack
            ?.takeIf { it.encoding == SdpEncoding.AAC }
            ?.let { AacRtpAssembler(it.aacSizeLength, it.aacIndexLength, it.aacIndexDeltaLength) }
        val writer = Mp4RecordingWriter(outputFile, activeAudioTrack)
        val startedAt = android.os.SystemClock.elapsedRealtime()
        var audioPackets = 0
        var audioAccessUnits = 0

        return try {
            while (
                shouldContinueRtspRecording(
                    stopRequested = stopSignal.isStopped(),
                    elapsedMs = android.os.SystemClock.elapsedRealtime() - startedAt,
                    maxDurationMs = maxDurationMs,
                )
            ) {
                coroutineContext.ensureActive()
                when (val frame = readNextFrame(currentInput) ?: continue) {
                    is InterleavedFrame.Rtp -> {
                        val packet = RtpPacketParser.parse(frame.payload) ?: continue
                        when (frame.channel) {
                            trackChannels.videoChannel -> {
                                h264Assembler.consume(packet)?.let { writer.writeVideo(it) }
                            }
                            trackChannels.audioChannel -> {
                                audioPackets += 1
                                if (aacAssembler != null) {
                                    aacAssembler.consume(packet).forEach { accessUnit ->
                                        audioAccessUnits += 1
                                        writer.writeAac(accessUnit)
                                    }
                                } else if (activeAudioTrack?.encoding == SdpEncoding.PCMU) {
                                    audioAccessUnits += 1
                                    writer.writePcmAudio(packet.timestamp, G711Codec.decodePcmu(packet.payload))
                                } else if (activeAudioTrack?.encoding == SdpEncoding.PCMA) {
                                    audioAccessUnits += 1
                                    writer.writePcmAudio(packet.timestamp, G711Codec.decodePcma(packet.payload))
                                }
                            }
                        }
                    }
                    InterleavedFrame.RtspMessage -> Unit
                }
            }

            if (!writer.hasWrittenSamples()) {
                writer.close()
                RtspRecordingResult.Failure(RecordingProbeError.NoSamples)
            } else {
                writer.finish()
                val warning = recordingAudioWarning(
                    audioSamplesWritten = writer.audioSamplesWritten,
                    writerWarning = writer.warning,
                )
                Timber.tag(LOG_TAG).i(
                    "gravacao RTSP finalizada: audio=${activeAudioTrack?.encoding ?: "nenhum"} " +
                        "pacotesAudio=$audioPackets unidadesAudio=$audioAccessUnits " +
                        "samplesAudio=${writer.audioSamplesWritten} avisoAudio=${warning != null}",
                )
                RtspRecordingResult.Success(warning = warning)
            }
        } catch (exception: CancellationException) {
            writer.close()
            throw exception
        } catch (throwable: Throwable) {
            writer.close()
            Timber.tag(LOG_TAG).w(
                throwable,
                "erro gravando MP4 via RTSP proprio: ${DiagnosticsSanitizer.sanitize(throwable.message.orEmpty())}",
            )
            RtspRecordingResult.Failure(RecordingProbeError.WriteFailed)
        }
    }

    private fun request(
        method: String,
        uri: String,
        extraHeaders: Map<String, String> = emptyMap(),
    ): RtspResponse {
        val response = sendRequest(method, uri, extraHeaders, authorize = true)
        if (response.statusCode == 401 && endpoint.hasCredentials) {
            challenge = RtspAuthChallengeParser.parse(response.header("WWW-Authenticate"))
            return sendRequest(method, uri, extraHeaders, authorize = true)
        }
        return response
    }

    private fun sendRequest(
        method: String,
        uri: String,
        extraHeaders: Map<String, String>,
        authorize: Boolean,
    ): RtspResponse {
        val currentOutput = output ?: error("RTSP sem output")
        val headers = linkedMapOf(
            "CSeq" to (cseq++).toString(),
            "User-Agent" to "SentinelaCamTV-Recorder",
        )
        sessionHeader?.let { headers["Session"] = it }
        headers.putAll(extraHeaders)
        if (authorize) {
            authenticator.authorizationHeader(method, uri, endpoint, challenge)
                ?.let { headers["Authorization"] = it }
        }

        val rawRequest = buildString {
            append(method).append(' ').append(uri).append(" RTSP/1.0\r\n")
            headers.forEach { (name, value) -> append(name).append(": ").append(value).append("\r\n") }
            append("\r\n")
        }
        currentOutput.write(rawRequest.toByteArray(Charsets.ISO_8859_1))
        currentOutput.flush()

        val response = readRtspResponse(input ?: error("RTSP sem input"))
        response.header("Session")
            ?.substringBefore(';')
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { sessionHeader = it }
        return response
    }

    private fun readRtspResponse(input: InputStream, firstByte: Int? = null): RtspResponse {
        val headerBytes = ByteArrayOutputStream()
        var current: Int
        var recent = ""
        if (firstByte != null) {
            headerBytes.write(firstByte)
            recent += firstByte.toChar()
        }
        while (true) {
            current = input.read()
            if (current < 0) error("RTSP fechado")
            headerBytes.write(current)
            recent += current.toChar()
            if (recent.length > 4) recent = recent.takeLast(4)
            if (recent == "\r\n\r\n") break
        }
        val headerText = String(headerBytes.toByteArray(), Charsets.ISO_8859_1)
        val contentLength = headerText
            .lineSequence()
            .firstOrNull { it.startsWith("Content-Length:", ignoreCase = true) }
            ?.substringAfter(':')
            ?.trim()
            ?.toIntOrNull()
            ?: 0
        val body = ByteArray(contentLength)
        var read = 0
        while (read < contentLength) {
            val count = input.read(body, read, contentLength - read)
            if (count < 0) error("RTSP body fechado")
            read += count
        }
        return RtspResponseParser.parse(headerBytes.toByteArray() + body)
            ?: error("resposta RTSP invalida")
    }

    private fun readNextFrame(input: InputStream): InterleavedFrame? {
        val firstByte = input.read()
        if (firstByte < 0) return null
        if (firstByte == INTERLEAVED_MAGIC) {
            val channel = input.read()
            val sizeHigh = input.read()
            val sizeLow = input.read()
            if (channel < 0 || sizeHigh < 0 || sizeLow < 0) return null
            val length = (sizeHigh shl 8) or sizeLow
            val payload = ByteArray(length)
            var read = 0
            while (read < length) {
                val count = input.read(payload, read, length - read)
                if (count < 0) return null
                read += count
            }
            return InterleavedFrame.Rtp(channel = channel, payload = payload)
        }

        // Câmeras podem enviar mensagens RTSP assíncronas no mesmo socket.
        if (firstByte == 'R'.code) {
            readRtspResponse(input, firstByte)
            return InterleavedFrame.RtspMessage
        }
        return null
    }

    override fun close() {
        runCatching { socket?.close() }
        socket = null
        input = null
        output = null
    }

    private data class TrackChannels(
        val videoChannel: Int,
        val audioChannel: Int,
    )

    private sealed interface InterleavedFrame {
        data class Rtp(val channel: Int, val payload: ByteArray) : InterleavedFrame
        data object RtspMessage : InterleavedFrame
    }

    private companion object {
        const val LOG_TAG = "SentinelaRecording"
        const val INTERLEAVED_MAGIC = '$'.code
        const val CHANNELS_PER_TRACK = 2
    }
}

internal fun shouldContinueRtspRecording(
    stopRequested: Boolean,
    elapsedMs: Long,
    maxDurationMs: Long,
): Boolean =
    !stopRequested && (maxDurationMs == Long.MAX_VALUE || elapsedMs < maxDurationMs)

internal fun recordingAudioWarning(
    audioSamplesWritten: Int,
    writerWarning: String?,
): String? =
    if (audioSamplesWritten > 0) writerWarning else RECORDING_WITHOUT_AUDIO_WARNING

internal fun parseInterleavedRtpChannel(transportHeader: String?): Int? {
    val value = transportHeader ?: return null
    val interleaved = value
        .split(';')
        .firstOrNull { it.trim().startsWith("interleaved=", ignoreCase = true) }
        ?.substringAfter('=', "")
        ?.trim()
        ?: return null
    return interleaved
        .substringBefore('-')
        .trim()
        .toIntOrNull()
        ?.takeIf { it >= 0 }
}
