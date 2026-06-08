package com.sentinela.camtv.ui.subscription

import com.sentinela.camtv.billing.BillingState
import com.sentinela.camtv.billing.SubscriptionOffer
import com.sentinela.camtv.billing.SubscriptionPlan
import com.sentinela.camtv.billing.SubscriptionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionTextTest {
    @Test
    fun trialTextAppearsOnlyWhenFreeTrialEligible() {
        val eligible = billingState(SubscriptionStatus.FreeTrialEligible, hasFreeTrial = true)
        val notEligible = billingState(SubscriptionStatus.FreeNoTrial, hasFreeTrial = false)

        assertTrue(planCardLines(eligible).any { it.contains("7 dias de teste") })
        assertTrue(dialogMessage(eligible, SubscriptionPlan.Monthly).contains("7 dias de teste"))

        assertFalse(planCardLines(notEligible).any { it.contains("7 dias de teste") })
        assertFalse(dialogMessage(notEligible, SubscriptionPlan.Monthly).contains("7 dias de teste"))
    }

    @Test
    fun planChangeDialogsDoNotMentionTrial() {
        val monthlyActive = billingState(
            status = SubscriptionStatus.MonthlyActive,
            activePlan = SubscriptionPlan.Monthly,
            hasFreeTrial = true,
        )

        val message = dialogMessage(monthlyActive, SubscriptionPlan.Annual)

        assertFalse(message.contains("7 dias de teste"))
        assertTrue(message.contains("Google Play mostrará as condições finais"))
    }

    @Test
    fun annualSavingsUsesOnlySameCurrencyAndValidValues() {
        val monthly = offer(SubscriptionPlan.Monthly, 9_900_000, "BRL")
        val annual = offer(SubscriptionPlan.Annual, 99_900_000, "BRL")
        val annualDifferentCurrency = offer(SubscriptionPlan.Annual, 99_900_000, "USD")
        val annualNotCheaper = offer(SubscriptionPlan.Annual, 120_000_000, "BRL")

        assertEquals("R$ 18,90", annualSavingsText(monthly, annual))
        assertNull(annualSavingsText(monthly, annualDifferentCurrency))
        assertNull(annualSavingsText(monthly, annualNotCheaper))
    }

    @Test
    fun activePlansShowCurrentStateAndPlanSwitchAction() {
        val monthly = subscriptionActions(
            billingState(
                status = SubscriptionStatus.MonthlyActive,
                activePlan = SubscriptionPlan.Monthly,
            ),
        )
        val annual = subscriptionActions(
            billingState(
                status = SubscriptionStatus.AnnualActive,
                activePlan = SubscriptionPlan.Annual,
            ),
        )

        assertEquals(SubscriptionActionKind.CurrentMonthly, monthly[0].kind)
        assertFalse(monthly[0].enabled)
        assertEquals(SubscriptionActionKind.SubscribeAnnual, monthly[1].kind)

        assertEquals(SubscriptionActionKind.SubscribeMonthly, annual[0].kind)
        assertEquals(SubscriptionActionKind.CurrentAnnual, annual[1].kind)
        assertFalse(annual[1].enabled)
    }

    @Test
    fun onHoldAndGracePeriodUsePaymentRecoveryActions() {
        val grace = subscriptionActions(billingState(SubscriptionStatus.GracePeriod))
        val onHold = subscriptionActions(billingState(SubscriptionStatus.OnHold))

        assertEquals(SubscriptionActionKind.UpdatePayment, grace[0].kind)
        assertEquals(SubscriptionActionKind.UpdatePayment, onHold[0].kind)
        assertTrue(planCardLines(SubscriptionStatus.GracePeriod.toState()).any { it.contains("ainda liberado") })
        assertTrue(planCardLines(SubscriptionStatus.OnHold.toState()).any { it.contains("1 câmera ativa") })
    }

    @Test
    fun expiredLimitsAndCanceledUntilExpiryKeepsAccessText() {
        assertTrue(planCardLines(SubscriptionStatus.Expired.toState()).any { it.contains("1 câmera ativa") })
        assertTrue(
            planCardLines(SubscriptionStatus.CanceledUntilExpiry.toState())
                .any { it.contains("Mosaico completo liberado") },
        )
    }

    @Test
    fun planChangeDialogDoesNotSayItUnlocksMosaic() {
        val monthlyActive = billingState(
            status = SubscriptionStatus.MonthlyActive,
            activePlan = SubscriptionPlan.Monthly,
        )

        val message = dialogMessage(monthlyActive, SubscriptionPlan.Annual)

        assertFalse(message.contains("Libera o mosaico completo"))
        assertTrue(message.contains("A Google Play mostrará as condições finais da troca"))
        assertTrue(message.contains("Anual: R$ 99,90/ano"))
    }
}

private fun SubscriptionStatus.toState(): BillingState = billingState(this)

private fun billingState(
    status: SubscriptionStatus,
    activePlan: SubscriptionPlan? = null,
    hasFreeTrial: Boolean = status == SubscriptionStatus.FreeTrialEligible,
): BillingState =
    BillingState(
        status = status,
        monthlyOffer = offer(SubscriptionPlan.Monthly, 9_900_000, "BRL", hasFreeTrial),
        annualOffer = offer(SubscriptionPlan.Annual, 99_900_000, "BRL", hasFreeTrial),
        activePlan = activePlan,
    )

private fun offer(
    plan: SubscriptionPlan,
    priceAmountMicros: Long,
    currency: String,
    hasFreeTrial: Boolean = false,
): SubscriptionOffer =
    SubscriptionOffer(
        plan = plan,
        formattedPrice = when (plan) {
            SubscriptionPlan.Monthly -> "R$ 9,90/mês"
            SubscriptionPlan.Annual -> "R$ 99,90/ano"
        },
        billingPeriod = plan.basePlanId,
        offerToken = "test-${plan.basePlanId}",
        priceAmountMicros = priceAmountMicros,
        priceCurrencyCode = currency,
        hasFreeTrial = hasFreeTrial,
        trialPeriod = if (hasFreeTrial) "P7D" else null,
    )
