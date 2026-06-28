package com.sentinela.camtv.ui.settings

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentinela.camtv.BuildConfig
import com.sentinela.camtv.R
import com.sentinela.camtv.config.ProjectLinks
import com.sentinela.camtv.data.update.AppUpdateInstallResult
import com.sentinela.camtv.data.update.AppUpdateInstaller
import com.sentinela.camtv.data.update.AvailableUpdate
import com.sentinela.camtv.data.update.DownloadedUpdate
import com.sentinela.camtv.data.update.UpdateCheckResult
import com.sentinela.camtv.data.update.UpdateRepository
import com.sentinela.camtv.localization.AppLanguage
import com.sentinela.camtv.logging.LogRepository
import com.sentinela.camtv.preferences.SettingsRepository
import com.sentinela.camtv.ui.text.UiText
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val exportMessage: UiText? = null,
    val updateMessage: UiText? = null,
    val showUpdateDialog: Boolean = false,
    val checkingForUpdate: Boolean = false,
    val downloadingUpdate: Boolean = false,
    val availableUpdate: AvailableUpdate? = null,
    val downloadedUpdate: DownloadedUpdate? = null,
    val versionName: String = BuildConfig.VERSION_NAME,
    val license: String = "GPL-3.0-or-later",
    val siteUrl: String = ProjectLinks.SITE_URL,
    val appLanguage: AppLanguage = AppLanguage.System,
)

class SettingsViewModel(
    private val logRepository: LogRepository,
    private val updateRepository: UpdateRepository,
    private val appUpdateInstaller: AppUpdateInstaller,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val exportMessage = MutableStateFlow<UiText?>(null)
    private val updateState = MutableStateFlow(UpdateUiState())

    val state: StateFlow<SettingsUiState> = combine(
        exportMessage,
        updateState,
        settingsRepository.observePreferences(),
    ) { export, update, preferences ->
        SettingsUiState(
            exportMessage = export,
            updateMessage = update.message,
            showUpdateDialog = update.showDialog,
            checkingForUpdate = update.checking,
            downloadingUpdate = update.downloading,
            availableUpdate = update.availableUpdate,
            downloadedUpdate = update.downloadedUpdate,
            appLanguage = AppLanguage.fromTag(preferences.appLanguageTag),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun exportSupportLogs() {
        exportFile { logRepository.exportSupportLogs() }
    }

    fun exportCrashReport() {
        exportFile { logRepository.exportCrashReport() }
    }

    fun clearExportMessage() {
        exportMessage.value = null
    }

    fun checkForUpdate() {
        if (updateState.value.checking || updateState.value.downloading) return

        viewModelScope.launch {
            updateState.value = UpdateUiState(
                checking = true,
                showDialog = true,
                message = UiText.Resource(R.string.update_checking),
            )
            val result = updateRepository.checkForUpdate(
                currentVersionName = BuildConfig.VERSION_NAME,
                supportedAbis = Build.SUPPORTED_ABIS.toList(),
            ).getOrElse { error ->
                updateState.value = UpdateUiState(
                    showDialog = true,
                    message = UiText.Resource(
                        R.string.update_check_failed,
                        listOf(error.message ?: "erro desconhecido"),
                    ),
                )
                return@launch
            }

            updateState.value = when (result) {
                is UpdateCheckResult.Available -> updateStateForAvailableUpdate(result.update)
                UpdateCheckResult.UpToDate -> UpdateUiState(
                    showDialog = true,
                    message = UiText.Resource(R.string.update_up_to_date),
                )
            }
        }
    }

    fun downloadUpdate() {
        val update = updateState.value.availableUpdate ?: return
        if (updateState.value.downloading) return

        viewModelScope.launch {
            updateState.value = updateState.value.copy(
                downloading = true,
                showDialog = true,
                message = UiText.Resource(R.string.update_downloading),
            )
            updateRepository.downloadUpdate(update).fold(
                onSuccess = { downloaded ->
                    updateState.value = UpdateUiState(
                        showDialog = true,
                        message = UiText.Resource(R.string.update_downloaded_opening_installer),
                        availableUpdate = update,
                        downloadedUpdate = downloaded,
                    )
                    openInstaller(downloaded)
                },
                onFailure = { error ->
                    updateState.value = updateState.value.copy(
                        downloading = false,
                        showDialog = true,
                        message = UiText.Resource(
                            R.string.update_download_failed,
                            listOf(error.message ?: "erro desconhecido"),
                        ),
                    )
                },
            )
        }
    }

    fun installDownloadedUpdate() {
        val downloaded = updateState.value.downloadedUpdate ?: return
        updateState.value = updateState.value.copy(showDialog = true)
        openInstaller(downloaded)
    }

    fun retryInstallerAfterPermissionResume() {
        val current = updateState.value
        val downloaded = current.downloadedUpdate ?: return
        if (!UpdateUiStateReducer.shouldRetryInstallerOnResume(
                state = current,
                canRequestPackageInstalls = appUpdateInstaller.canRequestPackageInstalls(),
            )
        ) {
            return
        }

        updateState.value = current.copy(
            showDialog = true,
            message = UiText.Resource(R.string.update_permission_granted),
        )
        openInstaller(downloaded)
    }

    fun dismissUpdateDialog() {
        updateState.value = updateState.value.copy(showDialog = false)
    }

    fun selectAppLanguage(language: AppLanguage) {
        viewModelScope.launch {
            settingsRepository.setAppLanguageTag(language.storageTag)
        }
    }

    private suspend fun updateStateForAvailableUpdate(update: AvailableUpdate): UpdateUiState {
        val downloaded = updateRepository.findDownloadedUpdate(update).getOrNull()
        return if (downloaded != null) {
            UpdateUiState(
                showDialog = true,
                message = UiText.Resource(R.string.update_already_downloaded),
                availableUpdate = update,
                downloadedUpdate = downloaded,
            )
        } else {
            UpdateUiState(
                showDialog = true,
                message = UiText.Resource(R.string.update_version_available, listOf(update.versionName)),
                availableUpdate = update,
            )
        }
    }

    private fun exportFile(block: suspend () -> Result<File>) {
        viewModelScope.launch {
            exportMessage.value = UiText.Resource(R.string.support_exporting)
            exportMessage.value = block()
                .fold(
                    onSuccess = SupportExportMessage::forExportedFile,
                    onFailure = { error ->
                        UiText.Resource(
                            R.string.support_export_failed,
                            listOf(error.message ?: "erro desconhecido"),
                        )
                    },
                )
        }
    }

    private fun openInstaller(downloaded: DownloadedUpdate) {
        updateState.value = UpdateUiStateReducer.afterInstallResult(
            current = updateState.value,
            downloaded = downloaded,
            result = appUpdateInstaller.openInstaller(downloaded),
        )
    }
}

internal data class UpdateUiState(
    val message: UiText? = null,
    val showDialog: Boolean = false,
    val checking: Boolean = false,
    val downloading: Boolean = false,
    val availableUpdate: AvailableUpdate? = null,
    val downloadedUpdate: DownloadedUpdate? = null,
    val waitingForInstallPermission: Boolean = false,
)

internal object UpdateUiStateReducer {
    fun afterInstallResult(
        current: UpdateUiState,
        downloaded: DownloadedUpdate,
        result: AppUpdateInstallResult,
    ): UpdateUiState =
        when (result) {
            AppUpdateInstallResult.InstallerOpened -> current.copy(
                downloading = false,
                showDialog = true,
                message = UiText.Resource(R.string.update_installer_opened),
                downloadedUpdate = downloaded,
                waitingForInstallPermission = false,
            )

            AppUpdateInstallResult.PermissionRequired -> current.copy(
                downloading = false,
                showDialog = true,
                message = UiText.Resource(R.string.update_permission_required),
                downloadedUpdate = downloaded,
                waitingForInstallPermission = true,
            )

            is AppUpdateInstallResult.Failed -> current.copy(
                downloading = false,
                showDialog = true,
                message = UiText.Raw(result.message),
                downloadedUpdate = downloaded,
                waitingForInstallPermission = false,
            )
        }

    fun shouldRetryInstallerOnResume(
        state: UpdateUiState,
        canRequestPackageInstalls: Boolean,
    ): Boolean =
        state.waitingForInstallPermission &&
            state.downloadedUpdate != null &&
            !state.checking &&
            !state.downloading &&
            canRequestPackageInstalls
}
