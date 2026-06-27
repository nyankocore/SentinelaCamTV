package com.sentinela.camtv.recording.rtsp

data class AacAccessUnit(
    val timestamp: Long,
    val payload: ByteArray,
)

class AacRtpAssembler(
    private val sizeLength: Int,
    private val indexLength: Int,
    private val indexDeltaLength: Int,
) {
    fun consume(packet: RtpPacket): List<AacAccessUnit> {
        val payload = packet.payload
        if (payload.size < AU_HEADERS_LENGTH_BYTES) return emptyList()

        val auHeadersLengthBits = ((payload[0].toInt() and 0xFF) shl 8) or
            (payload[1].toInt() and 0xFF)
        val auHeadersLengthBytes = (auHeadersLengthBits + 7) / 8
        val auHeadersStart = AU_HEADERS_LENGTH_BYTES
        val auPayloadStart = auHeadersStart + auHeadersLengthBytes
        if (payload.size < auPayloadStart || auHeadersLengthBits <= 0) {
            return emptyList()
        }

        val bitReader = PayloadBitReader(payload, auHeadersStart, auHeadersLengthBits)
        val headerBits = sizeLength + indexLength
        val nextHeaderBits = sizeLength + indexDeltaLength
        val sizes = mutableListOf<Int>()
        var remainingHeaderBits = auHeadersLengthBits
        var first = true
        while (remainingHeaderBits >= if (first) headerBits else nextHeaderBits) {
            val size = bitReader.readBits(sizeLength)
            val indexBits = if (first) indexLength else indexDeltaLength
            if (indexBits > 0) {
                bitReader.readBits(indexBits)
            }
            sizes += size
            remainingHeaderBits -= if (first) headerBits else nextHeaderBits
            first = false
        }

        val units = mutableListOf<AacAccessUnit>()
        var offset = auPayloadStart
        sizes.forEach { size ->
            val byteSize = size
            if (byteSize > 0 && offset + byteSize <= payload.size) {
                units += AacAccessUnit(
                    timestamp = packet.timestamp,
                    payload = payload.copyOfRange(offset, offset + byteSize),
                )
                offset += byteSize
            }
        }
        return units
    }

    private companion object {
        const val AU_HEADERS_LENGTH_BYTES = 2
    }
}

object G711Codec {
    fun decodePcmu(payload: ByteArray): ByteArray =
        decode(payload, ::decodePcmuSample)

    fun decodePcma(payload: ByteArray): ByteArray =
        decode(payload, ::decodePcmaSample)

    private fun decode(payload: ByteArray, decoder: (Int) -> Short): ByteArray {
        val output = ByteArray(payload.size * PCM16_BYTES)
        payload.forEachIndexed { index, byte ->
            val sample = decoder(byte.toInt() and 0xFF).toInt()
            output[index * 2] = (sample and 0xFF).toByte()
            output[index * 2 + 1] = ((sample ushr 8) and 0xFF).toByte()
        }
        return output
    }

    private fun decodePcmuSample(value: Int): Short {
        val uLaw = value.inv() and 0xFF
        val sign = uLaw and 0x80
        val exponent = (uLaw ushr 4) and 0x07
        val mantissa = uLaw and 0x0F
        var sample = ((mantissa shl 3) + BIAS) shl exponent
        sample -= BIAS
        return (if (sign != 0) -sample else sample).toShort()
    }

    private fun decodePcmaSample(value: Int): Short {
        val aLaw = value xor 0x55
        val sign = aLaw and 0x80
        val exponent = (aLaw and 0x70) ushr 4
        val mantissa = aLaw and 0x0F
        var sample = if (exponent == 0) {
            (mantissa shl 4) + 8
        } else {
            ((mantissa shl 4) + 0x108) shl (exponent - 1)
        }
        if (sign == 0) {
            sample = -sample
        }
        return sample.toShort()
    }

    private const val BIAS = 0x84
    private const val PCM16_BYTES = 2
}

private class PayloadBitReader(
    private val data: ByteArray,
    private val byteOffset: Int,
    private val bitLength: Int,
) {
    private var bitOffset = 0

    fun readBits(count: Int): Int {
        var value = 0
        repeat(count) {
            val absoluteBit = bitOffset
            val byteIndex = byteOffset + absoluteBit / 8
            val bitIndex = 7 - absoluteBit % 8
            val bit = if (absoluteBit < bitLength && byteIndex < data.size) {
                (data[byteIndex].toInt() ushr bitIndex) and 1
            } else {
                0
            }
            value = (value shl 1) or bit
            bitOffset += 1
        }
        return value
    }
}
