package com.sentinela.camtv.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

object SentinelaTvSpacing {
    val xSmall: Dp = 4.dp
    val small: Dp = 7.dp
    val medium: Dp = 10.dp
    val large: Dp = 16.dp
    val xLarge: Dp = 22.dp
    val screenHorizontal: Dp = 56.dp
    val screenVertical: Dp = 40.dp
    val mosaicOuter: Dp = 16.dp
    val mosaicTileGap: Dp = 4.dp
}

object SentinelaTvSize {
    val buttonMinHeight: Dp = 44.dp
    val tabMinHeight: Dp = 48.dp
    val fieldMinHeight: Dp = 44.dp
    val panelMinHeight: Dp = 420.dp
    val focusBorder: Dp = 2.dp
}

object SentinelaTvPadding {
    val panel = PaddingValues(18.dp)
    val dialog = PaddingValues(22.dp)
}

object SentinelaTvColors {
    val screenBackground = Color(0xFF071821)
    val mosaicBackground = screenBackground
    val playerBackground = Color.Black
    val panel = Color(0xFF0B2833)
    val panelBorder = Color(0xFF315766)
    val control = Color(0xFF123D4C)
    val controlFocused = Color(0xFF22D3F5)
    val controlSelected = Color(0xFF16495A)
    val field = Color(0xFF123A48)
    val fieldBorder = Color(0xFF5F8390)
    val mutedText = Color(0xFFB8D3DB)
    val divider = Color(0xFF1E4654)
    val cameraTileBorder = Color(0xFF375866)
    val cameraTileFocusedBorder = Color(0xFF27D3FF)
    val cameraTileEditSelectedBorder = Color(0xFFFFD166)
    val cameraTileInfoScrim = Color(0xCC000000)
    val playerInfoScrim = Color(0x99000000)
    val playerInfoText = Color(0xFF9CCEDB)
    val playerErrorText = Color(0xFFFF7777)
    val onVideoOverlay = Color.White
}

object SentinelaTvShape {
    val panel = RoundedCornerShape(10.dp)
    val dialog = RoundedCornerShape(14.dp)
    val control = RoundedCornerShape(14.dp)
    val overlay = RoundedCornerShape(12.dp)
}

@Composable
fun SentinelaPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .background(SentinelaTvColors.panel)
            .border(
                width = 1.dp,
                color = SentinelaTvColors.panelBorder,
            )
            .padding(SentinelaTvPadding.panel),
        content = content,
    )
}

@Composable
fun Modifier.sentinelaSelectedBorder(selected: Boolean): Modifier =
    border(
        width = if (selected) SentinelaTvSize.focusBorder else 1.dp,
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.Transparent
        },
    )

fun Modifier.sentinelaButtonSize(): Modifier =
    defaultMinSize(minHeight = SentinelaTvSize.buttonMinHeight)

@Composable
fun SentinelaOverlayCard(
    text: String,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 560.dp,
) {
    Box(
        modifier = modifier
            .widthIn(min = 260.dp, max = maxWidth)
            .background(
                color = SentinelaTvColors.panel.copy(alpha = 0.92f),
                shape = SentinelaTvShape.overlay,
            )
            .border(
                width = 1.dp,
                color = SentinelaTvColors.panelBorder,
                shape = SentinelaTvShape.overlay,
            )
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun SentinelaTvDialog(
    title: String,
    message: String,
    confirmLabel: String = "OK",
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String? = null,
    onDismiss: (() -> Unit)? = null,
) {
    val firstFocusRequester = remember { FocusRequester() }
    val dismissAllowed = onDismiss != null

    Dialog(
        onDismissRequest = { if (dismissAllowed) onDismiss?.invoke() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = dismissAllowed,
            dismissOnClickOutside = false,
        ),
    ) {
        LaunchedEffect(Unit) {
            firstFocusRequester.requestFocus()
        }

        Column(
            modifier = modifier
                .widthIn(min = 360.dp, max = 560.dp)
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (dismissLabel != null && onDismiss != null) {
                    SentinelaDialogButton(
                        label = dismissLabel,
                        onClick = onDismiss,
                        modifier = Modifier.focusRequester(firstFocusRequester),
                    )
                    SentinelaDialogButton(
                        label = confirmLabel,
                        onClick = onConfirm,
                    )
                } else {
                    SentinelaDialogButton(
                        label = confirmLabel,
                        onClick = onConfirm,
                        modifier = Modifier.focusRequester(firstFocusRequester),
                    )
                }
            }
        }
    }
}

@Composable
fun SentinelaDialogButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 112.dp, minHeight = 46.dp)
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
            .background(
                color = SentinelaTvColors.control,
                shape = SentinelaTvShape.control,
            )
            .border(
                width = if (focused) 3.dp else 1.dp,
                color = if (focused) SentinelaTvColors.controlFocused else SentinelaTvColors.panelBorder,
                shape = SentinelaTvShape.control,
            )
            .padding(horizontal = 20.dp, vertical = 11.dp)
            .focusable(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun Key.isConfirmKey(): Boolean =
    this == Key.DirectionCenter ||
        this == Key.Enter ||
        this == Key.NumPadEnter
