package com.sentinela.camtv.recording.rtsp

data class RtspResponse(
    val statusCode: Int,
    val statusMessage: String,
    val headers: Map<String, List<String>>,
    val body: ByteArray,
) {
    fun header(name: String): String? =
        headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }
            ?.value
            ?.firstOrNull()

    fun isSuccess(): Boolean = statusCode in 200..299

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as RtspResponse
        return statusCode == other.statusCode &&
            statusMessage == other.statusMessage &&
            headers == other.headers &&
            body.contentEquals(other.body)
    }

    override fun hashCode(): Int {
        var result = statusCode
        result = 31 * result + statusMessage.hashCode()
        result = 31 * result + headers.hashCode()
        result = 31 * result + body.contentHashCode()
        return result
    }
}

object RtspResponseParser {
    fun parse(rawResponse: ByteArray): RtspResponse? {
        val separator = "\r\n\r\n".toByteArray()
        val headerEnd = rawResponse.indexOf(separator)
        if (headerEnd < 0) return null

        val headerText = String(rawResponse.copyOfRange(0, headerEnd), Charsets.ISO_8859_1)
        val lines = headerText.split("\r\n")
        val statusLine = lines.firstOrNull().orEmpty()
        val statusParts = statusLine.split(" ", limit = 3)
        if (statusParts.size < 2 || !statusParts[0].startsWith("RTSP/", ignoreCase = true)) {
            return null
        }

        val headers = linkedMapOf<String, MutableList<String>>()
        lines.drop(1).forEach { line ->
            val colon = line.indexOf(':')
            if (colon > 0) {
                val key = line.substring(0, colon).trim()
                val value = line.substring(colon + 1).trim()
                headers.getOrPut(key) { mutableListOf() }.add(value)
            }
        }

        val bodyStart = headerEnd + separator.size
        val body = if (bodyStart < rawResponse.size) {
            rawResponse.copyOfRange(bodyStart, rawResponse.size)
        } else {
            ByteArray(0)
        }

        return RtspResponse(
            statusCode = statusParts[1].toIntOrNull() ?: return null,
            statusMessage = statusParts.getOrNull(2).orEmpty(),
            headers = headers,
            body = body,
        )
    }

    private fun ByteArray.indexOf(pattern: ByteArray): Int {
        if (pattern.isEmpty() || size < pattern.size) return -1
        for (index in 0..(size - pattern.size)) {
            var matches = true
            for (patternIndex in pattern.indices) {
                if (this[index + patternIndex] != pattern[patternIndex]) {
                    matches = false
                    break
                }
            }
            if (matches) return index
        }
        return -1
    }
}
