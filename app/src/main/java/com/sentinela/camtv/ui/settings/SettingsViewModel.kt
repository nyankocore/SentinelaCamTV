package com.sentinela.camtv.ui.settings

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentinela.camtv.BuildConfig
import com.sentinela.camtv.config.ProjectLinks
import com.sentinela.camtv.data.update.AppUpdateInstallResult
import com.sentinela.camtv.data.update.AppUpdateInstaller
import com.sentinela.camtv.data.update.AvailableUpdate
import com.sentinela.camtv.data.update.DownloadedUpdate
import com.sentinela.camtv.data.update.UpdateCheckResult
import com.sentinela.camtv.data.update.UpdateRepository
import com.sentinela.camtv.logging.LogRepository
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val exportMessage: String? = null,
    val updateMessage: String? = null,
    val showUpdateDialog: Boolean = false,
    val checkingForUpdate: Boolean = false,
    val downloadingUpdate: Boolean = false,
    val availableUpdate: AvailableUpdate? = null,
    val downloadedUpdate: DownloadedUpdate? = null,
    val versionName: String = BuildConfig.VERSION_NAME,
    val license: String = "GPL-3.0-or-later",
    val siteUrl: String = ProjectLinks.SITE_URL,
)

class SettingsViewModel(
    private val logRepository: LogRepository,
    private val updateRepository: UpdateRepository,
    private val appUpdateInstaller: AppUpdateInstaller,
) : ViewModel() {
    private val exportMessage = MutableStateFlow<String?>(null)
    private val updateState = MutableStateFlow(UpdateUiState())

    val state: StateFlow<SettingsUiState> = combine(
        exportMessage,
        updateState,
    ) { export, update ->
        SettingsUiState(
            exportMessage = export,
            updateMessage = update.message,
            showUpdateDialog = update.showDialog,
            checkingForUpdate = update.checking,
            downloadingUpdate = update.downloading,
            availableUpdate = update.availableUpdate,
            downloadedUpdate = update.downloadedUpdate,
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
                message = "Buscando atualização...",
            )
            val result = updateRepository.checkForUpdate(
                currentVersionName = BuildConfig.VERSION_NAME,
                supportedAbis = Build.SUPPORTED_ABIS.toList(),
            ).getOrElse { error ->
                updateState.value = UpdateUiState(
                    showDialog = true,
                    message = "Falha ao buscar atualização: ${error.message ?: "erro desconhecido"}",
                )
                return@launch
            }

            updateState.value = when (result) {
                is UpdateCheckResult.Available -> updateStateForAvailableUpdate(result.update)
                UpdateCheckResult.UpToDate -> UpdateUiState(
                    showDialog = true,
                    message = "Você já está na versão mais recente.",
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
                message = "Baixando atualização...",
            )
            updateRepository.downloadUpdate(update).fold(
                onSuccess = { downloaded ->
                    updateState.value = UpdateUiState(
                        showDialog = true,
                        message = "Atualização baixada. Abrindo instalador...",
                        availableUpdate = update,
                        downloadedUpdate = downloaded,
                    )
                    openInstaller(downloaded)
                },
                onFailure = { error ->
                    updateState.value = updateState.value.copy(
                        downloading = false,
                        showDialog = true,
                        message = "Falha ao baixar atualização: ${error.message ?: "erro desconhecido"}",
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
            message = "Permissão concedida. Abrindo instalador...",
        )
        openInstaller(downloaded)
    }

    fun dismissUpdateDialog() {
        updateState.value = updateState.value.copy(showDialog = false)
    }

    private suspend fun updateStateForAvailableUpdate(update: AvailableUpdate): UpdateUiState {
        val downloaded = updateRepository.findDownloadedUpdate(update).getOrNull()
        return if (downloaded != null) {
            UpdateUiState(
                showDialog = true,
                message = "Atualização já baixada.",
                availableUpdate = update,
                downloadedUpdate = downloaded,
            )
        } else {
            UpdateUiState(
                showDialog = true,
                message = "Versão ${update.versionName} disponível.",
                availableUpdate = update,
            )
        }
    }

    private fun exportFile(block: suspend () -> Result<File>) {
        viewModelScope.launch {
            exportMessage.value = "Exportando..."
            exportMessage.value = block()
                .fold(
                    onSuccess = SupportExportMessage::forExportedFile,
                    onFailure = { error -> "Falha ao exportar: ${error.message ?: "erro desconhecido"}" },
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
    val message: String? = null,
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
                message = "Instalador aberto. Confirme a atualização no Android.",
                downloadedUpdate = downloaded,
                waitingForInstallPermission = false,
            )

            AppUpdateInstallResult.PermissionRequired -> current.copy(
                downloading = false,
                showDialog = true,
                message = "Permissão necessária. Ative a instalação por este app e volte para continuar.",
                downloadedUpdate = downloaded,
                waitingForInstallPermission = true,
            )

            is AppUpdateInstallResult.Failed -> current.copy(
                downloading = false,
                showDialog = true,
                message = result.message,
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
