package com.sentinela.camtv.debug

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.sentinela.camtv.BuildConfig
import com.sentinela.camtv.billing.BillingRepository
import com.sentinela.camtv.billing.DebugBillingRepository
import com.sentinela.camtv.billing.SubscriptionPlan
import com.sentinela.camtv.billing.SubscriptionStatus
import com.sentinela.camtv.billing.debugDisplayName
import com.sentinela.camtv.billing.toDebugStatusKey
import com.sentinela.camtv.billing.toDebugSubscriptionStatus
import com.sentinela.camtv.data.camera.CameraRepository
import com.sentinela.camtv.diagnostics.DiagnosticsReporter
import com.sentinela.camtv.entitlement.EntitlementRepository
import com.sentinela.camtv.logging.CrashReporter
import com.sentinela.camtv.logging.FileTimberTree
import com.sentinela.camtv.logging.LogRepository
import com.sentinela.camtv.player.StreamQuality
import com.sentinela.camtv.player.TransmissionMode
import com.sentinela.camtv.preferences.SettingsRepository
import com.sentinela.camtv.ui.design.SentinelaDialogButton
import com.sentinela.camtv.ui.design.SentinelaTransientMessage
import com.sentinela.camtv.ui.design.SentinelaTvColors
import com.sentinela.camtv.ui.design.SentinelaTvDialog
import com.sentinela.camtv.ui.design.SentinelaTvPadding
import com.sentinela.camtv.ui.design.SentinelaTvShape
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Context.debugPanelDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "debug_panel_preferences",
)

fun createDebugFeatureProvider(
    context: Context,
    billingRepository: BillingRepository,
    entitlementRepository: EntitlementRepository,
    settingsRepository: SettingsRepository,
    cameraRepository: CameraRepository,
    logRepository: LogRepository,
    fileTimberTree: FileTimberTree,
    crashReporter: CrashReporter,
    diagnosticsReporter: DiagnosticsReporter,
): DebugFeatureProvider =
    AndroidDebugFeatureProvider(
        context = context.applicationContext,
        debugBillingRepository = billingRepository as? DebugBillingRepository,
        entitlementRepository = entitlementRepository,
        settingsRepository = settingsRepository,
        cameraRepository = cameraRepository,
        fileTimberTree = fileTimberTree,
        crashReporter = crashReporter,
        diagnosticsReporter = diagnosticsReporter,
    )

private class AndroidDebugFeatureProvider(
    private val context: Context,
    private val debugBillingRepository: DebugBillingRepository?,
    private val entitlementRepository: EntitlementRepository,
    private val settingsRepository: SettingsRepository,
    private val cameraRepository: CameraRepository,
    private val fileTimberTree: FileTimberTree,
    private val crashReporter: CrashReporter,
    private val diagnosticsReporter: DiagnosticsReporter,
) : DebugFeatureProvider {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val preferences = DebugPanelPreferencesRepository(context.debugPanelDataStore)
    private val panelVisible = MutableStateFlow(false)
    private val selectedSection = MutableStateFlow(DebugSection.Subscription)
    private val transientMessage = MutableStateFlow<String?>(null)
    private val detailText = MutableStateFlow<DebugPanelDetail?>(null)
    private val confirmation = MutableStateFlow<DebugConfirmation?>(null)

    override val state: StateFlow<DebugFeatureState> = combine(
        debugBillingRepository?.state ?: MutableStateFlow(com.sentinela.camtv.billing.BillingState()),
        preferences.state,
    ) { billing, _ ->
        DebugFeatureState(
            homeActionLabel = "Debug",
            quickMenuActionLabel = "Debug",
            footerSuffix = "Estado debug: ${billing.status.debugDisplayName()}",
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = DebugFeatureState(
            homeActionLabel = "Debug",
            quickMenuActionLabel = "Debug",
        ),
    )

    init {
        scope.launch {
            preferences.state.collect { state ->
                state.statusKey?.let { key ->
                    debugBillingRepository?.setDebugStatusKey(key)
                }
            }
        }
    }

    override fun openPanel() {
        panelVisible.value = true
    }

    @Composable
    override fun Render() {
        val visible by panelVisible.collectAsState()
        val billing by (debugBillingRepository?.state ?: MutableStateFlow(com.sentinela.camtv.billing.BillingState()))
            .collectAsState()
        val section by selectedSection.collectAsState()
        val message by transientMessage.collectAsState()
        val detail by detailText.collectAsState()
        val pendingConfirmation by confirmation.collectAsState()
        val playerSnapshot by DebugPlayerRegistry.state.collectAsState()

        if (visible) {
            DebugPanelDialog(
                selectedSection = section,
                onSectionSelected = {
                    selectedSection.value = it
                    detailText.value = null
                },
                detailText = debugDetailTextForSection(
                    selectedSection = section,
                    detail = detail,
                ),
                actions = actionsFor(section, billing.status, playerSnapshot),
                onDismiss = {
                    panelVisible.value = false
                    detailText.value = null
                },
            )
        }

        if (!message.isNullOrBlank()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                SentinelaTransientMessage(
                    message = message.orEmpty(),
                    onTimeout = { transientMessage.value = null },
                    modifier = Modifier.padding(bottom = 34.dp),
                )
            }
        }

        pendingConfirmation?.let { request ->
            SentinelaTvDialog(
                title = request.title,
                message = request.message,
                confirmLabel = request.confirmLabel,
                onConfirm = {
                    confirmation.value = null
                    request.onConfirm()
                },
                dismissLabel = "Cancelar",
                onDismiss = { confirmation.value = null },
            )
        }
    }

    private fun actionsFor(
        section: DebugSection,
        status: SubscriptionStatus,
        playerSnapshot: DebugPlayerSnapshot,
    ): List<DebugPanelAction> =
        when (section) {
            DebugSection.Subscription -> subscriptionActions()
            DebugSection.FreeLimit -> freeLimitActions()
            DebugSection.LocalData -> localDataActions()
            DebugSection.Diagnostics -> diagnosticsActions()
            DebugSection.TechnicalInfo -> technicalInfoActions(status)
            DebugSection.PlayerRtsp -> playerActions(playerSnapshot)
            DebugSection.FakeData -> fakeDataActions()
        }

    private fun subscriptionActions(): List<DebugPanelAction> =
        listOf(
            statusAction("Grátis com teste", SubscriptionStatus.FreeTrialEligible),
            statusAction("Grátis sem teste", SubscriptionStatus.FreeNoTrial),
            statusAction("Mensal ativo", SubscriptionStatus.MonthlyActive),
            statusAction("Anual ativo", SubscriptionStatus.AnnualActive),
            statusAction("Pagamento pendente", SubscriptionStatus.GracePeriod),
            statusAction("Assinatura suspensa", SubscriptionStatus.OnHold),
            statusAction("Billing indisponível", SubscriptionStatus.BillingUnavailable),
            statusAction("Verificando assinatura", SubscriptionStatus.Checking),
            statusAction("Erro ao verificar", SubscriptionStatus.Error),
            statusAction("Expirada", SubscriptionStatus.Expired),
            statusAction("Cancelada até vencer", SubscriptionStatus.CanceledUntilExpiry),
        )

    private fun statusAction(
        label: String,
        status: SubscriptionStatus,
    ): DebugPanelAction =
        DebugPanelAction(label) {
            scope.launch {
                preferences.setStatusKey(status.toDebugStatusKey())
                debugBillingRepository?.setDebugStatus(status)
                showMessage("Estado debug alterado para: $label")
            }
        }

    private fun freeLimitActions(): List<DebugPanelAction> =
        listOf(
            statusAction("Ativar modo grátis", SubscriptionStatus.FreeNoTrial),
            statusAction("Liberar premium simulado", SubscriptionStatus.MonthlyActive),
            DebugPanelAction("Limpar câmera ativa grátis") {
                scope.launch {
                    entitlementRepository.setFreeActiveCameraId(null)
                    showMessage("Câmera ativa grátis limpa.")
                }
            },
            DebugPanelAction("Simular bloqueio do mosaico completo") {
                scope.launch {
                    preferences.setStatusKey(SubscriptionStatus.FreeNoTrial.toDebugStatusKey())
                    debugBillingRepository?.setDebugStatus(SubscriptionStatus.FreeNoTrial)
                    showMessage("Modo grátis ativo. Abra o mosaico para validar o bloqueio.")
                }
            },
        )

    private fun localDataActions(): List<DebugPanelAction> =
        listOf(
            DebugPanelAction("Limpar preferências do player") {
                confirm(
                    title = "Limpar preferências?",
                    message = "Isso redefine preferências locais deste build debug, sem apagar câmeras reais.",
                ) {
                    scope.launch {
                        resetPlayerPreferences()
                        showMessage("Preferências locais redefinidas.")
                    }
                }
            },
            DebugPanelAction("Limpar estado de assinatura debug") {
                scope.launch {
                    preferences.clearStatusKey()
                    debugBillingRepository?.resetDebugStatus()
                    showMessage("Estado de assinatura debug limpo.")
                }
            },
            DebugPanelAction("Limpar câmeras fake") {
                scope.launch {
                    val removed = clearFakeCameras()
                    showMessage("$removed câmera(s) fake removida(s).")
                }
            },
            DebugPanelAction("Resetar dados debug seguros", danger = true) {
                confirm(
                    title = "Resetar dados debug?",
                    message = "Isso limpa preferências, logs, câmeras fake e estado debug. Câmeras reais serão preservadas.",
                    confirmLabel = "Resetar",
                ) {
                    scope.launch {
                        resetPlayerPreferences()
                        clearFakeCameras()
                        clearLocalLogs()
                        preferences.clearAll()
                        debugBillingRepository?.resetDebugStatus()
                        showMessage("Dados debug seguros redefinidos.")
                    }
                }
            },
        )

    private fun diagnosticsActions(): List<DebugPanelAction> =
        listOf(
            DebugPanelAction("Ver logs recentes") {
                scope.launch {
                    setDetail(DebugSection.Diagnostics, recentLogsText())
                }
            },
            DebugPanelAction("Exportar logs sanitizados") {
                scope.launch {
                    val file = exportSanitizedLogs()
                    showMessage("Logs exportados: ${file.name}")
                    setDetail(DebugSection.Diagnostics, file.absolutePath)
                }
            },
            DebugPanelAction("Exportar crashes sanitizados") {
                scope.launch {
                    val file = exportSanitizedCrashes()
                    showMessage("Crashes exportados: ${file.name}")
                    setDetail(DebugSection.Diagnostics, file.absolutePath)
                }
            },
            DebugPanelAction("Limpar logs e crashes") {
                confirm(
                    title = "Limpar logs?",
                    message = "Isso apaga logs e crashes locais deste build debug.",
                    confirmLabel = "Limpar",
                ) {
                    scope.launch {
                        clearLocalLogs()
                        showMessage("Logs locais limpos.")
                    }
                }
            },
            DebugPanelAction("Forçar diagnóstico de teste") {
                diagnosticsReporter.log("Debug diagnostic test")
                diagnosticsReporter.recordNonFatal(
                    throwable = IllegalStateException("Debug non-fatal test"),
                    message = "Debug Panel non-fatal test",
                )
                showMessage("Diagnóstico de teste enviado.")
            },
            DebugPanelAction("Simular crash de teste", danger = true) {
                confirm(
                    title = "Simular crash?",
                    message = "Isso encerrará o app de propósito para validar captura de crash no debug.",
                    confirmLabel = "Crash",
                ) {
                    throw RuntimeException("Debug crash test from Debug Panel")
                }
            },
        )

    private fun technicalInfoActions(status: SubscriptionStatus): List<DebugPanelAction> =
        listOf(
            DebugPanelAction("Atualizar informações técnicas") {
                scope.launch {
                    setDetail(DebugSection.TechnicalInfo, technicalInfoText(status))
                }
            },
        )

    private fun playerActions(playerSnapshot: DebugPlayerSnapshot): List<DebugPanelAction> =
        listOf(
            DebugPanelAction(
                label = "Forçar reconexão das câmeras",
                enabled = playerSnapshot.activePlayers > 0,
            ) {
                DebugPlayerRegistry.forceReconnect()
                showMessage("Reconexão debug solicitada.")
            },
            DebugPanelAction("Mostrar resumo de players") {
                setDetail(DebugSection.PlayerRtsp, playerSummaryText(playerSnapshot))
            },
        )

    private fun fakeDataActions(): List<DebugPanelAction> =
        listOf(
            DebugPanelAction("Criar 1 câmera fake") {
                scope.launch { createFakeCameras(count = 1) }
            },
            DebugPanelAction("Criar 4 câmeras fake") {
                scope.launch { createFakeCameras(count = 4) }
            },
            DebugPanelAction("Criar 6 câmeras fake") {
                scope.launch { createFakeCameras(count = 6) }
            },
            DebugPanelAction("Criar câmera offline fake") {
                scope.launch {
                    saveFakeCamera(
                        index = 90,
                        name = "DEBUG Offline",
                        url = "rtsp://debug.invalid/offline",
                    )
                    showMessage("Câmera offline fake criada.")
                }
            },
            DebugPanelAction("Criar câmera com nome longo") {
                scope.launch {
                    saveFakeCamera(
                        index = 91,
                        name = "DEBUG Câmera com nome muito longo para testar layout TV",
                        url = "rtsp://debug.invalid/long-name",
                    )
                    showMessage("Câmera fake com nome longo criada.")
                }
            },
            DebugPanelAction("Criar câmera com erro de autenticação") {
                scope.launch {
                    val id = fakeCameraId(92)
                    saveFakeCamera(
                        index = 92,
                        name = "DEBUG Autenticação",
                        url = "rtsp://debug.invalid/auth",
                    )
                    cameraRepository.setAuthenticationFailure(id, true)
                    showMessage("Câmera fake com erro de autenticação criada.")
                }
            },
            DebugPanelAction("Limpar câmeras fake") {
                scope.launch {
                    val removed = clearFakeCameras()
                    showMessage("$removed câmera(s) fake removida(s).")
                }
            },
        )

    private suspend fun resetPlayerPreferences() {
        settingsRepository.setShowPlayerInfo(false)
        settingsRepository.setShowMosaicInfo(false)
        settingsRepository.setShowFullscreenInfo(false)
        settingsRepository.setFullscreenQuickMenuHintSeen(false)
        settingsRepository.setMosaicStreamQuality(StreamQuality.SD)
        settingsRepository.setGlobalTransmissionMode(TransmissionMode.MENOR_LATENCIA)
        settingsRepository.setFreeActiveCameraId(null)
        settingsRepository.setDiagnosticsEnabled(true)
        settingsRepository.setPremiumGraceUntilEpochMillis(0L)
    }

    private suspend fun createFakeCameras(count: Int) {
        repeat(count) { index ->
            saveFakeCamera(
                index = index + 1,
                name = "DEBUG CAM ${index + 1}",
                url = "rtsp://debug.invalid/camera-${index + 1}",
            )
        }
        showMessage("$count câmera(s) fake criada(s).")
    }

    private suspend fun saveFakeCamera(
        index: Int,
        name: String,
        url: String,
    ) {
        cameraRepository.saveManualRtspCamera(
            id = fakeCameraId(index),
            name = name,
            rtspUrl = url,
            subRtspUrl = url,
            username = null,
            password = null,
            position = 1_000 + index,
        )
    }

    private suspend fun clearFakeCameras(): Int {
        val fakeIds = cameraRepository.observeAllCameras()
            .first()
            .map { it.id }
            .filter { it.startsWith(FAKE_CAMERA_ID_PREFIX) }
        fakeIds.forEach { id -> cameraRepository.deleteCamera(id) }
        return fakeIds.size
    }

    private suspend fun recentLogsText(): String = withContext(Dispatchers.IO) {
        val text = fileTimberTree.logFiles()
            .flatMap { file -> file.readLines().takeLast(40) }
            .takeLast(80)
            .joinToString(separator = "\n")
        sanitizeDebugText(text.ifBlank { "Sem logs recentes." })
    }

    private suspend fun exportSanitizedLogs(): File = withContext(Dispatchers.IO) {
        val output = debugDownloadFile("sentinela-debug-logs-sanitizados.txt")
        output.writeText(
            fileTimberTree.logFiles().joinToString(separator = "\n") { file ->
                "===== ${file.name} =====\n${sanitizeDebugText(file.readText())}"
            },
        )
        output
    }

    private suspend fun exportSanitizedCrashes(): File = withContext(Dispatchers.IO) {
        val output = debugDownloadFile("sentinela-debug-crashes-sanitizados.txt")
        output.writeText(
            crashReporter.crashFiles().joinToString(separator = "\n") { file ->
                "===== ${file.name} =====\n${sanitizeDebugText(file.readText())}"
            },
        )
        output
    }

    private suspend fun clearLocalLogs() = withContext(Dispatchers.IO) {
        fileTimberTree.logFiles().forEach { it.delete() }
        crashReporter.crashFiles().forEach { it.delete() }
    }

    private fun debugDownloadFile(name: String): File =
        (context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir)
            .apply { mkdirs() }
            .resolve(name)

    private suspend fun technicalInfoText(status: SubscriptionStatus): String {
        val memoryInfo = context.memoryInfo()
        val displayMetrics = context.resources.displayMetrics
        val diagnosticsEnabled = settingsRepository.observePreferences().first().diagnosticsEnabled
        val billingState = debugBillingRepository?.state?.value
        return formatDebugTechnicalInfo(
            snapshot = DebugTechnicalInfoSnapshot(
                versionName = BuildConfig.VERSION_NAME,
                versionCode = BuildConfig.VERSION_CODE,
                packageName = context.packageName,
                buildType = if (BuildConfig.DEBUG) "debug" else "release",
                billingLabel = "simulado",
                statusLabel = status.debugPortugueseLabel(),
                statusIdentifier = status.debugDisplayName(),
                activePlanLabel = (billingState?.activePlan).debugPortuguesePlanLabel(),
                activeBasePlan = billingState?.activePlan?.basePlanId ?: "nenhum",
                manufacturer = Build.MANUFACTURER,
                model = Build.MODEL,
                androidRelease = Build.VERSION.RELEASE,
                apiLevel = Build.VERSION.SDK_INT,
                abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "desconhecida",
                screenWidthPx = displayMetrics.widthPixels,
                screenHeightPx = displayMetrics.heightPixels,
                availableRamMb = memoryInfo.availableMb,
                totalRamMb = memoryInfo.totalMb,
                lowMemory = memoryInfo.lowMemory,
                lowMemoryThresholdMb = memoryInfo.thresholdMb,
                crashlyticsConfigured = BuildConfig.CRASHLYTICS_CONFIGURED,
                diagnosticsEnabled = diagnosticsEnabled,
            ),
        )
    }

    private fun playerSummaryText(playerSnapshot: DebugPlayerSnapshot): String =
        listOf(
            "Players ativos: ${playerSnapshot.activePlayers}",
            "Reconexões: ${playerSnapshot.reconnectRequests}",
            "Última reconexão: ${playerSnapshot.lastReconnectReason ?: "nenhuma"}",
            "Último erro: ${playerSnapshot.lastError ?: "nenhum"}",
        ).joinToString("\n")

    private fun confirm(
        title: String,
        message: String,
        confirmLabel: String = "Confirmar",
        onConfirm: () -> Unit,
    ) {
        confirmation.value = DebugConfirmation(
            title = title,
            message = message,
            confirmLabel = confirmLabel,
            onConfirm = onConfirm,
        )
    }

    private fun showMessage(message: String) {
        transientMessage.value = message
    }

    private fun setDetail(section: DebugSection, text: String) {
        detailText.value = DebugPanelDetail(section = section, text = text)
    }
}

@Composable
private fun DebugPanelDialog(
    selectedSection: DebugSection,
    onSectionSelected: (DebugSection) -> Unit,
    detailText: String?,
    actions: List<DebugPanelAction>,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
    ) {
        val firstSectionFocusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) { firstSectionFocusRequester.requestFocus() }

        Row(
            modifier = Modifier
                .widthIn(min = 980.dp, max = 1120.dp)
                .heightIn(min = 560.dp, max = 640.dp)
                .background(
                    color = SentinelaTvColors.panel.copy(alpha = 0.98f),
                    shape = SentinelaTvShape.dialog,
                )
                .border(
                    width = 1.dp,
                    color = SentinelaTvColors.panelBorder,
                    shape = SentinelaTvShape.dialog,
                )
                .padding(SentinelaTvPadding.dialog)
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(
                modifier = Modifier
                    .width(270.dp)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Text(
                    text = "Debug",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                DebugSection.entries.forEachIndexed { index, section ->
                    DebugPanelButton(
                        label = section.label,
                        selected = section == selectedSection,
                        onClick = { onSectionSelected(section) },
                        modifier = if (index == 0) {
                            Modifier.focusRequester(firstSectionFocusRequester)
                        } else {
                            Modifier
                        },
                    )
                }
                SentinelaDialogButton(
                    label = "Fechar",
                    onClick = onDismiss,
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = selectedSection.label,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                actions.forEach { action ->
                    DebugPanelButton(
                        label = action.label,
                        enabled = action.enabled,
                        danger = action.danger,
                        onClick = action.onClick,
                    )
                }
                if (!detailText.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = SentinelaTvColors.screenBackground.copy(alpha = 0.72f),
                                shape = SentinelaTvShape.panel,
                            )
                            .border(
                                width = 1.dp,
                                color = SentinelaTvColors.panelBorder,
                                shape = SentinelaTvShape.panel,
                            )
                            .padding(14.dp),
                    ) {
                        Text(
                            text = detailText,
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DebugPanelButton(
    label: String,
    selected: Boolean = false,
    enabled: Boolean = true,
    danger: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val background = when {
        selected -> SentinelaTvColors.controlSelected
        enabled -> SentinelaTvColors.control
        else -> SentinelaTvColors.control.copy(alpha = 0.38f)
    }
    val textColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.56f)
        danger -> Color(0xFFFFC9C9)
        else -> MaterialTheme.colorScheme.onBackground
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (enabled && event.type == KeyEventType.KeyUp && event.key.isConfirmKey()) {
                    onClick()
                    true
                } else {
                    false
                }
            }
            .semantics { role = Role.Button }
            .background(background, SentinelaTvShape.control)
            .border(
                width = if (focused) 3.dp else 1.dp,
                color = if (focused) SentinelaTvColors.controlFocused else SentinelaTvColors.panelBorder,
                shape = SentinelaTvShape.control,
            )
            .padding(horizontal = 16.dp, vertical = 11.dp)
            .focusable(enabled),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

private data class DebugPanelAction(
    val label: String,
    val enabled: Boolean = true,
    val danger: Boolean = false,
    val onClick: () -> Unit,
)

private data class DebugConfirmation(
    val title: String,
    val message: String,
    val confirmLabel: String,
    val onConfirm: () -> Unit,
)

private data class DebugPanelDetail(
    val section: DebugSection,
    val text: String,
)

private fun debugDetailTextForSection(
    selectedSection: DebugSection,
    detail: DebugPanelDetail?,
): String? =
    detail?.text?.takeIf { shouldShowDebugDetail(selectedSection.label, detail.section.label) }

internal fun shouldShowDebugDetail(
    selectedSectionLabel: String,
    detailSectionLabel: String,
): Boolean = selectedSectionLabel == detailSectionLabel

private enum class DebugSection(val label: String) {
    Subscription("Assinatura"),
    FreeLimit("Limite grátis"),
    LocalData("Dados locais"),
    Diagnostics("Diagnóstico e logs"),
    TechnicalInfo("Informações técnicas"),
    PlayerRtsp("Player/RTSP"),
    FakeData("Dados falsos para UI"),
}

internal fun debugSectionLabels(): List<String> = DebugSection.entries.map { it.label }

private data class DebugPanelPreferences(
    val statusKey: String? = null,
)

private class DebugPanelPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) {
    val state: StateFlow<DebugPanelPreferences>
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        state = dataStore.data
            .map { preferences ->
                DebugPanelPreferences(
                    statusKey = preferences[STATUS_KEY],
                )
            }
            .stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue = DebugPanelPreferences(),
            )
    }

    suspend fun setStatusKey(statusKey: String) {
        dataStore.edit { preferences -> preferences[STATUS_KEY] = statusKey }
    }

    suspend fun clearStatusKey() {
        dataStore.edit { preferences -> preferences.remove(STATUS_KEY) }
    }

    suspend fun clearAll() {
        dataStore.edit { preferences -> preferences.clear() }
    }

    private companion object {
        val STATUS_KEY = stringPreferencesKey("debug_subscription_status")
    }
}

private data class DeviceMemoryInfo(
    val availableMb: Int,
    val totalMb: Int,
    val lowMemory: Boolean,
    val thresholdMb: Int,
)

private fun Context.memoryInfo(): DeviceMemoryInfo {
    val activityManager = getSystemService(ActivityManager::class.java)
    val info = ActivityManager.MemoryInfo()
    activityManager?.getMemoryInfo(info)
    return DeviceMemoryInfo(
        availableMb = (info.availMem / MB).toInt(),
        totalMb = (info.totalMem / MB).toInt(),
        lowMemory = info.lowMemory,
        thresholdMb = (info.threshold / MB).toInt(),
    )
}

internal data class DebugTechnicalInfoSnapshot(
    val versionName: String,
    val versionCode: Int,
    val packageName: String,
    val buildType: String,
    val billingLabel: String,
    val statusLabel: String,
    val statusIdentifier: String,
    val activePlanLabel: String,
    val activeBasePlan: String,
    val manufacturer: String,
    val model: String,
    val androidRelease: String,
    val apiLevel: Int,
    val abi: String,
    val screenWidthPx: Int,
    val screenHeightPx: Int,
    val availableRamMb: Int,
    val totalRamMb: Int,
    val lowMemory: Boolean,
    val lowMemoryThresholdMb: Int,
    val crashlyticsConfigured: Boolean,
    val diagnosticsEnabled: Boolean,
)

internal fun formatDebugTechnicalInfo(snapshot: DebugTechnicalInfoSnapshot): String =
    buildString {
        appendLine("Aplicativo")
        appendLine("Versão: ${snapshot.versionName}")
        appendLine("Código da versão: ${snapshot.versionCode}")
        appendLine("Pacote: ${snapshot.packageName}")
        appendLine("Tipo de build: ${snapshot.buildType}")
        appendLine()
        appendLine("Assinatura")
        appendLine("Billing: ${snapshot.billingLabel}")
        appendLine("Estado: ${snapshot.statusLabel}")
        appendLine("Identificador: ${snapshot.statusIdentifier}")
        appendLine("Plano ativo: ${snapshot.activePlanLabel}")
        appendLine("Base plan: ${snapshot.activeBasePlan}")
        appendLine()
        appendLine("Dispositivo")
        appendLine("Fabricante/modelo: ${snapshot.manufacturer} ${snapshot.model}".trimEnd())
        appendLine("Android: ${snapshot.androidRelease} (API ${snapshot.apiLevel})")
        appendLine("ABI: ${snapshot.abi}")
        appendLine("Tela: ${snapshot.screenWidthPx} × ${snapshot.screenHeightPx} px")
        appendLine()
        appendLine("Memória — instantâneo")
        appendLine("RAM disponível: ${snapshot.availableRamMb} MB de ${snapshot.totalRamMb} MB")
        appendLine("Memória baixa: ${snapshot.lowMemory.toPortugueseYesNo()}")
        appendLine("Limite de memória baixa: ${snapshot.lowMemoryThresholdMb} MB")
        appendLine()
        appendLine("Diagnóstico")
        appendLine("Crashlytics: ${snapshot.crashlyticsConfigured.toConfiguredLabel()}")
        append("Diagnóstico automático: ${snapshot.diagnosticsEnabled.toEnabledLabel()}.")
    }

internal fun SubscriptionStatus.debugPortugueseLabel(): String =
    when (this) {
        SubscriptionStatus.FreeTrialEligible -> "Grátis com teste"
        SubscriptionStatus.FreeNoTrial -> "Grátis sem teste"
        SubscriptionStatus.MonthlyActive -> "Mensal ativo"
        SubscriptionStatus.AnnualActive -> "Anual ativo"
        SubscriptionStatus.GracePeriod -> "Pagamento pendente"
        SubscriptionStatus.OnHold -> "Assinatura suspensa"
        SubscriptionStatus.BillingUnavailable -> "Billing indisponível"
        SubscriptionStatus.Checking -> "Verificando assinatura"
        SubscriptionStatus.Error -> "Erro ao verificar"
        SubscriptionStatus.Expired -> "Expirada"
        SubscriptionStatus.CanceledUntilExpiry -> "Cancelada até vencer"
    }

internal fun SubscriptionPlan?.debugPortuguesePlanLabel(): String =
    when (this) {
        SubscriptionPlan.Monthly -> "mensal"
        SubscriptionPlan.Annual -> "anual"
        null -> "nenhum"
    }

private fun Boolean.toPortugueseYesNo(): String = if (this) "sim" else "não"

private fun Boolean.toEnabledLabel(): String = if (this) "ativado" else "desativado"

private fun Boolean.toConfiguredLabel(): String = if (this) "configurado" else "não configurado"

internal fun sanitizeDebugText(text: String): String =
    text
        .replace(Regex("rtsp://[^\\s)]+", RegexOption.IGNORE_CASE), "rtsp://<oculto>")
        .replace(Regex("https?://[^\\s)]+", RegexOption.IGNORE_CASE), "https://<oculto>")
        .replace(Regex("(?i)(password|senha|token|user|usuario|usuário)=([^\\s&]+)"), "$1=<oculto>")
        .replace(Regex("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b"), "<ip>")

private fun fakeCameraId(index: Int): String = "$FAKE_CAMERA_ID_PREFIX$index"

private fun Key.isConfirmKey(): Boolean =
    this == Key.DirectionCenter ||
        this == Key.Enter ||
        this == Key.NumPadEnter

private const val FAKE_CAMERA_ID_PREFIX = "debug-fake-"
private const val MB = 1024L * 1024L
