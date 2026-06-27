package com.sentinela.camtv.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentinela.camtv.data.camera.CameraRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppDestination {
    Loading,
    Home,
    Mosaic,
    Cameras,
    Captures,
    Settings,
}

data class AppUiState(
    val destination: AppDestination = AppDestination.Loading,
    val hasCameras: Boolean = false,
)

class AppViewModel(
    private val cameraRepository: CameraRepository,
) : ViewModel() {
    private val navigator = AppNavigator()
    private val _state = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val hasCameras = cameraRepository.hasEnabledCameras()
            navigator.initialize(hasCameras)
            publishNavigationState()
        }
        viewModelScope.launch {
            cameraRepository.observeEnabledCameras().collect { cameras ->
                navigator.setCameraAvailability(cameras.isNotEmpty())
                publishNavigationState()
            }
        }
    }

    fun openHome() {
        navigator.openHome()
        publishNavigationState()
    }

    fun openMosaic() {
        navigator.openMosaic()
        publishNavigationState()
    }

    fun openCameras() {
        navigator.openCameras()
        publishNavigationState()
    }

    fun openCaptures() {
        navigator.openCaptures()
        publishNavigationState()
    }

    fun openSettings() {
        navigator.openSettings()
        publishNavigationState()
    }

    fun goBack() {
        navigator.goBack()
        publishNavigationState()
    }

    private fun publishNavigationState() {
        _state.value = navigator.state
    }
}
