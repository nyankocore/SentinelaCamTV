package com.sentinela.camtv.ui.mosaic

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.ui.PlayerView
import com.sentinela.camtv.R
import com.sentinela.camtv.capture.CaptureRepository
import com.sentinela.camtv.capture.CaptureRequest
import com.sentinela.camtv.capture.userMessage as captureUserMessage
import com.sentinela.camtv.config.AppDvrConfig
import com.sentinela.camtv.config.DvrConnectionConfig
import com.sentinela.camtv.config.isConfigured
import com.sentinela.camtv.domain.Camera
import com.sentinela.camtv.domain.DvrRtspChannel
import com.sentinela.camtv.player.DvrRtspUrlBuilder
import com.sentinela.camtv.player.PlayerMode
import com.sentinela.camtv.player.streamRequestFor
import com.sentinela.camtv.recording.RecordingProbeRepository
import com.sentinela.camtv.recording.RecordingProbeRequest
import com.sentinela.camtv.recording.RecordingStopSignal
import com.sentinela.camtv.recording.userMessage as recordingUserMessage
import com.sentinela.camtv.ui.common.QuickActionDock
import com.sentinela.camtv.ui.common.QuickActionDockAction
import com.sentinela.camtv.ui.common.QuickActionIcon
import com.sentinela.camtv.ui.common.quickActionModeIcon
import com.sentinela.camtv.ui.design.SentinelaOverlayCard
import com.sentinela.camtv.ui.design.SentinelaTvColors
import com.sentinela.camtv.ui.design.SentinelaTvDialog
import com.sentinela.camtv.ui.design.SentinelaTvSpacing
import com.sentinela.camtv.ui.labels.localizedInfoMenuLabel
import com.sentinela.camtv.ui.labels.localizedStreamQualityLabel
import com.sentinela.camtv.ui.labels.localizedTransmissionModeMenuLabel
import com.sentinela.camtv.ui.player.FullscreenCameraScreen
import com.sentinela.camtv.ui.player.FullscreenPlayerViewModel
import com.sentinela.camtv.ui.text.asString
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val CAMERA_FOCUS_HIDE_DELAY_MS = 5_000L
private const val MOSAIC_QUICK_MENU_HINT_DURATION_MS = 4_000L

@Composable
fun MosaicScreen(
    viewModelFactory: ViewModelProvider.Factory,
    onOpenHome: () -> Unit,
    onExitApp: () -> Unit,
    captureRepository: CaptureRepository? = null,
    recordingProbeRepository: RecordingProbeRepository? = null,
    dvrConfig: DvrConnectionConfig = AppDvrConfig.localDebugDvr,
) {
    val mosaicViewModel: MosaicViewModel = viewModel(factory = viewModelFactory)
    val fullscreenViewModel: FullscreenPlayerViewModel = viewModel(factory = viewModelFactory)
    val state by mosaicViewModel.state.collectAsState()
    val fullscreenState by fullscreenViewModel.state.collectAsState()
    val rtspUrlBuilder = remember(dvrConfig) {
        DvrRtspUrlBuilder(dvrConfig)
    }
    var showCameraFocusIndicator by remember { mutableStateOf(true) }
    var focusActivityToken by remember { mutableIntStateOf(0) }
    var videoAspectRatios by remember { mutableStateOf<Map<MosaicAspectRatioKey, Float>>(emptyMap()) }
    var pendingMosaicSwitch by remember { mutableStateOf<MosaicSwitchTarget?>(null) }
    var fullscreenPlayerView by remember { mutableStateOf<PlayerView?>(null) }
    var fullscreenRenderedFirstFrame by remember { mutableStateOf(false) }
    var fullscreenMessage by remember { mutableStateOf<String?>(null) }
    var recordingStopSignal by remember { mutableStateOf<RecordingStopSignal?>(null) }
    var recordingJob by remember { mutableStateOf<Job?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val photoUnavailableMessage = stringResource(R.string.mosaic_photo_unavailable)
    val savingPhotoMessage = stringResource(R.string.mosaic_saving_photo)
    val recordingUnavailableMessage = stringResource(R.string.mosaic_recording_unavailable)
    val recordingStartedMessage = stringResource(R.string.mosaic_recording_started)
    val recordingFinalizingMessage = stringResource(R.string.mosaic_recording_finalizing)

    BackHandler {
        if (shouldReturnHomeOnMosaicBack(state)) {
            onOpenHome()
        } else {
            mosaicViewModel.onBackPressed()
        }
    }

    LaunchedEffect(focusActivityToken, showCameraFocusIndicator) {
        if (showCameraFocusIndicator) {
            delay(CAMERA_FOCUS_HIDE_DELAY_MS)
            showCameraFocusIndicator = false
        }
    }

    LaunchedEffect(state.cameras) {
        val activeCameraIds = state.cameras.mapTo(mutableSetOf()) { it.id }
        videoAspectRatios = videoAspectRatios.filterKeys { key -> key.cameraId in activeCameraIds }
    }

    val fullscreenCamera = state.fullscreenCamera
    LaunchedEffect(fullscreenCamera?.id) {
        recordingStopSignal?.stop()
        fullscreenCamera?.let(fullscreenViewModel::open)
        fullscreenPlayerView = null
        fullscreenRenderedFirstFrame = false
        fullscreenMessage = null
    }

    DisposableEffect(Unit) {
        onDispose {
            recordingStopSignal?.stop()
            recordingJob?.cancel()
        }
    }

    fun showCameraFocus() {
        showCameraFocusIndicator = true
        focusActivityToken += 1
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(SentinelaTvColors.mosaicBackground),
    ) {
        val mosaicAspectRatios = state.mosaicAspectRatios(videoAspectRatios)
        val navigationLayout = remember(
            state.cameras,
            state.streamQuality,
            state.autoQualityOverrides,
            videoAspectRatios,
            maxWidth,
            maxHeight,
        ) {
            MosaicLayoutPolicy.calculate(
                cameraCount = state.cameras.size,
                availableWidth = (maxWidth.value - SentinelaTvSpacing.mosaicOuter.value * 2f).coerceAtLeast(0f),
                availableHeight = (maxHeight.value - SentinelaTvSpacing.mosaicOuter.value * 2f).coerceAtLeast(0f),
                gap = SentinelaTvSpacing.mosaicTileGap.value,
                aspectRatios = mosaicAspectRatios,
            )
        }

        if (fullscreenCamera != null) {
            val fullscreenRequest = fullscreenState.streamRequest()
            val fullscreenRtspUrl = fullscreenRequest?.let(rtspUrlBuilder::build)

            fun showFullscreenMessage(message: String) {
                fullscreenMessage = message
            }

            fun takeFullscreenPhoto() {
                val repository = captureRepository
                val camera = fullscreenState.camera
                if (repository == null || camera == null) {
                    showFullscreenMessage(photoUnavailableMessage)
                    return
                }
                coroutineScope.launch {
                    showFullscreenMessage(savingPhotoMessage)
                    val result = repository.takePhoto(
                        request = CaptureRequest(
                            cameraName = camera.name,
                            renderedFirstFrame = fullscreenRenderedFirstFrame,
                        ),
                        playerView = fullscreenPlayerView,
                    )
                    showFullscreenMessage(result.captureUserMessage().asString(context))
                }
            }

            fun startRecordingProbe() {
                val repository = recordingProbeRepository
                val camera = fullscreenState.camera
                val rtspUrl = fullscreenRtspUrl
                if (repository == null || camera == null || rtspUrl.isNullOrBlank()) {
                    showFullscreenMessage(recordingUnavailableMessage)
                    return
                }
                if (recordingJob?.isActive == true) {
                    return
                }
                val stopSignal = RecordingStopSignal()
                recordingStopSignal = stopSignal
                recordingJob = coroutineScope.launch {
                    showFullscreenMessage(recordingStartedMessage)
                    val result = repository.recordVideoProbe(
                        request = RecordingProbeRequest(
                            cameraName = camera.name,
                            rtspUrl = rtspUrl,
                        ),
                        stopSignal = stopSignal,
                    )
                    showFullscreenMessage(result.recordingUserMessage().asString(context))
                    recordingStopSignal = null
                    recordingJob = null
                }
            }

            fun stopRecordingProbe() {
                recordingStopSignal?.stop()
                showFullscreenMessage(recordingFinalizingMessage)
            }

            FullscreenCameraScreen(
                state = fullscreenState,
                rtspUrlBuilder = rtspUrlBuilder,
                onExit = {
                    recordingStopSignal?.stop()
                    fullscreenViewModel.dismissQuickMenu()
                    mosaicViewModel.closeFullscreen()
                },
                onShowQuickMenu = fullscreenViewModel::showQuickMenu,
                onDismissQuickMenu = fullscreenViewModel::dismissQuickMenu,
                onToggleAudio = fullscreenViewModel::toggleAudio,
                onToggleStreamQuality = fullscreenViewModel::toggleStreamQuality,
                onToggleInfo = fullscreenViewModel::toggleInfo,
                onToggleTransmissionMode = fullscreenViewModel::toggleTransmissionMode,
                onTakePhoto = ::takeFullscreenPhoto,
                recordingProbeActive = recordingJob?.isActive == true,
                onStartRecordingProbe = ::startRecordingProbe,
                onStopRecordingProbe = ::stopRecordingProbe,
                transientMessage = fullscreenMessage,
                onTransientMessageTimeout = { fullscreenMessage = null },
                onPlayerViewChanged = { playerView -> fullscreenPlayerView = playerView },
                onRenderedFirstFrameChanged = { rendered -> fullscreenRenderedFirstFrame = rendered },
                onOpenHome = {
                    recordingStopSignal?.stop()
                    fullscreenViewModel.dismissQuickMenu()
                    mosaicViewModel.closeFullscreen()
                    onOpenHome()
                },
                onNavigateDirection = { direction ->
                    mosaicViewModel.navigateFullscreen(direction, navigationLayout)
                },
                onExitApp = {
                    recordingStopSignal?.stop()
                    onExitApp()
                },
                onQuickMenuHintShown = fullscreenViewModel::markQuickMenuHintSeen,
                modifier = Modifier.fillMaxSize(),
            )
            return@BoxWithConstraints
        }

        if (state.isLoading) {
            LoadingMosaicMessage()
            return@BoxWithConstraints
        }

        if (state.cameras.isEmpty()) {
            EmptyMosaicMessage(
                hasRegisteredCameras = state.registeredCameraCount > 0,
                activeMosaicIndex = state.activeMosaicIndex,
                onMosaicBoundarySwitch = { target -> pendingMosaicSwitch = target },
            )
            pendingMosaicSwitch?.let { target ->
                MosaicSwitchDialog(
                    target = target,
                    onDismiss = { pendingMosaicSwitch = null },
                    onConfirm = {
                        pendingMosaicSwitch = null
                        mosaicViewModel.switchToMosaic(target.toIndex)
                    },
                )
            }
            return@BoxWithConstraints
        }

        if (state.cameras.any { it.source is DvrRtspChannel } && !dvrConfig.isConfigured()) {
            MissingDvrConfigMessage()
            return@BoxWithConstraints
        }

        MosaicGrid(
            state = state,
            rtspUrlBuilder = rtspUrlBuilder,
            onCameraClick = mosaicViewModel::onCameraClick,
            onCameraLongClick = mosaicViewModel::requestCameraDeletion,
            onMosaicHdSoftwareDecoder = mosaicViewModel::fallbackCameraToSdFromSoftwareDecoder,
            onMosaicHdDecoderFailure = mosaicViewModel::reportMosaicHdDecoderFailure,
            onVideoAspectRatioChanged = { cameraId, subtype, width, height ->
                val aspectRatio = MosaicLayoutPolicy.validatedAspectRatio(width, height)
                if (aspectRatio != null) {
                    val key = MosaicAspectRatioKey(cameraId, subtype)
                    if (videoAspectRatios[key] != aspectRatio) {
                        videoAspectRatios = videoAspectRatios + (key to aspectRatio)
                    }
                }
            },
            videoAspectRatios = videoAspectRatios,
            tilesFocusable = !state.quickMenuVisible &&
                state.cameraPendingDeletion == null &&
                pendingMosaicSwitch == null,
            showFocusIndicator = showCameraFocusIndicator || state.reorderMode,
            onDirectionalActivity = ::showCameraFocus,
            onMosaicBoundarySwitch = { target -> pendingMosaicSwitch = target },
            modifier = Modifier.fillMaxSize(),
        )

        val showMosaicQuickMenuHint = state.showQuickMenuHint &&
            !state.quickMenuVisible &&
            !state.reorderMode &&
            state.cameraPendingDeletion == null &&
            pendingMosaicSwitch == null

        LaunchedEffect(showMosaicQuickMenuHint) {
            if (showMosaicQuickMenuHint) {
                delay(MOSAIC_QUICK_MENU_HINT_DURATION_MS)
                mosaicViewModel.markQuickMenuHintSeen()
            }
        }

        if (state.reorderMode) {
            ReorderHint(
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }

        if (state.quickMenuVisible) {
            MosaicQuickMenu(
                state = state,
                onExitApp = onExitApp,
                onToggleInfo = mosaicViewModel::toggleInfo,
                onToggleStreamQuality = mosaicViewModel::toggleStreamQuality,
                onStartReorder = mosaicViewModel::startReorderMode,
                onToggleTransmissionMode = mosaicViewModel::toggleTransmissionMode,
                onOpenHome = {
                    mosaicViewModel.dismissQuickMenu()
                    onOpenHome()
                },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp),
            )
        } else if (showMosaicQuickMenuHint) {
            MosaicQuickMenuHint(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp),
            )
        }

        state.cameraPendingDeletion?.let { camera ->
            CameraDeletionDialog(
                cameraName = camera.name,
                onDismiss = mosaicViewModel::dismissCameraDeletion,
                onConfirm = mosaicViewModel::confirmCameraDeletion,
            )
        }

        pendingMosaicSwitch?.let { target ->
            MosaicSwitchDialog(
                target = target,
                onDismiss = { pendingMosaicSwitch = null },
                onConfirm = {
                    pendingMosaicSwitch = null
                    mosaicViewModel.switchToMosaic(target.toIndex)
                },
            )
        }
    }
}

@Composable
fun SentinelaCamTvScreen(
    viewModelFactory: ViewModelProvider.Factory,
    onOpenHome: () -> Unit,
    onExitApp: () -> Unit,
) {
    MosaicScreen(
        viewModelFactory = viewModelFactory,
        onOpenHome = onOpenHome,
        onExitApp = onExitApp,
    )
}

@Composable
private fun MosaicGrid(
    state: MosaicUiState,
    rtspUrlBuilder: DvrRtspUrlBuilder,
    onCameraClick: (Camera) -> Unit,
    onCameraLongClick: (Camera) -> Unit,
    onMosaicHdSoftwareDecoder: (cameraId: String, reason: String) -> Unit,
    onMosaicHdDecoderFailure: (cameraId: String, reason: String) -> Unit,
    onVideoAspectRatioChanged: (cameraId: String, subtype: Int, width: Int, height: Int) -> Unit,
    videoAspectRatios: Map<MosaicAspectRatioKey, Float>,
    tilesFocusable: Boolean,
    showFocusIndicator: Boolean,
    onDirectionalActivity: () -> Unit,
    onMosaicBoundarySwitch: (MosaicSwitchTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.padding(SentinelaTvSpacing.mosaicOuter),
    ) {
        var focusedCameraId by remember { mutableStateOf<String?>(null) }
        val aspectRatios = state.mosaicAspectRatios(videoAspectRatios)
        val layout = remember(
            state.cameras,
            state.streamQuality,
            state.autoQualityOverrides,
            videoAspectRatios,
            maxWidth,
            maxHeight,
        ) {
            MosaicLayoutPolicy.calculate(
                cameraCount = state.cameras.size,
                availableWidth = maxWidth.value,
                availableHeight = maxHeight.value,
                gap = SentinelaTvSpacing.mosaicTileGap.value,
                aspectRatios = aspectRatios,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { keyEvent ->
                    val direction = keyEvent.key.mosaicNavigationDirection() ?: return@onPreviewKeyEvent false
                    if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                    val focusedIndex = state.cameras.indexOfFirst { camera -> camera.id == focusedCameraId }
                    val switchTarget = if (
                        tilesFocusable &&
                        showFocusIndicator &&
                        !state.reorderMode &&
                        focusedIndex >= 0 &&
                        MosaicBoundaryNavigationPolicy.isBoundaryTile(layout.tiles, focusedIndex, direction)
                    ) {
                        MosaicBoundaryNavigationPolicy.switchTarget(state.activeMosaicIndex, direction)
                    } else {
                        null
                    }

                    onDirectionalActivity()
                    if (switchTarget != null) {
                        onMosaicBoundarySwitch(switchTarget)
                        true
                    } else {
                        false
                    }
                },
        ) {
            layout.tiles.forEach { tile ->
                val camera = state.cameras.getOrNull(tile.index) ?: return@forEach
                key(camera.id) {
                    val effectiveQuality = state.effectiveStreamQuality(camera.id)
                    val autoQualityDowngraded = state.isAutoQualityDowngraded(camera.id)
                    val request = remember(camera, effectiveQuality, state.transmissionMode) {
                        camera.streamRequestFor(PlayerMode.Mosaic).copy(
                            subtype = effectiveQuality.subtype,
                            transmissionMode = state.transmissionMode,
                        )
                    }
                    val rtspUrl = remember(request, rtspUrlBuilder) {
                        rtspUrlBuilder.build(request)
                    }

                    RtspCameraTile(
                        request = request,
                        rtspUrl = rtspUrl,
                        showPlayerInfo = state.showInfo,
                        autoQualityDowngraded = autoQualityDowngraded,
                        selectedForReorder = state.selectedForSwapId == camera.id,
                        requestInitialFocus = camera.id == state.cameras.firstOrNull()?.id,
                        focusEnabled = tilesFocusable,
                        showFocusIndicator = showFocusIndicator,
                        onMosaicHdSoftwareDecoder = onMosaicHdSoftwareDecoder,
                        onMosaicHdDecoderFailure = onMosaicHdDecoderFailure,
                        onVideoAspectRatioChanged = onVideoAspectRatioChanged,
                        onFocusChanged = { focused ->
                            if (focused) {
                                focusedCameraId = camera.id
                            } else if (focusedCameraId == camera.id) {
                                focusedCameraId = null
                            }
                        },
                        onClick = {
                            onCameraClick(camera)
                        },
                        onLongClick = if (state.reorderMode) {
                            { onCameraLongClick(camera) }
                        } else {
                            null
                        },
                        modifier = Modifier
                            .offset(x = tile.x.dp, y = tile.y.dp)
                            .size(width = tile.width.dp, height = tile.height.dp),
                    )
                }
            }
        }
    }
}

private fun MosaicUiState.mosaicAspectRatios(
    videoAspectRatios: Map<MosaicAspectRatioKey, Float>,
): List<Float> = cameras.map { camera ->
    val effectiveQuality = effectiveStreamQuality(camera.id)
    MosaicLayoutPolicy.aspectRatioFor(
        cameraId = camera.id,
        subtype = effectiveQuality.subtype,
        streamQuality = effectiveQuality,
        aspectRatios = videoAspectRatios,
    )
}

@Composable
private fun MosaicQuickMenu(
    state: MosaicUiState,
    onExitApp: () -> Unit,
    onToggleInfo: () -> Unit,
    onToggleStreamQuality: () -> Unit,
    onStartReorder: () -> Unit,
    onToggleTransmissionMode: () -> Unit,
    onOpenHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    QuickActionDock(
        actions = listOf(
            QuickActionDockAction(stringResource(R.string.mosaic_quick_edit), QuickActionIcon.Edit, onStartReorder, width = 136.dp),
            QuickActionDockAction(localizedStreamQualityLabel(state.streamQuality), QuickActionIcon.Video, onToggleStreamQuality, width = 116.dp),
            QuickActionDockAction(localizedInfoMenuLabel(state.showInfo), QuickActionIcon.Info, onToggleInfo, width = 128.dp),
            QuickActionDockAction(localizedTransmissionModeMenuLabel(state.transmissionMode), state.transmissionMode.quickActionModeIcon(), onToggleTransmissionMode, width = 154.dp),
            QuickActionDockAction(stringResource(R.string.mosaic_quick_home), QuickActionIcon.Home, onOpenHome, width = 126.dp),
            QuickActionDockAction(stringResource(R.string.mosaic_quick_exit_app), QuickActionIcon.Exit, onExitApp, width = 118.dp),
        ),
        modifier = modifier,
    )
}

@Composable
private fun ReorderHint(
    modifier: Modifier = Modifier,
) {
    SentinelaOverlayCard(
        text = stringResource(R.string.mosaic_reorder_hint),
        maxWidth = 860.dp,
        modifier = modifier.padding(top = 14.dp),
    )
}

@Composable
private fun MosaicQuickMenuHint(
    modifier: Modifier = Modifier,
) {
    SentinelaOverlayCard(
        text = stringResource(R.string.mosaic_quick_menu_hint),
        maxWidth = 560.dp,
        modifier = modifier,
    )
}

@Composable
private fun CameraDeletionDialog(
    cameraName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    SentinelaTvDialog(
        title = stringResource(R.string.mosaic_remove_title),
        message = "$cameraName\n\n${stringResource(R.string.mosaic_remove_message)}",
        dismissLabel = stringResource(R.string.common_cancel),
        onDismiss = onDismiss,
        confirmLabel = stringResource(R.string.common_remove),
        onConfirm = onConfirm,
    )
}

@Composable
private fun MosaicSwitchDialog(
    target: MosaicSwitchTarget,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    SentinelaTvDialog(
        title = stringResource(R.string.mosaic_switch_title),
        message = stringResource(R.string.mosaic_switch_message, target.toIndex + 1),
        dismissLabel = stringResource(R.string.common_cancel),
        onDismiss = onDismiss,
        confirmLabel = stringResource(R.string.common_open),
        onConfirm = onConfirm,
    )
}

@Composable
private fun LoadingMosaicMessage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        MosaicMessageCard(stringResource(R.string.mosaic_loading))
    }
}

@Composable
private fun EmptyMosaicMessage(
    hasRegisteredCameras: Boolean,
    activeMosaicIndex: Int,
    onMosaicBoundarySwitch: (MosaicSwitchTarget) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(hasRegisteredCameras, activeMosaicIndex) {
        if (hasRegisteredCameras) {
            focusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable(enabled = hasRegisteredCameras)
            .onPreviewKeyEvent { keyEvent ->
                if (!hasRegisteredCameras || keyEvent.type != KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent false
                }
                val direction = keyEvent.key.mosaicNavigationDirection() ?: return@onPreviewKeyEvent false
                val target = MosaicBoundaryNavigationPolicy.switchTarget(activeMosaicIndex, direction)
                    ?: return@onPreviewKeyEvent false
                onMosaicBoundarySwitch(target)
                true
            },
        contentAlignment = Alignment.Center,
    ) {
        MosaicMessageCard(
            if (hasRegisteredCameras) {
                stringResource(R.string.mosaic_empty_active)
            } else {
                stringResource(R.string.mosaic_empty_no_cameras)
            },
        )
    }
}

@Composable
private fun MissingDvrConfigMessage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        MosaicMessageCard(
            message = stringResource(R.string.mosaic_missing_dvr_config),
        )
    }
}

@Composable
private fun MosaicMessageCard(
    message: String,
    modifier: Modifier = Modifier,
) {
    SentinelaOverlayCard(
        text = message,
        maxWidth = 720.dp,
        modifier = modifier,
    )
}

private fun Key.isDirectionalKey(): Boolean =
    this == Key.DirectionLeft ||
        this == Key.DirectionRight ||
        this == Key.DirectionUp ||
        this == Key.DirectionDown

private fun Key.mosaicNavigationDirection(): MosaicNavigationDirection? = when (this) {
    Key.DirectionUp -> MosaicNavigationDirection.Up
    Key.DirectionDown -> MosaicNavigationDirection.Down
    Key.DirectionLeft -> MosaicNavigationDirection.Left
    Key.DirectionRight -> MosaicNavigationDirection.Right
    else -> null
}
