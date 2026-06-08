package com.sentinela.camtv.ui.subscription

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.sentinela.camtv.billing.SubscriptionPlan
import com.sentinela.camtv.ui.common.AppInfoFooter
import com.sentinela.camtv.ui.design.SentinelaTvColors
import com.sentinela.camtv.ui.design.SentinelaTvDialog
import com.sentinela.camtv.ui.design.SentinelaTransientMessage

@Composable
fun SubscriptionScreen(
    state: SubscriptionUiState,
    onSubscribe: (SubscriptionPlan) -> Unit,
    onRestoreSubscription: () -> Unit,
    onUpdatePayment: () -> Unit,
    onMessageTimeout: () -> Unit,
    onOpenHome: () -> Unit,
    onBack: () -> Unit,
    footerSuffix: String? = null,
) {
    val focusRequester = remember { FocusRequester() }
    var pendingPlan by remember { mutableStateOf<SubscriptionPlan?>(null) }

    BackHandler(onBack = onBack)

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    pendingPlan?.let { plan ->
        SentinelaTvDialog(
            title = dialogTitle(state.billing, plan),
            message = dialogMessage(state.billing, plan),
            confirmLabel = "Continuar",
            onConfirm = {
                pendingPlan = null
                onSubscribe(plan)
            },
            dismissLabel = "Cancelar",
            onDismiss = { pendingPlan = null },
        )
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
            Column(
                modifier = Modifier.offset(x = 78f.sdp(scale), y = 78f.sdp(scale)),
                verticalArrangement = Arrangement.spacedBy(16f.sdp(scale)),
            ) {
                val actions = subscriptionActions(state.billing)
                val firstFocusableIndex = actions.indexOfFirst { it.enabled }.coerceAtLeast(0)
                actions.forEachIndexed { index, action ->
                    SubscriptionActionButton(
                        action = action,
                        scale = scale,
                        onClick = {
                            when (action.kind) {
                                SubscriptionActionKind.SubscribeMonthly -> pendingPlan = SubscriptionPlan.Monthly
                                SubscriptionActionKind.SubscribeAnnual -> pendingPlan = SubscriptionPlan.Annual
                                SubscriptionActionKind.UpdatePayment -> onUpdatePayment()
                                SubscriptionActionKind.Restore -> onRestoreSubscription()
                                SubscriptionActionKind.Home -> onOpenHome()
                                SubscriptionActionKind.CurrentMonthly,
                                SubscriptionActionKind.CurrentAnnual -> Unit
                            }
                        },
                        modifier = if (index == firstFocusableIndex) {
                            Modifier.focusRequester(focusRequester)
                        } else {
                            Modifier
                        },
                    )
                }
            }

            PlanCard(
                lines = planCardLines(state.billing),
                scale = scale,
                modifier = Modifier
                    .offset(x = 585f.sdp(scale), y = 78f.sdp(scale))
                    .size(width = 575f.sdp(scale), height = 420f.sdp(scale)),
            )

            state.message?.takeIf { it.isNotBlank() }?.let { message ->
                SentinelaTransientMessage(
                    message = message,
                    onTimeout = onMessageTimeout,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 44f.sdp(scale)),
                )
            }

            AppInfoFooter(
                versionName = state.versionName,
                scale = scale,
                modifier = Modifier.offset(x = 82f.sdp(scale), y = 610f.sdp(scale)),
                suffix = footerSuffix,
            )
        }
    }
}

@Composable
private fun PlanCard(
    lines: List<String>,
    scale: Float,
    modifier: Modifier,
) {
    val scrollState = rememberScrollState()
    Box(
        modifier = modifier.background(
            color = SentinelaTvColors.panel,
            shape = RoundedCornerShape(14f.sdp(scale)),
        ),
        contentAlignment = Alignment.TopStart,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28f.sdp(scale), vertical = 22f.sdp(scale))
                .verticalScroll(scrollState),
        ) {
            Text(
                text = "Seu plano",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 22f.ssp(scale),
                lineHeight = 27f.ssp(scale),
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(16f.sdp(scale)))
            lines.forEach { line ->
                Text(
                    text = line,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18f.ssp(scale),
                    lineHeight = 25f.ssp(scale),
                    fontWeight = if (line.endsWith("ativo") || line == "Grátis") {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    },
                )
            }
        }
    }
}

@Composable
private fun SubscriptionActionButton(
    action: SubscriptionAction,
    scale: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentPlanState = action.kind == SubscriptionActionKind.CurrentMonthly ||
        action.kind == SubscriptionActionKind.CurrentAnnual
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(18f.sdp(scale))
    val backgroundColor = when {
        currentPlanState -> SentinelaTvColors.controlSelected
        action.enabled -> SentinelaTvColors.control
        else -> SentinelaTvColors.control.copy(alpha = 0.45f)
    }
    val contentColor = if (action.enabled || currentPlanState) {
        MaterialTheme.colorScheme.onBackground
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f)
    }

    Box(
        modifier = modifier
            .width(430f.sdp(scale))
            .height(64f.sdp(scale))
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (action.enabled && event.type == KeyEventType.KeyUp && event.key.isConfirmKey()) {
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
            .focusable(action.enabled),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = action.label,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30f.sdp(scale)),
            color = contentColor,
            fontSize = 20f.ssp(scale),
            lineHeight = 24f.ssp(scale),
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
