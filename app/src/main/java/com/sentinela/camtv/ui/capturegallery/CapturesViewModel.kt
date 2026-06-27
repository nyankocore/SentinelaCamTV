package com.sentinela.camtv.ui.capturegallery

import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentinela.camtv.capture.CaptureLocationLabels
import com.sentinela.camtv.preferences.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CapturesUiState(
    val photoLocationLabel: String = CaptureLocationLabels.defaultPhotoLocationLabel(Build.VERSION.SDK_INT),
    val photoLocationDescription: String = defaultLocationDescription(Build.VERSION.SDK_INT),
    val usingCustomLocation: Boolean = false,
    val message: String? = null,
)

class CapturesViewModel(
    private val settingsRepository: SettingsRepository,
    private val customPhotoLocationEnabled: Boolean = true,
) : ViewModel() {
    private val message = MutableStateFlow<String?>(null)

    val state: StateFlow<CapturesUiState> = combine(
        settingsRepository.observePreferences(),
        message,
    ) { preferences, currentMessage ->
        val usingCustom = customPhotoLocationEnabled && preferences.photoCaptureTreeUri != null
        CapturesUiState(
            photoLocationLabel = if (usingCustom) {
                CaptureLocationLabels.CUSTOM_PHOTOS
            } else {
                CaptureLocationLabels.defaultPhotoLocationLabel(Build.VERSION.SDK_INT)
            },
            photoLocationDescription = if (usingCustom) {
                "Fotos serão salvas na pasta escolhida."
            } else {
                defaultLocationDescription(Build.VERSION.SDK_INT)
            },
            usingCustomLocation = usingCustom,
            message = currentMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CapturesUiState(),
    )

    fun setCustomPhotoLocation(uri: Uri) {
        viewModelScope.launch {
            settingsRepository.setPhotoCaptureTreeUri(uri.toString())
            message.value = "Local das fotos atualizado."
        }
    }

    fun showCustomPhotoLocationError() {
        message.value = "Não foi possível manter acesso a essa pasta."
    }

    fun showPhotoLocationPickerUnavailable() {
        message.value = "Seletor de pasta indisponível neste Android TV. Use o local padrão."
    }

    fun useDefaultPhotoLocation() {
        viewModelScope.launch {
            settingsRepository.setPhotoCaptureTreeUri(null)
            message.value = "Local padrão restaurado."
        }
    }

    fun clearMessage() {
        message.value = null
    }
}

internal fun defaultLocationDescription(sdkInt: Int): String =
    if (sdkInt >= 29) {
        "As fotos aparecem na pasta de imagens do Android."
    } else {
        "Neste Android, as fotos ficam na pasta de imagens do app."
    }
