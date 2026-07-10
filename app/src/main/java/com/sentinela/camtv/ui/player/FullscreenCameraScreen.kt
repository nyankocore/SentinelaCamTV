package com.sentinela.camtv.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.ui.PlayerView
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.sentinela.camtv.R
import com.sentinela.camtv.player.DvrRtspUrlBuilder
import com.sentinela.camtv.ui.common.QuickActionDock
import com.sentinela.camtv.ui.common.QuickActionDockAction
import com.sentinela.camtv.ui.common.QuickActionIcon
import com.sentinela.camtv.ui.common.quickActionModeIcon
import com.sentinela.camtv.ui.design.SentinelaOverlayCard
import com.sentinela.camtv.ui.design.SentinelaTransientMessage
import com.sentinela.camtv.ui.design.SentinelaTvColors
import com.sentinela.camtv.ui.design.SentinelaTvShape
import com.sentinela.camtv.ui.labels.localizedAudioLabel
import com.sentinela.camtv.ui.labels.localizedInfoMenuLabel
import com.sentinela.camtv.ui.labels.localizedStreamQualityLabel
import com.sentinela.camtv.ui.labels.localizedTransmissionModeMenuLabel
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
                onDismissQuickMenu = onDismissQuickMenu,
                onOpenHome = onOpenHome,
                onExitApp = onExitApp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp),
            )
        } else if (state.showQuickMenuHint) {
            FullscreenQuickMenuHint(
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        if (recordingProbeActive && !state.quickMenuVisible) {
            RecordingIndicator(
                modifier = Modifier.align(Alignment.TopStart).padding(start = 28.dp, top = 28.dp),
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
    onDismissQuickMenu: () -> Unit,
    onOpenHome: () -> Unit,
    onExitApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    QuickActionDock(
        actions = listOf(
            QuickActionDockAction(
                label = stringResource(R.string.fullscreen_take_photo),
                icon = QuickActionIcon.Photo,
                onClick = {
                    onDismissQuickMenu()
                    onTakePhoto()
                },
                width = 118.dp,
            ),
            QuickActionDockAction(
                label = stringResource(
                    if (recordingProbeActive) {
                        R.string.fullscreen_stop_recording
                    } else {
                        R.string.fullscreen_start_recording
                    },
                ),
                icon = if (recordingProbeActive) QuickActionIcon.Stop else QuickActionIcon.Record,
                onClick = {
                    onDismissQuickMenu()
                    if (recordingProbeActive) {
                        onStopRecordingProbe()
                    } else {
                        onStartRecordingProbe()
                    }
                },
                width = 104.dp,
            ),
            QuickActionDockAction(localizedAudioLabel(state.audioMode), QuickActionIcon.Audio, onToggleAudio, width = 126.dp),
            QuickActionDockAction(localizedStreamQualityLabel(state.streamQuality), QuickActionIcon.Video, onToggleStreamQuality, width = 116.dp),
            QuickActionDockAction(localizedInfoMenuLabel(state.showInfo), QuickActionIcon.Info, onToggleInfo, width = 128.dp),
            QuickActionDockAction(localizedTransmissionModeMenuLabel(state.transmissionMode), state.transmissionMode.quickActionModeIcon(), onToggleTransmissionMode, width = 154.dp),
            QuickActionDockAction(stringResource(R.string.mosaic_quick_home), QuickActionIcon.Home, onOpenHome, width = 126.dp),
            QuickActionDockAction(stringResource(R.string.mosaic_quick_exit_app), QuickActionIcon.Exit, onExitApp, width = 118.dp),
        ),
        initialFocusedIndex = if (recordingProbeActive) 1 else 0,
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
            text = stringResource(R.string.fullscreen_opening_camera),
        )
    }
}

@Composable
private fun FullscreenQuickMenuHint(
    modifier: Modifier = Modifier,
) {
    FullscreenOverlayCard(
        text = stringResource(R.string.fullscreen_quick_menu_hint),
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

@Composable
private fun RecordingIndicator(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .widthIn(min = 84.dp)
            .background(
                color = SentinelaTvColors.panel.copy(alpha = 0.78f),
                shape = SentinelaTvShape.overlay,
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "REC",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
    }
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
    recordingProbeActive -> "Parar"
    else -> "Gravar"
}

private const val FULLSCREEN_QUICK_MENU_HINT_DURATION_MS = 4_000L
