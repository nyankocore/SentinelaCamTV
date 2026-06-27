package com.sentinela.camtv.recording.rtsp

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class RtspEndpoint(
    val requestUri: String,
    val host: String,
    val port: Int,
    val username: String?,
    val password: String?,
) {
    val hasCredentials: Boolean = !username.isNullOrBlank()

    companion object {
        fun parse(rtspUrl: String): RtspEndpoint? {
            val uri = runCatching { URI(rtspUrl) }.getOrNull() ?: return null
            if (!uri.scheme.equals("rtsp", ignoreCase = true)) {
                return null
            }
            val host = uri.host ?: return null
            val port = if (uri.port > 0) uri.port else DEFAULT_RTSP_PORT
            val userInfo = uri.rawUserInfo
                ?.split(":", limit = 2)
                ?.map { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }
            val path = uri.rawPath?.takeIf { it.isNotBlank() } ?: "/"
            val query = uri.rawQuery?.let { "?$it" }.orEmpty()
            val requestUri = buildString {
                append("rtsp://")
                append(host)
                if (port != DEFAULT_RTSP_PORT) {
                    append(":")
                    append(port)
                }
                append(path)
                append(query)
            }

            return RtspEndpoint(
                requestUri = requestUri,
                host = host,
                port = port,
                username = userInfo?.getOrNull(0),
                password = userInfo?.getOrNull(1),
            )
        }

        private const val DEFAULT_RTSP_PORT = 554
    }
}
