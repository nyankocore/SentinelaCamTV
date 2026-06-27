package com.sentinela.camtv.diagnostics

import java.net.URI

object DiagnosticsSanitizer {
    private val rtspUrlPattern = Regex(
        pattern = "(?i)rtsp://[^\\s]+",
    )

    fun sanitize(message: String): String =
        rtspUrlPattern.replace(message) { match ->
            sanitizeRtspUrl(match.value)
        }

    private fun sanitizeRtspUrl(rawUrl: String): String {
        val normalized = rawUrl.trimEnd('.', ',', ';', ')', ']')
        val suffix = rawUrl.removePrefix(normalized)
        return runCatching {
            val uri = URI(normalized)
            val host = uri.host ?: return@runCatching "rtsp://<host-removido>/<caminho-removido>"
            val port = if (uri.port > 0) ":${uri.port}" else ""
            "rtsp://$host$port/<caminho-removido>"
        }.getOrDefault("rtsp://<url-removida>") + suffix
    }
}
