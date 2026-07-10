package com.sentinela.camtv.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.sentinela.camtv.player.TransmissionMode
import com.sentinela.camtv.ui.design.SentinelaTvColors
import com.sentinela.camtv.ui.design.SentinelaTvShape
import kotlinx.coroutines.launch

data class QuickMenuAction(
    val label: String,
    val onClick: () -> Unit,
)

enum class QuickActionIcon {
    Photo,
    Record,
    Stop,
    Audio,
    Video,
    Info,
    ModeEthernet,
    ModeStability,
    Edit,
    Home,
    Exit,
}

enum class QuickActionDockDirection {
    Left,
    Right,
}

data class QuickActionDockAction(
    val label: String,
    val icon: QuickActionIcon,
    val onClick: () -> Unit,
    val width: Dp = 118.dp,
)

@Composable
fun QuickActionDock(
    actions: List<QuickActionDockAction>,
    modifier: Modifier = Modifier,
    initialFocusedIndex: Int = 0,
) {
    if (actions.isEmpty()) return

    val focusRequesters = remember(actions.size) {
        List(actions.size) { FocusRequester() }
    }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val layoutDirection = LocalLayoutDirection.current
    val safeInitialFocusedIndex = initialFocusedIndex.coerceIn(actions.indices)

    LaunchedEffect(actions.size, safeInitialFocusedIndex) {
        listState.scrollToItem(0)
        focusRequesters[safeInitialFocusedIndex].requestFocus()
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val dockWidth = quickActionDockWidth(
            availableWidth = maxWidth,
            actionWidths = actions.map(QuickActionDockAction::width),
        )

        LazyRow(
            state = listState,
            modifier = Modifier
                .width(dockWidth)
                .heightIn(min = 84.dp)
                .background(
                    color = SentinelaTvColors.panel.copy(alpha = 0.94f),
                    shape = SentinelaTvShape.dialog,
                )
                .border(
                    width = 1.dp,
                    color = SentinelaTvColors.panelBorder,
                    shape = SentinelaTvShape.dialog,
                )
                .focusGroup(),
            contentPadding = PaddingValues(
                horizontal = QUICK_ACTION_DOCK_CONTENT_PADDING,
                vertical = 12.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(QUICK_ACTION_DOCK_ITEM_GAP),
            verticalAlignment = Alignment.CenterVertically,
            userScrollEnabled = false,
        ) {
            itemsIndexed(
                items = actions,
                key = { index, _ -> index },
            ) { index, action ->
                QuickActionDockButton(
                    action = action,
                    onHorizontalMove = { direction ->
                        val targetIndex = quickActionDockTargetIndex(
                            currentIndex = index,
                            actionCount = actions.size,
                            direction = direction,
                            isRtl = layoutDirection == LayoutDirection.Rtl,
                        )
                        if (targetIndex != null) {
                            coroutineScope.launch {
                                val visibleItems = listState.layoutInfo.visibleItemsInfo
                                val scrollAnchor = quickActionDockScrollAnchor(
                                    currentIndex = index,
                                    targetIndex = targetIndex,
                                    actionCount = actions.size,
                                    firstVisibleIndex = visibleItems.minOfOrNull { it.index },
                                    lastVisibleIndex = visibleItems.maxOfOrNull { it.index },
                                )
                                if (scrollAnchor != null) {
                                    listState.animateScrollToItem(scrollAnchor)
                                }
                                focusRequesters[targetIndex].requestFocus()
                            }
                        }
                    },
                    modifier = Modifier.focusRequester(focusRequesters[index]),
                )
            }
        }
    }
}

@Composable
private fun QuickActionDockButton(
    action: QuickActionDockAction,
    onHorizontalMove: (QuickActionDockDirection) -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .width(action.width)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { keyEvent ->
                when (keyEvent.key) {
                    Key.DirectionLeft, Key.DirectionRight -> {
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            onHorizontalMove(
                                if (keyEvent.key == Key.DirectionLeft) {
                                    QuickActionDockDirection.Left
                                } else {
                                    QuickActionDockDirection.Right
                                },
                            )
                        }
                        true
                    }

                    else -> {
                        if (keyEvent.type == KeyEventType.KeyUp && keyEvent.key.isConfirmKey()) {
                            action.onClick()
                            true
                        } else {
                            false
                        }
                    }
                }
            }
            .semantics { role = Role.Button }
            .background(
                color = SentinelaTvColors.control,
                shape = SentinelaTvShape.control,
            )
            .border(
                width = if (focused) 3.dp else 0.dp,
                color = if (focused) SentinelaTvColors.controlFocused else Color.Transparent,
                shape = SentinelaTvShape.control,
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .focusable(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        QuickActionDockIcon(
            icon = action.icon,
            focused = focused,
        )
        Text(
            text = action.label,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun QuickActionDockIcon(
    icon: QuickActionIcon,
    focused: Boolean,
) {
    val color = if (focused) SentinelaTvColors.controlFocused else MaterialTheme.colorScheme.onBackground
    Canvas(modifier = Modifier.size(24.dp)) {
        val stroke = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
        val thinStroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        val w = size.width
        val h = size.height
        when (icon) {
            QuickActionIcon.Photo -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.14f, h * 0.28f),
                    size = Size(w * 0.72f, h * 0.52f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
                    style = stroke,
                )
                drawRect(
                    color = color,
                    topLeft = Offset(w * 0.30f, h * 0.18f),
                    size = Size(w * 0.24f, h * 0.12f),
                )
                drawCircle(
                    color = color,
                    radius = w * 0.15f,
                    center = Offset(w * 0.52f, h * 0.54f),
                    style = stroke,
                )
            }

            QuickActionIcon.Record -> drawCircle(
                color = Color(0xFFFF5A5A),
                radius = w * 0.28f,
                center = Offset(w / 2f, h / 2f),
            )

            QuickActionIcon.Stop -> drawRoundRect(
                color = Color(0xFFFF5A5A),
                topLeft = Offset(w * 0.26f, h * 0.26f),
                size = Size(w * 0.48f, h * 0.48f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
            )

            QuickActionIcon.Audio -> {
                drawLine(color, Offset(w * 0.16f, h * 0.42f), Offset(w * 0.34f, h * 0.42f), strokeWidth = 2.4.dp.toPx())
                drawLine(color, Offset(w * 0.16f, h * 0.58f), Offset(w * 0.34f, h * 0.58f), strokeWidth = 2.4.dp.toPx())
                drawLine(color, Offset(w * 0.34f, h * 0.42f), Offset(w * 0.58f, h * 0.24f), strokeWidth = 2.4.dp.toPx())
                drawLine(color, Offset(w * 0.34f, h * 0.58f), Offset(w * 0.58f, h * 0.76f), strokeWidth = 2.4.dp.toPx())
                drawLine(color, Offset(w * 0.58f, h * 0.24f), Offset(w * 0.58f, h * 0.76f), strokeWidth = 2.4.dp.toPx())
                drawArc(color, -45f, 90f, false, Offset(w * 0.52f, h * 0.30f), Size(w * 0.32f, h * 0.40f), style = thinStroke)
            }

            QuickActionIcon.Video -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.15f, h * 0.24f),
                    size = Size(w * 0.56f, h * 0.52f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
                    style = stroke,
                )
                drawLine(color, Offset(w * 0.71f, h * 0.42f), Offset(w * 0.88f, h * 0.32f), strokeWidth = 2.4.dp.toPx())
                drawLine(color, Offset(w * 0.71f, h * 0.58f), Offset(w * 0.88f, h * 0.68f), strokeWidth = 2.4.dp.toPx())
                drawLine(color, Offset(w * 0.88f, h * 0.32f), Offset(w * 0.88f, h * 0.68f), strokeWidth = 2.4.dp.toPx())
            }

            QuickActionIcon.Info -> {
                drawCircle(color = color, radius = w * 0.34f, center = Offset(w / 2f, h / 2f), style = stroke)
                drawLine(color, Offset(w * 0.50f, h * 0.46f), Offset(w * 0.50f, h * 0.66f), strokeWidth = 2.6.dp.toPx(), cap = StrokeCap.Round)
                drawCircle(color = color, radius = w * 0.035f, center = Offset(w * 0.50f, h * 0.34f))
            }

            QuickActionIcon.ModeEthernet -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.24f, h * 0.14f),
                    size = Size(w * 0.52f, h * 0.54f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                    style = thinStroke,
                )
                repeat(4) { pin ->
                    val x = w * (0.33f + pin * 0.11f)
                    drawLine(
                        color = color,
                        start = Offset(x, h * 0.19f),
                        end = Offset(x, h * 0.34f),
                        strokeWidth = 1.6.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
                drawLine(color, Offset(w * 0.34f, h * 0.50f), Offset(w * 0.42f, h * 0.58f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.42f, h * 0.58f), Offset(w * 0.58f, h * 0.58f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.58f, h * 0.58f), Offset(w * 0.66f, h * 0.50f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.42f, h * 0.68f), Offset(w * 0.42f, h * 0.90f), strokeWidth = 2.2.dp.toPx(), cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.58f, h * 0.68f), Offset(w * 0.58f, h * 0.90f), strokeWidth = 2.2.dp.toPx(), cap = StrokeCap.Round)
            }

            QuickActionIcon.ModeStability -> {
                drawArc(
                    color = color,
                    startAngle = 210f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(w * 0.10f, h * 0.08f),
                    size = Size(w * 0.80f, h * 0.62f),
                    style = stroke,
                )
                drawArc(
                    color = color,
                    startAngle = 215f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = Offset(w * 0.24f, h * 0.28f),
                    size = Size(w * 0.52f, h * 0.40f),
                    style = thinStroke,
                )
                drawCircle(
                    color = color,
                    radius = w * 0.08f,
                    center = Offset(w * 0.50f, h * 0.72f),
                    style = thinStroke,
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.54f, h * 0.66f),
                    end = Offset(w * 0.72f, h * 0.44f),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            QuickActionIcon.Edit -> {
                drawLine(color, Offset(w * 0.25f, h * 0.76f), Offset(w * 0.68f, h * 0.33f), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.62f, h * 0.27f), Offset(w * 0.74f, h * 0.39f), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.20f, h * 0.82f), Offset(w * 0.36f, h * 0.78f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            }

            QuickActionIcon.Home -> {
                drawLine(color, Offset(w * 0.18f, h * 0.50f), Offset(w * 0.50f, h * 0.22f), strokeWidth = 2.6.dp.toPx(), cap = StrokeCap.Round)
                drawLine(color, Offset(w * 0.50f, h * 0.22f), Offset(w * 0.82f, h * 0.50f), strokeWidth = 2.6.dp.toPx(), cap = StrokeCap.Round)
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.28f, h * 0.48f),
                    size = Size(w * 0.44f, h * 0.34f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                    style = thinStroke,
                )
            }

            QuickActionIcon.Exit -> {
                drawArc(color, 35f, 290f, false, Offset(w * 0.22f, h * 0.26f), Size(w * 0.56f, h * 0.56f), style = stroke)
                drawLine(color, Offset(w * 0.50f, h * 0.18f), Offset(w * 0.50f, h * 0.48f), strokeWidth = 2.8.dp.toPx(), cap = StrokeCap.Round)
            }
        }
    }
}

fun TransmissionMode.quickActionModeIcon(): QuickActionIcon = when (this) {
    TransmissionMode.MENOR_LATENCIA -> QuickActionIcon.ModeEthernet
    TransmissionMode.QUALIDADE -> QuickActionIcon.ModeStability
}

internal fun quickActionDockTargetIndex(
    currentIndex: Int,
    actionCount: Int,
    direction: QuickActionDockDirection,
    isRtl: Boolean,
): Int? {
    if (currentIndex !in 0 until actionCount) return null
    val movesForward = if (isRtl) {
        direction == QuickActionDockDirection.Left
    } else {
        direction == QuickActionDockDirection.Right
    }
    val targetIndex = currentIndex + if (movesForward) 1 else -1
    return targetIndex.takeIf { it in 0 until actionCount }
}

internal fun quickActionDockNeedsScrolling(
    availableWidth: Dp,
    actionWidths: List<Dp>,
): Boolean = quickActionDockContentWidth(actionWidths) > quickActionDockSafeWidth(availableWidth)

internal fun quickActionDockScrollAnchor(
    currentIndex: Int,
    targetIndex: Int,
    actionCount: Int,
    firstVisibleIndex: Int?,
    lastVisibleIndex: Int?,
): Int? {
    if (currentIndex !in 0 until actionCount || targetIndex !in 0 until actionCount) return null
    val firstVisible = firstVisibleIndex ?: return null
    val lastVisible = lastVisibleIndex ?: return null
    return when {
        targetIndex > currentIndex &&
            targetIndex >= lastVisible &&
            lastVisible < actionCount - 1 -> actionCount - 1

        targetIndex < currentIndex &&
            targetIndex <= firstVisible &&
            firstVisible > 0 -> 0

        else -> null
    }
}

internal fun quickActionDockWidth(
    availableWidth: Dp,
    actionWidths: List<Dp>,
): Dp = minOf(
    quickActionDockContentWidth(actionWidths),
    quickActionDockSafeWidth(availableWidth),
)

private fun quickActionDockContentWidth(actionWidths: List<Dp>): Dp =
    actionWidths.fold(0.dp) { total, width -> total + width } +
        QUICK_ACTION_DOCK_CONTENT_PADDING * 2 +
        QUICK_ACTION_DOCK_ITEM_GAP * (actionWidths.size - 1).coerceAtLeast(0)

private fun quickActionDockSafeWidth(availableWidth: Dp): Dp =
    maxOf(0.dp, availableWidth - QUICK_ACTION_DOCK_SAFE_MARGIN * 2)

private val QUICK_ACTION_DOCK_SAFE_MARGIN = 16.dp
private val QUICK_ACTION_DOCK_CONTENT_PADDING = 14.dp
private val QUICK_ACTION_DOCK_ITEM_GAP = 8.dp

@Composable
fun QuickMenu(
    actions: List<QuickMenuAction>,
    modifier: Modifier = Modifier,
) {
    val firstItemFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (actions.isNotEmpty()) {
            firstItemFocusRequester.requestFocus()
        }
    }

    Column(
        modifier = modifier
            .widthIn(min = 260.dp, max = 340.dp)
            .background(
                color = SentinelaTvColors.panel.copy(alpha = 0.94f),
                shape = SentinelaTvShape.dialog,
            )
            .border(
                width = 1.dp,
                color = SentinelaTvColors.panelBorder,
                shape = SentinelaTvShape.dialog,
            )
            .padding(18.dp)
            .focusGroup(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        actions.forEachIndexed { index, action ->
            QuickMenuButton(
                label = action.label,
                onClick = action.onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (index == 0) {
                            Modifier.focusRequester(firstItemFocusRequester)
                        } else {
                            Modifier
                        },
                    ),
            )
        }
    }
}

@Composable
private fun QuickMenuButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyUp && keyEvent.key.isConfirmKey()) {
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
                width = if (focused) 3.dp else 0.dp,
                color = if (focused) SentinelaTvColors.controlFocused else Color.Transparent,
                shape = SentinelaTvShape.control,
            )
            .padding(horizontal = 18.dp, vertical = 13.dp)
            .focusable(),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun Key.isConfirmKey(): Boolean =
    this == Key.DirectionCenter ||
        this == Key.Enter ||
        this == Key.NumPadEnter
