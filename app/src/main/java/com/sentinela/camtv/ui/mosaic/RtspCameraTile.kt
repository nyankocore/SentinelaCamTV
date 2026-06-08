package com.sentinela.camtv.ui.mosaic

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sentinela.camtv.BuildConfig
import com.sentinela.camtv.player.CameraStreamRequest
import com.sentinela.camtv.ui.design.SentinelaTvColors
import com.sentinela.camtv.ui.player.RtspPlayerSurface
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RtspCameraTile(
    request: CameraStreamRequest,
    rtspUrl: String,
    showPlayerInfo: Boolean,
    autoQualityDowngraded: Boolean,
    selectedForReorder: Boolean,
    requestInitialFocus: Boolean,
    focusEnabled: Boolean,
    showFocusIndicator: Boolean,
    onMosaicHdSoftwareDecoder: (cameraId: String, reason: String) -> Unit,
    onMosaicHdDecoderFailure: (cameraId: String, reason: String) -> Unit,
    onVideoAspectRatioChanged: (cameraId: String, subtype: Int, width: Int, height: Int) -> Unit,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    var confirmKeyLongClickJob by remember { mutableStateOf<Job?>(null) }
    var confirmKeyDownAtMs by remember { mutableStateOf<Long?>(null) }
    val confirmKeyLongPressState = remember {
        ConfirmKeyLongPressState(CONFIRM_KEY_LONG_PRESS_DELAY_MS)
    }
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val latestOnClick by rememberUpdatedState(onClick)
    val latestOnLongClick by rememberUpdatedState(onLongClick)

    LaunchedEffect(requestInitialFocus, focusEnabled) {
        if (requestInitialFocus && focusEnabled) {
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(focusEnabled) {
        if (!focusEnabled) {
            confirmKeyLongClickJob?.cancel()
            confirmKeyLongClickJob = null
            confirmKeyDownAtMs = null
            confirmKeyLongPressState.reset()
        }
    }

    val showFocusedBorder = focused && focusEnabled && showFocusIndicator
    val tileBorder = cameraTileBorderSpec(
        focused = showFocusedBorder,
        selectedForReorder = selectedForReorder,
    )

    DisposableEffect(Unit) {
        onDispose {
            confirmKeyLongClickJob?.cancel()
        }
    }

    fun logConfirmKey(message: String) {
        if (BuildConfig.DEBUG) {
            Timber.tag("SentinelaInput").d("camera=${request.camera.id} $message")
        }
    }

    fun handleConfirmKeyAction(action: ConfirmKeyPressAction) {
        when (action) {
            ConfirmKeyPressAction.Click -> latestOnClick()
            ConfirmKeyPressAction.LongClick -> latestOnLongClick?.invoke()
            ConfirmKeyPressAction.None -> Unit
        }
    }

    Box(
        modifier = modifier
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { keyEvent ->
                if (!focusEnabled || onLongClick == null || !keyEvent.key.isConfirmKey()) {
                    return@onPreviewKeyEvent false
                }

                when (keyEvent.type) {
                    KeyEventType.KeyDown -> {
                        val nativeEvent = keyEvent.nativeKeyEvent
                        val nowMs = SystemClock.uptimeMillis()
                        if (confirmKeyDownAtMs == null) {
                            confirmKeyDownAtMs = nowMs
                        }
                        val action = confirmKeyLongPressState.onKeyDown(
                            downTimeMs = nowMs,
                            eventTimeMs = nowMs,
                            repeatCount = nativeEvent.repeatCount,
                        )
                        logConfirmKey(
                            "down repeat=${nativeEvent.repeatCount} nativeElapsed=" +
                                "${nativeEvent.eventTime - nativeEvent.downTime} localElapsed=0 action=$action",
                        )
                        if (action == ConfirmKeyPressAction.LongClick) {
                            confirmKeyLongClickJob?.cancel()
                            confirmKeyLongClickJob = null
                            handleConfirmKeyAction(action)
                            return@onPreviewKeyEvent true
                        }

                        if (confirmKeyLongClickJob == null && nativeEvent.repeatCount == 0) {
                            confirmKeyLongClickJob = coroutineScope.launch {
                                delay(CONFIRM_KEY_LONG_PRESS_DELAY_MS)
                                val timerAction = confirmKeyLongPressState.onTimerElapsed(
                                    SystemClock.uptimeMillis(),
                                )
                                val localElapsedMs = SystemClock.uptimeMillis() - (confirmKeyDownAtMs ?: SystemClock.uptimeMillis())
                                logConfirmKey("timer localElapsed=$localElapsedMs action=$timerAction")
                                confirmKeyLongClickJob = null
                                handleConfirmKeyAction(timerAction)
                            }
                        }
                        true
                    }

                    KeyEventType.KeyUp -> {
                        val nativeEvent = keyEvent.nativeKeyEvent
                        val nowMs = SystemClock.uptimeMillis()
                        val localElapsedMs = nowMs - (confirmKeyDownAtMs ?: nowMs)
                        val action = confirmKeyLongPressState.onKeyUp(nowMs)
                        logConfirmKey(
                            "up nativeElapsed=${nativeEvent.eventTime - nativeEvent.downTime} " +
                                "localElapsed=$localElapsedMs action=$action",
                        )
                        confirmKeyLongClickJob?.cancel()
                        confirmKeyLongClickJob = null
                        confirmKeyDownAtMs = null
                        handleConfirmKeyAction(action)
                        true
                    }

                    else -> false
                }
            }
            .then(
                if (tileBorder != null) {
                    Modifier.border(
                        width = tileBorder.width,
                        color = tileBorder.color,
                    )
                } else {
                    Modifier
                },
            )
            .onFocusChanged { focusState ->
                focused = focusState.isFocused
            }
            .combinedClickable(
                enabled = focusEnabled,
                onClick = onClick,
                onLongClick = null,
            )
            .focusable(enabled = focusEnabled),
    ) {
        RtspPlayerSurface(
            request = request,
            rtspUrl = rtspUrl,
            showPlayerInfo = showPlayerInfo,
            autoQualityDowngraded = autoQualityDowngraded,
            onSoftwareDecoderInMosaicHd = onMosaicHdSoftwareDecoder,
            onDecoderFailureInMosaicHd = onMosaicHdDecoderFailure,
            onVideoAspectRatioChanged = onVideoAspectRatioChanged,
            modifier = Modifier.fillMaxSize(),
        )

        if (request.camera.hasAuthenticationFailure) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(SentinelaTvColors.cameraTileInfoScrim)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                BasicText(
                    text = "Bloqueado",
                    style = TextStyle(
                        color = SentinelaTvColors.onVideoOverlay,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}

private const val CONFIRM_KEY_LONG_PRESS_DELAY_MS = 400L

internal data class CameraTileBorderSpec(
    val width: androidx.compose.ui.unit.Dp,
    val color: androidx.compose.ui.graphics.Color,
)

internal fun cameraTileBorderSpec(
    focused: Boolean,
    selectedForReorder: Boolean,
): CameraTileBorderSpec? =
    when {
        selectedForReorder -> CameraTileBorderSpec(
            width = 4.dp,
            color = SentinelaTvColors.cameraTileEditSelectedBorder,
        )

        focused -> CameraTileBorderSpec(
            width = 4.dp,
            color = SentinelaTvColors.cameraTileFocusedBorder,
        )

        else -> null
    }

private fun Key.isConfirmKey(): Boolean =
    this == Key.DirectionCenter ||
        this == Key.Enter ||
        this == Key.NumPadEnter
