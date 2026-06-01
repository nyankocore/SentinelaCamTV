package com.sentinela.camtv.ui.player

import android.os.SystemClock
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.ui.PlayerView
import com.sentinela.camtv.player.AudioMode
import com.sentinela.camtv.player.CameraStreamRequest
import com.sentinela.camtv.player.PlayerMode
import com.sentinela.camtv.player.PlayerConnectionState
import com.sentinela.camtv.player.PlayerErrorMapper
import com.sentinela.camtv.player.PlayerReconnectPolicy
import com.sentinela.camtv.player.ReconnectBackoffPolicy
import com.sentinela.camtv.player.RtspTransportMode
import com.sentinela.camtv.player.StreamQuality
import com.sentinela.camtv.player.applyAudioMode
import com.sentinela.camtv.player.buildRtspPlayer
import com.sentinela.camtv.player.defaultPlayerStreamConfig
import com.sentinela.camtv.player.defaultTransportMode
import com.sentinela.camtv.player.isTerminalError
import com.sentinela.camtv.player.shortMessage
import com.sentinela.camtv.player.statusText
import com.sentinela.camtv.ui.design.SentinelaTvColors
import kotlinx.coroutines.delay
import timber.log.Timber

private const val PLAYER_LOG_TAG = "SentinelaPlayer"
private const val UNKNOWN_BUFFER_MS = -1L
private const val PLAYER_INFO_REFRESH_MS = 1_000L

@Composable
fun RtspPlayerSurface(
    request: CameraStreamRequest,
    rtspUrl: String,
    showPlayerInfo: Boolean,
    modifier: Modifier = Modifier,
    autoQualityDowngraded: Boolean = false,
    onSoftwareDecoderInMosaicHd: (cameraId: String, decoderName: String) -> Unit = { _, _ -> },
    onDecoderFailureInMosaicHd: (cameraId: String, reason: String) -> Unit = { _, _ -> },
    onVideoAspectRatioChanged: (cameraId: String, subtype: Int, width: Int, height: Int) -> Unit = { _, _, _, _ -> },
) {
    var connectionState by remember(rtspUrl, request.subtype, request.audioMode, request.transmissionMode) {
        mutableStateOf<PlayerConnectionState>(PlayerConnectionState.Connecting)
    }
    var videoInfo by remember(rtspUrl, request.subtype) { mutableStateOf("") }
    var videoFrameRate by remember(rtspUrl, request.subtype) { mutableStateOf<Float?>(null) }
    var videoMimeType by remember(rtspUrl, request.subtype) { mutableStateOf<String?>(null) }
    var videoCodecs by remember(rtspUrl, request.subtype) { mutableStateOf<String?>(null) }
    var decoderName by remember(rtspUrl, request.subtype) { mutableStateOf<String?>(null) }
    var bandwidthEstimateBps by remember(rtspUrl, request.subtype) { mutableStateOf<Long?>(null) }
    var droppedFrames by remember(rtspUrl, request.subtype) { mutableIntStateOf(0) }
    var reconnectAttempt by remember(rtspUrl, request.subtype, request.transmissionMode) { mutableIntStateOf(0) }
    var reconnectGeneration by remember(rtspUrl, request.subtype, request.transmissionMode) { mutableIntStateOf(0) }
    var reconnectToken by remember(rtspUrl, request.subtype, request.transmissionMode) { mutableIntStateOf(0) }
    var playerEnabled by remember(rtspUrl, request.subtype, request.transmissionMode) { mutableStateOf(true) }
    var consecutiveFailures by remember(rtspUrl, request.subtype, request.transmissionMode) { mutableIntStateOf(0) }
    var readyMs by remember(rtspUrl, request.subtype, reconnectGeneration) { mutableStateOf<Long?>(null) }
    var firstFrameMs by remember(rtspUrl, request.subtype, reconnectGeneration) { mutableStateOf<Long?>(null) }
    var renderedFirstFrame by remember(rtspUrl, request.subtype, reconnectGeneration) { mutableStateOf(false) }
    var lastReconnectReason by remember(rtspUrl, request.subtype, request.transmissionMode) { mutableStateOf<String?>(null) }
    var lastWatchdogReason by remember(rtspUrl, request.subtype, request.transmissionMode) { mutableStateOf<String?>(null) }
    var lastError by remember(rtspUrl, request.subtype, request.transmissionMode) { mutableStateOf<String?>(null) }
    var softwareDecoderReported by remember(rtspUrl, request.subtype, request.mode) { mutableStateOf(false) }
    var reportedVideoSize by remember(rtspUrl, request.subtype) { mutableStateOf<Pair<Int, Int>?>(null) }
    var decoderFallbackEnabled by remember(
        rtspUrl,
        request.subtype,
        request.transmissionMode,
        request.enableDecoderFallback,
    ) {
        mutableStateOf(request.enableDecoderFallback)
    }
    var transportMode by remember(rtspUrl, request.subtype, request.transmissionMode) {
        mutableStateOf(request.transmissionMode.defaultTransportMode())
    }
    val initialTransportMode = remember(rtspUrl, request.subtype, request.transmissionMode) {
        request.transmissionMode.defaultTransportMode()
    }

    val context = LocalContext.current
    val showPlayerInfoState by rememberUpdatedState(showPlayerInfo)
    val backoffPolicy = remember { ReconnectBackoffPolicy() }
    val streamConfig = remember(
        request.mode,
        request.audioMode,
        request.transmissionMode,
        transportMode,
        decoderFallbackEnabled,
    ) {
        defaultPlayerStreamConfig(
            mode = request.mode,
            audioMode = request.audioMode,
            transmissionMode = request.transmissionMode,
            transportMode = transportMode,
            enableDecoderFallback = decoderFallbackEnabled,
        )
    }
    val playerStartedAtMs = remember(
        rtspUrl,
        request.mode,
        request.audioMode,
        request.transmissionMode,
        reconnectGeneration,
        transportMode,
        decoderFallbackEnabled,
    ) {
        SystemClock.elapsedRealtime()
    }

    val player = if (playerEnabled) {
        remember(
            rtspUrl,
            request.mode,
            request.audioMode,
            request.transmissionMode,
            reconnectGeneration,
            transportMode,
            decoderFallbackEnabled,
        ) {
            logRtspInfo(
                request = request,
                transportMode = transportMode,
                reconnectAttempt = reconnectAttempt,
                consecutiveFailures = consecutiveFailures,
                message = "criando player",
            )
            buildRtspPlayer(
                context = context,
                rtspUrl = rtspUrl,
                streamConfig = streamConfig,
            )
        }
    } else {
        null
    }

    LaunchedEffect(player, request.audioMode) {
        player?.applyAudioMode(request.audioMode)
    }

    fun scheduleReconnect(
        message: String,
        logMessage: String = message,
        throwable: Throwable? = null,
    ) {
        if (!playerEnabled || connectionState is PlayerConnectionState.Reconnecting) {
            return
        }

        val nextFailureCount = consecutiveFailures + 1
        val nextTransportMode = if (request.transmissionMode.defaultTransportMode() == RtspTransportMode.TcpOnly) {
            RtspTransportMode.TcpOnly
        } else {
            PlayerReconnectPolicy.transportModeForFailureCount(nextFailureCount)
        }
        lastReconnectReason = message
        if (
            message.contains("watchdog", ignoreCase = true) ||
            logMessage.contains("watchdog", ignoreCase = true) ||
            message.contains("sem primeiro frame", ignoreCase = true) ||
            logMessage.contains("sem primeiro frame", ignoreCase = true) ||
            logMessage.contains("black frame", ignoreCase = true)
        ) {
            lastWatchdogReason = logMessage
        }

        logRtspWarning(
            request = request,
            transportMode = nextTransportMode,
            reconnectAttempt = reconnectAttempt,
            consecutiveFailures = nextFailureCount,
            message = "agendando reconexao: $logMessage",
            throwable = throwable,
        )

        consecutiveFailures = nextFailureCount
        transportMode = nextTransportMode
        connectionState = PlayerConnectionState.Reconnecting(message.ifBlank { null })
        playerEnabled = false
        reconnectToken += 1
    }

    fun clearRecoveredDiagnostics() {
        lastError = null
        lastReconnectReason = null
        lastWatchdogReason = null
    }

    fun reportVideoSize(
        width: Int,
        height: Int,
    ) {
        if (width <= 0 || height <= 0) return

        videoInfo = "${width}x$height"
        val nextSize = width to height
        if (reportedVideoSize != nextSize) {
            reportedVideoSize = nextSize
            onVideoAspectRatioChanged(request.camera.id, request.subtype, width, height)
        }
    }

    LaunchedEffect(reconnectToken) {
        if (reconnectToken == 0) {
            return@LaunchedEffect
        }

        val delayMs = backoffPolicy.delayForAttempt(
            attempt = reconnectAttempt,
            jitterSeed = request.camera.id,
        )
        logRtspInfo(
            request = request,
            transportMode = transportMode,
            reconnectAttempt = reconnectAttempt,
            consecutiveFailures = consecutiveFailures,
            message = "aguardando ${delayMs}ms antes da reconexao",
        )
        delay(delayMs)
        reconnectAttempt += 1
        connectionState = PlayerConnectionState.Connecting
        reconnectGeneration += 1
        playerEnabled = true
    }

    LaunchedEffect(playerEnabled, reconnectGeneration, connectionState, request.mode) {
        if (!playerEnabled) {
            return@LaunchedEffect
        }

        val watchedState = connectionState
        val timeoutMs = PlayerReconnectPolicy.watchdogTimeoutFor(
            mode = request.mode,
            state = watchedState,
        ) ?: return@LaunchedEffect

        delay(timeoutMs)

        if (playerEnabled && connectionState == watchedState) {
            scheduleReconnect(
                "watchdog: ${watchedState.logLabel()} por ${timeoutMs}ms",
            )
        }
    }

    LaunchedEffect(playerEnabled, reconnectGeneration, connectionState, renderedFirstFrame, request.mode) {
        if (!playerEnabled || connectionState !is PlayerConnectionState.Playing || renderedFirstFrame) {
            return@LaunchedEffect
        }

        val timeoutMs = PlayerReconnectPolicy.firstFrameWatchdogTimeoutFor(request.mode)
        delay(timeoutMs)

        if (playerEnabled && connectionState is PlayerConnectionState.Playing && !renderedFirstFrame) {
            val reason = if (videoInfo.isBlank()) {
                "sem primeiro frame"
            } else {
                "playing sem vídeo útil"
            }
            scheduleReconnect(
                message = "watchdog: $reason",
                logMessage = "black frame watchdog: $reason; surface sem frame renderizado",
            )
        }
    }

    DisposableEffect(player) {
        if (player == null) {
            onDispose {}
        } else {
            val listener = object : Player.Listener {
                private var readyLogged = false
                private var playingLogged = false

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> PlayerConnectionState.Buffering
                        Player.STATE_READY -> PlayerConnectionState.Playing
                        Player.STATE_ENDED -> {
                            scheduleReconnect("stream finalizado")
                            PlayerConnectionState.Reconnecting("Estado: stream finalizado")
                        }

                        Player.STATE_IDLE -> PlayerConnectionState.Connecting
                        else -> connectionState
                    }.also { nextState ->
                        if (nextState is PlayerConnectionState.Playing && !readyLogged) {
                            readyLogged = true
                            readyMs = SystemClock.elapsedRealtime() - playerStartedAtMs
                            clearRecoveredDiagnostics()
                            logRtspInfo(
                                request = request,
                                transportMode = transportMode,
                                reconnectAttempt = reconnectAttempt,
                                consecutiveFailures = consecutiveFailures,
                                message = "READY em ${readyMs}ms",
                            )
                        }

                        if (connectionState !is PlayerConnectionState.Reconnecting) {
                            connectionState = nextState
                        }
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        clearRecoveredDiagnostics()
                        if (!playingLogged) {
                            playingLogged = true
                            logRtspInfo(
                                request = request,
                                transportMode = transportMode,
                                reconnectAttempt = reconnectAttempt,
                                consecutiveFailures = consecutiveFailures,
                                message = "reproducao ativa; resetando contadores",
                            )
                        }
                        reconnectAttempt = 0
                        consecutiveFailures = 0
                        connectionState = PlayerConnectionState.Playing
                    }
                }

                override fun onRenderedFirstFrame() {
                    if (!renderedFirstFrame) {
                        renderedFirstFrame = true
                        firstFrameMs = SystemClock.elapsedRealtime() - playerStartedAtMs
                        clearRecoveredDiagnostics()
                        logRtspInfo(
                            request = request,
                            transportMode = transportMode,
                            reconnectAttempt = reconnectAttempt,
                            consecutiveFailures = consecutiveFailures,
                            message = "primeiro frame renderizado em ${firstFrameMs}ms",
                        )
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    val mappedState = PlayerErrorMapper.map(error, transportMode)
                    if (
                        mappedState == PlayerConnectionState.UnsupportedCodec &&
                        request.mode == PlayerMode.Mosaic &&
                        request.subtype == StreamQuality.HD.subtype
                    ) {
                        connectionState = mappedState
                        lastError = error.shortMessage()
                        logRtspWarning(
                            request = request,
                            transportMode = transportMode,
                            reconnectAttempt = reconnectAttempt,
                            consecutiveFailures = consecutiveFailures,
                            message = "falha de decoder em HD no mosaico; mantendo HD ate falha repetida",
                            throwable = error,
                        )
                        onDecoderFailureInMosaicHd(request.camera.id, error.shortMessage())
                        scheduleReconnect(
                            message = mappedState.statusText(),
                            logMessage = error.detailedMessage(),
                            throwable = error,
                        )
                        return
                    }
                    if (
                        mappedState == PlayerConnectionState.UnsupportedCodec &&
                        !decoderFallbackEnabled
                    ) {
                        decoderFallbackEnabled = true
                    }
                    connectionState = mappedState
                    lastError = error.shortMessage()
                    scheduleReconnect(
                        message = mappedState.statusText(),
                        logMessage = error.detailedMessage(),
                        throwable = error,
                    )
                }

                override fun onTracksChanged(tracks: Tracks) {
                    logTracks(request, tracks)
                }

                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    reportVideoSize(videoSize.width, videoSize.height)
                }
            }

            val analyticsListener = object : AnalyticsListener {
                override fun onBandwidthEstimate(
                    eventTime: AnalyticsListener.EventTime,
                    totalLoadTimeMs: Int,
                    totalBytesLoaded: Long,
                    bitrateEstimate: Long,
                ) {
                    if (showPlayerInfoState && bitrateEstimate > 0) {
                        bandwidthEstimateBps = bitrateEstimate
                    }
                }

                override fun onVideoDecoderInitialized(
                    eventTime: AnalyticsListener.EventTime,
                    decoderNameValue: String,
                    initializedTimestampMs: Long,
                    initializationDurationMs: Long,
                ) {
                    decoderName = decoderNameValue
                    if (
                        request.mode == PlayerMode.Mosaic &&
                        request.subtype == StreamQuality.HD.subtype &&
                        DecoderClassifier.classify(decoderNameValue) == DecoderKind.Software &&
                        !softwareDecoderReported
                    ) {
                        softwareDecoderReported = true
                        onSoftwareDecoderInMosaicHd(request.camera.id, decoderNameValue)
                    }
                }

                override fun onVideoInputFormatChanged(
                    eventTime: AnalyticsListener.EventTime,
                    format: Format,
                    decoderReuseEvaluation: DecoderReuseEvaluation?,
                ) {
                    if (showPlayerInfoState) {
                        videoMimeType = format.sampleMimeType
                        videoCodecs = format.codecs
                        videoFrameRate = format.frameRate.takeIf { it > 0f }
                    }
                    reportVideoSize(format.width, format.height)
                }

                override fun onDroppedVideoFrames(
                    eventTime: AnalyticsListener.EventTime,
                    droppedFramesValue: Int,
                    elapsedMs: Long,
                ) {
                    if (showPlayerInfoState) {
                        droppedFrames += droppedFramesValue
                    }
                }
            }

            player.addListener(listener)
            player.addAnalyticsListener(analyticsListener)

            onDispose {
                player.removeAnalyticsListener(analyticsListener)
                player.removeListener(listener)
                player.release()
            }
        }
    }

    val diagnostics = PlayerDiagnostics(
        cameraName = request.camera.name,
        connectionState = connectionState,
        subtype = request.subtype,
        transmissionMode = request.transmissionMode,
        initialTransportMode = initialTransportMode,
        transportMode = transportMode,
        decoderFallbackEnabled = decoderFallbackEnabled,
        autoQualityDowngraded = autoQualityDowngraded,
        videoSize = videoInfo.takeIf { it.isNotBlank() },
        framesPerSecond = videoFrameRate,
        bandwidthEstimateBps = bandwidthEstimateBps,
        bufferMs = player?.totalBufferedDuration
            ?.takeIf { it != C.TIME_UNSET && it >= 0 }
            ?: UNKNOWN_BUFFER_MS,
        mimeType = videoMimeType,
        codecs = videoCodecs,
        decoderName = decoderName,
        droppedFrames = droppedFrames,
        reconnectAttempt = reconnectAttempt,
        consecutiveFailures = consecutiveFailures,
        readyMs = readyMs,
        firstFrameMs = firstFrameMs,
        renderedFirstFrame = renderedFirstFrame,
        lastReconnectReason = lastReconnectReason,
        lastWatchdogReason = lastWatchdogReason,
        lastError = lastError,
    )

    Box(
        modifier = modifier.background(SentinelaTvColors.playerBackground),
    ) {
        PlayerAndroidView(
            player = player,
            modifier = Modifier.fillMaxSize(),
        )

        if (showPlayerInfo) {
            PlayerInfoOverlay(
                diagnostics = diagnostics,
            )
        }
    }
}

private fun logRtspInfo(
    request: CameraStreamRequest,
    transportMode: RtspTransportMode,
    reconnectAttempt: Int,
    consecutiveFailures: Int,
    message: String,
) {
    Timber.tag(PLAYER_LOG_TAG).i(
        request.logPrefix(transportMode, reconnectAttempt, consecutiveFailures) +
            " $message",
    )
}

private fun logRtspWarning(
    request: CameraStreamRequest,
    transportMode: RtspTransportMode,
    reconnectAttempt: Int,
    consecutiveFailures: Int,
    message: String,
    throwable: Throwable? = null,
) {
    val fullMessage = request.logPrefix(transportMode, reconnectAttempt, consecutiveFailures) +
        " $message"
    if (throwable == null) {
        Timber.tag(PLAYER_LOG_TAG).w(fullMessage)
    } else {
        Timber.tag(PLAYER_LOG_TAG).w(throwable, fullMessage)
    }
}

private fun CameraStreamRequest.logPrefix(
    transportMode: RtspTransportMode,
    reconnectAttempt: Int,
    consecutiveFailures: Int,
): String =
    "camera=${camera.name} id=${camera.id} subtype=$subtype mode=$mode audio=$audioMode " +
        "transmission=$transmissionMode rtsp=$transportMode softwareFallback=$enableDecoderFallback " +
        "attempt=$reconnectAttempt failures=$consecutiveFailures"

private fun PlayerConnectionState.logLabel(): String = when (this) {
    PlayerConnectionState.Connecting -> "conectando"
    PlayerConnectionState.Buffering -> "buffer"
    PlayerConnectionState.Playing -> "reproduzindo"
    is PlayerConnectionState.Reconnecting -> "reconectando"
    PlayerConnectionState.NetworkOffline -> "rede offline"
    PlayerConnectionState.ConnectionRefused -> "conexão recusada"
    PlayerConnectionState.AuthenticationFailed -> "autenticação"
    PlayerConnectionState.Timeout -> "tempo esgotado"
    PlayerConnectionState.UnsupportedCodec -> "codec"
    PlayerConnectionState.UdpLikelyBlocked -> "udp instável"
    is PlayerConnectionState.UnknownError -> "erro"
}

private fun PlaybackException.detailedMessage(): String {
    val causeChain = generateSequence(cause) { throwable -> throwable.cause }
        .take(4)
        .joinToString(separator = " <- ") { throwable ->
            "${throwable.javaClass.simpleName}: ${throwable.message ?: "sem mensagem"}"
        }

    return if (causeChain.isBlank()) {
        shortMessage()
    } else {
        "${shortMessage()} | cause=$causeChain"
    }
}

private fun logTracks(
    request: CameraStreamRequest,
    tracks: Tracks,
) {
    if (tracks.groups.isEmpty()) {
        Timber.tag(PLAYER_LOG_TAG).i("${request.basicLogPrefix()} tracks=none")
        return
    }

    tracks.groups.forEachIndexed { groupIndex, group ->
        for (trackIndex in 0 until group.length) {
            val format = group.getTrackFormat(trackIndex)
            val details = buildList {
                add("type=${trackTypeLabel(group.type)}")
                add("mime=${format.sampleMimeType ?: "unknown"}")
                format.codecs?.let { add("codecs=$it") }
                if (format.width > 0 && format.height > 0) {
                    add("size=${format.width}x${format.height}")
                }
                if (format.sampleRate > 0) {
                    add("sampleRate=${format.sampleRate}")
                }
                if (format.channelCount > 0) {
                    add("channels=${format.channelCount}")
                }
                add("supported=${group.isTrackSupported(trackIndex)}")
                add("selected=${group.isTrackSelected(trackIndex)}")
            }.joinToString(separator = " ")

            Timber.tag(PLAYER_LOG_TAG).i(
                "${request.basicLogPrefix()} track[$groupIndex:$trackIndex] $details",
            )
        }
    }
}

private fun CameraStreamRequest.basicLogPrefix(): String =
    "camera=${camera.name} id=${camera.id} subtype=$subtype mode=$mode audio=$audioMode " +
        "transmission=$transmissionMode"

private fun trackTypeLabel(trackType: Int): String = when (trackType) {
    C.TRACK_TYPE_VIDEO -> "video"
    C.TRACK_TYPE_AUDIO -> "audio"
    C.TRACK_TYPE_TEXT -> "text"
    C.TRACK_TYPE_METADATA -> "metadata"
    else -> trackType.toString()
}

@Composable
private fun PlayerAndroidView(
    player: ExoPlayer?,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = false
                isFocusable = false
                isFocusableInTouchMode = false
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
        },
        update = { playerView ->
            playerView.player = player
        },
    )
}

@Composable
private fun BoxScope.PlayerInfoOverlay(
    diagnostics: PlayerDiagnostics,
) {
    val latestDiagnostics by rememberUpdatedState(diagnostics)
    var visibleDiagnostics by remember { mutableStateOf(diagnostics) }

    LaunchedEffect(Unit) {
        while (true) {
            visibleDiagnostics = latestDiagnostics
            delay(PLAYER_INFO_REFRESH_MS)
        }
    }

    val lines = PlayerDiagnosticsFormatter.overlayLines(visibleDiagnostics)
    val compact = visibleDiagnostics.subtype != 0
    val fontSize = if (compact) 9.sp else 12.sp
    val lineHeight = if (compact) 11.sp else 15.sp

    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .background(SentinelaTvColors.playerInfoScrim)
            .widthIn(max = if (compact) 260.dp else 520.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Column {
            lines.forEachIndexed { index, line ->
                BasicText(
                    text = line,
                    style = TextStyle(
                        color = when {
                            visibleDiagnostics.connectionState.isTerminalError() && index >= lines.lastIndex - 1 ->
                                SentinelaTvColors.playerErrorText
                            index == 0 -> SentinelaTvColors.onVideoOverlay
                            else -> SentinelaTvColors.playerInfoText
                        },
                        fontSize = fontSize,
                        lineHeight = lineHeight,
                        fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal,
                    ),
                )
            }
        }
    }
}
