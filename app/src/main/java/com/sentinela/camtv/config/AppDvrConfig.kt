package com.sentinela.camtv.config

import com.sentinela.camtv.BuildConfig

data class DvrConnectionConfig(
    val host: String,
    val username: String,
    val password: String,
    val rtspPort: Int,
)

fun DvrConnectionConfig.isConfigured(): Boolean = host.isNotBlank()

object AppDvrConfig {
    val localDebugDvr = DvrConnectionConfig(
        host = BuildConfig.SENTINELA_DVR_HOST,
        username = BuildConfig.SENTINELA_DVR_USERNAME,
        password = BuildConfig.SENTINELA_DVR_PASSWORD,
        rtspPort = BuildConfig.SENTINELA_DVR_RTSP_PORT,
    )
}
