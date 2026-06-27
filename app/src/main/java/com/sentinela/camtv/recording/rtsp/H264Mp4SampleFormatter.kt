package com.sentinela.camtv.recording.rtsp

object H264Mp4SampleFormatter {
    private val START_CODE = byteArrayOf(0x00, 0x00, 0x00, 0x01)

    fun withStartCode(nalUnit: ByteArray): ByteArray =
        if (nalUnit.hasStartCode()) {
            nalUnit
        } else {
            START_CODE + nalUnit
        }

    fun toAnnexB(nalUnits: List<ByteArray>): ByteArray {
        val normalized = nalUnits.filter { it.isNotEmpty() }
        val totalSize = normalized.sumOf {
            if (it.hasStartCode()) it.size else START_CODE.size + it.size
        }
        val output = ByteArray(totalSize)
        var offset = 0
        normalized.forEach { nal ->
            if (!nal.hasStartCode()) {
                START_CODE.copyInto(output, offset)
                offset += START_CODE.size
            }
            nal.copyInto(output, offset)
            offset += nal.size
        }
        return output
    }

    private fun ByteArray.hasStartCode(): Boolean =
        size >= 4 &&
            this[0] == 0.toByte() &&
            this[1] == 0.toByte() &&
            ((this[2] == 0.toByte() && this[3] == 1.toByte()) || this[2] == 1.toByte())
}
