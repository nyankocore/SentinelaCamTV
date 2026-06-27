package com.sentinela.camtv.recording.rtsp

object RtspBase64 {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    private val decodeTable = IntArray(256) { -1 }.apply {
        ALPHABET.forEachIndexed { index, char -> this[char.code] = index }
    }

    fun encodeToString(bytes: ByteArray): String {
        val output = StringBuilder((bytes.size + 2) / 3 * 4)
        var index = 0
        while (index < bytes.size) {
            val b0 = bytes[index++].toInt() and 0xFF
            val b1 = if (index < bytes.size) bytes[index++].toInt() and 0xFF else -1
            val b2 = if (index < bytes.size) bytes[index++].toInt() and 0xFF else -1
            output.append(ALPHABET[b0 ushr 2])
            output.append(ALPHABET[((b0 and 0x03) shl 4) or ((b1.coerceAtLeast(0)) ushr 4)])
            output.append(if (b1 >= 0) ALPHABET[((b1 and 0x0F) shl 2) or ((b2.coerceAtLeast(0)) ushr 6)] else '=')
            output.append(if (b2 >= 0) ALPHABET[b2 and 0x3F] else '=')
        }
        return output.toString()
    }

    fun decode(value: String): ByteArray {
        val clean = value.filterNot { it.isWhitespace() }
        val output = ArrayList<Byte>(clean.length * 3 / 4)
        var index = 0
        while (index < clean.length) {
            val c0 = clean.getOrNull(index++) ?: break
            val c1 = clean.getOrNull(index++) ?: break
            val c2 = clean.getOrNull(index++) ?: '='
            val c3 = clean.getOrNull(index++) ?: '='
            val b0 = decodeValue(c0)
            val b1 = decodeValue(c1)
            val b2 = if (c2 == '=') -1 else decodeValue(c2)
            val b3 = if (c3 == '=') -1 else decodeValue(c3)
            if (b0 < 0 || b1 < 0 || (c2 != '=' && b2 < 0) || (c3 != '=' && b3 < 0)) {
                error("Base64 inválido")
            }
            output += ((b0 shl 2) or (b1 ushr 4)).toByte()
            if (c2 != '=') {
                output += (((b1 and 0x0F) shl 4) or (b2 ushr 2)).toByte()
            }
            if (c3 != '=') {
                output += (((b2 and 0x03) shl 6) or b3).toByte()
            }
        }
        return output.toByteArray()
    }

    private fun decodeValue(char: Char): Int =
        if (char.code < decodeTable.size) decodeTable[char.code] else -1
}
