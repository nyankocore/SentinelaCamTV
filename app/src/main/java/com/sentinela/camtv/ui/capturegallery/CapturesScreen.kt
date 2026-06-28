package com.sentinela.camtv.ui.capturegallery

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.sentinela.camtv.R
import com.sentinela.camtv.ui.design.SentinelaTransientMessage
import com.sentinela.camtv.ui.design.SentinelaTvColors
import com.sentinela.camtv.ui.text.asString

@Composable
fun CapturesScreen(
    state: CapturesUiState,
    onChoosePhotoLocation: () -> Unit,
    onUseDefaultPhotoLocation: () -> Unit,
    onMessageTimeout: () -> Unit,
    onOpenHome: () -> Unit,
    onBack: () -> Unit,
    customPhotoLocationEnabled: Boolean,
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
            Text(
                text = stringResource(R.string.captures_title),
                modifier = Modifier.offset(x = 76f.sdp(scale), y = 64f.sdp(scale)),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 31f.ssp(scale),
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.captures_subtitle),
                modifier = Modifier.offset(x = 78f.sdp(scale), y = 108f.sdp(scale)),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 18f.ssp(scale),
            )
            Spacer(
                modifier = Modifier
                    .offset(x = 75f.sdp(scale), y = 132f.sdp(scale))
                    .size(width = 1130f.sdp(scale), height = 1.dp)
                    .background(SentinelaTvColors.divider),
            )

            Column(
                modifier = Modifier
                    .offset(x = 78f.sdp(scale), y = 190f.sdp(scale))
                    .focusGroup(),
                verticalArrangement = Arrangement.spacedBy(14f.sdp(scale)),
            ) {
                if (customPhotoLocationEnabled) {
                    CapturesActionButton(
                        label = stringResource(R.string.captures_change_location),
                        scale = scale,
                        onClick = onChoosePhotoLocation,
                        modifier = Modifier.focusRequester(focusRequester),
                    )
                    CapturesActionButton(
                        label = stringResource(R.string.captures_use_default_location),
                        scale = scale,
                        onClick = onUseDefaultPhotoLocation,
                        enabled = state.usingCustomLocation,
                    )
                }
                CapturesActionButton(
                    label = stringResource(R.string.mosaic_quick_home),
                    scale = scale,
                    onClick = onOpenHome,
                    modifier = if (customPhotoLocationEnabled) Modifier else Modifier.focusRequester(focusRequester),
                )
            }

            Column(
                modifier = Modifier.offset(x = 520f.sdp(scale), y = 188f.sdp(scale)),
                verticalArrangement = Arrangement.spacedBy(14f.sdp(scale)),
            ) {
                CaptureInfoCard(
                    title = stringResource(R.string.captures_photos_title),
                    lines = listOf(
                        stringResource(R.string.captures_location_line, state.photoLocationLabel.asString()),
                        state.photoLocationDescription.asString(),
                        stringResource(R.string.captures_photo_format),
                        stringResource(R.string.captures_clean_video),
                    ),
                    scale = scale,
                )
                CaptureInfoCard(
                    title = stringResource(R.string.captures_recording_title),
                    lines = localizedRecordingInfoLines(),
                    scale = scale,
                )
                CaptureInfoCard(
                    title = stringResource(R.string.captures_storage_title),
                    lines = localizedStorageInfoLines(customPhotoLocationEnabled),
                    scale = scale,
                )
            }

            state.message?.let { message ->
                SentinelaTransientMessage(
                    message = message.asString(),
                    onTimeout = onMessageTimeout,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 30f.sdp(scale)),
                )
            }
        }
    }
}

@Composable
private fun localizedRecordingInfoLines(): List<String> = listOf(
    stringResource(R.string.captures_recording_available_fullscreen),
    stringResource(R.string.captures_recording_use_buttons),
    stringResource(R.string.captures_recording_format),
)

internal fun recordingInfoLines(): List<String> = listOf(
    "Disponível na tela cheia pelo Menu Rápido.",
    "Use Iniciar gravação e Parar gravação.",
    "Formato: MP4, com áudio quando a câmera enviar áudio compatível.",
)

@Composable
private fun localizedStorageInfoLines(customPhotoLocationEnabled: Boolean): List<String> =
    if (customPhotoLocationEnabled) {
        listOf(
            stringResource(R.string.captures_storage_choose_folder),
            stringResource(R.string.captures_storage_external_support),
        )
    } else {
        listOf(
            stringResource(R.string.captures_storage_custom_unavailable),
            stringResource(R.string.captures_storage_default_folders),
        )
    }

internal fun storageInfoLines(customPhotoLocationEnabled: Boolean): List<String> =
    if (customPhotoLocationEnabled) {
        listOf(
            "Use Alterar local para escolher uma pasta.",
            "Pendrive, HD ou SSD dependem do suporte do Android TV.",
        )
    } else {
        listOf(
            "Local personalizado indisponível nesta versão.",
            "Fotos e vídeos usam as pastas padrão do Android.",
        )
    }

@Composable
private fun CapturesInfoText(
    text: String,
    scale: Float,
    muted: Boolean = false,
) {
    Text(
        text = text,
        color = if (muted) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onBackground
        },
        fontSize = 17f.ssp(scale),
        lineHeight = 24f.ssp(scale),
    )
}

@Composable
private fun CaptureInfoCard(
    title: String,
    lines: List<String>,
    scale: Float,
) {
    Column(
        modifier = Modifier
            .width(650f.sdp(scale))
            .background(
                color = SentinelaTvColors.panel,
                shape = RoundedCornerShape(12f.sdp(scale)),
            )
            .border(
                width = 1.dp,
                color = SentinelaTvColors.panelBorder,
                shape = RoundedCornerShape(12f.sdp(scale)),
            )
            .padding(horizontal = 22f.sdp(scale), vertical = 18f.sdp(scale)),
        verticalArrangement = Arrangement.spacedBy(6f.sdp(scale)),
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 18f.ssp(scale),
            fontWeight = FontWeight.Bold,
        )
        lines.forEachIndexed { index, line ->
            CapturesInfoText(
                text = line,
                scale = scale,
                muted = index > 0,
            )
        }
    }
}

@Composable
private fun CapturesActionButton(
    label: String,
    scale: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(18f.sdp(scale))

    Box(
        modifier = modifier
            .width(342f.sdp(scale))
            .height(58f.sdp(scale))
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
            .background(
                color = if (enabled) SentinelaTvColors.control else SentinelaTvColors.panel,
                shape = shape,
            )
            .border(
                width = if (focused && enabled) 3f.sdp(scale) else 0.dp,
                color = if (focused && enabled) SentinelaTvColors.controlFocused else Color.Transparent,
                shape = shape,
            )
            .focusable(enabled),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(start = 30f.sdp(scale)),
            color = if (enabled) {
                MaterialTheme.colorScheme.onBackground
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
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
