package com.sentinela.camtv.ui.capturegallery

import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentinela.camtv.R
import com.sentinela.camtv.capture.CaptureLocationLabels
import com.sentinela.camtv.preferences.SettingsRepository
import com.sentinela.camtv.ui.text.UiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CapturesUiState(
    val photoLocationLabel: UiText = defaultPhotoLocationLabelText(Build.VERSION.SDK_INT),
    val photoLocationDescription: UiText = defaultLocationDescriptionText(Build.VERSION.SDK_INT),
    val usingCustomLocation: Boolean = false,
    val message: UiText? = null,
)

class CapturesViewModel(
    private val settingsRepository: SettingsRepository,
    private val customPhotoLocationEnabled: Boolean = true,
) : ViewModel() {
    private val message = MutableStateFlow<UiText?>(null)

    val state: StateFlow<CapturesUiState> = combine(
        settingsRepository.observePreferences(),
        message,
    ) { preferences, currentMessage ->
        val usingCustom = customPhotoLocationEnabled && preferences.photoCaptureTreeUri != null
            CapturesUiState(
                photoLocationLabel = if (usingCustom) {
                    UiText.Resource(R.string.capture_location_custom_photos)
                } else {
                    defaultPhotoLocationLabelText(Build.VERSION.SDK_INT)
                },
                photoLocationDescription = if (usingCustom) {
                    UiText.Resource(R.string.captures_custom_location_description)
                } else {
                    defaultLocationDescriptionText(Build.VERSION.SDK_INT)
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
            message.value = UiText.Resource(R.string.captures_location_updated)
        }
    }

    fun showCustomPhotoLocationError() {
        message.value = UiText.Resource(R.string.captures_location_access_failed)
    }

    fun showPhotoLocationPickerUnavailable() {
        message.value = UiText.Resource(R.string.captures_folder_picker_unavailable)
    }

    fun useDefaultPhotoLocation() {
        viewModelScope.launch {
            settingsRepository.setPhotoCaptureTreeUri(null)
            message.value = UiText.Resource(R.string.captures_default_location_restored)
        }
    }

    fun clearMessage() {
        message.value = null
    }
}

internal fun defaultPhotoLocationLabelText(sdkInt: Int): UiText =
    if (sdkInt >= 29) {
        UiText.Resource(R.string.capture_location_standard_photos)
    } else {
        UiText.Resource(R.string.capture_location_app_external_photos)
    }

internal fun defaultLocationDescriptionText(sdkInt: Int): UiText =
    if (sdkInt >= 29) {
        UiText.Resource(R.string.captures_default_location_public_description)
    } else {
        UiText.Resource(R.string.captures_default_location_app_description)
    }

internal fun defaultLocationDescription(sdkInt: Int): String =
    if (sdkInt >= 29) {
        "As fotos aparecem na pasta de imagens do Android."
    } else {
        "Neste Android, as fotos ficam na pasta de imagens do app."
    }
