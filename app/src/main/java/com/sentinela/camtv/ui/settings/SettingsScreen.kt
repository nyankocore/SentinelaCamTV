package com.sentinela.camtv.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.sentinela.camtv.ui.common.AppInfoFooter
import com.sentinela.camtv.ui.design.SentinelaTvColors
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onExportSupportLogs: () -> Unit,
    onExportCrashReport: () -> Unit,
    onCheckForUpdate: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onInstallDownloadedUpdate: () -> Unit,
    onResumeAfterUpdatePermission: () -> Unit,
    onDismissUpdateDialog: () -> Unit,
    onOpenHome: () -> Unit,
    onBack: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val lifecycleOwner = LocalLifecycleOwner.current
    BackHandler(onBack = onBack)

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    DisposableEffect(lifecycleOwner, onResumeAfterUpdatePermission) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onResumeAfterUpdatePermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        val scale = remember(maxWidth, maxHeight) {
            minOf(maxWidth / 1280.dp, maxHeight / 720.dp)
        }
        val contentWidth = 1280.dp * scale
        val contentHeight = 720.dp * scale

        Box(
            modifier = Modifier
                .size(contentWidth, contentHeight)
                .align(Alignment.Center),
        ) {
            Spacer(
                modifier = Modifier
                    .offset(x = 75f.sdp(scale), y = 132f.sdp(scale))
                    .size(width = 1130f.sdp(scale), height = 1.dp)
                    .background(SentinelaTvColors.divider),
            )

            Column(
                modifier = Modifier.offset(x = 78f.sdp(scale), y = 190f.sdp(scale)),
            ) {
                SupportActionButton(
                    label = "Exportar logs para suporte",
                    scale = scale,
                    onClick = onExportSupportLogs,
                    modifier = Modifier.focusRequester(focusRequester),
                )
                Spacer(Modifier.height(16f.sdp(scale)))
                SupportActionButton(
                    label = "Exportar logs de crashes",
                    scale = scale,
                    onClick = onExportCrashReport,
                )
                Spacer(Modifier.height(16f.sdp(scale)))
                SupportActionButton(
                    label = "Buscar atualização",
                    scale = scale,
                    onClick = onCheckForUpdate,
                    enabled = !state.checkingForUpdate && !state.downloadingUpdate,
                )
                Spacer(Modifier.height(16f.sdp(scale)))
                SupportActionButton(
                    label = "Ir para início",
                    scale = scale,
                    onClick = onOpenHome,
                )
            }

            state.exportMessage?.let { message ->
                SupportMessageCard(
                    text = message,
                    scale = scale,
                    modifier = Modifier
                        .offset(x = 585f.sdp(scale), y = 190f.sdp(scale))
                        .size(width = 575f.sdp(scale), height = 268f.sdp(scale)),
                )
            }

            AppInfoFooter(
                versionName = state.versionName,
                license = state.license,
                siteLabel = state.siteUrl.removePrefix("https://"),
                scale = scale,
                modifier = Modifier.offset(x = 82f.sdp(scale), y = 580f.sdp(scale)),
            )
        }

        if (state.showUpdateDialog) {
            UpdateStatusDialog(
                state = state,
                scale = scale,
                onDownloadUpdate = onDownloadUpdate,
                onInstallDownloadedUpdate = onInstallDownloadedUpdate,
                onDismiss = onDismissUpdateDialog,
            )
        }
    }
}

@Composable
private fun UpdateStatusDialog(
    state: SettingsUiState,
    scale: Float,
    onDownloadUpdate: () -> Unit,
    onInstallDownloadedUpdate: () -> Unit,
    onDismiss: () -> Unit,
) {
    val primaryFocusRequester = remember { FocusRequester() }
    val changelogScrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var changelogFocused by remember { mutableStateOf(false) }
    val title = when {
        state.checkingForUpdate -> "Buscando atualização..."
        state.downloadingUpdate -> "Baixando atualização..."
        state.downloadedUpdate != null -> "Atualização baixada"
        state.availableUpdate != null -> "Versão ${state.availableUpdate.versionName} disponível"
        else -> "Atualização"
    }
    val message = when {
        state.availableUpdate != null && !state.downloadingUpdate && state.downloadedUpdate == null -> {
            state.availableUpdate.changelog.ifBlank { "Sem changelog informado." }
        }
        else -> state.updateMessage.orEmpty()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        LaunchedEffect(Unit) {
            primaryFocusRequester.requestFocus()
        }

        Box(
            modifier = Modifier
                .width(780f.sdp(scale))
                .height(464f.sdp(scale))
                .background(
                    color = SentinelaTvColors.panel,
                    shape = RoundedCornerShape(20f.sdp(scale)),
                )
                .border(
                    width = 2f.sdp(scale),
                    color = SentinelaTvColors.controlFocused,
                    shape = RoundedCornerShape(20f.sdp(scale)),
                ),
        ) {
            Text(
                text = title,
                modifier = Modifier.offset(x = 44f.sdp(scale), y = 42f.sdp(scale)),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 26f.ssp(scale),
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (
                    state.availableUpdate != null &&
                    state.downloadedUpdate == null &&
                    !state.downloadingUpdate
                ) {
                    "Changelog"
                } else {
                    "Status"
                },
                modifier = Modifier.offset(x = 44f.sdp(scale), y = 94f.sdp(scale)),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 18f.ssp(scale),
            )
            Box(
                modifier = Modifier
                    .offset(x = 44f.sdp(scale), y = 134f.sdp(scale))
                    .size(width = 692f.sdp(scale), height = 188f.sdp(scale))
                    .border(
                        width = if (changelogFocused) 2f.sdp(scale) else 1.dp,
                        color = if (changelogFocused) {
                            SentinelaTvColors.controlFocused
                        } else {
                            SentinelaTvColors.panelBorder
                        },
                        shape = RoundedCornerShape(10f.sdp(scale)),
                    )
                    .onFocusChanged { changelogFocused = it.isFocused }
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) {
                            return@onPreviewKeyEvent false
                        }
                        val step = (70f * scale).toInt().coerceAtLeast(1)
                        when (event.key) {
                            Key.DirectionDown -> {
                                if (changelogScrollState.value >= changelogScrollState.maxValue) {
                                    return@onPreviewKeyEvent false
                                }
                                coroutineScope.launch {
                                    changelogScrollState.scrollTo(
                                        (changelogScrollState.value + step)
                                            .coerceAtMost(changelogScrollState.maxValue),
                                    )
                                }
                                true
                            }
                            Key.DirectionUp -> {
                                if (changelogScrollState.value <= 0) {
                                    return@onPreviewKeyEvent false
                                }
                                coroutineScope.launch {
                                    changelogScrollState.scrollTo(
                                        (changelogScrollState.value - step).coerceAtLeast(0),
                                    )
                                }
                                true
                            }
                            else -> false
                        }
                    }
                    .focusable()
                    .verticalScroll(changelogScrollState)
                    .padding(horizontal = 12f.sdp(scale), vertical = 10f.sdp(scale)),
            ) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18f.ssp(scale),
                    lineHeight = 27f.ssp(scale),
                )
            }

            Row(
                modifier = Modifier.offset(x = 40f.sdp(scale), y = 356f.sdp(scale)),
            ) {
                when {
                    state.availableUpdate != null &&
                        state.downloadedUpdate == null &&
                        !state.downloadingUpdate -> {
                        DialogActionButton(
                            label = "Baixar",
                            scale = scale,
                            enabled = !state.checkingForUpdate,
                            onClick = onDownloadUpdate,
                            modifier = Modifier.focusRequester(primaryFocusRequester),
                        )
                        Spacer(Modifier.width(24f.sdp(scale)))
                        DialogActionButton(
                            label = "Fechar",
                            scale = scale,
                            onClick = onDismiss,
                        )
                    }
                    state.downloadedUpdate != null -> {
                        DialogActionButton(
                            label = "Instalar",
                            scale = scale,
                            enabled = !state.downloadingUpdate && !state.checkingForUpdate,
                            onClick = onInstallDownloadedUpdate,
                            modifier = Modifier.focusRequester(primaryFocusRequester),
                        )
                        Spacer(Modifier.width(24f.sdp(scale)))
                        DialogActionButton(
                            label = "Fechar",
                            scale = scale,
                            onClick = onDismiss,
                        )
                    }
                    else -> {
                        DialogActionButton(
                            label = "Fechar",
                            scale = scale,
                            onClick = onDismiss,
                            modifier = Modifier.focusRequester(primaryFocusRequester),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SupportMessageCard(
    text: String,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(
                color = SentinelaTvColors.panel,
                shape = RoundedCornerShape(14f.sdp(scale)),
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        val scrollState = rememberScrollState()
        Text(
            text = text,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24f.sdp(scale), vertical = 16f.sdp(scale))
                .verticalScroll(scrollState),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 16f.ssp(scale),
            lineHeight = 24f.ssp(scale),
        )
    }
}

@Composable
private fun SupportActionButton(
    label: String,
    scale: Float,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    TvActionButton(
        label = label,
        scale = scale,
        width = 390f.sdp(scale),
        height = 64f.sdp(scale),
        enabled = enabled,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun DialogActionButton(
    label: String,
    scale: Float,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    TvActionButton(
        label = label,
        scale = scale,
        width = 220f.sdp(scale),
        height = 62f.sdp(scale),
        enabled = enabled,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun TvActionButton(
    label: String,
    scale: Float,
    width: Dp,
    height: Dp,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(18f.sdp(scale))
    val backgroundColor = if (enabled) {
        SentinelaTvColors.control
    } else {
        SentinelaTvColors.control.copy(alpha = 0.45f)
    }
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onBackground
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    }

    Box(
        modifier = modifier
            .width(width)
            .height(height)
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
            .background(backgroundColor, shape)
            .border(
                width = if (focused) 3f.sdp(scale) else 0.dp,
                color = if (focused) SentinelaTvColors.controlFocused else Color.Transparent,
                shape = shape,
            )
            .focusable(enabled),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = label,
            modifier = Modifier.offset(x = 30f.sdp(scale)),
            color = contentColor,
            fontSize = 20f.ssp(scale),
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun Key.isConfirmKey(): Boolean =
    this == Key.DirectionCenter ||
        this == Key.Enter ||
        this == Key.NumPadEnter

private fun Float.sdp(scale: Float): Dp = (this * scale).dp

private fun Float.ssp(scale: Float) = (this * scale).sp
