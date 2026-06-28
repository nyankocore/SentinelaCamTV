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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.sentinela.camtv.R
import com.sentinela.camtv.localization.AppLanguage
import com.sentinela.camtv.ui.common.AppInfoFooter
import com.sentinela.camtv.ui.design.SentinelaTvColors
import com.sentinela.camtv.ui.text.asString
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onExportSupportLogs: () -> Unit,
    onExportCrashReport: () -> Unit,
    onBack: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    BackHandler(onBack = onBack)

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
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
                    label = stringResource(R.string.support_export_logs),
                    scale = scale,
                    onClick = onExportSupportLogs,
                    modifier = Modifier.focusRequester(focusRequester),
                )
                Spacer(Modifier.height(16f.sdp(scale)))
                SupportActionButton(
                    label = stringResource(R.string.support_export_crashes),
                    scale = scale,
                    onClick = onExportCrashReport,
                )
            }

            state.exportMessage?.let { message ->
                SupportMessageCard(
                    text = message.asString(),
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

    }
}

@Composable
internal fun LanguageDialog(
    selectedLanguage: AppLanguage,
    scale: Float,
    onSelectLanguage: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    val firstFocusRequester = remember { FocusRequester() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        LaunchedEffect(Unit) {
            firstFocusRequester.requestFocus()
        }

        Column(
            modifier = Modifier
                .width(620f.sdp(scale))
                .background(
                    color = SentinelaTvColors.panel,
                    shape = RoundedCornerShape(20f.sdp(scale)),
                )
                .border(
                    width = 2f.sdp(scale),
                    color = SentinelaTvColors.controlFocused,
                    shape = RoundedCornerShape(20f.sdp(scale)),
                )
                .padding(32f.sdp(scale)),
        ) {
            Text(
                text = stringResource(R.string.language_dialog_title),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 26f.ssp(scale),
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8f.sdp(scale)))
            Text(
                text = stringResource(R.string.language_dialog_message),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 17f.ssp(scale),
                lineHeight = 23f.ssp(scale),
            )
            Spacer(Modifier.height(22f.sdp(scale)))

            AppLanguage.selectable.forEachIndexed { index, language ->
                LanguageOptionButton(
                    label = if (language == selectedLanguage) {
                        stringResource(
                            R.string.language_selected_label,
                            stringResource(language.labelRes),
                        )
                    } else {
                        stringResource(language.labelRes)
                    },
                    scale = scale,
                    onClick = { onSelectLanguage(language) },
                    modifier = if (index == 0) {
                        Modifier.focusRequester(firstFocusRequester)
                    } else {
                        Modifier
                    },
                )
                if (index < AppLanguage.selectable.lastIndex) {
                    Spacer(Modifier.height(10f.sdp(scale)))
                }
            }

            Spacer(Modifier.height(18f.sdp(scale)))
            DialogActionButton(
                label = stringResource(R.string.common_cancel),
                scale = scale,
                onClick = onDismiss,
            )
        }
    }
}

@Composable
internal fun UpdateStatusDialog(
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
        state.checkingForUpdate -> stringResource(R.string.update_checking)
        state.downloadingUpdate -> stringResource(R.string.update_downloading)
        state.downloadedUpdate != null -> stringResource(R.string.update_dialog_downloaded_title)
        state.availableUpdate != null -> stringResource(R.string.update_version_available, state.availableUpdate.versionName)
        else -> stringResource(R.string.update_dialog_default_title)
    }
    val message = when {
        state.availableUpdate != null && !state.downloadingUpdate && state.downloadedUpdate == null -> {
            state.availableUpdate.changelog.ifBlank { stringResource(R.string.update_dialog_no_changelog) }
        }
        else -> state.updateMessage?.asString().orEmpty()
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
                    stringResource(R.string.update_dialog_changelog)
                } else {
                    stringResource(R.string.update_dialog_status)
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
                            label = stringResource(R.string.common_download),
                            scale = scale,
                            enabled = !state.checkingForUpdate,
                            onClick = onDownloadUpdate,
                            modifier = Modifier.focusRequester(primaryFocusRequester),
                        )
                        Spacer(Modifier.width(24f.sdp(scale)))
                        DialogActionButton(
                            label = stringResource(R.string.common_close),
                            scale = scale,
                            onClick = onDismiss,
                        )
                    }
                    state.downloadedUpdate != null -> {
                        DialogActionButton(
                            label = stringResource(R.string.common_install),
                            scale = scale,
                            enabled = !state.downloadingUpdate && !state.checkingForUpdate,
                            onClick = onInstallDownloadedUpdate,
                            modifier = Modifier.focusRequester(primaryFocusRequester),
                        )
                        Spacer(Modifier.width(24f.sdp(scale)))
                        DialogActionButton(
                            label = stringResource(R.string.common_close),
                            scale = scale,
                            onClick = onDismiss,
                        )
                    }
                    else -> {
                        DialogActionButton(
                            label = stringResource(R.string.common_close),
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
private fun LanguageOptionButton(
    label: String,
    scale: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TvActionButton(
        label = label,
        scale = scale,
        width = 556f.sdp(scale),
        height = 54f.sdp(scale),
        enabled = true,
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
