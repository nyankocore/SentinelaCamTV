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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sentinela.camtv.config.AppDvrConfig
import com.sentinela.camtv.config.DvrConnectionConfig
import com.sentinela.camtv.config.isConfigured
import com.sentinela.camtv.domain.Camera
import com.sentinela.camtv.domain.DvrRtspChannel
import com.sentinela.camtv.player.DvrRtspUrlBuilder
import com.sentinela.camtv.player.PlayerMode
import com.sentinela.camtv.player.streamRequestFor
import com.sentinela.camtv.ui.common.QuickMenu
import com.sentinela.camtv.ui.common.QuickMenuAction
import com.sentinela.camtv.ui.design.SentinelaOverlayCard
import com.sentinela.camtv.ui.design.SentinelaTvColors
import com.sentinela.camtv.ui.design.SentinelaTvDialog
import com.sentinela.camtv.ui.design.SentinelaTvSpacing
import com.sentinela.camtv.ui.labels.infoMenuLabel
import com.sentinela.camtv.ui.labels.streamQualityLabel
import com.sentinela.camtv.ui.labels.transmissionModeMenuLabel
import com.sentinela.camtv.ui.player.FullscreenCameraScreen
import com.sentinela.camtv.ui.player.FullscreenPlayerViewModel
import kotlinx.coroutines.delay

private const val CAMERA_FOCUS_HIDE_DELAY_MS = 5_000L

@Composable
fun MosaicScreen(
    viewModelFactory: ViewModelProvider.Factory,
    onOpenHome: () -> Unit,
    onOpenSettings: () -> Unit,
    onExitApp: () -> Unit,
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
        fullscreenCamera?.let(fullscreenViewModel::open)
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
            FullscreenCameraScreen(
                state = fullscreenState,
                rtspUrlBuilder = rtspUrlBuilder,
                onExit = {
                    fullscreenViewModel.dismissQuickMenu()
                    mosaicViewModel.closeFullscreen()
                },
                onShowQuickMenu = fullscreenViewModel::showQuickMenu,
                onDismissQuickMenu = fullscreenViewModel::dismissQuickMenu,
                onToggleAudio = fullscreenViewModel::toggleAudio,
                onToggleStreamQuality = fullscreenViewModel::toggleStreamQuality,
                onToggleInfo = fullscreenViewModel::toggleInfo,
                onToggleTransmissionMode = fullscreenViewModel::toggleTransmissionMode,
                onOpenHome = {
                    fullscreenViewModel.dismissQuickMenu()
                    mosaicViewModel.closeFullscreen()
                    onOpenHome()
                },
                onOpenSettings = {
                    fullscreenViewModel.dismissQuickMenu()
                    mosaicViewModel.closeFullscreen()
                    onOpenSettings()
                },
                onNavigateDirection = { direction ->
                    mosaicViewModel.navigateFullscreen(direction, navigationLayout)
                },
                onExitApp = onExitApp,
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
                onOpenSettings = {
                    mosaicViewModel.dismissQuickMenu()
                    onOpenSettings()
                },
                modifier = Modifier.align(Alignment.Center),
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
    onOpenSettings: () -> Unit,
    onExitApp: () -> Unit,
) {
    MosaicScreen(
        viewModelFactory = viewModelFactory,
        onOpenHome = onOpenHome,
        onOpenSettings = onOpenSettings,
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
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    QuickMenu(
        actions = listOf(
            QuickMenuAction("Sair do app", onExitApp),
            QuickMenuAction(infoMenuLabel(state.showInfo), onToggleInfo),
            QuickMenuAction(streamQualityLabel(state.streamQuality), onToggleStreamQuality),
            QuickMenuAction("Editar mosaico", onStartReorder),
            QuickMenuAction(transmissionModeMenuLabel(state.transmissionMode), onToggleTransmissionMode),
            QuickMenuAction("Ir para início", onOpenHome),
            QuickMenuAction("Ir para suporte", onOpenSettings),
        ),
        modifier = modifier,
    )
}

@Composable
private fun ReorderHint(
    modifier: Modifier = Modifier,
) {
    SentinelaOverlayCard(
        text = MosaicUiText.REORDER_HINT,
        maxWidth = 860.dp,
        modifier = modifier.padding(top = 14.dp),
    )
}

@Composable
private fun CameraDeletionDialog(
    cameraName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    SentinelaTvDialog(
        title = MosaicUiText.REMOVE_CAMERA_FROM_MOSAIC_CONFIRMATION,
        message = "$cameraName\n\n${MosaicUiText.REMOVE_CAMERA_FROM_MOSAIC_MESSAGE}",
        dismissLabel = "Cancelar",
        onDismiss = onDismiss,
        confirmLabel = "Remover",
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
        title = "Trocar mosaico?",
        message = "Abrir Mosaico ${target.toIndex + 1}. O mosaico atual será fechado para preservar desempenho.",
        dismissLabel = "Cancelar",
        onDismiss = onDismiss,
        confirmLabel = "Abrir",
        onConfirm = onConfirm,
    )
}

@Composable
private fun LoadingMosaicMessage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        MosaicMessageCard("Carregando câmeras...")
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
                "Mosaico ativo vazio. Use esquerda ou direita para trocar de mosaico."
            } else {
                "Nenhuma câmera cadastrada."
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
            message = "Configure sentinela.dvr.host no local.properties para testar canais DVR locais.",
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
