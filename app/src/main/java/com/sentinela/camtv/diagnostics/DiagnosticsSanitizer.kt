package com.sentinela.camtv.diagnostics

import java.net.URI

object DiagnosticsSanitizer {
    private val rtspUrlRegex = Regex("""rtsp://\S+""", RegexOption.IGNORE_CASE)
    private val credentialPairRegex = Regex(
        pattern = """(?i)\b(user(name)?|usuario|usu[aá]rio|password|senha)\s*[:=]\s*[^,\s;]+""",
    )

    fun sanitize(value: String): String =
        value
            .replace(rtspUrlRegex) { match -> sanitizeRtspUrl(match.value) }
            .replace(credentialPairRegex) { match ->
                "${match.value.substringBeforeAny(':', '=')}=<removido>"
            }

    fun sanitizeThrowable(throwable: Throwable): Throwable =
        SanitizedThrowable(
            originalClassName = throwable::class.java.name,
            sanitizedMessage = sanitize(throwable.message.orEmpty()).ifBlank { null },
            stackTraceElements = throwable.stackTrace,
        )

    private fun sanitizeRtspUrl(rawUrl: String): String =
        runCatching {
            val uri = URI(rawUrl)
            buildString {
                append("rtsp://")
                append(uri.host ?: "<host>")
                if (uri.port > 0) append(":").append(uri.port)
                append("/<caminho-removido>")
            }
        }.getOrDefault("rtsp://<removido>")

    private fun String.substringBeforeAny(vararg delimiters: Char): String {
        val index = delimiters.map { delimiter -> indexOf(delimiter) }
            .filter { it >= 0 }
            .minOrNull()
            ?: return this
        return substring(0, index)
    }
}

private class SanitizedThrowable(
    originalClassName: String,
    sanitizedMessage: String?,
    stackTraceElements: Array<StackTraceElement>,
) : RuntimeException("$originalClassName: ${sanitizedMessage.orEmpty()}".trim()) {
    init {
        stackTrace = stackTraceElements
    }
}
