package com.sentinela.camtv.ui.mosaic

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
    var freeLimitDialogDismissed by remember { mutableStateOf(false) }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SentinelaTvColors.mosaicBackground)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key.isDirectionalKey()) {
                    showCameraFocusIndicator = true
                    focusActivityToken += 1
                }
                false
            },
    ) {
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
                onExitApp = onExitApp,
                onQuickMenuHintShown = fullscreenViewModel::markQuickMenuHintSeen,
                modifier = Modifier.fillMaxSize(),
            )
            return@Box
        }

        if (state.isLoading) {
            LoadingMosaicMessage()
            return@Box
        }

        if (state.cameras.isEmpty()) {
            EmptyMosaicMessage()
            return@Box
        }

        if (state.cameras.any { it.source is DvrRtspChannel } && !dvrConfig.isConfigured()) {
            MissingDvrConfigMessage()
            return@Box
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
            tilesFocusable = !state.quickMenuVisible && state.cameraPendingDeletion == null,
            showFocusIndicator = showCameraFocusIndicator || state.reorderMode,
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

        if (
            state.freeLimitActive &&
            state.hiddenByFreeLimitCount > 0 &&
            !state.quickMenuVisible &&
            state.cameraPendingDeletion == null &&
            !freeLimitDialogDismissed
        ) {
            FreeLimitDialog(
                hiddenCameraCount = state.hiddenByFreeLimitCount,
                onDismiss = { freeLimitDialogDismissed = true },
                onOpenSettings = {
                    freeLimitDialogDismissed = true
                    onOpenSettings()
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
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.padding(SentinelaTvSpacing.mosaicOuter),
    ) {
        val aspectRatios = state.cameras.map { camera ->
            val effectiveQuality = state.effectiveStreamQuality(camera.id)
            MosaicLayoutPolicy.aspectRatioFor(
                cameraId = camera.id,
                subtype = effectiveQuality.subtype,
                streamQuality = effectiveQuality,
                aspectRatios = videoAspectRatios,
            )
        }
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
        title = MosaicUiText.DELETE_CAMERA_CONFIRMATION,
        message = cameraName,
        dismissLabel = "Cancelar",
        onDismiss = onDismiss,
        confirmLabel = "Excluir",
        onConfirm = onConfirm,
    )
}

@Composable
private fun FreeLimitDialog(
    hiddenCameraCount: Int,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    SentinelaTvDialog(
        title = "Modo grátis",
        message = "O modo grátis permite 1 câmera ativa. Assine para liberar o mosaico completo. Câmeras ocultas agora: $hiddenCameraCount.",
        dismissLabel = "Continuar",
        onDismiss = onDismiss,
        confirmLabel = "Assinar",
        onConfirm = onOpenSettings,
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
private fun EmptyMosaicMessage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        MosaicMessageCard("Nenhuma câmera cadastrada.")
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
