package com.sentinela.camtv.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.media3.ui.PlayerView
import com.sentinela.camtv.player.DvrRtspUrlBuilder
import com.sentinela.camtv.ui.common.QuickMenu
import com.sentinela.camtv.ui.common.QuickMenuAction
import com.sentinela.camtv.ui.design.SentinelaOverlayCard
import com.sentinela.camtv.ui.design.SentinelaTransientMessage
import com.sentinela.camtv.ui.design.SentinelaTvColors
import com.sentinela.camtv.ui.labels.audioLabel
import com.sentinela.camtv.ui.labels.infoMenuLabel
import com.sentinela.camtv.ui.labels.streamQualityLabel
import com.sentinela.camtv.ui.labels.transmissionModeMenuLabel
import com.sentinela.camtv.ui.mosaic.MosaicNavigationDirection
import kotlinx.coroutines.delay

@Composable
fun FullscreenCameraScreen(
    state: FullscreenPlayerUiState,
    rtspUrlBuilder: DvrRtspUrlBuilder,
    onExit: () -> Unit,
    onShowQuickMenu: () -> Unit,
    onDismissQuickMenu: () -> Unit,
    onToggleAudio: () -> Unit,
    onToggleStreamQuality: () -> Unit,
    onToggleInfo: () -> Unit,
    onToggleTransmissionMode: () -> Unit,
    onTakePhoto: () -> Unit = {},
    recordingProbeActive: Boolean = false,
    onStartRecordingProbe: () -> Unit = {},
    onStopRecordingProbe: () -> Unit = {},
    transientMessage: String? = null,
    onTransientMessageTimeout: () -> Unit = {},
    onPlayerViewChanged: (PlayerView?) -> Unit = {},
    onRenderedFirstFrameChanged: (Boolean) -> Unit = {},
    onOpenHome: () -> Unit,
    onNavigateDirection: (MosaicNavigationDirection) -> Unit = {},
    onExitApp: () -> Unit,
    onQuickMenuHintShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val request = state.streamRequest()
    val rtspUrl = remember(request, rtspUrlBuilder) {
        request?.let(rtspUrlBuilder::build)
    }

    BackHandler {
        if (state.quickMenuVisible) {
            onDismissQuickMenu()
        } else {
            onExit()
        }
    }

    LaunchedEffect(state.camera?.id, state.quickMenuVisible) {
        if (!state.quickMenuVisible) {
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(state.camera?.id, state.showQuickMenuHint) {
        if (state.showQuickMenuHint) {
            delay(FULLSCREEN_QUICK_MENU_HINT_DURATION_MS)
            onQuickMenuHintShown()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SentinelaTvColors.playerBackground)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { keyEvent ->
                if (state.quickMenuVisible) {
                    return@onPreviewKeyEvent false
                }
                keyEvent.key.fullscreenNavigationDirection()?.let { direction ->
                    if (keyEvent.type == KeyEventType.KeyUp) {
                        onNavigateDirection(direction)
                    }
                    return@onPreviewKeyEvent true
                }
                when {
                    keyEvent.type == KeyEventType.KeyUp && keyEvent.key.opensFullscreenQuickMenu() -> {
                        onShowQuickMenu()
                        true
                    }

                    else -> false
                }
            }
            .focusable(enabled = !state.quickMenuVisible),
    ) {
        if (request == null || rtspUrl.isNullOrBlank()) {
            OpeningFullscreenMessage()
        } else {
            RtspPlayerSurface(
                request = request,
                rtspUrl = rtspUrl,
                showPlayerInfo = state.showInfo,
                onPlayerViewChanged = onPlayerViewChanged,
                onRenderedFirstFrameChanged = onRenderedFirstFrameChanged,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (state.quickMenuVisible) {
            FullscreenQuickMenu(
                state = state,
                onToggleAudio = onToggleAudio,
                onToggleStreamQuality = onToggleStreamQuality,
                onToggleInfo = onToggleInfo,
                onToggleTransmissionMode = onToggleTransmissionMode,
                onTakePhoto = onTakePhoto,
                recordingProbeActive = recordingProbeActive,
                onStartRecordingProbe = onStartRecordingProbe,
                onStopRecordingProbe = onStopRecordingProbe,
                onOpenHome = onOpenHome,
                onExitApp = onExitApp,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        } else if (state.showQuickMenuHint) {
            FullscreenQuickMenuHint(
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        transientMessage?.let { message ->
            SentinelaTransientMessage(
                message = message,
                onTimeout = onTransientMessageTimeout,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp),
            )
        }
    }
}

@Composable
private fun FullscreenQuickMenu(
    state: FullscreenPlayerUiState,
    onToggleAudio: () -> Unit,
    onToggleStreamQuality: () -> Unit,
    onToggleInfo: () -> Unit,
    onToggleTransmissionMode: () -> Unit,
    onTakePhoto: () -> Unit,
    recordingProbeActive: Boolean,
    onStartRecordingProbe: () -> Unit,
    onStopRecordingProbe: () -> Unit,
    onOpenHome: () -> Unit,
    onExitApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    QuickMenu(
        actions = listOf(
            QuickMenuAction(audioLabel(state.audioMode), onToggleAudio),
            QuickMenuAction(streamQualityLabel(state.streamQuality), onToggleStreamQuality),
            QuickMenuAction(infoMenuLabel(state.showInfo), onToggleInfo),
            QuickMenuAction(transmissionModeMenuLabel(state.transmissionMode), onToggleTransmissionMode),
            QuickMenuAction("Tirar foto", onTakePhoto),
            QuickMenuAction(
                label = fullscreenRecordingMenuLabel(recordingProbeActive),
                onClick = if (recordingProbeActive) onStopRecordingProbe else onStartRecordingProbe,
            ),
            QuickMenuAction("Ir para início", onOpenHome),
            QuickMenuAction("Sair do app", onExitApp),
        ),
        modifier = modifier,
    )
}

@Composable
private fun OpeningFullscreenMessage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        FullscreenOverlayCard(
            text = "Abrindo câmera...",
        )
    }
}

@Composable
private fun FullscreenQuickMenuHint(
    modifier: Modifier = Modifier,
) {
    FullscreenOverlayCard(
        text = "Pressione OK/Enter para abrir o menu rápido.",
        modifier = modifier.padding(bottom = 28.dp),
    )
}

@Composable
private fun FullscreenOverlayCard(
    text: String,
    modifier: Modifier = Modifier,
) {
    SentinelaOverlayCard(
        text = text,
        maxWidth = 540.dp,
        modifier = modifier,
    )
}

internal fun Key.opensFullscreenQuickMenu(): Boolean =
    this == Key.Enter ||
        this == Key.NumPadEnter ||
        this == Key.DirectionCenter

internal fun Key.fullscreenNavigationDirection(): MosaicNavigationDirection? = when (this) {
    Key.DirectionUp -> MosaicNavigationDirection.Up
    Key.DirectionDown -> MosaicNavigationDirection.Down
    Key.DirectionLeft -> MosaicNavigationDirection.Left
    Key.DirectionRight -> MosaicNavigationDirection.Right
    else -> null
}

internal fun fullscreenRecordingMenuLabel(
    recordingProbeActive: Boolean,
): String = when {
    recordingProbeActive -> "Parar gravação"
    else -> "Iniciar gravação"
}

private const val FULLSCREEN_QUICK_MENU_HINT_DURATION_MS = 4_000L
