package com.sentinela.camtv.ui.mosaic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentinela.camtv.data.camera.CameraRepository
import com.sentinela.camtv.domain.Camera
import com.sentinela.camtv.player.StreamQuality
import com.sentinela.camtv.player.TransmissionMode
import com.sentinela.camtv.player.next
import com.sentinela.camtv.preferences.PlayerUiPreferences
import com.sentinela.camtv.preferences.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

data class MosaicUiState(
    val cameras: List<Camera> = emptyList(),
    val isLoading: Boolean = true,
    val showInfo: Boolean = true,
    val quickMenuVisible: Boolean = false,
    val reorderMode: Boolean = false,
    val selectedForSwapId: String? = null,
    val cameraPendingDeletion: Camera? = null,
    val fullscreenCamera: Camera? = null,
    val streamQuality: StreamQuality = StreamQuality.SD,
    val autoQualityOverrides: Map<String, StreamQuality> = emptyMap(),
    val transmissionMode: TransmissionMode = TransmissionMode.MENOR_LATENCIA,
    val preferences: PlayerUiPreferences = PlayerUiPreferences(),
) {
    fun effectiveStreamQuality(cameraId: String): StreamQuality =
        autoQualityOverrides[cameraId] ?: streamQuality

    fun isAutoQualityDowngraded(cameraId: String): Boolean =
        streamQuality == StreamQuality.HD && autoQualityOverrides[cameraId] == StreamQuality.SD
}

private data class MosaicCoreState(
    val cameras: List<Camera>,
    val isLoading: Boolean,
    val preferences: PlayerUiPreferences,
    val quickMenuVisible: Boolean,
    val reorderMode: Boolean,
    val selectedForSwapId: String?,
)

private sealed interface CameraListState {
    data object Loading : CameraListState
    data class Loaded(val cameras: List<Camera>) : CameraListState
}

class MosaicViewModel(
    private val cameraRepository: CameraRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val quickMenuVisible = MutableStateFlow(false)
    private val reorderMode = MutableStateFlow(false)
    private val selectedForSwapId = MutableStateFlow<String?>(null)
    private val cameraPendingDeletionId = MutableStateFlow<String?>(null)
    private val fullscreenCameraId = MutableStateFlow<String?>(null)
    private val autoQualityOverrides = MutableStateFlow<Map<String, StreamQuality>>(emptyMap())
    private val hdDecoderFailureCounts = mutableMapOf<String, Int>()
    private val cameraListState = cameraRepository.observeEnabledCameras()
        .map<List<Camera>, CameraListState> { cameras -> CameraListState.Loaded(cameras) }
        .onStart { emit(CameraListState.Loading) }

    private val coreState = combine(
        cameraListState,
        settingsRepository.observePreferences(),
        quickMenuVisible,
        reorderMode,
        selectedForSwapId,
    ) { cameraState, preferences, menuVisible, reorder, selectedId ->
        val cameras = when (cameraState) {
            is CameraListState.Loaded -> cameraState.cameras
            CameraListState.Loading -> emptyList()
        }
        MosaicCoreState(
            cameras = cameras,
            isLoading = cameraState == CameraListState.Loading,
            preferences = preferences,
            quickMenuVisible = menuVisible,
            reorderMode = reorder,
            selectedForSwapId = selectedId,
        )
    }

    val state: StateFlow<MosaicUiState> = combine(
        coreState,
        fullscreenCameraId,
        cameraPendingDeletionId,
        autoQualityOverrides,
    ) { core, fullscreenId, pendingDeletionId, qualityOverrides ->
        val validCameraIds = core.cameras.mapTo(mutableSetOf()) { it.id }
        val validOverrides = qualityOverrides.filterKeys { it in validCameraIds }
        MosaicUiState(
            cameras = core.cameras,
            isLoading = core.isLoading,
            showInfo = core.preferences.showMosaicInfo,
            quickMenuVisible = core.quickMenuVisible,
            reorderMode = core.reorderMode,
            selectedForSwapId = core.selectedForSwapId,
            cameraPendingDeletion = core.cameras.firstOrNull { it.id == pendingDeletionId },
            fullscreenCamera = core.cameras.firstOrNull { it.id == fullscreenId },
            streamQuality = core.preferences.mosaicStreamQuality,
            autoQualityOverrides = validOverrides,
            transmissionMode = core.preferences.globalTransmissionMode,
            preferences = core.preferences,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MosaicUiState(),
    )

    fun onBackPressed() {
        when {
            cameraPendingDeletionId.value != null -> cameraPendingDeletionId.value = null
            quickMenuVisible.value -> quickMenuVisible.value = false
            reorderMode.value -> {
                reorderMode.value = false
                selectedForSwapId.value = null
            }
            else -> quickMenuVisible.value = true
        }
    }

    fun dismissQuickMenu() {
        quickMenuVisible.value = false
    }

    fun onCameraClick(camera: Camera) {
        if (!reorderMode.value) {
            fullscreenCameraId.value = camera.id
            return
        }

        val firstId = selectedForSwapId.value
        if (firstId == null) {
            selectedForSwapId.value = camera.id
            return
        }
        if (firstId == camera.id) {
            selectedForSwapId.value = null
            return
        }

        val reordered = state.value.cameras.toMutableList()
        val firstIndex = reordered.indexOfFirst { it.id == firstId }
        val secondIndex = reordered.indexOfFirst { it.id == camera.id }
        if (firstIndex >= 0 && secondIndex >= 0) {
            val temp = reordered[firstIndex]
            reordered[firstIndex] = reordered[secondIndex]
            reordered[secondIndex] = temp
            viewModelScope.launch {
                cameraRepository.updateCameraOrder(reordered.map { it.id })
            }
        }
        selectedForSwapId.value = null
    }

    fun closeFullscreen() {
        fullscreenCameraId.value = null
    }

    internal fun navigateFullscreen(
        direction: MosaicNavigationDirection,
        layout: MosaicLayout,
    ) {
        val currentId = fullscreenCameraId.value ?: return
        val currentIndex = state.value.cameras.indexOfFirst { camera -> camera.id == currentId }
        if (currentIndex < 0) return

        val targetIndex = MosaicDirectionalNavigationPolicy.targetIndex(
            tiles = layout.tiles,
            currentIndex = currentIndex,
            direction = direction,
        ) ?: return
        val targetCamera = state.value.cameras.getOrNull(targetIndex) ?: return
        fullscreenCameraId.value = targetCamera.id
    }

    fun toggleInfo() {
        viewModelScope.launch {
            settingsRepository.setShowMosaicInfo(!state.value.showInfo)
        }
    }

    fun toggleStreamQuality() {
        hdDecoderFailureCounts.clear()
        autoQualityOverrides.value = emptyMap()
        viewModelScope.launch {
            settingsRepository.setMosaicStreamQuality(state.value.streamQuality.next())
        }
    }

    fun fallbackCameraToSdFromSoftwareDecoder(cameraId: String, decoderName: String) {
        applyAutoSdFallback(
            cameraId = cameraId,
            reason = "decoder software em HD no mosaico: $decoderName",
        )
    }

    fun reportMosaicHdDecoderFailure(cameraId: String, reason: String) {
        if (state.value.streamQuality != StreamQuality.HD) {
            return
        }
        if (autoQualityOverrides.value[cameraId] == StreamQuality.SD) {
            return
        }

        val failureCount = (hdDecoderFailureCounts[cameraId] ?: 0) + 1
        hdDecoderFailureCounts[cameraId] = failureCount
        Timber.tag("SentinelaPlayer").w(
            "cameraId=$cameraId falha de decoder HD no mosaico $failureCount/" +
                "${MosaicAutoQualityPolicy.DECODER_FAILURES_BEFORE_SD}: $reason",
        )

        if (MosaicAutoQualityPolicy.shouldFallbackToSdAfterDecoderFailures(failureCount)) {
            applyAutoSdFallback(
                cameraId = cameraId,
                reason = "falhas repetidas de decoder em HD: $reason",
            )
        }
    }

    private fun applyAutoSdFallback(cameraId: String, reason: String) {
        if (state.value.streamQuality != StreamQuality.HD) {
            return
        }
        if (autoQualityOverrides.value[cameraId] == StreamQuality.SD) {
            return
        }
        hdDecoderFailureCounts.remove(cameraId)
        Timber.tag("SentinelaPlayer").w(
            "cameraId=$cameraId HD no mosaico alternado para SD automaticamente: $reason",
        )
        autoQualityOverrides.value = autoQualityOverrides.value + (cameraId to StreamQuality.SD)
    }

    fun startReorderMode() {
        quickMenuVisible.value = false
        reorderMode.value = true
        selectedForSwapId.value = null
    }

    fun requestCameraDeletion(camera: Camera) {
        if (reorderMode.value) {
            cameraPendingDeletionId.value = camera.id
        }
    }

    fun dismissCameraDeletion() {
        cameraPendingDeletionId.value = null
    }

    fun confirmCameraDeletion() {
        val cameraId = cameraPendingDeletionId.value ?: return
        cameraPendingDeletionId.value = null
        if (selectedForSwapId.value == cameraId) {
            selectedForSwapId.value = null
        }
        if (fullscreenCameraId.value == cameraId) {
            fullscreenCameraId.value = null
        }
        hdDecoderFailureCounts.remove(cameraId)
        autoQualityOverrides.value = autoQualityOverrides.value - cameraId
        viewModelScope.launch {
            cameraRepository.deleteCamera(cameraId)
        }
    }

    fun toggleTransmissionMode() {
        viewModelScope.launch {
            settingsRepository.setGlobalTransmissionMode(state.value.transmissionMode.next())
        }
    }
}
