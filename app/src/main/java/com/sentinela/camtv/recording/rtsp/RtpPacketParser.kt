package com.sentinela.camtv.recording.rtsp

data class RtpPacket(
    val payloadType: Int,
    val marker: Boolean,
    val sequenceNumber: Int,
    val timestamp: Long,
    val ssrc: Long,
    val payload: ByteArray,
)

object RtpPacketParser {
    fun parse(packet: ByteArray): RtpPacket? {
        if (packet.size < RTP_HEADER_BYTES) return null
        val version = (packet[0].toInt() ushr 6) and 0x03
        if (version != RTP_VERSION) return null

        val hasExtension = (packet[0].toInt() and 0x10) != 0
        val csrcCount = packet[0].toInt() and 0x0F
        var offset = RTP_HEADER_BYTES + csrcCount * CSRC_BYTES
        if (packet.size < offset) return null

        if (hasExtension) {
            if (packet.size < offset + EXTENSION_HEADER_BYTES) return null
            val extensionLengthWords = packet.readUInt16(offset + 2)
            offset += EXTENSION_HEADER_BYTES + extensionLengthWords * 4
            if (packet.size < offset) return null
        }

        return RtpPacket(
            payloadType = packet[1].toInt() and 0x7F,
            marker = (packet[1].toInt() and 0x80) != 0,
            sequenceNumber = packet.readUInt16(2),
            timestamp = packet.readUInt32(4),
            ssrc = packet.readUInt32(8),
            payload = packet.copyOfRange(offset, packet.size),
        )
    }

    private fun ByteArray.readUInt16(offset: Int): Int =
        ((this[offset].toInt() and 0xFF) shl 8) or (this[offset + 1].toInt() and 0xFF)

    private fun ByteArray.readUInt32(offset: Int): Long =
        ((this[offset].toLong() and 0xFF) shl 24) or
            ((this[offset + 1].toLong() and 0xFF) shl 16) or
            ((this[offset + 2].toLong() and 0xFF) shl 8) or
            (this[offset + 3].toLong() and 0xFF)

    private const val RTP_VERSION = 2
    private const val RTP_HEADER_BYTES = 12
    private const val CSRC_BYTES = 4
    private const val EXTENSION_HEADER_BYTES = 4
}
