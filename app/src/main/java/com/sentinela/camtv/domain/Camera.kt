package com.sentinela.camtv.domain

data class Camera(
    val id: String,
    val name: String,
    val source: CameraSource,
    val position: Int = 0,
    val enabled: Boolean = true,
    val hasAuthenticationFailure: Boolean = false,
)

sealed interface CameraSource

data class DvrRtspChannel(
    val channel: Int,
    val subtype: Int,
) : CameraSource

data class RtspCameraSource(
    val mainRtspUrl: String,
    val subRtspUrl: String?,
) : CameraSource

data class OnvifCameraSource(
    val deviceServiceUrl: String,
    val mainRtspUrl: String,
    val subRtspUrl: String?,
) : CameraSource

enum class CameraSourceType {
    // Nome preservado para compatibilidade com bancos Room antigos.
    INTELBRAS_DVR_CHANNEL,
    ONVIF,
    RTSP_MANUAL,
}
