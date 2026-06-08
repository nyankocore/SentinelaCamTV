package com.sentinela.camtv.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentinela.camtv.BuildConfig
import com.sentinela.camtv.preferences.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val message: String? = null,
    val diagnosticsEnabled: Boolean = true,
    val versionName: String = BuildConfig.VERSION_NAME,
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val message = MutableStateFlow<String?>(null)

    val state: StateFlow<SettingsUiState> = combine(
        message,
        settingsRepository.observePreferences(),
    ) { currentMessage, preferences ->
        SettingsUiState(
            message = currentMessage,
            diagnosticsEnabled = preferences.diagnosticsEnabled,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun toggleDiagnostics() {
        viewModelScope.launch {
            val nextEnabled = !state.value.diagnosticsEnabled
            settingsRepository.setDiagnosticsEnabled(nextEnabled)
            message.value = if (nextEnabled) {
                "Diagnóstico automático ativado."
            } else {
                "Diagnóstico automático desativado."
            }
        }
    }
}
