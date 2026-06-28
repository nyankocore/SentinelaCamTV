package com.sentinela.camtv.preferences

import com.sentinela.camtv.player.StreamQuality
import com.sentinela.camtv.player.TransmissionMode

data class PlayerUiPreferences(
    val showPlayerInfo: Boolean = true,
    val showMosaicInfo: Boolean = true,
    val showFullscreenInfo: Boolean = true,
    val fullscreenQuickMenuHintSeen: Boolean = false,
    val mosaicStreamQuality: StreamQuality = StreamQuality.SD,
    val globalTransmissionMode: TransmissionMode = TransmissionMode.MENOR_LATENCIA,
    val activeMosaicIndex: Int = 0,
    val photoCaptureTreeUri: String? = null,
    val appLanguageTag: String? = null,
)
