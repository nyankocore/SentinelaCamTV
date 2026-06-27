package com.sentinela.camtv.recording.rtsp

data class H264VideoSize(
    val width: Int,
    val height: Int,
)

object H264SpsParser {
    fun parseSize(sps: ByteArray): H264VideoSize? = runCatching {
        val rbsp = removeEmulationPreventionBytes(sps.dropNalHeader())
        val reader = BitReader(rbsp)
        val profileIdc = reader.readBits(8)
        reader.readBits(8)
        reader.readBits(8)
        reader.readUnsignedExpGolomb()

        var chromaFormatIdc = 1
        if (profileIdc in HIGH_PROFILE_IDS) {
            chromaFormatIdc = reader.readUnsignedExpGolomb()
            if (chromaFormatIdc == 3) {
                reader.readBit()
            }
            reader.readUnsignedExpGolomb()
            reader.readUnsignedExpGolomb()
            reader.readBit()
            if (reader.readBit()) {
                val scalingListCount = if (chromaFormatIdc != 3) 8 else 12
                repeat(scalingListCount) { index ->
                    if (reader.readBit()) {
                        skipScalingList(reader, if (index < 6) 16 else 64)
                    }
                }
            }
        }

        reader.readUnsignedExpGolomb()
        val picOrderCntType = reader.readUnsignedExpGolomb()
        if (picOrderCntType == 0) {
            reader.readUnsignedExpGolomb()
        } else if (picOrderCntType == 1) {
            reader.readBit()
            reader.readSignedExpGolomb()
            reader.readSignedExpGolomb()
            repeat(reader.readUnsignedExpGolomb()) {
                reader.readSignedExpGolomb()
            }
        }

        reader.readUnsignedExpGolomb()
        reader.readBit()
        val picWidthInMbsMinus1 = reader.readUnsignedExpGolomb()
        val picHeightInMapUnitsMinus1 = reader.readUnsignedExpGolomb()
        val frameMbsOnlyFlag = reader.readBit()
        if (!frameMbsOnlyFlag) {
            reader.readBit()
        }
        reader.readBit()

        var frameCropLeftOffset = 0
        var frameCropRightOffset = 0
        var frameCropTopOffset = 0
        var frameCropBottomOffset = 0
        if (reader.readBit()) {
            frameCropLeftOffset = reader.readUnsignedExpGolomb()
            frameCropRightOffset = reader.readUnsignedExpGolomb()
            frameCropTopOffset = reader.readUnsignedExpGolomb()
            frameCropBottomOffset = reader.readUnsignedExpGolomb()
        }

        val width = (picWidthInMbsMinus1 + 1) * 16
        val frameHeightMultiplier = 2 - (if (frameMbsOnlyFlag) 1 else 0)
        val height = frameHeightMultiplier * (picHeightInMapUnitsMinus1 + 1) * 16
        val cropUnitX: Int
        val cropUnitY: Int
        when (chromaFormatIdc) {
            0 -> {
                cropUnitX = 1
                cropUnitY = frameHeightMultiplier
            }
            1 -> {
                cropUnitX = 2
                cropUnitY = 2 * frameHeightMultiplier
            }
            2 -> {
                cropUnitX = 2
                cropUnitY = frameHeightMultiplier
            }
            else -> {
                cropUnitX = 1
                cropUnitY = frameHeightMultiplier
            }
        }

        H264VideoSize(
            width = width - (frameCropLeftOffset + frameCropRightOffset) * cropUnitX,
            height = height - (frameCropTopOffset + frameCropBottomOffset) * cropUnitY,
        ).takeIf { it.width > 0 && it.height > 0 }
    }.getOrNull()

    private fun ByteArray.dropNalHeader(): ByteArray =
        if (isNotEmpty() && (this[0].toInt() and 0x1F) == H264_NAL_SPS) {
            copyOfRange(1, size)
        } else {
            this
        }

    private fun removeEmulationPreventionBytes(bytes: ByteArray): ByteArray {
        val output = ArrayList<Byte>(bytes.size)
        var index = 0
        while (index < bytes.size) {
            if (
                index + 2 < bytes.size &&
                bytes[index] == 0.toByte() &&
                bytes[index + 1] == 0.toByte() &&
                bytes[index + 2] == 3.toByte()
            ) {
                output += 0.toByte()
                output += 0.toByte()
                index += 3
            } else {
                output += bytes[index]
                index += 1
            }
        }
        return output.toByteArray()
    }

    private fun skipScalingList(reader: BitReader, size: Int) {
        var lastScale = 8
        var nextScale = 8
        repeat(size) {
            if (nextScale != 0) {
                val deltaScale = reader.readSignedExpGolomb()
                nextScale = (lastScale + deltaScale + 256) % 256
            }
            lastScale = if (nextScale == 0) lastScale else nextScale
        }
    }

    private val HIGH_PROFILE_IDS = setOf(100, 110, 122, 244, 44, 83, 86, 118, 128, 138, 139, 134)
}

private class BitReader(
    private val data: ByteArray,
) {
    private var bitOffset = 0

    fun readBit(): Boolean = readBits(1) == 1

    fun readBits(count: Int): Int {
        var value = 0
        repeat(count) {
            val byteIndex = bitOffset / 8
            val bitIndex = 7 - (bitOffset % 8)
            val bit = if (byteIndex < data.size) {
                (data[byteIndex].toInt() ushr bitIndex) and 1
            } else {
                0
            }
            value = (value shl 1) or bit
            bitOffset += 1
        }
        return value
    }

    fun readUnsignedExpGolomb(): Int {
        var leadingZeroBits = 0
        while (!readBit()) {
            leadingZeroBits += 1
        }
        val suffix = if (leadingZeroBits == 0) 0 else readBits(leadingZeroBits)
        return (1 shl leadingZeroBits) - 1 + suffix
    }

    fun readSignedExpGolomb(): Int {
        val value = readUnsignedExpGolomb()
        val sign = if (value % 2 == 0) -1 else 1
        return sign * ((value + 1) / 2)
    }
}
