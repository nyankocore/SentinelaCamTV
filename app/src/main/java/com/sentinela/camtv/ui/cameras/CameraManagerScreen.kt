package com.sentinela.camtv.ui.cameras

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.sentinela.camtv.data.mosaic.MOSAIC_COUNT
import com.sentinela.camtv.data.mosaic.MOSAIC_MAX_SLOTS
import com.sentinela.camtv.domain.Camera
import com.sentinela.camtv.domain.DvrRtspChannel
import com.sentinela.camtv.domain.OnvifCameraSource
import com.sentinela.camtv.domain.RtspCameraSource
import com.sentinela.camtv.ui.common.SentinelaScreen
import com.sentinela.camtv.ui.design.SentinelaTvColors
import com.sentinela.camtv.ui.design.SentinelaTvSize
import com.sentinela.camtv.ui.design.SentinelaTvDialog
import com.sentinela.onvif.DiscoveredOnvifDevice

private enum class CameraManagerTab(
    val label: String,
) {
    ONVIF("ONVIF"),
    RTSP("RTSP direto"),
    CONNECTED("Conectadas"),
    MOSAICS("Mosaicos"),
}

private object CameraTextFieldId {
    const val ONVIF_USERNAME = "onvif_username"
    const val ONVIF_PASSWORD = "onvif_password"
    const val RTSP_NAME = "rtsp_name"
    const val RTSP_MAIN_URL = "rtsp_main_url"
    const val RTSP_SUB_URL = "rtsp_sub_url"
    const val RTSP_USERNAME = "rtsp_username"
    const val RTSP_PASSWORD = "rtsp_password"
}

internal enum class CursorMoveDirection {
    Left,
    Right,
}

internal object TextFieldCursorController {
    fun moveCursor(
        value: TextFieldValue,
        direction: CursorMoveDirection,
    ): TextFieldValue {
        val selectionStart = minOf(value.selection.start, value.selection.end)
        val selectionEnd = maxOf(value.selection.start, value.selection.end)
        val collapsed = value.selection.start == value.selection.end
        val position = when (direction) {
            CursorMoveDirection.Left -> if (collapsed) {
                (selectionStart - 1).coerceAtLeast(0)
            } else {
                selectionStart
            }

            CursorMoveDirection.Right -> if (collapsed) {
                (selectionEnd + 1).coerceAtMost(value.text.length)
            } else {
                selectionEnd
            }
        }
        return value.copy(selection = TextRange(position))
    }

    fun syncExternalText(
        current: TextFieldValue,
        newText: String,
    ): TextFieldValue {
        if (current.text == newText) return current

        val selection = if (current.text.isBlank()) {
            TextRange(newText.length)
        } else {
            TextRange(
                current.selection.start.coerceIn(0, newText.length),
                current.selection.end.coerceIn(0, newText.length),
            )
        }
        return current.copy(
            text = newText,
            selection = selection,
        )
    }
}

internal object TextFieldEditModePolicy {
    fun shouldMoveCursor(
        isEditing: Boolean,
        key: Key,
    ): Boolean = isEditing && key.isHorizontalCursorKey()

    fun shouldEnterEditing(
        focused: Boolean,
        isEditing: Boolean,
        key: Key,
    ): Boolean = focused && !isEditing && key.isConfirmKey()
}

internal const val CONNECTED_TAB_DESCRIPTION =
    "Confira as câmeras conectadas. Para organizar posições, use a aba Mosaicos."

private const val MOSAICS_TAB_DESCRIPTION =
    "Organize até 3 mosaicos. Cada câmera pode ficar em apenas um slot."

internal const val OUTSIDE_MOSAIC_TITLE = "Fora do mosaico"

private data class CameraManagerMetrics(
    val scale: Float,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val headerTabsGap: Dp,
    val onvifTabWidth: Dp,
    val rtspTabWidth: Dp,
    val connectedTabWidth: Dp,
    val mosaicsTabWidth: Dp,
    val tabHeight: Dp,
    val contentTopGap: Dp,
    val contentGap: Dp,
    val leftWidth: Dp,
    val rightWidth: Dp,
    val compactButtonWidth: Dp,
    val primaryButtonWidth: Dp,
    val secondaryButtonWidth: Dp,
    val fieldHeight: Dp,
    val statusCardWidth: Dp,
    val statusCardHeight: Dp,
    val rowCardHeight: Dp,
)

private class CameraManagerFocusRequesters {
    val onvifTab = FocusRequester()
    val rtspTab = FocusRequester()
    val connectedTab = FocusRequester()
    val mosaicsTab = FocusRequester()
    val firstContent = FocusRequester()
    val viewMosaic = FocusRequester()
    val onvifUsername = FocusRequester()
    val onvifPassword = FocusRequester()
    val rtspMainUrl = FocusRequester()
    val rtspCopyToSubUrl = FocusRequester()
    val rtspSubUrl = FocusRequester()
    val rtspUsername = FocusRequester()
    val rtspPassword = FocusRequester()

    fun tab(tab: CameraManagerTab): FocusRequester =
        when (tab) {
            CameraManagerTab.ONVIF -> onvifTab
            CameraManagerTab.RTSP -> rtspTab
            CameraManagerTab.CONNECTED -> connectedTab
            CameraManagerTab.MOSAICS -> mosaicsTab
        }
}

@Composable
fun CameraManagerScreen(
    state: CameraManagerUiState,
    onDiscoverOnvif: () -> Unit,
    onSelectOnvifDevice: (String) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSaveSelectedOnvifCamera: () -> Unit,
    onRtspNameChanged: (String) -> Unit,
    onRtspMainUrlChanged: (String) -> Unit,
    onRtspSubUrlChanged: (String) -> Unit,
    onRtspUsernameChanged: (String) -> Unit,
    onRtspPasswordChanged: (String) -> Unit,
    onCopyRtspMainUrlToSubUrl: () -> Unit,
    onConnectManualRtspCamera: () -> Unit,
    onDismissAuthDialog: () -> Unit,
    onOpenMosaic: () -> Unit,
    onSetFreeActiveCamera: (String) -> Unit,
    onSelectActiveMosaic: (Int) -> Unit,
    onPlaceCameraInMosaic: (mosaicIndex: Int, slotIndex: Int, cameraId: String) -> Unit,
    onRemoveCameraFromMosaic: (String) -> Unit,
    onBack: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf(CameraManagerTab.ONVIF) }
    var textInputOpen by remember { mutableStateOf(false) }
    var activeEditingFieldId by rememberSaveable { mutableStateOf<String?>(null) }
    val focusRequesters = remember { CameraManagerFocusRequesters() }
    val keyboardController = LocalSoftwareKeyboardController.current

    BackHandler(enabled = activeEditingFieldId != null) {
        activeEditingFieldId = null
        textInputOpen = false
        keyboardController?.hide()
    }

    BackHandler(enabled = !textInputOpen && activeEditingFieldId == null, onBack = onBack)

    LaunchedEffect(Unit) {
        focusRequesters.onvifTab.requestFocus()
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
    ) {
        val metrics = remember(maxWidth, maxHeight) {
            cameraManagerMetrics(maxWidth, maxHeight)
        }

        SentinelaScreen(
            horizontalPadding = metrics.horizontalPadding,
            verticalPadding = metrics.verticalPadding,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                CameraManagerHeader(
                    metrics = metrics,
                    focusRequesters = focusRequesters,
                    onTabSelected = { selectedTab = it },
                    onOpenMosaic = onOpenMosaic,
                )

                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(SentinelaTvColors.divider),
                )

                Spacer(Modifier.height(metrics.contentTopGap))

                when (selectedTab) {
                    CameraManagerTab.ONVIF -> OnvifTab(
                        state = state,
                        metrics = metrics,
                        focusRequesters = focusRequesters,
                        textInputOpen = textInputOpen,
                        onTextInputOpenChanged = { textInputOpen = it },
                        activeEditingFieldId = activeEditingFieldId,
                        onActiveEditingFieldIdChanged = { activeEditingFieldId = it },
                        onDiscoverOnvif = onDiscoverOnvif,
                        onSelectOnvifDevice = onSelectOnvifDevice,
                        onUsernameChanged = onUsernameChanged,
                        onPasswordChanged = onPasswordChanged,
                        onSaveSelectedOnvifCamera = onSaveSelectedOnvifCamera,
                    )
                    CameraManagerTab.RTSP -> RtspTab(
                        state = state,
                        metrics = metrics,
                        focusRequesters = focusRequesters,
                        textInputOpen = textInputOpen,
                        onTextInputOpenChanged = { textInputOpen = it },
                        activeEditingFieldId = activeEditingFieldId,
                        onActiveEditingFieldIdChanged = { activeEditingFieldId = it },
                        onRtspNameChanged = onRtspNameChanged,
                        onRtspMainUrlChanged = onRtspMainUrlChanged,
                        onRtspSubUrlChanged = onRtspSubUrlChanged,
                        onRtspUsernameChanged = onRtspUsernameChanged,
                        onRtspPasswordChanged = onRtspPasswordChanged,
                        onCopyRtspMainUrlToSubUrl = onCopyRtspMainUrlToSubUrl,
                        onConnectManualRtspCamera = onConnectManualRtspCamera,
                    )
                    CameraManagerTab.CONNECTED -> ConnectedTab(
                        state = state,
                        metrics = metrics,
                        focusRequesters = focusRequesters,
                        onOpenMosaic = onOpenMosaic,
                        onSetFreeActiveCamera = onSetFreeActiveCamera,
                    )
                    CameraManagerTab.MOSAICS -> MosaicsTab(
                        state = state,
                        metrics = metrics,
                        focusRequesters = focusRequesters,
                        onSelectActiveMosaic = onSelectActiveMosaic,
                        onPlaceCameraInMosaic = onPlaceCameraInMosaic,
                        onRemoveCameraFromMosaic = onRemoveCameraFromMosaic,
                    )
                }
            }

            state.authDialogMessage?.let { message ->
                SentinelaTvDialog(
                    title = cameraDialogTitle(message),
                    message = message,
                    confirmLabel = if (state.authDialogAction == CameraManagerDialogAction.ORGANIZE_MOSAIC) {
                        "Organizar mosaico"
                    } else {
                        "OK"
                    },
                    onConfirm = {
                        onDismissAuthDialog()
                        if (state.authDialogAction == CameraManagerDialogAction.ORGANIZE_MOSAIC) {
                            selectedTab = CameraManagerTab.MOSAICS
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun CameraManagerHeader(
    metrics: CameraManagerMetrics,
    focusRequesters: CameraManagerFocusRequesters,
    onTabSelected: (CameraManagerTab) -> Unit,
    onOpenMosaic: () -> Unit,
) {
    Column {
        TitleText("Adicione câmeras por ONVIF ou RTSP direto.", metrics)
        Spacer(Modifier.height(metrics.dp(14f)))
        Row(
            modifier = Modifier
                .focusGroup()
                .fillMaxWidth()
                .padding(bottom = metrics.dp(20f)),
            horizontalArrangement = Arrangement.spacedBy(metrics.headerTabsGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CameraManagerTab.entries.forEach { tab ->
                SentinelaTab(
                    label = tab.label,
                    width = metrics.tabWidth(tab),
                    height = metrics.tabHeight,
                    metrics = metrics,
                    modifier = Modifier
                        .focusRequester(focusRequesters.tab(tab))
                        .focusProperties {
                            down = focusRequesters.firstContent
                        },
                    onClick = { onTabSelected(tab) },
                )
            }
            SentinelaActionButton(
                label = "Ver câmeras",
                onClick = onOpenMosaic,
                width = metrics.dp(176f),
                height = metrics.tabHeight,
                metrics = metrics,
                modifier = Modifier
                    .focusRequester(focusRequesters.viewMosaic)
                    .focusProperties {
                        down = focusRequesters.firstContent
                    },
            )
        }
    }
}

@Composable
private fun OnvifTab(
    state: CameraManagerUiState,
    metrics: CameraManagerMetrics,
    focusRequesters: CameraManagerFocusRequesters,
    textInputOpen: Boolean,
    onTextInputOpenChanged: (Boolean) -> Unit,
    activeEditingFieldId: String?,
    onActiveEditingFieldIdChanged: (String?) -> Unit,
    onDiscoverOnvif: () -> Unit,
    onSelectOnvifDevice: (String) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSaveSelectedOnvifCamera: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.width(metrics.leftWidth),
        ) {
            SectionHeading("Descoberta ONVIF", metrics)
            SectionDescription("Procure dispositivos na rede local, selecione um e conecte com usuário e senha.", metrics)
            Spacer(Modifier.height(metrics.dp(14f)))
            Row(
                horizontalArrangement = Arrangement.spacedBy(metrics.dp(18f)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SentinelaActionButton(
                    label = if (state.scanning) "Procurando ONVIF..." else "Buscar ONVIF na rede",
                    onClick = onDiscoverOnvif,
                    enabled = !state.busy,
                    width = metrics.primaryButtonWidth,
                    height = metrics.dp(62f),
                    metrics = metrics,
                    modifier = Modifier.focusRequester(focusRequesters.firstContent),
                )
                SentinelaActionButton(
                    label = if (state.saving) "Conectando..." else "Conectar selecionado",
                    onClick = onSaveSelectedOnvifCamera,
                    enabled = !state.busy && state.selectedDevice != null,
                    width = metrics.secondaryButtonWidth,
                    height = metrics.dp(56f),
                    metrics = metrics,
                )
            }
        }

        Spacer(Modifier.height(metrics.dp(32f)))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(metrics.contentGap),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.width(metrics.leftWidth),
            ) {
                SectionHeading("Credenciais", metrics)
                Spacer(Modifier.height(metrics.dp(10f)))
                Row(horizontalArrangement = Arrangement.spacedBy(metrics.dp(24f))) {
                    SentinelaTextField(
                        fieldId = CameraTextFieldId.ONVIF_USERNAME,
                        label = "Usuário",
                        value = state.username,
                        onValueChange = onUsernameChanged,
                        width = (metrics.leftWidth - metrics.dp(24f)) / 2,
                        height = metrics.fieldHeight,
                        metrics = metrics,
                        modifier = Modifier
                            .focusRequester(focusRequesters.onvifUsername)
                            .focusProperties {
                                right = focusRequesters.onvifPassword
                            },
                        textInputOpen = textInputOpen,
                        onTextInputOpenChanged = onTextInputOpenChanged,
                        activeEditingFieldId = activeEditingFieldId,
                        onActiveEditingFieldIdChanged = onActiveEditingFieldIdChanged,
                    )
                    SentinelaTextField(
                        fieldId = CameraTextFieldId.ONVIF_PASSWORD,
                        label = "Senha",
                        value = state.password,
                        onValueChange = onPasswordChanged,
                        width = (metrics.leftWidth - metrics.dp(24f)) / 2,
                        height = metrics.fieldHeight,
                        metrics = metrics,
                        password = true,
                        modifier = Modifier
                            .focusRequester(focusRequesters.onvifPassword)
                            .focusProperties {
                                left = focusRequesters.onvifUsername
                            },
                        textInputOpen = textInputOpen,
                        onTextInputOpenChanged = onTextInputOpenChanged,
                        activeEditingFieldId = activeEditingFieldId,
                        onActiveEditingFieldIdChanged = onActiveEditingFieldIdChanged,
                    )
                }
            }
            Column(
                modifier = Modifier.width(metrics.statusCardWidth),
            ) {
                Spacer(Modifier.height(metrics.dp(38f)))
                InfoCard(
                    message = "Usuário e senha são salvos de forma criptografada neste aparelho.",
                    width = metrics.statusCardWidth,
                    height = metrics.dp(76f),
                    metrics = metrics,
                )
            }
        }

        Spacer(Modifier.height(metrics.dp(30f)))

        SectionHeading("Dispositivos encontrados", metrics)
        Spacer(Modifier.height(metrics.dp(10f)))
        OnvifDevicesRow(
            devices = state.discoveredDevices,
            selectedDeviceKey = state.selectedDeviceKey,
            busy = state.busy,
            metrics = metrics,
            onSelectOnvifDevice = onSelectOnvifDevice,
        )
    }
}

@Composable
private fun RtspTab(
    state: CameraManagerUiState,
    metrics: CameraManagerMetrics,
    focusRequesters: CameraManagerFocusRequesters,
    textInputOpen: Boolean,
    onTextInputOpenChanged: (Boolean) -> Unit,
    activeEditingFieldId: String?,
    onActiveEditingFieldIdChanged: (String?) -> Unit,
    onRtspNameChanged: (String) -> Unit,
    onRtspMainUrlChanged: (String) -> Unit,
    onRtspSubUrlChanged: (String) -> Unit,
    onRtspUsernameChanged: (String) -> Unit,
    onRtspPasswordChanged: (String) -> Unit,
    onCopyRtspMainUrlToSubUrl: () -> Unit,
    onConnectManualRtspCamera: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(metrics.contentGap),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.width(metrics.leftWidth),
        ) {
            SectionHeading("RTSP direto", metrics)
            SectionDescription("Informe a URL RTSP e, se necessário, usuário e senha.", metrics)
            Spacer(Modifier.height(metrics.dp(22f)))
            SentinelaTextField(
                fieldId = CameraTextFieldId.RTSP_NAME,
                label = "Nome",
                value = state.rtspName,
                onValueChange = onRtspNameChanged,
                width = metrics.dp(390f),
                height = metrics.fieldHeight,
                metrics = metrics,
                placeholder = "Ex.: Portão",
                modifier = Modifier.focusRequester(focusRequesters.firstContent),
                textInputOpen = textInputOpen,
                onTextInputOpenChanged = onTextInputOpenChanged,
                activeEditingFieldId = activeEditingFieldId,
                onActiveEditingFieldIdChanged = onActiveEditingFieldIdChanged,
            )
            Spacer(Modifier.height(metrics.dp(16f)))
            Row(
                horizontalArrangement = Arrangement.spacedBy(metrics.dp(18f)),
                verticalAlignment = Alignment.Bottom,
            ) {
                SentinelaTextField(
                    fieldId = CameraTextFieldId.RTSP_MAIN_URL,
                    label = "URL RTSP principal",
                    value = state.rtspMainUrl,
                    onValueChange = onRtspMainUrlChanged,
                    width = metrics.leftWidth - metrics.compactButtonWidth - metrics.dp(18f),
                    height = metrics.fieldHeight,
                    metrics = metrics,
                    placeholder = "rtsp://192.168.0.10:554/...",
                    modifier = Modifier
                        .focusRequester(focusRequesters.rtspMainUrl)
                        .focusProperties {
                            right = focusRequesters.rtspCopyToSubUrl
                            down = focusRequesters.rtspSubUrl
                        },
                    textInputOpen = textInputOpen,
                    onTextInputOpenChanged = onTextInputOpenChanged,
                    activeEditingFieldId = activeEditingFieldId,
                    onActiveEditingFieldIdChanged = onActiveEditingFieldIdChanged,
                )
                SentinelaActionButton(
                    label = "Colar embaixo",
                    onClick = onCopyRtspMainUrlToSubUrl,
                    enabled = !state.busy,
                    width = metrics.compactButtonWidth,
                    height = metrics.fieldHeight,
                    metrics = metrics,
                    modifier = Modifier
                        .focusRequester(focusRequesters.rtspCopyToSubUrl)
                        .focusProperties {
                            left = focusRequesters.rtspMainUrl
                            down = focusRequesters.rtspSubUrl
                        },
                )
            }
            Spacer(Modifier.height(metrics.dp(16f)))
            SentinelaTextField(
                fieldId = CameraTextFieldId.RTSP_SUB_URL,
                label = "URL RTSP secundária",
                value = state.rtspSubUrl,
                onValueChange = onRtspSubUrlChanged,
                width = metrics.leftWidth,
                height = metrics.fieldHeight,
                metrics = metrics,
                placeholder = "Opcional",
                modifier = Modifier
                    .focusRequester(focusRequesters.rtspSubUrl)
                    .focusProperties {
                        up = focusRequesters.rtspMainUrl
                        left = FocusRequester.Cancel
                        right = focusRequesters.rtspUsername
                    },
                textInputOpen = textInputOpen,
                onTextInputOpenChanged = onTextInputOpenChanged,
                activeEditingFieldId = activeEditingFieldId,
                onActiveEditingFieldIdChanged = onActiveEditingFieldIdChanged,
            )
        }

        Column(
            modifier = Modifier.width(metrics.rightWidth),
        ) {
            SectionHeading("Credenciais", metrics)
            Spacer(Modifier.height(metrics.dp(16f)))
            SentinelaTextField(
                fieldId = CameraTextFieldId.RTSP_USERNAME,
                label = "Usuário",
                value = state.rtspUsername,
                onValueChange = onRtspUsernameChanged,
                width = metrics.rightWidth,
                height = metrics.fieldHeight,
                metrics = metrics,
                placeholder = "Opcional",
                modifier = Modifier
                    .focusRequester(focusRequesters.rtspUsername)
                    .focusProperties {
                        left = focusRequesters.rtspSubUrl
                        down = focusRequesters.rtspPassword
                    },
                textInputOpen = textInputOpen,
                onTextInputOpenChanged = onTextInputOpenChanged,
                activeEditingFieldId = activeEditingFieldId,
                onActiveEditingFieldIdChanged = onActiveEditingFieldIdChanged,
            )
            Spacer(Modifier.height(metrics.dp(16f)))
            SentinelaTextField(
                fieldId = CameraTextFieldId.RTSP_PASSWORD,
                label = "Senha",
                value = state.rtspPassword,
                onValueChange = onRtspPasswordChanged,
                width = metrics.rightWidth,
                height = metrics.fieldHeight,
                metrics = metrics,
                placeholder = "Opcional",
                password = true,
                modifier = Modifier
                    .focusRequester(focusRequesters.rtspPassword)
                    .focusProperties {
                        left = focusRequesters.rtspCopyToSubUrl
                        up = focusRequesters.rtspUsername
                    },
                textInputOpen = textInputOpen,
                onTextInputOpenChanged = onTextInputOpenChanged,
                activeEditingFieldId = activeEditingFieldId,
                onActiveEditingFieldIdChanged = onActiveEditingFieldIdChanged,
            )
            Spacer(Modifier.height(metrics.dp(14f)))
            InfoCard(
                message = "Credenciais são criptografadas neste aparelho.",
                width = metrics.rightWidth,
                height = metrics.dp(58f),
                metrics = metrics,
            )
            Spacer(Modifier.height(metrics.dp(12f)))
            SentinelaActionButton(
                label = if (state.rtspConnecting) "Conectando..." else "Conectar",
                onClick = onConnectManualRtspCamera,
                enabled = !state.busy,
                width = metrics.rightWidth,
                height = metrics.dp(58f),
                metrics = metrics,
            )
        }
    }
}

@Composable
private fun ConnectedTab(
    state: CameraManagerUiState,
    metrics: CameraManagerMetrics,
    focusRequesters: CameraManagerFocusRequesters,
    onOpenMosaic: () -> Unit,
    onSetFreeActiveCamera: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(metrics.contentGap),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.width(metrics.leftWidth),
        ) {
            SectionHeading("Câmeras conectadas", metrics)
            SectionDescription(CONNECTED_TAB_DESCRIPTION, metrics)
            Spacer(Modifier.height(metrics.dp(20f)))
            if (state.cameras.isEmpty()) {
                CameraListItem(
                    title = "Nenhuma câmera conectada",
                    subtitle = "Use ONVIF ou RTSP direto para conectar uma câmera.",
                    height = metrics.rowCardHeight,
                    metrics = metrics,
                    modifier = Modifier
                        .focusRequester(focusRequesters.firstContent)
                        .focusProperties {
                            right = focusRequesters.viewMosaic
                        },
                )
            } else {
                val sortedCameras = connectedCamerasForDisplay(state.cameras)
                LazyColumn(
                    modifier = Modifier.height(metrics.dp(360f)),
                    verticalArrangement = Arrangement.spacedBy(metrics.dp(8f)),
                ) {
                    itemsIndexed(
                        items = sortedCameras,
                        key = { _, camera -> camera.id },
                    ) { index, camera ->
                        CameraListItem(
                            title = camera.name,
                            subtitle = camera.connectedSubtitle(
                                freeLimitActive = state.freeLimitActive,
                                freeActiveCameraId = state.freeActiveCameraId,
                            ),
                            height = metrics.rowCardHeight,
                            metrics = metrics,
                            onClick = {
                                if (state.freeLimitActive) {
                                    onSetFreeActiveCamera(camera.id)
                                }
                            },
                            modifier = if (index == 0) {
                                Modifier
                                    .focusRequester(focusRequesters.firstContent)
                                    .focusProperties {
                                        right = focusRequesters.viewMosaic
                                    }
                            } else {
                                Modifier.focusProperties {
                                    right = focusRequesters.viewMosaic
                                }
                            },
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.width(metrics.rightWidth),
        ) {
            StatusCard(
                title = "Status",
                message = cameraCountText(state.cameras.size, suffix = " conectadas."),
                width = metrics.statusCardWidth,
                height = metrics.dp(92f),
                metrics = metrics,
            )
            Spacer(Modifier.height(metrics.dp(34f)))
            SectionHeading("Resumo", metrics)
            Spacer(Modifier.height(metrics.dp(14f)))
            PanelText("ONVIF: ${state.cameras.count { it.source is OnvifCameraSource || it.source is DvrRtspChannel }}", metrics)
            Spacer(Modifier.height(metrics.dp(12f)))
            PanelText("RTSP direto: ${state.cameras.count { it.source is RtspCameraSource }}", metrics)
            if (state.freeLimitActive) {
                Spacer(Modifier.height(metrics.dp(12f)))
                PanelText("Modo grátis: 1 câmera ativa", metrics)
            }
        }
    }
}

@Composable
private fun MosaicsTab(
    state: CameraManagerUiState,
    metrics: CameraManagerMetrics,
    focusRequesters: CameraManagerFocusRequesters,
    onSelectActiveMosaic: (Int) -> Unit,
    onPlaceCameraInMosaic: (mosaicIndex: Int, slotIndex: Int, cameraId: String) -> Unit,
    onRemoveCameraFromMosaic: (String) -> Unit,
) {
    var selectedCameraId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedCamera = state.cameras.firstOrNull { camera -> camera.id == selectedCameraId }
    val selectedIsAssigned = selectedCameraId != null &&
        state.mosaicSlots.any { slot -> slot.cameraId == selectedCameraId }

    LaunchedEffect(state.activeMosaicIndex) {
        selectedCameraId = null
    }
    LaunchedEffect(state.cameras, state.mosaicSlots) {
        if (selectedCameraId != null && state.cameras.none { camera -> camera.id == selectedCameraId }) {
            selectedCameraId = null
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(metrics.contentGap),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.width(metrics.leftWidth),
        ) {
            SectionHeading("Mosaicos", metrics)
            SectionDescription(MOSAICS_TAB_DESCRIPTION, metrics)
            Spacer(Modifier.height(metrics.dp(14f)))
            Row(horizontalArrangement = Arrangement.spacedBy(metrics.dp(12f))) {
                repeat(MOSAIC_COUNT) { index ->
                    SentinelaActionButton(
                        label = "Mosaico ${index + 1}",
                        onClick = { onSelectActiveMosaic(index) },
                        width = metrics.dp(150f),
                        height = metrics.dp(48f),
                        metrics = metrics,
                        modifier = if (index == 0) {
                            Modifier.focusRequester(focusRequesters.firstContent)
                        } else {
                            Modifier
                        },
                        selected = index == state.activeMosaicIndex,
                    )
                }
            }
            Spacer(Modifier.height(metrics.dp(20f)))
            SectionDescription(
                text = selectedCamera?.let { camera ->
                    "Selecionada: ${camera.name}. Escolha um slot para mover ou trocar."
                } ?: "Selecione uma câmera e escolha o slot de destino.",
                metrics = metrics,
            )
            Spacer(Modifier.height(metrics.dp(12f)))
            MosaicSlotsGrid(
                state = state,
                selectedCameraId = selectedCameraId,
                metrics = metrics,
                onSlotClick = { slotIndex, camera ->
                    val selectedId = selectedCameraId
                    if (selectedId != null) {
                        onPlaceCameraInMosaic(state.activeMosaicIndex, slotIndex, selectedId)
                        selectedCameraId = null
                    } else if (camera != null) {
                        selectedCameraId = camera.id
                    }
                },
            )
        }

        Column(
            modifier = Modifier.width(metrics.rightWidth),
        ) {
            SectionHeading(OUTSIDE_MOSAIC_TITLE, metrics)
            SectionDescription("Câmeras cadastradas que ainda não ocupam um slot.", metrics)
            Spacer(Modifier.height(metrics.dp(10f)))
            val unassigned = state.unassignedCameras()
            LazyColumn(
                modifier = Modifier.height(metrics.dp(150f)),
                verticalArrangement = Arrangement.spacedBy(metrics.dp(8f)),
            ) {
                if (unassigned.isEmpty()) {
                    item {
                        CameraListItem(
                            title = "Nenhuma câmera",
                            subtitle = "Todas as câmeras estão em um mosaico.",
                            height = metrics.rowCardHeight,
                            metrics = metrics,
                        )
                    }
                } else {
                    itemsIndexed(
                        items = unassigned,
                        key = { _, camera -> camera.id },
                    ) { _, camera ->
                        CameraListItem(
                            title = camera.name,
                            subtitle = if (camera.id == selectedCameraId) {
                                "selecionada"
                            } else {
                                "${camera.source.connectedLabel()} - pressione OK para posicionar"
                            },
                            height = metrics.rowCardHeight,
                            metrics = metrics,
                            onClick = { selectedCameraId = camera.id },
                        )
                    }
                }
            }
            Spacer(Modifier.height(metrics.dp(12f)))
            StatusCard(
                title = "Resumo",
                message = mosaicSummaryText(state),
                width = metrics.statusCardWidth,
                height = metrics.dp(116f),
                metrics = metrics,
            )
            Spacer(Modifier.height(metrics.dp(12f)))
            SentinelaActionButton(
                label = "Remover do mosaico",
                onClick = {
                    selectedCameraId?.let(onRemoveCameraFromMosaic)
                    selectedCameraId = null
                },
                enabled = selectedIsAssigned,
                width = metrics.statusCardWidth,
                height = metrics.dp(54f),
                metrics = metrics,
            )
        }
    }
}

@Composable
private fun MosaicSlotsGrid(
    state: CameraManagerUiState,
    selectedCameraId: String?,
    metrics: CameraManagerMetrics,
    onSlotClick: (slotIndex: Int, camera: Camera?) -> Unit,
) {
    val slotWidth = (metrics.leftWidth - metrics.dp(36f)) / 5
    Column(
        verticalArrangement = Arrangement.spacedBy(metrics.dp(8f)),
    ) {
        repeat(3) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(metrics.dp(9f))) {
                repeat(5) { column ->
                    val slotIndex = row * 5 + column
                    val camera = state.cameraInSlot(state.activeMosaicIndex, slotIndex)
                    MosaicSlotButton(
                        slotNumber = slotIndex + 1,
                        camera = camera,
                        selected = isMosaicSlotSelected(camera?.id, selectedCameraId),
                        width = slotWidth,
                        height = metrics.dp(70f),
                        metrics = metrics,
                        onClick = { onSlotClick(slotIndex, camera) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MosaicSlotButton(
    slotNumber: Int,
    camera: Camera?,
    selected: Boolean,
    width: Dp,
    height: Dp,
    metrics: CameraManagerMetrics,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val occupied = camera != null
    val shape = RoundedCornerShape(metrics.dp(8f))
    val borderColor = when {
        focused -> MaterialTheme.colorScheme.primary
        selected -> MaterialTheme.colorScheme.primary
        occupied -> SentinelaTvColors.fieldBorder
        else -> Color.Transparent
    }
    val backgroundColor = when {
        selected -> SentinelaTvColors.controlSelected
        occupied -> SentinelaTvColors.control
        else -> SentinelaTvColors.panel.copy(alpha = 0.62f)
    }

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp && event.key.isConfirmKey()) {
                    onClick()
                    true
                } else {
                    false
                }
            }
            .semantics { role = Role.Button }
            .background(backgroundColor, shape)
            .border(
                width = if (focused || selected) SentinelaTvSize.focusBorder else 1.dp,
                color = borderColor,
                shape = shape,
            )
            .padding(horizontal = metrics.dp(10f), vertical = metrics.dp(8f))
            .focusable(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = slotNumber.toString(),
                style = TextStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = metrics.sp(14f),
                    lineHeight = metrics.sp(16f),
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                text = camera?.name ?: "Vazio",
                style = TextStyle(
                    color = if (occupied) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        SentinelaTvColors.mutedText
                    },
                    fontSize = metrics.sp(16f),
                    lineHeight = metrics.sp(19f),
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun OnvifDevicesRow(
    devices: List<DiscoveredOnvifDevice>,
    selectedDeviceKey: String?,
    busy: Boolean,
    metrics: CameraManagerMetrics,
    onSelectOnvifDevice: (String) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(metrics.dp(20f)),
        verticalAlignment = Alignment.Top,
    ) {
        if (devices.isEmpty()) {
            SelectableInfoItem(
                title = "Nenhum dispositivo encontrado",
                subtitle = "Use Buscar ONVIF na rede para atualizar a lista.",
                selected = false,
                width = metrics.leftWidth,
                height = metrics.dp(70f),
                metrics = metrics,
                enabled = false,
                onClick = {},
            )
        } else {
            devices.take(2).forEach { device ->
                val selected = selectedDeviceKey == device.stableKey()
                SelectableInfoItem(
                    title = device.displayLabel(),
                    subtitle = device.primaryXAddr().orEmpty(),
                    selected = selected,
                    width = (metrics.leftWidth - metrics.dp(20f)) / 2,
                    height = metrics.dp(70f),
                    metrics = metrics,
                    enabled = !busy,
                    onClick = { onSelectOnvifDevice(device.stableKey()) },
                )
            }
            if (devices.size == 1) {
                SelectableInfoItem(
                    title = "Nenhum outro dispositivo",
                    subtitle = "Use Buscar ONVIF na rede para atualizar a lista.",
                    selected = false,
                    width = (metrics.leftWidth - metrics.dp(20f)) / 2,
                    height = metrics.dp(70f),
                    metrics = metrics,
                    enabled = false,
                    onClick = {},
                )
            }
        }
    }
}

@Composable
private fun SentinelaTab(
    label: String,
    width: Dp,
    height: Dp,
    metrics: CameraManagerMetrics,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(metrics.dp(22f))

    Button(
        onClick = onClick,
        modifier = modifier
            .width(width)
            .height(height)
            .onFocusChanged { focused = it.isFocused }
            .border(
                width = if (focused) SentinelaTvSize.focusBorder else 1.dp,
                color = if (focused) MaterialTheme.colorScheme.primary else SentinelaTvColors.control,
                shape = shape,
            )
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp && event.key.isConfirmKey()) {
                    onClick()
                    true
                } else {
                    false
                }
            },
        scale = ButtonDefaults.scale(focusedScale = 1f),
        shape = ButtonDefaults.shape(
            shape = shape,
            focusedShape = shape,
            pressedShape = shape,
            disabledShape = shape,
        ),
        colors = tabColors(),
        contentPadding = PaddingValues(horizontal = metrics.dp(22f), vertical = 0.dp),
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontSize = metrics.sp(20f),
                lineHeight = metrics.sp(24f),
                fontWeight = FontWeight.SemiBold,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SentinelaActionButton(
    label: String,
    onClick: () -> Unit,
    width: Dp,
    height: Dp,
    metrics: CameraManagerMetrics,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(metrics.dp(24f))

    Button(
        onClick = onClick,
        modifier = modifier
            .width(width)
            .height(height)
            .onFocusChanged { focused = it.isFocused }
            .border(
                width = if (focused || selected) SentinelaTvSize.focusBorder else 1.dp,
                color = when {
                    focused -> MaterialTheme.colorScheme.primary
                    selected -> MaterialTheme.colorScheme.primary
                    else -> SentinelaTvColors.control
                },
                shape = shape,
            )
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp && event.key.isConfirmKey()) {
                    onClick()
                    true
                } else {
                    false
                }
            },
        enabled = enabled,
        scale = ButtonDefaults.scale(focusedScale = 1f),
        shape = ButtonDefaults.shape(
            shape = shape,
            focusedShape = shape,
            pressedShape = shape,
            disabledShape = shape,
        ),
        colors = actionButtonColors(),
        contentPadding = PaddingValues(horizontal = metrics.dp(24f), vertical = 0.dp),
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontSize = metrics.sp(22f),
                lineHeight = metrics.sp(26f),
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SentinelaTextField(
    fieldId: String,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    width: Dp,
    height: Dp,
    metrics: CameraManagerMetrics,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    password: Boolean = false,
    textInputOpen: Boolean,
    onTextInputOpenChanged: (Boolean) -> Unit,
    activeEditingFieldId: String?,
    onActiveEditingFieldIdChanged: (String?) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    var fieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = value,
                selection = TextRange(value.length),
            ),
        )
    }
    val keyboardController = LocalSoftwareKeyboardController.current
    val isEditing = activeEditingFieldId == fieldId
    val keyboardOpen = textInputOpen && isEditing

    LaunchedEffect(value) {
        fieldValue = TextFieldCursorController.syncExternalText(
            current = fieldValue,
            newText = value,
        )
    }

    LaunchedEffect(keyboardOpen) {
        if (keyboardOpen) {
            keyboardController?.show()
        } else {
            keyboardController?.hide()
        }
    }

    fun handleTextFieldKeyEvent(event: androidx.compose.ui.input.key.KeyEvent): Boolean {
        return when {
            TextFieldEditModePolicy.shouldMoveCursor(isEditing, event.key) -> {
                if (event.type == KeyEventType.KeyDown) {
                    fieldValue = TextFieldCursorController.moveCursor(
                        value = fieldValue,
                        direction = if (event.key == Key.DirectionLeft) {
                            CursorMoveDirection.Left
                        } else {
                            CursorMoveDirection.Right
                        },
                    )
                }
                true
            }

            TextFieldEditModePolicy.shouldEnterEditing(
                focused = focused,
                isEditing = isEditing,
                key = event.key,
            ) -> {
                if (event.type == KeyEventType.KeyDown) {
                    onActiveEditingFieldIdChanged(fieldId)
                    onTextInputOpenChanged(true)
                    keyboardController?.show()
                }
                true
            }

            else -> false
        }
    }

    Column(
        modifier = Modifier
            .width(width)
            .onPreviewKeyEvent { event -> handleTextFieldKeyEvent(event) },
        verticalArrangement = Arrangement.spacedBy(metrics.dp(4f)),
    ) {
        LabelText(label, metrics)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .background(SentinelaTvColors.field)
                .border(
                    width = if (focused) SentinelaTvSize.focusBorder else 1.dp,
                    color = if (focused) MaterialTheme.colorScheme.primary else SentinelaTvColors.fieldBorder,
                )
                .padding(horizontal = metrics.dp(14f)),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (fieldValue.text.isBlank() && placeholder.isNotBlank()) {
                Text(
                    text = placeholder,
                    style = fieldTextStyle(metrics).copy(color = SentinelaTvColors.mutedText),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = fieldValue,
                onValueChange = { updatedValue ->
                    fieldValue = updatedValue
                    if (updatedValue.text != value) {
                        onValueChange(updatedValue.text)
                    }
                },
                singleLine = true,
                textStyle = fieldTextStyle(metrics),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (password) KeyboardType.Password else KeyboardType.Text,
                ),
                readOnly = !isEditing,
                visualTransformation = if (password) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
                modifier = Modifier
                    .then(modifier)
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        focused = focusState.isFocused
                        if (focusState.isFocused && !keyboardOpen) {
                            keyboardController?.hide()
                        } else if (!focusState.isFocused) {
                            if (isEditing) {
                                onActiveEditingFieldIdChanged(null)
                            }
                            if (keyboardOpen) {
                                onTextInputOpenChanged(false)
                            }
                            keyboardController?.hide()
                        }
                    }
            )
        }
    }
}

@Composable
private fun StatusCard(
    title: String,
    message: String,
    width: Dp,
    height: Dp,
    metrics: CameraManagerMetrics,
) {
    Column(
        modifier = Modifier
            .width(width)
            .heightIn(min = height)
            .background(SentinelaTvColors.panel, RoundedCornerShape(metrics.dp(10f)))
            .padding(horizontal = metrics.dp(18f), vertical = metrics.dp(14f)),
        verticalArrangement = Arrangement.spacedBy(metrics.dp(8f)),
    ) {
        Text(
            text = title,
            style = TextStyle(
                color = MaterialTheme.colorScheme.primary,
                fontSize = metrics.sp(16f),
                lineHeight = metrics.sp(20f),
                fontWeight = FontWeight.Bold,
            ),
        )
        PanelText(message, metrics)
    }
}

@Composable
private fun InfoCard(
    message: String,
    width: Dp,
    height: Dp,
    metrics: CameraManagerMetrics,
) {
    Box(
        modifier = Modifier
            .width(width)
            .heightIn(min = height)
            .background(SentinelaTvColors.panel, RoundedCornerShape(metrics.dp(10f)))
            .padding(horizontal = metrics.dp(18f), vertical = metrics.dp(12f)),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = message,
            style = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = metrics.sp(18f),
                lineHeight = metrics.sp(24f),
                fontWeight = FontWeight.Normal,
            ),
        )
    }
}

@Composable
private fun SelectableInfoItem(
    title: String,
    subtitle: String,
    selected: Boolean,
    width: Dp,
    height: Dp,
    metrics: CameraManagerMetrics,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(metrics.dp(10f))

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .width(width)
            .height(height)
            .onFocusChanged { focused = it.isFocused }
            .border(
                width = if (focused) SentinelaTvSize.focusBorder else 1.dp,
                color = if (focused) {
                    MaterialTheme.colorScheme.primary
                } else {
                    if (selected) SentinelaTvColors.controlSelected else SentinelaTvColors.field
                },
                shape = shape,
            ),
        scale = ButtonDefaults.scale(focusedScale = 1f),
        shape = ButtonDefaults.shape(
            shape = shape,
            focusedShape = shape,
            pressedShape = shape,
            disabledShape = shape,
        ),
        colors = listItemButtonColors(selected),
        contentPadding = PaddingValues(horizontal = metrics.dp(16f), vertical = metrics.dp(8f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = TextStyle(
                    fontSize = metrics.sp(21f),
                    lineHeight = metrics.sp(25f),
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = TextStyle(
                    fontSize = metrics.sp(15f),
                    lineHeight = metrics.sp(19f),
                    fontWeight = FontWeight.Normal,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CameraListItem(
    title: String,
    subtitle: String,
    height: Dp,
    metrics: CameraManagerMetrics,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(metrics.dp(9f))

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .onFocusChanged { focused = it.isFocused }
            .border(
                width = if (focused) SentinelaTvSize.focusBorder else 1.dp,
                color = if (focused) {
                    MaterialTheme.colorScheme.primary
                } else {
                    SentinelaTvColors.field
                },
                shape = shape,
            ),
        scale = ButtonDefaults.scale(focusedScale = 1f),
        shape = ButtonDefaults.shape(
            shape = shape,
            focusedShape = shape,
            pressedShape = shape,
            disabledShape = shape,
        ),
        colors = listItemButtonColors(selected = false),
        contentPadding = PaddingValues(horizontal = metrics.dp(18f), vertical = metrics.dp(10f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = metrics.sp(21f),
                    lineHeight = metrics.sp(25f),
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = TextStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = metrics.sp(16f),
                    lineHeight = metrics.sp(20f),
                    fontWeight = FontWeight.Normal,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TitleText(
    text: String,
    metrics: CameraManagerMetrics,
) {
    Text(
        text = text,
        style = TextStyle(
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = metrics.sp(32f),
            lineHeight = metrics.sp(38f),
            fontWeight = FontWeight.Bold,
        ),
    )
}

@Composable
private fun SectionHeading(
    text: String,
    metrics: CameraManagerMetrics,
) {
    Text(
        text = text,
        style = TextStyle(
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = metrics.sp(26f),
            lineHeight = metrics.sp(31f),
            fontWeight = FontWeight.Bold,
        ),
    )
}

@Composable
private fun SectionDescription(
    text: String,
    metrics: CameraManagerMetrics,
) {
    Text(
        text = text,
        style = TextStyle(
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = metrics.sp(18f),
            lineHeight = metrics.sp(23f),
            fontWeight = FontWeight.Normal,
        ),
    )
}

@Composable
private fun LabelText(
    text: String,
    metrics: CameraManagerMetrics,
) {
    Text(
        text = text,
        style = TextStyle(
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = metrics.sp(17f),
            lineHeight = metrics.sp(21f),
            fontWeight = FontWeight.Bold,
        ),
    )
}

@Composable
private fun PanelText(
    text: String,
    metrics: CameraManagerMetrics,
) {
    Text(
        text = text,
        style = TextStyle(
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = metrics.sp(19f),
            lineHeight = metrics.sp(25f),
            fontWeight = FontWeight.Normal,
        ),
    )
}

@Composable
private fun tabColors() =
    ButtonDefaults.colors(
        containerColor = SentinelaTvColors.control,
        contentColor = MaterialTheme.colorScheme.onSurface,
        focusedContainerColor = SentinelaTvColors.control,
        focusedContentColor = MaterialTheme.colorScheme.onSurface,
        pressedContainerColor = SentinelaTvColors.control,
        pressedContentColor = MaterialTheme.colorScheme.onSurface,
        disabledContainerColor = SentinelaTvColors.control.copy(alpha = 0.38f),
        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
    )

@Composable
private fun actionButtonColors() =
    ButtonDefaults.colors(
        containerColor = SentinelaTvColors.control,
        contentColor = MaterialTheme.colorScheme.onSurface,
        focusedContainerColor = SentinelaTvColors.control,
        focusedContentColor = MaterialTheme.colorScheme.onSurface,
        pressedContainerColor = SentinelaTvColors.control,
        pressedContentColor = MaterialTheme.colorScheme.onSurface,
        disabledContainerColor = SentinelaTvColors.control.copy(alpha = 0.54f),
        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
    )

@Composable
private fun listItemButtonColors(selected: Boolean) =
    ButtonDefaults.colors(
        containerColor = if (selected) SentinelaTvColors.controlSelected else SentinelaTvColors.field,
        contentColor = MaterialTheme.colorScheme.onSurface,
        focusedContainerColor = if (selected) SentinelaTvColors.controlSelected else SentinelaTvColors.field,
        focusedContentColor = MaterialTheme.colorScheme.onSurface,
        pressedContainerColor = if (selected) SentinelaTvColors.controlSelected else SentinelaTvColors.field,
        pressedContentColor = MaterialTheme.colorScheme.onSurface,
        disabledContainerColor = SentinelaTvColors.field,
        disabledContentColor = if (selected) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f)
        },
    )

@Composable
private fun fieldTextStyle(metrics: CameraManagerMetrics): TextStyle =
    TextStyle(
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = metrics.sp(18f),
        lineHeight = metrics.sp(23f),
        fontWeight = FontWeight.Normal,
    )

private fun cameraManagerMetrics(
    width: Dp,
    height: Dp,
): CameraManagerMetrics {
    val scale = minOf(width.value / 1280f, height.value / 720f)
        .coerceIn(0.65f, 1.0f)

    fun dp(px: Float): Dp = (px * scale).dp

    return CameraManagerMetrics(
        scale = scale,
        horizontalPadding = dp(76f),
        verticalPadding = dp(60f),
        headerTabsGap = dp(14f),
        onvifTabWidth = dp(126f),
        rtspTabWidth = dp(176f),
        connectedTabWidth = dp(170f),
        mosaicsTabWidth = dp(150f),
        tabHeight = dp(50f),
        contentTopGap = dp(30f),
        contentGap = dp(28f),
        leftWidth = dp(705f),
        rightWidth = dp(390f),
        compactButtonWidth = dp(210f),
        primaryButtonWidth = dp(365f),
        secondaryButtonWidth = dp(300f),
        fieldHeight = dp(50f),
        statusCardWidth = dp(390f),
        statusCardHeight = dp(92f),
        rowCardHeight = dp(70f),
    )
}

private fun CameraManagerMetrics.dp(px: Float): Dp = (px * scale).dp

private fun CameraManagerMetrics.sp(px: Float) = (px * scale).sp

private fun CameraManagerMetrics.tabWidth(tab: CameraManagerTab): Dp =
    when (tab) {
        CameraManagerTab.ONVIF -> onvifTabWidth
        CameraManagerTab.RTSP -> rtspTabWidth
        CameraManagerTab.CONNECTED -> connectedTabWidth
        CameraManagerTab.MOSAICS -> mosaicsTabWidth
    }

private fun CameraManagerUiState.cameraInSlot(
    mosaicIndex: Int,
    slotIndex: Int,
): Camera? {
    val cameraId = mosaicSlots.firstOrNull { slot ->
        slot.mosaicIndex == mosaicIndex && slot.slotIndex == slotIndex
    }?.cameraId ?: return null
    return cameras.firstOrNull { camera -> camera.id == cameraId }
}

private fun CameraManagerUiState.unassignedCameras(): List<Camera> {
    val assignedIds = mosaicSlots.mapTo(mutableSetOf()) { slot -> slot.cameraId }
    return connectedCamerasForDisplay(cameras).filterNot { camera -> camera.id in assignedIds }
}

internal fun isMosaicSlotSelected(
    cameraId: String?,
    selectedCameraId: String?,
): Boolean =
    cameraId != null && cameraId == selectedCameraId

internal fun mosaicSummaryText(state: CameraManagerUiState): String =
    buildString {
        repeat(MOSAIC_COUNT) { index ->
            if (index > 0) append('\n')
            val count = state.mosaicSlots.count { slot -> slot.mosaicIndex == index }
            append("Mosaico ${index + 1}: $count câmera")
            if (count != 1) append('s')
        }
        val unassignedCount = state.unassignedCameras().size
        append("\n$OUTSIDE_MOSAIC_TITLE: $unassignedCount câmera")
        if (unassignedCount != 1) append('s')
    }

internal fun cameraDialogTitle(message: String): String =
    when {
        message.contains("modo grátis", ignoreCase = true) ||
            message.contains("atualizada", ignoreCase = true) -> "Câmera ativa atualizada"
        message.contains("conectada", ignoreCase = true) -> "Câmera(s) conectada(s)"
        message.contains("ONVIF", ignoreCase = true) -> "Falha ONVIF"
        else -> "Falha ao conectar câmera"
    }

private fun Camera.connectedSubtitle(
    freeLimitActive: Boolean,
    freeActiveCameraId: String?,
): String {
    val base = "${source.connectedLabel()} - ${streamLabel()}"
    if (!freeLimitActive) return base

    val activeLabel = if (id == freeActiveCameraId) {
        "ativa no modo grátis"
    } else {
        "pressione OK para ativar no modo grátis"
    }
    return "$base - $activeLabel"
}

internal fun connectedCamerasForDisplay(cameras: List<Camera>): List<Camera> =
    cameras.sortedBy { it.position }

private fun Any.connectedLabel(): String =
    when (this) {
        is DvrRtspChannel -> "ONVIF"
        is OnvifCameraSource -> "ONVIF"
        is RtspCameraSource -> "RTSP direto"
        else -> "Câmera"
    }

private fun Camera.streamLabel(): String =
    when (source) {
        is DvrRtspChannel -> "stream principal e secundário"
        is OnvifCameraSource -> if (source.subRtspUrl.isNullOrBlank()) {
            "stream principal"
        } else {
            "stream principal e secundário"
        }
        is RtspCameraSource -> if (source.subRtspUrl.isNullOrBlank()) {
            "stream principal"
        } else {
            "stream principal e secundário"
        }
    }

private fun cameraCountText(
    count: Int,
    suffix: String = "",
): String =
    if (count == 1) {
        "1 câmera$suffix"
    } else {
        "$count câmeras$suffix"
    }

private fun DiscoveredOnvifDevice.primaryXAddr(): String? =
    xAddrs.firstOrNull { address -> address.startsWith("http", ignoreCase = true) }
        ?: xAddrs.firstOrNull()

private fun Key.isConfirmKey(): Boolean =
    this == Key.DirectionCenter ||
        this == Key.Enter ||
        this == Key.NumPadEnter

private fun Key.isHorizontalCursorKey(): Boolean =
    this == Key.DirectionLeft ||
        this == Key.DirectionRight
