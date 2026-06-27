package com.sentinela.camtv.recording.rtsp

import com.sentinela.camtv.recording.RECORDING_WITHOUT_AUDIO_WARNING
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class RtspCoreTest {
    @Test
    fun endpointRemovesCredentialsFromRequestUri() {
        val endpoint = RtspEndpoint.parse(
            "rtsp://user:pass" +
                "@198.51.100.10:554/cam/realmonitor?channel=1&subtype=0",
        )

        assertNotNull(endpoint)
        assertEquals("198.51.100.10", endpoint?.host)
        assertEquals("user", endpoint?.username)
        assertEquals("pass", endpoint?.password)
        assertEquals(
            "rtsp://198.51.100.10/cam/realmonitor?channel=1&subtype=0",
            endpoint?.requestUri,
        )
    }

    @Test
    fun basicAuthUsesUserAndPassword() {
        val endpoint = RtspEndpoint.parse("rtsp://user:pass" + "@198.51.100.11/live")!!
        val header = RtspAuthenticator().authorizationHeader(
            method = "DESCRIBE",
            requestUri = endpoint.requestUri,
            endpoint = endpoint,
            challenge = RtspAuthChallenge.Basic(realm = "Camera"),
        )

        assertEquals(
            "Basic ${Base64.getEncoder().encodeToString("user:pass".toByteArray(Charsets.ISO_8859_1))}",
            header,
        )
    }

    @Test
    fun digestAuthMatchesRfcSample() {
        val endpoint = RtspEndpoint(
            requestUri = "/dir/index.html",
            host = "example.com",
            port = 554,
            username = "Mufasa",
            password = "Circle Of Life",
        )
        val authenticator = RtspAuthenticator(cnonceFactory = { "0a4f113b" })

        val header = authenticator.authorizationHeader(
            method = "GET",
            requestUri = endpoint.requestUri,
            endpoint = endpoint,
            challenge = RtspAuthChallenge.Digest(
                realm = "testrealm@host.com",
                nonce = "dcd98b7102dd2f0e8b11d0f600bfb0c093",
                qop = "auth",
                algorithm = "MD5",
                opaque = null,
            ),
        ).orEmpty()

        assertTrue(header.contains("response=\"6629fae49393a05397450978507c4ef1\""))
    }

    @Test
    fun responseParserKeepsHeadersAndBody() {
        val raw = (
            "RTSP/1.0 200 OK\r\n" +
                "CSeq: 2\r\n" +
                "Content-Length: 4\r\n" +
                "\r\n" +
                "body"
            ).toByteArray(Charsets.ISO_8859_1)

        val response = RtspResponseParser.parse(raw)

        assertEquals(200, response?.statusCode)
        assertEquals("2", response?.header("cseq"))
        assertEquals("body", String(response?.body ?: ByteArray(0), Charsets.ISO_8859_1))
    }

    @Test
    fun unlimitedRecordingContinuesUntilStopSignal() {
        assertTrue(
            shouldContinueRtspRecording(
                stopRequested = false,
                elapsedMs = 60_000L,
                maxDurationMs = Long.MAX_VALUE,
            ),
        )
        assertFalse(
            shouldContinueRtspRecording(
                stopRequested = true,
                elapsedMs = 1L,
                maxDurationMs = Long.MAX_VALUE,
            ),
        )
    }

    @Test
    fun finiteRecordingStopsAfterMaxDuration() {
        assertTrue(
            shouldContinueRtspRecording(
                stopRequested = false,
                elapsedMs = 19_999L,
                maxDurationMs = 20_000L,
            ),
        )
        assertFalse(
            shouldContinueRtspRecording(
                stopRequested = false,
                elapsedMs = 20_000L,
                maxDurationMs = 20_000L,
            ),
        )
    }

    @Test
    fun audioWarningAppearsWhenNoAudioSamplesWereWritten() {
        assertEquals(
            RECORDING_WITHOUT_AUDIO_WARNING,
            recordingAudioWarning(audioSamplesWritten = 0, writerWarning = null),
        )
        assertNull(recordingAudioWarning(audioSamplesWritten = 3, writerWarning = null))
    }

    @Test
    fun sdpParserAcceptsH264VariantsAndAudioTracks() {
        val session = SdpParser.parse(
            """
            v=0
            a=control:*
            m=video 0 RTP/AVP 96
            a=rtpmap:96 H264H/90000
            a=fmtp:96 packetization-mode=1;profile-level-id=640014;sprop-parameter-sets=Z0IAH5WoFAFuQA==,aM48gA==
            a=control:trackID=1
            m=audio 0 RTP/AVP 97
            a=rtpmap:97 MPEG4-GENERIC/8000/1
            a=fmtp:97 streamtype=5;profile-level-id=1;mode=AAC-hbr;config=1588;SizeLength=13;IndexLength=3;IndexDeltaLength=3
            a=control:trackID=2
            """.trimIndent(),
        )

        assertEquals(SdpEncoding.H264H, session.videoTrack?.encoding)
        assertEquals(SdpEncoding.AAC, session.audioTrack?.encoding)
        assertNotNull(session.videoTrack?.sps)
        assertArrayEquals(byteArrayOf(0x15, 0x88.toByte()), session.audioTrack?.aacConfig)
    }

    @Test
    fun sdpParserAcceptsPcmuAndPcma() {
        val pcmu = SdpParser.parse(
            """
            m=audio 0 RTP/AVP 0
            a=rtpmap:0 PCMU/8000
            """.trimIndent(),
        )
        val pcma = SdpParser.parse(
            """
            m=audio 0 RTP/AVP 8
            a=rtpmap:8 PCMA/8000
            """.trimIndent(),
        )

        assertEquals(SdpEncoding.PCMU, pcmu.audioTrack?.encoding)
        assertEquals(SdpEncoding.PCMA, pcma.audioTrack?.encoding)
    }

    @Test
    fun rtpParserReadsHeaderAndPayload() {
        val packet = byteArrayOf(
            0x80.toByte(),
            0xE0.toByte(),
            0x12,
            0x34,
            0x00,
            0x00,
            0x00,
            0x2A,
            0x00,
            0x00,
            0x00,
            0x01,
            0x65,
            0x01,
            0x02,
        )

        val parsed = RtpPacketParser.parse(packet)

        assertEquals(96, parsed?.payloadType)
        assertTrue(parsed?.marker == true)
        assertEquals(0x1234, parsed?.sequenceNumber)
        assertEquals(42L, parsed?.timestamp)
        assertArrayEquals(byteArrayOf(0x65, 0x01, 0x02), parsed?.payload)
    }

    @Test
    fun h264AssemblerHandlesSingleNalAndKeyframe() {
        val assembler = H264RtpAssembler()
        val accessUnit = assembler.consume(
            rtpPacket(
                marker = true,
                timestamp = 10,
                payload = byteArrayOf(0x65, 0x01, 0x02),
            ),
        )

        assertTrue(accessUnit?.isKeyframe == true)
        assertEquals(1, accessUnit?.nalUnits?.size)
    }

    @Test
    fun h264AssemblerHandlesStapA() {
        val assembler = H264RtpAssembler()
        val accessUnit = assembler.consume(
            rtpPacket(
                marker = true,
                timestamp = 10,
                payload = byteArrayOf(
                    0x18,
                    0x00,
                    0x02,
                    0x67,
                    0x01,
                    0x00,
                    0x02,
                    0x68,
                    0x02,
                ),
            ),
        )

        assertEquals(H264_NAL_SPS, accessUnit?.nalUnits?.get(0)?.nalType())
        assertEquals(H264_NAL_PPS, accessUnit?.nalUnits?.get(1)?.nalType())
    }

    @Test
    fun h264AssemblerHandlesFuA() {
        val assembler = H264RtpAssembler()
        assertNull(
            assembler.consume(
                rtpPacket(
                    marker = false,
                    timestamp = 10,
                    payload = byteArrayOf(0x7C, 0x85.toByte(), 0x01, 0x02),
                ),
            ),
        )

        val accessUnit = assembler.consume(
            rtpPacket(
                marker = true,
                timestamp = 10,
                payload = byteArrayOf(0x7C, 0x45, 0x03, 0x04),
            ),
        )

        assertArrayEquals(byteArrayOf(0x65, 0x01, 0x02, 0x03, 0x04), accessUnit?.nalUnits?.single())
        assertTrue(accessUnit?.isKeyframe == true)
    }

    @Test
    fun aacAssemblerExtractsPayload() {
        val assembler = AacRtpAssembler(sizeLength = 13, indexLength = 3, indexDeltaLength = 3)

        val units = assembler.consume(
            rtpPacket(
                marker = true,
                timestamp = 10,
                    payload = byteArrayOf(
                        0x00,
                        0x10,
                        0x00,
                        0x10,
                        0x11,
                        0x22,
                    ),
            ),
        )

        assertEquals(1, units.size)
        assertArrayEquals(byteArrayOf(0x11, 0x22), units.single().payload)
    }

    @Test
    fun interleavedParserUsesServerNegotiatedRtpChannel() {
        assertEquals(
            4,
            parseInterleavedRtpChannel("RTP/AVP/TCP;unicast;interleaved=4-5"),
        )
        assertEquals(
            2,
            parseInterleavedRtpChannel("RTP/AVP/TCP;interleaved=2-3;ssrc=1234"),
        )
        assertNull(parseInterleavedRtpChannel("RTP/AVP/TCP;unicast"))
    }

    @Test
    fun g711DecodeProducesPcm16Bytes() {
        assertEquals(4, G711Codec.decodePcmu(byteArrayOf(0xFF.toByte(), 0x7F)).size)
        assertEquals(4, G711Codec.decodePcma(byteArrayOf(0xD5.toByte(), 0x55)).size)
    }

    @Test
    fun h264Mp4FormatterUsesAnnexBStartCodes() {
        val sample = H264Mp4SampleFormatter.toAnnexB(
            listOf(
                byteArrayOf(0x67, 0x01),
                byteArrayOf(0x65, 0x02),
            ),
        )

        assertArrayEquals(
            byteArrayOf(
                0x00,
                0x00,
                0x00,
                0x01,
                0x67,
                0x01,
                0x00,
                0x00,
                0x00,
                0x01,
                0x65,
                0x02,
            ),
            sample,
        )
    }

    @Test
    fun h264Mp4FormatterDoesNotDuplicateStartCode() {
        val sample = H264Mp4SampleFormatter.withStartCode(byteArrayOf(0x00, 0x00, 0x00, 0x01, 0x67))

        assertArrayEquals(byteArrayOf(0x00, 0x00, 0x00, 0x01, 0x67), sample)
    }

    private fun rtpPacket(
        marker: Boolean,
        timestamp: Long,
        payload: ByteArray,
    ): RtpPacket = RtpPacket(
        payloadType = 96,
        marker = marker,
        sequenceNumber = 1,
        timestamp = timestamp,
        ssrc = 1,
        payload = payload,
    )
}
