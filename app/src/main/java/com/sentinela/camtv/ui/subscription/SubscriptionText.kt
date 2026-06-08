package com.sentinela.camtv.ui.subscription

import com.sentinela.camtv.billing.BillingState
import com.sentinela.camtv.billing.SubscriptionOffer
import com.sentinela.camtv.billing.SubscriptionPlan
import com.sentinela.camtv.billing.SubscriptionStatus
import java.math.BigDecimal
import java.math.RoundingMode

internal data class SubscriptionAction(
    val label: String,
    val kind: SubscriptionActionKind,
    val enabled: Boolean = true,
)

internal enum class SubscriptionActionKind {
    SubscribeMonthly,
    SubscribeAnnual,
    CurrentMonthly,
    CurrentAnnual,
    UpdatePayment,
    Restore,
    Home,
}

internal fun planCardLines(state: BillingState): List<String> =
    when (state.status) {
        SubscriptionStatus.FreeTrialEligible -> listOf(
            "Grátis",
            "1 câmera ativa",
            "",
            "Assine para liberar:",
            "• mosaico completo",
            "• todos os recursos",
            "• 7 dias de teste sem cobrança",
        )

        SubscriptionStatus.FreeNoTrial -> listOf(
            "Grátis",
            "1 câmera ativa",
            "",
            "Assine para liberar:",
            "• mosaico completo",
            "• todos os recursos",
        )

        SubscriptionStatus.MonthlyActive -> listOfNotNull(
            "Mensal ativo",
            "Mosaico completo liberado.",
            "",
            annualSavingsText(state.monthlyOffer, state.annualOffer)
                ?.let { "Troque para o anual e economize $it por ano." }
        )

        SubscriptionStatus.AnnualActive -> listOf(
            "Anual ativo",
            "Mosaico completo liberado.",
            "",
            "Você está no plano com melhor custo anual.",
        )

        SubscriptionStatus.GracePeriod -> listOf(
            "Pagamento pendente",
            "Mosaico completo ainda liberado.",
            "",
            "Atualize sua forma de pagamento para evitar bloqueio.",
        )

        SubscriptionStatus.OnHold -> listOf(
            "Assinatura suspensa",
            "1 câmera ativa no modo grátis.",
            "",
            "Atualize o pagamento para restaurar o mosaico completo.",
        )

        SubscriptionStatus.Expired -> listOf(
            "Assinatura expirada",
            "1 câmera ativa no modo grátis.",
            "",
            "Assine novamente para restaurar o mosaico completo.",
        )

        SubscriptionStatus.CanceledUntilExpiry -> listOf(
            "Assinatura cancelada",
            "Mosaico completo liberado até o fim do período atual.",
            "",
            "Você pode assinar novamente quando quiser manter o acesso.",
        )

        SubscriptionStatus.Checking -> listOf("Verificando assinatura...")

        SubscriptionStatus.BillingUnavailable,
        SubscriptionStatus.Error -> listOf(
            "Não foi possível verificar sua assinatura.",
            "",
            "Tente restaurar assinatura ou verifique sua conexão.",
        )
    }

internal fun subscriptionActions(state: BillingState): List<SubscriptionAction> =
    when (state.status) {
        SubscriptionStatus.FreeTrialEligible,
        SubscriptionStatus.FreeNoTrial,
        SubscriptionStatus.Expired,
        SubscriptionStatus.CanceledUntilExpiry -> listOf(
            SubscriptionAction("Assinar mensal - ${state.monthlyPriceLabel()}", SubscriptionActionKind.SubscribeMonthly),
            SubscriptionAction("Assinar anual - ${state.annualPriceLabel()}", SubscriptionActionKind.SubscribeAnnual),
            SubscriptionAction("Restaurar assinatura", SubscriptionActionKind.Restore),
            SubscriptionAction("Ir para início", SubscriptionActionKind.Home),
        )

        SubscriptionStatus.MonthlyActive -> listOf(
            SubscriptionAction("✓ Mensal ativo", SubscriptionActionKind.CurrentMonthly, enabled = false),
            SubscriptionAction("Trocar para anual - ${state.annualPriceLabel()}", SubscriptionActionKind.SubscribeAnnual),
            SubscriptionAction("Restaurar assinatura", SubscriptionActionKind.Restore),
            SubscriptionAction("Ir para início", SubscriptionActionKind.Home),
        )

        SubscriptionStatus.AnnualActive -> listOf(
            SubscriptionAction("Trocar para mensal - ${state.monthlyPriceLabel()}", SubscriptionActionKind.SubscribeMonthly),
            SubscriptionAction("✓ Anual ativo", SubscriptionActionKind.CurrentAnnual, enabled = false),
            SubscriptionAction("Restaurar assinatura", SubscriptionActionKind.Restore),
            SubscriptionAction("Ir para início", SubscriptionActionKind.Home),
        )

        SubscriptionStatus.GracePeriod,
        SubscriptionStatus.OnHold -> listOf(
            SubscriptionAction("Atualizar pagamento", SubscriptionActionKind.UpdatePayment),
            SubscriptionAction("Restaurar assinatura", SubscriptionActionKind.Restore),
            SubscriptionAction("Ir para início", SubscriptionActionKind.Home),
        )

        SubscriptionStatus.Checking,
        SubscriptionStatus.BillingUnavailable,
        SubscriptionStatus.Error -> listOf(
            SubscriptionAction("Restaurar assinatura", SubscriptionActionKind.Restore),
            SubscriptionAction("Ir para início", SubscriptionActionKind.Home),
        )
    }

internal fun dialogTitle(
    state: BillingState,
    plan: SubscriptionPlan,
): String =
    if (state.activePlan != null && state.activePlan != plan) {
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

internal fun dialogMessage(
    state: BillingState,
    plan: SubscriptionPlan,
): String {
    val isPlanChange = state.activePlan != null && state.activePlan != plan
    val offer = state.offerFor(plan)
    return buildString {
        if (isPlanChange) {
            appendLine("${plan.displayName}: ${offer?.formattedPrice ?: state.fallbackPrice(plan)}")
            if (plan == SubscriptionPlan.Annual) {
                annualSavingsText(state.monthlyOffer, state.annualOffer)?.let {
                    appendLine()
                    appendLine("Economia aproximada: $it por ano.")
                }
            }
            appendLine()
            appendLine("A Google Play mostrará as condições finais da troca antes de confirmar.")
        } else {
            appendLine("${plan.displayName}: ${offer?.formattedPrice ?: state.fallbackPrice(plan)}")
            appendLine("Libera o mosaico completo de câmeras.")
            if (state.status == SubscriptionStatus.FreeTrialEligible && offer?.hasFreeTrial == true) {
                appendLine("Inclui 7 dias de teste sem cobrança.")
            }
            if (plan == SubscriptionPlan.Annual) {
                annualSavingsText(state.monthlyOffer, state.annualOffer)?.let {
                    appendLine()
                    appendLine("Economia aproximada: $it por ano.")
                }
            }
            appendLine()
            if (state.status == SubscriptionStatus.FreeTrialEligible && offer?.hasFreeTrial == true) {
                appendLine("A cobrança acontece pela Google Play após o teste, se você não cancelar.")
            } else {
                appendLine("A Google Play mostrará as condições finais antes de confirmar.")
            }
        }
    }.trim()
}

internal fun annualSavingsText(
    monthlyOffer: SubscriptionOffer?,
    annualOffer: SubscriptionOffer?,
): String? {
    val monthlyMicros = monthlyOffer?.priceAmountMicros ?: return null
    val annualMicros = annualOffer?.priceAmountMicros ?: return null
    val currency = monthlyOffer.priceCurrencyCode ?: return null
    if (currency != annualOffer.priceCurrencyCode) return null

    val yearlyMicros = monthlyMicros * 12
    val savingsMicros = yearlyMicros - annualMicros
    if (savingsMicros <= 0L) return null

    val savings = BigDecimal(savingsMicros)
        .divide(BigDecimal(1_000_000), 2, RoundingMode.HALF_UP)
    val amount = when (currency) {
        "BRL" -> "R$ ${savings.toPlainString().replace('.', ',')}"
        else -> "$currency ${savings.toPlainString()}"
    }
    return amount
}

private fun BillingState.offerFor(plan: SubscriptionPlan): SubscriptionOffer? =
    when (plan) {
        SubscriptionPlan.Monthly -> monthlyOffer
        SubscriptionPlan.Annual -> annualOffer
    }

private fun BillingState.monthlyPriceLabel(): String =
    monthlyOffer?.formattedPrice ?: fallbackPrice(SubscriptionPlan.Monthly)

private fun BillingState.annualPriceLabel(): String =
    annualOffer?.formattedPrice ?: fallbackPrice(SubscriptionPlan.Annual)

private fun BillingState.fallbackPrice(plan: SubscriptionPlan): String =
    when (plan) {
        SubscriptionPlan.Monthly -> "R$ 9,90/mês"
        SubscriptionPlan.Annual -> "R$ 99,90/ano"
    }

private val SubscriptionPlan.displayName: String
    get() = when (this) {
        SubscriptionPlan.Monthly -> "Mensal"
        SubscriptionPlan.Annual -> "Anual"
    }
