package com.sentinela.camtv.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.sentinela.camtv.BuildConfig
import com.sentinela.camtv.R
import com.sentinela.camtv.config.ProjectLinks
import com.sentinela.camtv.ui.common.AppInfoFooter
import com.sentinela.camtv.ui.design.SentinelaTvColors

@Composable
fun HomeScreen(
    onOpenMosaic: () -> Unit,
    onOpenCameras: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

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
            HomeLogo(
                scale = scale,
                modifier = Modifier
                    .offset(x = 112f.sdp(scale), y = 88f.sdp(scale))
                    .size(width = 96f.sdp(scale), height = 88f.sdp(scale)),
            )
            Text(
                text = "Sentinela Cam TV",
                modifier = Modifier.offset(x = 258f.sdp(scale), y = 76f.sdp(scale)),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 34f.ssp(scale),
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Monitoramento local, privado e open-source.",
                modifier = Modifier.offset(x = 260f.sdp(scale), y = 124f.sdp(scale)),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 18f.ssp(scale),
            )
            Spacer(
                modifier = Modifier
                    .offset(x = 75f.sdp(scale), y = 205f.sdp(scale))
                    .size(width = 1130f.sdp(scale), height = 1.dp)
                    .background(SentinelaTvColors.divider),
            )

            Column(
                modifier = Modifier.offset(x = 78f.sdp(scale), y = 250f.sdp(scale)),
            ) {
                HomeActionButton(
                    label = "Ver câmeras",
                    scale = scale,
                    onClick = onOpenMosaic,
                    modifier = Modifier.focusRequester(focusRequester),
                )
                Spacer(Modifier.height(16f.sdp(scale)))
                HomeActionButton(
                    label = "Cadastrar câmeras",
                    scale = scale,
                    onClick = onOpenCameras,
                )
                Spacer(Modifier.height(16f.sdp(scale)))
                HomeActionButton(
                    label = "Suporte",
                    scale = scale,
                    onClick = onOpenSettings,
                )
            }

            Image(
                painter = painterResource(R.drawable.home_mosaic_preview),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .offset(x = 520f.sdp(scale), y = 238f.sdp(scale))
                    .size(width = 725f.sdp(scale), height = 299f.sdp(scale)),
            )

            Box(
                modifier = Modifier
                    .offset(x = 520f.sdp(scale), y = 566f.sdp(scale))
                    .size(width = 690f.sdp(scale), height = 76f.sdp(scale))
                    .background(
                        color = SentinelaTvColors.panel,
                        shape = RoundedCornerShape(14f.sdp(scale)),
                    ),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = "Privacidade por padrão. Sem anúncios, sem telemetria.",
                    modifier = Modifier.offset(x = 24f.sdp(scale)),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18f.ssp(scale),
                )
            }

            AppInfoFooter(
                versionName = BuildConfig.VERSION_NAME,
                license = "GPL-3.0-or-later",
                siteLabel = ProjectLinks.SITE_LABEL,
                scale = scale,
                modifier = Modifier.offset(x = 82f.sdp(scale), y = 598f.sdp(scale)),
            )
        }
    }
}

@Composable
private fun HomeActionButton(
    label: String,
    scale: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(18f.sdp(scale))

    Box(
        modifier = modifier
            .width(342f.sdp(scale))
            .height(64f.sdp(scale))
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
            .background(SentinelaTvColors.control, shape)
            .border(
                width = if (focused) 3f.sdp(scale) else 0.dp,
                color = if (focused) SentinelaTvColors.controlFocused else Color.Transparent,
                shape = shape,
            )
            .focusable(),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = label,
            modifier = Modifier.offset(x = 32f.sdp(scale)),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 20f.ssp(scale),
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun HomeLogo(
    scale: Float,
    modifier: Modifier = Modifier,
) {
    val background = MaterialTheme.colorScheme.background
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 4f.sdp(scale).toPx())
        val thinStroke = Stroke(width = 2f.sdp(scale).toPx())
        val cyan = SentinelaTvColors.controlFocused

        drawOval(
            color = cyan,
            topLeft = Offset(size.width * 0.05f, size.height * 0.02f),
            size = Size(size.width * 0.9f, size.height * 0.44f),
            style = stroke,
        )
        drawCircle(
            color = cyan,
            radius = size.width * 0.09f,
            center = Offset(size.width * 0.5f, size.height * 0.24f),
        )
        drawCircle(
            color = background,
            radius = size.width * 0.04f,
            center = Offset(size.width * 0.5f, size.height * 0.24f),
        )

        val top = size.height * 0.56f
        val tileGap = size.width * 0.035f
        val tileWidth = size.width * 0.22f
        val tileHeight = size.height * 0.14f
        val startX = (size.width - (tileWidth * 3f + tileGap * 2f)) / 2f

        repeat(3) { index ->
            drawRect(
                color = cyan,
                topLeft = Offset(startX + index * (tileWidth + tileGap), top),
                size = Size(tileWidth, tileHeight),
                style = thinStroke,
            )
        }

        val bottomTileWidth = (tileWidth * 3f + tileGap) / 2f
        val bottomTop = top + tileHeight + tileGap
        repeat(2) { index ->
            drawRect(
                color = cyan,
                topLeft = Offset(startX + index * (bottomTileWidth + tileGap), bottomTop),
                size = Size(bottomTileWidth, tileHeight * 1.15f),
                style = thinStroke,
            )
        }
    }
}

private fun Key.isConfirmKey(): Boolean =
    this == Key.DirectionCenter ||
        this == Key.Enter ||
        this == Key.NumPadEnter

private fun Float.sdp(scale: Float): Dp = (this * scale).dp

private fun Float.ssp(scale: Float) = (this * scale).sp
