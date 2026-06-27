package com.sentinela.camtv.recording.rtsp

import java.security.MessageDigest
import java.util.Locale
import kotlin.random.Random

sealed interface RtspAuthChallenge {
    data class Basic(val realm: String?) : RtspAuthChallenge
    data class Digest(
        val realm: String,
        val nonce: String,
        val qop: String?,
        val algorithm: String?,
        val opaque: String?,
    ) : RtspAuthChallenge
}

object RtspAuthChallengeParser {
    fun parse(header: String?): RtspAuthChallenge? {
        val value = header?.trim().orEmpty()
        if (value.startsWith("Basic", ignoreCase = true)) {
            return RtspAuthChallenge.Basic(realm = parseParameters(value.removePrefixIgnoreCase("Basic"))["realm"])
        }
        if (value.startsWith("Digest", ignoreCase = true)) {
            val params = parseParameters(value.removePrefixIgnoreCase("Digest"))
            return RtspAuthChallenge.Digest(
                realm = params["realm"] ?: return null,
                nonce = params["nonce"] ?: return null,
                qop = params["qop"]?.substringBefore(',')?.trim(),
                algorithm = params["algorithm"],
                opaque = params["opaque"],
            )
        }
        return null
    }

    private fun parseParameters(raw: String): Map<String, String> {
        val result = linkedMapOf<String, String>()
        var index = 0
        while (index < raw.length) {
            while (index < raw.length && (raw[index].isWhitespace() || raw[index] == ',')) index++
            val keyStart = index
            while (index < raw.length && raw[index] != '=' && raw[index] != ',') index++
            if (index >= raw.length || raw[index] != '=') break
            val key = raw.substring(keyStart, index).trim().lowercase(Locale.US)
            index++
            val value = if (index < raw.length && raw[index] == '"') {
                index++
                val valueStart = index
                while (index < raw.length && raw[index] != '"') index++
                raw.substring(valueStart, index).also {
                    if (index < raw.length) index++
                }
            } else {
                val valueStart = index
                while (index < raw.length && raw[index] != ',') index++
                raw.substring(valueStart, index).trim()
            }
            if (key.isNotEmpty()) result[key] = value
        }
        return result
    }

    private fun String.removePrefixIgnoreCase(prefix: String): String =
        if (startsWith(prefix, ignoreCase = true)) substring(prefix.length) else this
}

class RtspAuthenticator(
    private val cnonceFactory: () -> String = {
        Random.nextBytes(8).joinToString("") { "%02x".format(it.toInt() and 0xFF) }
    },
) {
    private var nonceCount = 0

    fun authorizationHeader(
        method: String,
        requestUri: String,
        endpoint: RtspEndpoint,
        challenge: RtspAuthChallenge?,
    ): String? {
        val username = endpoint.username ?: return null
        val password = endpoint.password.orEmpty()
        return when (challenge) {
            is RtspAuthChallenge.Basic -> {
                val token = RtspBase64.encodeToString("$username:$password".toByteArray(Charsets.ISO_8859_1))
                "Basic $token"
            }
            is RtspAuthChallenge.Digest -> digestHeader(method, requestUri, username, password, challenge)
            null -> null
        }
    }

    private fun digestHeader(
        method: String,
        requestUri: String,
        username: String,
        password: String,
        challenge: RtspAuthChallenge.Digest,
    ): String {
        nonceCount += 1
        val nc = "%08x".format(nonceCount)
        val cnonce = cnonceFactory()
        val qop = challenge.qop?.takeIf { it.equals("auth", ignoreCase = true) }
        val ha1 = md5("$username:${challenge.realm}:$password")
        val ha2 = md5("${method.uppercase(Locale.US)}:$requestUri")
        val response = if (qop != null) {
            md5("$ha1:${challenge.nonce}:$nc:$cnonce:$qop:$ha2")
        } else {
            md5("$ha1:${challenge.nonce}:$ha2")
        }

        return buildString {
            append("Digest username=\"").append(username).append("\"")
            append(", realm=\"").append(challenge.realm).append("\"")
            append(", nonce=\"").append(challenge.nonce).append("\"")
            append(", uri=\"").append(requestUri).append("\"")
            append(", response=\"").append(response).append("\"")
            if (qop != null) {
                append(", qop=").append(qop)
                append(", nc=").append(nc)
                append(", cnonce=\"").append(cnonce).append("\"")
            }
            challenge.opaque?.let { append(", opaque=\"").append(it).append("\"") }
            challenge.algorithm?.let { append(", algorithm=").append(it) }
        }
    }

    private fun md5(value: String): String =
        MessageDigest.getInstance("MD5")
            .digest(value.toByteArray(Charsets.ISO_8859_1))
            .joinToString("") { "%02x".format(it) }
}
