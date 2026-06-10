package com.sentinela.camtv.ui.cameras

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentinela.camtv.data.camera.CameraRepository
import com.sentinela.camtv.data.camera.RtspUrlSanitizer
import com.sentinela.camtv.data.mosaic.MosaicLayoutRepository
import com.sentinela.camtv.data.mosaic.MosaicSlot
import com.sentinela.camtv.data.onvif.OnvifRepository
import com.sentinela.camtv.data.onvif.OnvifCameraProfileSelection
import com.sentinela.camtv.data.onvif.OnvifProfileSelector
import com.sentinela.camtv.data.onvif.ResolvedOnvifProfile
import com.sentinela.camtv.domain.Camera
import com.sentinela.camtv.entitlement.EntitlementRepository
import com.sentinela.camtv.player.RtspConnectionTestResult
import com.sentinela.camtv.player.RtspConnectionTester
import com.sentinela.camtv.player.userMessage
import com.sentinela.camtv.preferences.SettingsRepository
import com.sentinela.onvif.DiscoveredOnvifDevice
import com.sentinela.onvif.OnvifCredentials
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CameraManagerUiState(
    val cameras: List<Camera> = emptyList(),
    val mosaicSlots: List<MosaicSlot> = emptyList(),
    val activeMosaicIndex: Int = 0,
    val discoveredDevices: List<DiscoveredOnvifDevice> = emptyList(),
    val selectedDeviceKey: String? = null,
    val username: String = "",
    val password: String = "",
    val rtspName: String = "",
    val rtspMainUrl: String = "",
    val rtspSubUrl: String = "",
    val rtspUsername: String = "",
    val rtspPassword: String = "",
    val scanning: Boolean = false,
    val saving: Boolean = false,
    val rtspConnecting: Boolean = false,
    val authDialogMessage: String? = null,
    val authDialogAction: CameraManagerDialogAction? = null,
    val statusMessage: String? = null,
    val freeLimitActive: Boolean = false,
    val freeActiveCameraId: String? = null,
) {
    val selectedDevice: DiscoveredOnvifDevice?
        get() = discoveredDevices.firstOrNull { device -> device.stableKey() == selectedDeviceKey }

    val busy: Boolean
        get() = scanning || saving || rtspConnecting
}

enum class CameraManagerDialogAction {
    ORGANIZE_MOSAIC,
}

class CameraManagerViewModel(
    private val cameraRepository: CameraRepository,
    private val mosaicLayoutRepository: MosaicLayoutRepository,
    private val settingsRepository: SettingsRepository,
    private val entitlementRepository: EntitlementRepository,
    private val onvifRepository: OnvifRepository,
    private val rtspConnectionTester: RtspConnectionTester,
    private val rtspCameraDraftRepository: RtspCameraDraftRepository,
) : ViewModel() {
    private val discoveredDevices = MutableStateFlow<List<DiscoveredOnvifDevice>>(emptyList())
    private val selectedDeviceKey = MutableStateFlow<String?>(null)
    private val username = MutableStateFlow("")
    private val password = MutableStateFlow("")
    private val rtspName = MutableStateFlow("")
    private val rtspMainUrl = MutableStateFlow("")
    private val rtspSubUrl = MutableStateFlow("")
    private val rtspUsername = MutableStateFlow("")
    private val rtspPassword = MutableStateFlow("")
    private val scanning = MutableStateFlow(false)
    private val saving = MutableStateFlow(false)
    private val rtspConnecting = MutableStateFlow(false)
    private val authDialogMessage = MutableStateFlow<String?>(null)
    private val authDialogAction = MutableStateFlow<CameraManagerDialogAction?>(null)
    private val statusMessage = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            val draft = rtspCameraDraftRepository.observeDraft().first()
            rtspName.value = draft.name
            rtspMainUrl.value = draft.mainUrl
            rtspSubUrl.value = draft.subUrl
        }
    }

    val state: StateFlow<CameraManagerUiState> = combine(
        cameraRepository.observeAllCameras(),
        mosaicLayoutRepository.observeAllSlots(),
        settingsRepository.observePreferences(),
        discoveredDevices,
        selectedDeviceKey,
        username,
        password,
        rtspName,
        rtspMainUrl,
        rtspSubUrl,
        rtspUsername,
        rtspPassword,
        scanning,
        saving,
        rtspConnecting,
        authDialogMessage,
        authDialogAction,
        statusMessage,
        entitlementRepository.observeEntitlement(),
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val cameras = values[0] as List<Camera>
        @Suppress("UNCHECKED_CAST")
        val slots = values[1] as List<MosaicSlot>
        val preferences = values[2] as com.sentinela.camtv.preferences.PlayerUiPreferences
        @Suppress("UNCHECKED_CAST")
        val devices = values[3] as List<DiscoveredOnvifDevice>
        CameraManagerUiState(
            cameras = cameras,
            mosaicSlots = slots,
            activeMosaicIndex = preferences.activeMosaicIndex,
            discoveredDevices = devices,
            selectedDeviceKey = values[4] as String?,
            username = values[5] as String,
            password = values[6] as String,
            rtspName = values[7] as String,
            rtspMainUrl = values[8] as String,
            rtspSubUrl = values[9] as String,
            rtspUsername = values[10] as String,
            rtspPassword = values[11] as String,
            scanning = values[12] as Boolean,
            saving = values[13] as Boolean,
            rtspConnecting = values[14] as Boolean,
            authDialogMessage = values[15] as String?,
            authDialogAction = values[16] as CameraManagerDialogAction?,
            statusMessage = values[17] as String?,
            freeLimitActive = (values[18] as com.sentinela.camtv.entitlement.EntitlementState).freeLimitActive,
            freeActiveCameraId = (values[18] as com.sentinela.camtv.entitlement.EntitlementState).freeActiveCameraId,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CameraManagerUiState(),
    )

    fun discoverOnvifDevices() {
        viewModelScope.launch {
            authDialogMessage.value = null
            authDialogAction.value = null
            scanning.value = true
            statusMessage.value = "Procurando dispositivos ONVIF..."
            onvifRepository.discover()
                .onSuccess { devices ->
                    discoveredDevices.value = devices
                    selectedDeviceKey.value = devices.firstOrNull()?.stableKey()
                    statusMessage.value = if (devices.isEmpty()) {
                        "Nenhum dispositivo ONVIF encontrado."
                    } else {
                        "${devices.size} dispositivo(s) encontrado(s)."
                    }
                }
                .onFailure { error ->
                    val message = "Falha na descoberta ONVIF: ${error.message ?: "erro desconhecido"}"
                    statusMessage.value = message
                    authDialogMessage.value = message
                    authDialogAction.value = null
                }
            scanning.value = false
        }
    }

    fun selectDiscoveredDevice(deviceKey: String) {
        selectedDeviceKey.value = deviceKey
    }

    fun updateUsername(value: String) {
        username.value = value
    }

    fun updatePassword(value: String) {
        password.value = value
    }

    fun updateRtspName(value: String) {
        rtspName.value = value
    }

    fun updateRtspMainUrl(value: String) {
        rtspMainUrl.value = value
    }

    fun updateRtspSubUrl(value: String) {
        rtspSubUrl.value = value
    }

    fun updateRtspUsername(value: String) {
        rtspUsername.value = value
    }

    fun updateRtspPassword(value: String) {
        rtspPassword.value = value
    }

    fun copyRtspMainUrlToSubUrl() {
        rtspSubUrl.value = rtspMainUrl.value
    }

    fun setFreeActiveCamera(cameraId: String) {
        viewModelScope.launch {
            entitlementRepository.setFreeActiveCameraId(cameraId)
            authDialogMessage.value = "Câmera ativa no modo grátis atualizada."
            authDialogAction.value = null
        }
    }

    fun saveSelectedOnvifCamera() {
        viewModelScope.launch {
            authDialogMessage.value = null
            authDialogAction.value = null
            val currentState = state.value
            val isFirstRegistration = currentState.cameras.isEmpty()
            val device = currentState.selectedDevice
            if (device == null) {
                authDialogMessage.value = "Selecione um dispositivo ONVIF."
                authDialogAction.value = null
                return@launch
            }

            val deviceServiceUrl = device.primaryXAddr()
            if (deviceServiceUrl.isNullOrBlank()) {
                authDialogMessage.value = "O dispositivo ONVIF não informou endereço de serviço."
                authDialogAction.value = null
                return@launch
            }

            saving.value = true
            statusMessage.value = "Consultando serviços ONVIF..."

            runCatching {
                val credentials = currentState.credentialsOrNull()
                val capabilities = onvifRepository.getCapabilities(deviceServiceUrl, credentials).getOrThrow()
                val mediaServiceUrl = capabilities.mediaXAddr ?: deviceServiceUrl

                statusMessage.value = "Consultando perfis de mídia..."
                val profiles = onvifRepository.getProfiles(mediaServiceUrl, credentials).getOrThrow()
                if (profiles.isEmpty()) {
                    error("Nenhum perfil de mídia ONVIF encontrado.")
                }

                statusMessage.value = "Obtendo URLs RTSP..."
                val resolvedProfiles = profiles.map { profile ->
                    ResolvedOnvifProfile(
                        profile = profile,
                        streamUri = onvifRepository
                            .getStreamUri(mediaServiceUrl, profile.token, credentials)
                            .getOrThrow(),
                    )
                }
                val selections = OnvifProfileSelector.selectCameras(resolvedProfiles)
                if (selections.isEmpty()) {
                    error("Nenhum perfil de mídia ONVIF encontrado.")
                }

                val baseCameraId = device.stableCameraId()
                val baseName = device.displayLabel()
                val firstSelectionKey = selections.first().groupKey
                selections.forEachIndexed { index, selection ->
                    val cameraId = device.stableCameraIdForSelection(
                        baseCameraId = baseCameraId,
                        selectionKey = selection.groupKey,
                        firstSelectionKey = firstSelectionKey,
                    )
                    cameraRepository.saveOnvifCamera(
                        id = cameraId,
                        name = selection.displayName(baseName, index),
                        endpoint = deviceServiceUrl,
                        onvifDeviceServiceUrl = deviceServiceUrl,
                        mainRtspUrl = selection.main.streamUri.uri,
                        subRtspUrl = selection.sub?.streamUri?.uri,
                        username = currentState.username.takeIf { it.isNotBlank() },
                        password = currentState.password.takeIf { it.isNotBlank() },
                        position = currentState.positionForCamera(cameraId, index),
                    )
                }
            }.onSuccess {
                authDialogMessage.value = cameraConnectedMessage(isFirstRegistration)
                authDialogAction.value = cameraConnectedAction(isFirstRegistration)
            }.onFailure { error ->
                authDialogMessage.value = error.toOnvifUserMessage()
                authDialogAction.value = null
            }

            saving.value = false
        }
    }

    fun connectManualRtspCamera() {
        viewModelScope.launch {
            authDialogMessage.value = null
            authDialogAction.value = null
            val isFirstRegistration = state.value.cameras.isEmpty()
            val validation = RtspCameraFormValidator.validate(
                name = state.value.rtspName,
                mainRtspUrl = state.value.rtspMainUrl,
                subRtspUrl = state.value.rtspSubUrl,
                username = state.value.rtspUsername,
                password = state.value.rtspPassword,
            )
            if (validation is RtspCameraFormValidation.Invalid) {
                authDialogMessage.value = validation.message
                authDialogAction.value = null
                return@launch
            }
            val form = (validation as RtspCameraFormValidation.Valid).form
            val draft = RtspCameraDraft(
                name = form.name,
                mainUrl = form.mainRtspUrl,
                subUrl = form.subRtspUrl.orEmpty(),
            )

            rtspCameraDraftRepository.saveDraft(draft)
            rtspConnecting.value = true
            statusMessage.value = "Conectando RTSP..."

            val mainResult = testRtspUrl(
                url = form.mainRtspUrl,
                username = form.username,
                password = form.password,
                streamName = "Fluxo principal",
            )
            if (mainResult is RtspConnectionTestResult.Failure) {
                authDialogMessage.value = mainResult.userMessage("Fluxo principal")
                authDialogAction.value = null
                rtspConnecting.value = false
                return@launch
            }

            val subUrl = form.subRtspUrl
            if (!subUrl.isNullOrBlank()) {
                val subResult = testRtspUrl(
                    url = subUrl,
                    username = form.username,
                    password = form.password,
                    streamName = "Fluxo secundário",
                )
                if (subResult is RtspConnectionTestResult.Failure) {
                    authDialogMessage.value = subResult.userMessage("Fluxo secundário")
                    authDialogAction.value = null
                    rtspConnecting.value = false
                    return@launch
                }
            }

            runCatching {
                cameraRepository.saveManualRtspCamera(
                    id = "rtsp-${System.currentTimeMillis()}",
                    name = form.name,
                    rtspUrl = form.mainRtspUrl,
                    subRtspUrl = form.subRtspUrl,
                    username = form.username,
                    password = form.password,
                    position = state.value.cameras.size,
                )
            }.onSuccess {
                rtspName.value = draft.name
                rtspMainUrl.value = draft.mainUrl
                rtspSubUrl.value = draft.subUrl
                rtspPassword.value = ""
                authDialogMessage.value = cameraConnectedMessage(isFirstRegistration)
                authDialogAction.value = cameraConnectedAction(isFirstRegistration)
            }.onFailure { error ->
                authDialogMessage.value = "Não foi possível salvar a câmera: ${error.message ?: "URL inválida"}"
                authDialogAction.value = null
            }

            rtspConnecting.value = false
        }
    }

    private suspend fun testRtspUrl(
        url: String,
        username: String?,
        password: String?,
        streamName: String,
    ): RtspConnectionTestResult {
        statusMessage.value = "Conectando $streamName..."
        return rtspConnectionTester.test(
            RtspUrlSanitizer.withCredentials(
                sanitizedUrl = url,
                username = username,
                password = password,
            ),
        )
    }

    fun dismissAuthDialog() {
        authDialogMessage.value = null
        authDialogAction.value = null
    }

    fun selectActiveMosaic(index: Int) {
        viewModelScope.launch {
            settingsRepository.setActiveMosaicIndex(index)
        }
    }

    fun placeCameraInMosaic(
        mosaicIndex: Int,
        slotIndex: Int,
        cameraId: String,
    ) {
        viewModelScope.launch {
            mosaicLayoutRepository.placeCamera(
                mosaicIndex = mosaicIndex,
                slotIndex = slotIndex,
                cameraId = cameraId,
            )
        }
    }

    fun removeCameraFromMosaic(cameraId: String) {
        viewModelScope.launch {
            mosaicLayoutRepository.removeCameraFromLayout(cameraId)
        }
    }
}

internal fun cameraConnectedMessage(isFirstRegistration: Boolean): String =
    if (isFirstRegistration) {
        "Câmera(s) conectada(s). Organize seu mosaico na aba Mosaicos antes de visualizar a(s) câmera(s)."
    } else {
        "Câmera(s) conectada(s)."
    }

internal fun cameraConnectedAction(isFirstRegistration: Boolean): CameraManagerDialogAction? =
    if (isFirstRegistration) CameraManagerDialogAction.ORGANIZE_MOSAIC else null

fun DiscoveredOnvifDevice.stableKey(): String =
    endpointReference.takeIf { it.isNotBlank() }
        ?: primaryXAddr()
        ?: scopes.joinToString("|")

fun DiscoveredOnvifDevice.displayLabel(): String =
    scopes.firstScopeValue("name")
        ?: scopes.firstScopeValue("hardware")
        ?: scopes.firstScopeValue("model")
        ?: scopes.firstScopeValue("manufacturer")
        ?: primaryXAddr()
        ?: endpointReference

private fun List<String>.firstScopeValue(scopeName: String): String? =
    firstOrNull { scope ->
        scope.contains("/$scopeName/", ignoreCase = true)
    }?.substringAfterLast('/')?.takeIf { value -> value.isNotBlank() }

private fun DiscoveredOnvifDevice.primaryXAddr(): String? =
    xAddrs.firstOrNull { address -> address.startsWith("http", ignoreCase = true) }
        ?: xAddrs.firstOrNull()

private fun DiscoveredOnvifDevice.stableCameraId(): String =
    "onvif-${Integer.toHexString(stableKey().hashCode())}"

private fun DiscoveredOnvifDevice.stableCameraIdForSelection(
    baseCameraId: String,
    selectionKey: String,
    firstSelectionKey: String,
): String =
    if (selectionKey == firstSelectionKey) {
        baseCameraId
    } else {
        "$baseCameraId-${Integer.toHexString(selectionKey.hashCode())}"
    }

private fun OnvifCameraProfileSelection.displayName(
    baseName: String,
    index: Int,
): String =
    channelNumber?.let { channel -> "CAM$channel" }
        ?: main.profile.name.takeIf { it.isNotBlank() }
        ?: if (index == 0) baseName else "$baseName ${index + 1}"

private fun CameraManagerUiState.positionForCamera(cameraId: String, newCameraOffset: Int): Int =
    cameras.firstOrNull { camera -> camera.id == cameraId }?.position ?: (cameras.size + newCameraOffset)

private fun CameraManagerUiState.credentialsOrNull(): OnvifCredentials? =
    username.takeIf { it.isNotBlank() }?.let { user ->
        OnvifCredentials(
            username = user,
            password = password,
        )
    }

private fun Throwable.isLikelyAuthenticationError(): Boolean =
    message.orEmpty().lowercase().let { text ->
        "401" in text || "auth" in text || "authorized" in text || "senha" in text
    }

private fun Throwable.toOnvifUserMessage(): String =
    when {
        isLikelyAuthenticationError() ->
            "Falha de autenticação ONVIF. Confira usuário, senha e se o ONVIF está ativo no dispositivo."
        message.orEmpty().contains("Cleartext HTTP traffic", ignoreCase = true) ->
            "O Android bloqueou a conexão HTTP local do ONVIF."
        message.orEmpty().contains("ONVIF HTTP", ignoreCase = true) ->
            message ?: "ONVIF HTTP só é permitido na rede local."
        message.orEmpty().contains("timeout", ignoreCase = true) ->
            "Tempo esgotado ao consultar o dispositivo ONVIF."
        else ->
            "Falha ONVIF: ${message ?: "erro desconhecido"}"
    }
