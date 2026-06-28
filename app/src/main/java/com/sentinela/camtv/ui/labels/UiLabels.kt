package com.sentinela.camtv.ui.labels

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sentinela.camtv.player.AudioMode
import com.sentinela.camtv.R
import com.sentinela.camtv.player.StreamQuality
import com.sentinela.camtv.player.TransmissionMode

fun activationLabel(active: Boolean): String =
    if (active) "Ativadas" else "Desativadas"

fun statusLabel(active: Boolean): String =
    if (active) "Ativado" else "Desativado"

fun infoMenuLabel(active: Boolean): String =
    "Info: ${if (active) "Ativada" else "Desativada"}"

fun audioLabel(audioMode: AudioMode): String = when (audioMode) {
    AudioMode.Enabled -> "Áudio: Ativado"
    AudioMode.Disabled -> "Áudio: Desativado"
}

fun streamQualityLabel(streamQuality: StreamQuality): String = when (streamQuality) {
    StreamQuality.HD -> "Vídeo: HD"
    StreamQuality.SD -> "Vídeo: SD"
}

fun transmissionModeLabel(transmissionMode: TransmissionMode): String = when (transmissionMode) {
    TransmissionMode.MENOR_LATENCIA -> "Menor latência"
    TransmissionMode.QUALIDADE -> "Estabilidade"
}

fun transmissionModeMenuLabel(transmissionMode: TransmissionMode): String =
    "Modo: ${transmissionModeLabel(transmissionMode)}"

@Composable
fun localizedStatusLabel(active: Boolean): String =
    stringResource(if (active) R.string.common_enabled else R.string.common_disabled)

@Composable
fun localizedInfoMenuLabel(active: Boolean): String =
    stringResource(
        R.string.mosaic_info_label,
        stringResource(if (active) R.string.common_enabled_feminine else R.string.common_disabled_feminine),
    )

@Composable
fun localizedAudioLabel(audioMode: AudioMode): String =
    stringResource(
        R.string.mosaic_audio_label,
        stringResource(
            when (audioMode) {
                AudioMode.Enabled -> R.string.common_enabled
                AudioMode.Disabled -> R.string.common_disabled
            },
        ),
    )

@Composable
fun localizedStreamQualityLabel(streamQuality: StreamQuality): String =
    stringResource(R.string.mosaic_video_label, streamQuality.name)

@Composable
fun localizedTransmissionModeLabel(transmissionMode: TransmissionMode): String =
    stringResource(
        when (transmissionMode) {
            TransmissionMode.MENOR_LATENCIA -> R.string.mosaic_mode_low_latency
            TransmissionMode.QUALIDADE -> R.string.mosaic_mode_stability
        },
    )

@Composable
fun localizedTransmissionModeMenuLabel(transmissionMode: TransmissionMode): String =
    stringResource(R.string.mosaic_mode_label, localizedTransmissionModeLabel(transmissionMode))
