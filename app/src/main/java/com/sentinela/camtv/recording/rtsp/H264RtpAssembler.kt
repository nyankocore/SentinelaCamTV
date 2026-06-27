package com.sentinela.camtv.recording.rtsp

import java.io.ByteArrayOutputStream

data class H264AccessUnit(
    val timestamp: Long,
    val nalUnits: List<ByteArray>,
) {
    val isKeyframe: Boolean = nalUnits.any { it.nalType() == H264_NAL_IDR }
    val sps: ByteArray? = nalUnits.lastOrNull { it.nalType() == H264_NAL_SPS }
    val pps: ByteArray? = nalUnits.lastOrNull { it.nalType() == H264_NAL_PPS }
}

class H264RtpAssembler(
    initialSps: ByteArray? = null,
    initialPps: ByteArray? = null,
) {
    private var pendingTimestamp: Long? = null
    private val pendingNalUnits = mutableListOf<ByteArray>()
    private var fuTimestamp: Long? = null
    private var fuBuffer: ByteArrayOutputStream? = null

    init {
        initialSps?.let { pendingNalUnits += it }
        initialPps?.let { pendingNalUnits += it }
    }

    fun consume(packet: RtpPacket): H264AccessUnit? {
        if (packet.payload.isEmpty()) return null
        val payload = packet.payload
        val nalType = payload[0].toInt() and H264_NAL_TYPE_MASK

        val completedNalUnits = when (nalType) {
            in H264_SINGLE_NAL_RANGE -> listOf(payload)
            H264_NAL_STAP_A -> parseStapA(payload)
            H264_NAL_FU_A -> parseFuA(packet.timestamp, payload)
            else -> emptyList()
        }

        if (completedNalUnits.isEmpty()) return null

        if (pendingTimestamp != null && pendingTimestamp != packet.timestamp) {
            pendingNalUnits.clear()
        }
        pendingTimestamp = packet.timestamp
        pendingNalUnits += completedNalUnits

        if (!packet.marker) return null

        val accessUnit = H264AccessUnit(
            timestamp = packet.timestamp,
            nalUnits = pendingNalUnits.toList(),
        )
        pendingNalUnits.clear()
        pendingTimestamp = null
        return accessUnit
    }

    private fun parseStapA(payload: ByteArray): List<ByteArray> {
        val units = mutableListOf<ByteArray>()
        var offset = 1
        while (offset + 2 <= payload.size) {
            val nalSize = ((payload[offset].toInt() and 0xFF) shl 8) or
                (payload[offset + 1].toInt() and 0xFF)
            offset += 2
            if (nalSize <= 0 || offset + nalSize > payload.size) break
            units += payload.copyOfRange(offset, offset + nalSize)
            offset += nalSize
        }
        return units
    }

    private fun parseFuA(timestamp: Long, payload: ByteArray): List<ByteArray> {
        if (payload.size < 2) return emptyList()
        val fuIndicator = payload[0].toInt() and 0xFF
        val fuHeader = payload[1].toInt() and 0xFF
        val isStart = (fuHeader and 0x80) != 0
        val isEnd = (fuHeader and 0x40) != 0
        val nalType = fuHeader and H264_NAL_TYPE_MASK

        if (isStart) {
            fuTimestamp = timestamp
            fuBuffer = ByteArrayOutputStream(payload.size).apply {
                write(((fuIndicator and 0xE0) or nalType).toByte().toInt())
                write(payload, 2, payload.size - 2)
            }
            return emptyList()
        }

        val buffer = fuBuffer ?: return emptyList()
        if (fuTimestamp != timestamp) {
            fuBuffer = null
            fuTimestamp = null
            return emptyList()
        }
        buffer.write(payload, 2, payload.size - 2)

        return if (isEnd) {
            val nal = buffer.toByteArray()
            fuBuffer = null
            fuTimestamp = null
            listOf(nal)
        } else {
            emptyList()
        }
    }
}

fun ByteArray.nalType(): Int = if (isEmpty()) 0 else this[0].toInt() and H264_NAL_TYPE_MASK

const val H264_NAL_IDR = 5
const val H264_NAL_SPS = 7
const val H264_NAL_PPS = 8

private const val H264_NAL_STAP_A = 24
private const val H264_NAL_FU_A = 28
private const val H264_NAL_TYPE_MASK = 0x1F
private val H264_SINGLE_NAL_RANGE = 1..23
