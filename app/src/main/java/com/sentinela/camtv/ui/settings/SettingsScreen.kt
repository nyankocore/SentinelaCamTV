package com.sentinela.camtv.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.sentinela.camtv.billing.BillingState
import com.sentinela.camtv.billing.SubscriptionAccess
import com.sentinela.camtv.billing.SubscriptionOffer
import com.sentinela.camtv.billing.SubscriptionPlan
import com.sentinela.camtv.ui.common.AppInfoFooter
import com.sentinela.camtv.ui.design.SentinelaTvColors
import com.sentinela.camtv.ui.design.SentinelaTvDialog

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onSubscribeMonthly: () -> Unit,
    onSubscribeAnnual: () -> Unit,
    onRestoreSubscription: () -> Unit,
    onToggleDiagnostics: () -> Unit,
    onOpenHome: () -> Unit,
    onBack: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    var pendingSubscriptionPlan by remember { mutableStateOf<SubscriptionPlan?>(null) }
    BackHandler(onBack = onBack)

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    pendingSubscriptionPlan?.let { plan ->
        SubscriptionConfirmationDialog(
            state = state,
            plan = plan,
            onConfirm = {
                pendingSubscriptionPlan = null
                when (plan) {
                    SubscriptionPlan.Monthly -> onSubscribeMonthly()
                    SubscriptionPlan.Annual -> onSubscribeAnnual()
                }
            },
            onDismiss = { pendingSubscriptionPlan = null },
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
            Text(
                text = "Assinatura e suporte",
                modifier = Modifier.offset(x = 78f.sdp(scale), y = 58f.sdp(scale)),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 30f.ssp(scale),
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Use o modo grátis com 1 câmera ativa ou assine para liberar o mosaico completo.",
                modifier = Modifier.offset(x = 78f.sdp(scale), y = 100f.sdp(scale)),
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
                modifier = Modifier.offset(x = 78f.sdp(scale), y = 190f.sdp(scale)),
            ) {
                SupportActionButton(
                    label = state.monthlyLabel(),
                    scale = scale,
                    onClick = { pendingSubscriptionPlan = SubscriptionPlan.Monthly },
                    enabled = state.canChoosePlan(SubscriptionPlan.Monthly),
                    modifier = Modifier.focusRequester(focusRequester),
                )
                Spacer(Modifier.height(16f.sdp(scale)))
                SupportActionButton(
                    label = state.annualLabel(),
                    scale = scale,
                    onClick = { pendingSubscriptionPlan = SubscriptionPlan.Annual },
                    enabled = state.canChoosePlan(SubscriptionPlan.Annual),
                )
                Spacer(Modifier.height(16f.sdp(scale)))
                SupportActionButton(
                    label = "Restaurar assinatura",
                    scale = scale,
                    onClick = onRestoreSubscription,
                )
                Spacer(Modifier.height(16f.sdp(scale)))
                SupportActionButton(
                    label = if (state.diagnosticsEnabled) {
                        "Diagnóstico: Ativado"
                    } else {
                        "Diagnóstico: Desativado"
                    },
                    scale = scale,
                    onClick = onToggleDiagnostics,
                )
                Spacer(Modifier.height(16f.sdp(scale)))
                SupportActionButton(
                    label = "Ir para início",
                    scale = scale,
                    onClick = onOpenHome,
                )
            }

            Column(
                modifier = Modifier.offset(x = 585f.sdp(scale), y = 190f.sdp(scale)),
                verticalArrangement = Arrangement.spacedBy(14f.sdp(scale)),
            ) {
                SupportInfoCard(
                    title = "Plano",
                    lines = state.planLines(),
                    scale = scale,
                    width = 575f.sdp(scale),
                    height = 158f.sdp(scale),
                )
                SupportInfoCard(
                    title = "Diagnóstico",
                    lines = state.diagnosticsLines(),
                    scale = scale,
                    width = 575f.sdp(scale),
                    height = 118f.sdp(scale),
                )
                SupportInfoCard(
                    title = "Feedback",
                    lines = listOf("Em breve: canal para sugestões, melhorias e relatos."),
                    scale = scale,
                    width = 575f.sdp(scale),
                    height = 88f.sdp(scale),
                    enabled = false,
                )
            }

            AppInfoFooter(
                versionName = state.versionName,
                scale = scale,
                modifier = Modifier.offset(x = 82f.sdp(scale), y = 610f.sdp(scale)),
            )
        }
    }
}

@Composable
private fun SubscriptionConfirmationDialog(
    state: SettingsUiState,
    plan: SubscriptionPlan,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    SentinelaTvDialog(
        title = state.subscriptionDialogTitle(plan),
        message = state.subscriptionDialogMessage(plan),
        confirmLabel = "Continuar",
        onConfirm = onConfirm,
        dismissLabel = "Cancelar",
        onDismiss = onDismiss,
    )
}

private fun SettingsUiState.monthlyLabel(): String =
    when {
        billing.isCurrentPlan(SubscriptionPlan.Monthly) -> "Plano mensal atual"
        billing.monthlyOffer != null -> "Assinar mensal - ${billing.monthlyOffer.formattedPrice}"
        else -> "Assinar mensal"
    }

private fun SettingsUiState.annualLabel(): String =
    when {
        billing.isCurrentPlan(SubscriptionPlan.Annual) -> "Plano anual atual"
        billing.annualOffer != null -> "Assinar anual - ${billing.annualOffer.formattedPrice}"
        else -> "Assinar anual"
    }

private fun SettingsUiState.canChoosePlan(plan: SubscriptionPlan): Boolean =
    billing.offerFor(plan) != null && !billing.isCurrentPlan(plan)

private fun SettingsUiState.subscriptionDialogTitle(plan: SubscriptionPlan): String =
    if (billing.hasFullAccess && billing.activePlan != null && billing.activePlan != plan) {
        when (plan) {
            SubscriptionPlan.Monthly -> "Trocar para plano mensal"
            SubscriptionPlan.Annual -> "Trocar para plano anual"
        }
    } else {
        when (plan) {
            SubscriptionPlan.Monthly -> "Assinar plano mensal"
            SubscriptionPlan.Annual -> "Assinar plano anual"
        }
    }

private fun SettingsUiState.subscriptionDialogMessage(plan: SubscriptionPlan): String {
    val offer = billing.offerFor(plan)
    return buildString {
        appendLine("Plano: ${offer?.formattedPrice ?: "preço indisponível"}.")
        appendLine("Teste grátis: 7 dias com todos os recursos.")
        if (plan == SubscriptionPlan.Annual) {
            annualSavingsText(
                monthlyPrice = billing.monthlyOffer?.formattedPrice,
                annualPrice = billing.annualOffer?.formattedPrice,
            )?.let { appendLine(it) }
        }
        appendLine()
        if (billing.hasFullAccess) {
            appendLine("A Google Play mostrará as condições finais da troca antes de confirmar.")
        } else {
            appendLine("A cobrança acontece pela Google Play após o teste, se você não cancelar.")
        }
    }.trim()
}

private fun SettingsUiState.planLines(): List<String> = buildList {
    add(accessLabel)
    add("Teste grátis: 7 dias com todos os recursos.")
    billing.monthlyOffer?.let { add("Mensal: ${it.formattedPrice}.") }
    billing.annualOffer?.let { add("Anual: ${it.formattedPrice}.") }
    annualSavingsText(
        monthlyPrice = billing.monthlyOffer?.formattedPrice,
        annualPrice = billing.annualOffer?.formattedPrice,
    )?.let { add(it) }
    if (billing.access == SubscriptionAccess.FreeLimited) {
        add("Modo grátis: escolha 1 câmera ativa em Cadastrar câmeras > Conectadas.")
    }
    message?.let { add(it) }
}

private fun SettingsUiState.diagnosticsLines(): List<String> = listOf(
    "Diagnóstico automático: ${if (diagnosticsEnabled) "ativado" else "desativado"}.",
    "Não enviamos imagens, senhas ou URLs RTSP completas.",
)

private fun BillingState.offerFor(plan: SubscriptionPlan): SubscriptionOffer? =
    when (plan) {
        SubscriptionPlan.Monthly -> monthlyOffer
        SubscriptionPlan.Annual -> annualOffer
    }

private fun BillingState.isCurrentPlan(plan: SubscriptionPlan): Boolean =
    hasFullAccess && activePlan == plan

@Composable
private fun SupportInfoCard(
    title: String,
    lines: List<String>,
    scale: Float,
    width: Dp,
    height: Dp,
    enabled: Boolean = true,
) {
    val scrollState = rememberScrollState()
    val alpha = if (enabled) 1f else 0.58f

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .background(
                color = SentinelaTvColors.panel.copy(alpha = alpha),
                shape = RoundedCornerShape(14f.sdp(scale)),
            ),
        contentAlignment = Alignment.TopStart,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24f.sdp(scale), vertical = 16f.sdp(scale))
                .verticalScroll(scrollState),
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = alpha),
                fontSize = 18f.ssp(scale),
                lineHeight = 23f.ssp(scale),
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8f.sdp(scale)))
            lines.forEachIndexed { index, line ->
                Text(
                    text = line,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = alpha),
                    fontSize = 15f.ssp(scale),
                    lineHeight = 21f.ssp(scale),
                )
                if (index < lines.lastIndex) {
                    Spacer(Modifier.height(4f.sdp(scale)))
                }
            }
        }
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
        width = 430f.sdp(scale),
        height = 64f.sdp(scale),
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
