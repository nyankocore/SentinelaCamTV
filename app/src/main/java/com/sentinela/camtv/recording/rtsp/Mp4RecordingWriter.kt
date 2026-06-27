package com.sentinela.camtv.recording.rtsp

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import java.io.Closeable
import java.io.File
import java.nio.ByteBuffer

class Mp4RecordingWriter(
    outputFile: File,
    private val audioTrack: SdpTrack?,
) : Closeable {
    private val muxer = MediaMuxer(
        outputFile.absolutePath,
        MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
    )
    private var started = false
    private var closed = false
    private var videoTrackIndex = -1
    private var audioTrackIndex = -1
    private var videoBaseTimestamp: Long? = null
    private var audioBaseTimestamp: Long? = null
    private var sps: ByteArray? = null
    private var pps: ByteArray? = null
    private var videoSamples = 0
    private var audioSamples = 0
    private val g711AacEncoder: G711AacEncoder? = audioTrack?.takeIf {
        it.encoding == SdpEncoding.PCMU || it.encoding == SdpEncoding.PCMA
    }?.let { track ->
        runCatching {
            G711AacEncoder(
                sampleRate = track.clockRate,
                channelCount = track.channels.coerceAtLeast(1),
            )
        }.getOrNull()
    }

    val warning: String? = when {
        audioTrack == null -> null
        audioTrack.encoding == SdpEncoding.AAC && audioTrack.aacConfig != null -> null
        (audioTrack.encoding == SdpEncoding.PCMU || audioTrack.encoding == SdpEncoding.PCMA) &&
            g711AacEncoder != null -> null
        audioTrack.encoding == SdpEncoding.PCMU || audioTrack.encoding == SdpEncoding.PCMA ->
            "Áudio G.711 não pôde ser convertido neste aparelho; vídeo gravado sem áudio."
        else -> "Áudio não suportado neste teste; vídeo gravado sem áudio."
    }

    val audioSamplesWritten: Int
        get() = audioSamples

    fun writeVideo(accessUnit: H264AccessUnit): Boolean {
        accessUnit.sps?.let { sps = it }
        accessUnit.pps?.let { pps = it }

        if (!started) {
            if (!accessUnit.isKeyframe || sps == null || pps == null) {
                return false
            }
            startMuxer()
        }

        val sampleNalUnits = if (accessUnit.isKeyframe) {
            listOfNotNull(sps, pps) + accessUnit.nalUnits
                .filterNot { it.nalType() == H264_NAL_SPS || it.nalType() == H264_NAL_PPS }
        } else {
            accessUnit.nalUnits
                .filterNot { it.nalType() == H264_NAL_SPS || it.nalType() == H264_NAL_PPS }
        }
        val sampleData = H264Mp4SampleFormatter.toAnnexB(sampleNalUnits)
        if (sampleData.isEmpty()) return false

        val base = videoBaseTimestamp ?: accessUnit.timestamp.also { videoBaseTimestamp = it }
        val bufferInfo = MediaCodec.BufferInfo().apply {
            set(
                0,
                sampleData.size,
                rtpTimestampToUs(accessUnit.timestamp, base, VIDEO_CLOCK_RATE),
                if (accessUnit.isKeyframe) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0,
            )
        }
        muxer.writeSampleData(videoTrackIndex, ByteBuffer.wrap(sampleData), bufferInfo)
        videoSamples += 1
        return true
    }

    fun writeAac(accessUnit: AacAccessUnit) {
        if (!started || audioTrackIndex < 0) return
        val base = audioBaseTimestamp ?: accessUnit.timestamp.also { audioBaseTimestamp = it }
        val bufferInfo = MediaCodec.BufferInfo().apply {
            set(
                0,
                accessUnit.payload.size,
                rtpTimestampToUs(accessUnit.timestamp, base, audioTrack?.clockRate ?: AAC_DEFAULT_CLOCK_RATE),
                0,
            )
        }
        muxer.writeSampleData(audioTrackIndex, ByteBuffer.wrap(accessUnit.payload), bufferInfo)
        audioSamples += 1
    }

    fun writePcmAudio(timestamp: Long, pcm16Le: ByteArray) {
        val encoder = g711AacEncoder ?: return
        if (!started || audioTrackIndex < 0) return
        val base = audioBaseTimestamp ?: timestamp.also { audioBaseTimestamp = it }
        val presentationTimeUs = rtpTimestampToUs(timestamp, base, audioTrack?.clockRate ?: AAC_DEFAULT_CLOCK_RATE)
        encoder.encode(pcm16Le, presentationTimeUs) { encoded, bufferInfo ->
            muxer.writeSampleData(audioTrackIndex, encoded, bufferInfo)
            audioSamples += 1
        }
    }

    fun hasWrittenSamples(): Boolean = videoSamples > 0

    fun finish() {
        if (closed) return
        runCatching {
            if (started) {
                if (audioTrackIndex >= 0) {
                    g711AacEncoder?.finish { encoded, bufferInfo ->
                        muxer.writeSampleData(audioTrackIndex, encoded, bufferInfo)
                        audioSamples += 1
                    }
                }
                muxer.stop()
            }
        }
        close()
    }

    override fun close() {
        if (closed) return
        closed = true
        g711AacEncoder?.close()
        runCatching { muxer.release() }
    }

    private fun startMuxer() {
        val currentSps = sps ?: error("SPS ausente")
        val currentPps = pps ?: error("PPS ausente")
        val videoSize = H264SpsParser.parseSize(currentSps) ?: DEFAULT_VIDEO_SIZE
        val videoFormat = MediaFormat.createVideoFormat(
            MediaFormat.MIMETYPE_VIDEO_AVC,
            videoSize.width,
            videoSize.height,
        ).apply {
            setByteBuffer("csd-0", ByteBuffer.wrap(H264Mp4SampleFormatter.withStartCode(currentSps)))
            setByteBuffer("csd-1", ByteBuffer.wrap(H264Mp4SampleFormatter.withStartCode(currentPps)))
        }
        videoTrackIndex = muxer.addTrack(videoFormat)

        val currentAudioTrack = audioTrack
        if (
            currentAudioTrack?.encoding == SdpEncoding.AAC &&
            currentAudioTrack.aacConfig != null
        ) {
            val audioFormat = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_AAC,
                currentAudioTrack.clockRate,
                currentAudioTrack.channels,
            ).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setByteBuffer("csd-0", ByteBuffer.wrap(currentAudioTrack.aacConfig))
            }
            audioTrackIndex = muxer.addTrack(audioFormat)
        } else if (
            currentAudioTrack != null &&
            (currentAudioTrack.encoding == SdpEncoding.PCMU || currentAudioTrack.encoding == SdpEncoding.PCMA) &&
            g711AacEncoder != null
        ) {
            audioTrackIndex = muxer.addTrack(
                createAacAudioFormat(
                    sampleRate = currentAudioTrack.clockRate,
                    channelCount = currentAudioTrack.channels.coerceAtLeast(1),
                ),
            )
        }

        muxer.start()
        g711AacEncoder?.start()
        started = true
    }

    private fun createAacAudioFormat(sampleRate: Int, channelCount: Int): MediaFormat =
        MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            sampleRate,
            channelCount,
        ).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, AAC_G711_BITRATE)
            createAacLcConfig(sampleRate, channelCount)?.let {
                setByteBuffer("csd-0", ByteBuffer.wrap(it))
            }
        }

    private fun rtpTimestampToUs(timestamp: Long, baseTimestamp: Long, clockRate: Int): Long {
        val delta = (timestamp - baseTimestamp).and(RTP_TIMESTAMP_MASK)
        return (delta * 1_000_000L) / clockRate.coerceAtLeast(1)
    }

    private fun createAacLcConfig(sampleRate: Int, channelCount: Int): ByteArray? {
        val sampleRateIndex = AAC_SAMPLE_RATES.indexOf(sampleRate)
        if (sampleRateIndex < 0) return null
        val audioObjectType = 2
        val config = (audioObjectType shl 11) or (sampleRateIndex shl 7) or
            (channelCount.coerceIn(1, 7) shl 3)
        return byteArrayOf(((config ushr 8) and 0xFF).toByte(), (config and 0xFF).toByte())
    }

    private companion object {
        const val VIDEO_CLOCK_RATE = 90_000
        const val AAC_DEFAULT_CLOCK_RATE = 8_000
        const val AAC_G711_BITRATE = 32_000
        const val RTP_TIMESTAMP_MASK = 0xFFFF_FFFFL
        val AAC_SAMPLE_RATES = listOf(96_000, 88_200, 64_000, 48_000, 44_100, 32_000, 24_000, 22_050, 16_000, 12_000, 11_025, 8_000)
        val DEFAULT_VIDEO_SIZE = H264VideoSize(width = 1920, height = 1080)
    }
}

private class G711AacEncoder(
    private val sampleRate: Int,
    private val channelCount: Int,
) : Closeable {
    private val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
    private var started = false

    init {
        codec.configure(
            MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, 32_000)
            },
            null,
            null,
            MediaCodec.CONFIGURE_FLAG_ENCODE,
        )
    }

    fun start() {
        if (started) return
        codec.start()
        started = true
    }

    fun encode(
        pcm16Le: ByteArray,
        presentationTimeUs: Long,
        writeSample: (ByteBuffer, MediaCodec.BufferInfo) -> Unit,
    ) {
        if (!started) return
        val inputIndex = codec.dequeueInputBuffer(CODEC_TIMEOUT_US)
        if (inputIndex >= 0) {
            val inputBuffer = codec.getInputBuffer(inputIndex)
            inputBuffer?.clear()
            val size = minOf(inputBuffer?.remaining() ?: 0, pcm16Le.size)
            inputBuffer?.put(pcm16Le, 0, size)
            codec.queueInputBuffer(inputIndex, 0, size, presentationTimeUs, 0)
        }
        drain(writeSample)
    }

    fun finish(writeSample: (ByteBuffer, MediaCodec.BufferInfo) -> Unit) {
        if (!started) return
        val inputIndex = codec.dequeueInputBuffer(CODEC_TIMEOUT_US)
        if (inputIndex >= 0) {
            codec.queueInputBuffer(
                inputIndex,
                0,
                0,
                0L,
                MediaCodec.BUFFER_FLAG_END_OF_STREAM,
            )
        }
        drain(writeSample, waitForEndOfStream = true)
    }

    override fun close() {
        runCatching {
            if (started && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                codec.stop()
            }
        }
        runCatching { codec.release() }
        started = false
    }

    private fun drain(
        writeSample: (ByteBuffer, MediaCodec.BufferInfo) -> Unit,
        waitForEndOfStream: Boolean = false,
    ) {
        val bufferInfo = MediaCodec.BufferInfo()
        var tryAgainCount = 0
        while (true) {
            val timeoutUs = if (waitForEndOfStream) CODEC_TIMEOUT_US else 0L
            when (val outputIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)) {
                MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!waitForEndOfStream || tryAgainCount++ >= END_OF_STREAM_DRAIN_ATTEMPTS) {
                        return
                    }
                }
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Unit
                in 0..Int.MAX_VALUE -> {
                    val outputBuffer = codec.getOutputBuffer(outputIndex)
                    if (
                        outputBuffer != null &&
                        bufferInfo.size > 0 &&
                        (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0
                    ) {
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        val sample = ByteBuffer.allocate(bufferInfo.size)
                        sample.put(outputBuffer)
                        sample.flip()
                        val sampleInfo = MediaCodec.BufferInfo().apply {
                            set(0, bufferInfo.size, bufferInfo.presentationTimeUs, bufferInfo.flags)
                        }
                        writeSample(sample, sampleInfo)
                    }
                    val reachedEndOfStream =
                        (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                    codec.releaseOutputBuffer(outputIndex, false)
                    if (reachedEndOfStream) return
                }
                else -> return
            }
        }
    }

    private companion object {
        const val CODEC_TIMEOUT_US = 10_000L
        const val END_OF_STREAM_DRAIN_ATTEMPTS = 8
    }
}
