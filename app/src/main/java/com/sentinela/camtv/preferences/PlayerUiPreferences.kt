package com.sentinela.camtv.preferences

import com.sentinela.camtv.player.StreamQuality
import com.sentinela.camtv.player.TransmissionMode

data class PlayerUiPreferences(
    val showPlayerInfo: Boolean = false,
    val showMosaicInfo: Boolean = false,
    val showFullscreenInfo: Boolean = false,
    val fullscreenQuickMenuHintSeen: Boolean = false,
    val mosaicStreamQuality: StreamQuality = StreamQuality.SD,
    val globalTransmissionMode: TransmissionMode = TransmissionMode.MENOR_LATENCIA,
    val freeActiveCameraId: String? = null,
    val diagnosticsEnabled: Boolean = true,
    val premiumGraceUntilEpochMillis: Long = 0L,
)
