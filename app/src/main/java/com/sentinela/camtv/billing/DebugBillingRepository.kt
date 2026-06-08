package com.sentinela.camtv.billing

import android.app.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DebugBillingRepository(
    debugEntitlement: String,
) : BillingRepository {
    private val initialStatus = debugEntitlement.toDebugSubscriptionStatus()

    private val _state = MutableStateFlow(stateForStatus(initialStatus))

    override val state: StateFlow<BillingState> = _state

    override fun start() = Unit

    override fun refresh() = Unit

    override fun launchPurchase(activity: Activity, plan: SubscriptionPlan): Result<Unit> {
        simulatePurchase(plan)
        return Result.success(Unit)
    }

    fun setDebugStatus(status: SubscriptionStatus) {
        _state.value = stateForStatus(status)
    }

    fun setDebugStatusKey(statusKey: String) {
        setDebugStatus(statusKey.toDebugSubscriptionStatus())
    }

    fun resetDebugStatus() {
        setDebugStatus(initialStatus)
    }

    internal fun simulatePurchase(plan: SubscriptionPlan) {
        _state.value = _state.value.copy(
            access = SubscriptionAccess.Premium,
            status = when (plan) {
                SubscriptionPlan.Monthly -> SubscriptionStatus.MonthlyActive
                SubscriptionPlan.Annual -> SubscriptionStatus.AnnualActive
            },
            activePlan = plan,
            message = null,
            loading = false,
        )
    }

    private fun stateForStatus(status: SubscriptionStatus): BillingState =
        BillingState(
            access = status.toAccess(),
            status = status,
            monthlyOffer = debugOffer(
                plan = SubscriptionPlan.Monthly,
                status = status,
            ),
            annualOffer = debugOffer(
                plan = SubscriptionPlan.Annual,
                status = status,
            ),
            activePlan = status.toActivePlan(),
            loading = status == SubscriptionStatus.Checking,
            message = null,
        )
}

fun String.toDebugSubscriptionStatus(): SubscriptionStatus =
    when (lowercase()) {
        "premium",
        "monthly",
        "monthly_active" -> SubscriptionStatus.MonthlyActive

        "annual",
        "annual_active" -> SubscriptionStatus.AnnualActive

        "trial",
        "free_trial_eligible" -> SubscriptionStatus.FreeTrialEligible

        "free",
        "free_no_trial" -> SubscriptionStatus.FreeNoTrial

        "grace",
        "grace_period" -> SubscriptionStatus.GracePeriod

        "hold",
        "on_hold" -> SubscriptionStatus.OnHold

        "expired" -> SubscriptionStatus.Expired

        "canceled",
        "cancelled",
        "canceled_until_expiry",
        "cancelled_until_expiry" -> SubscriptionStatus.CanceledUntilExpiry

        "unavailable",
        "billing_unavailable" -> SubscriptionStatus.BillingUnavailable

        "checking" -> SubscriptionStatus.Checking
        "error" -> SubscriptionStatus.Error
        else -> SubscriptionStatus.FreeTrialEligible
    }

fun SubscriptionStatus.toDebugStatusKey(): String =
    when (this) {
        SubscriptionStatus.FreeTrialEligible -> "free_trial_eligible"
        SubscriptionStatus.FreeNoTrial -> "free_no_trial"
        SubscriptionStatus.MonthlyActive -> "monthly_active"
        SubscriptionStatus.AnnualActive -> "annual_active"
        SubscriptionStatus.GracePeriod -> "grace_period"
        SubscriptionStatus.OnHold -> "on_hold"
        SubscriptionStatus.Expired -> "expired"
        SubscriptionStatus.CanceledUntilExpiry -> "canceled_until_expiry"
        SubscriptionStatus.BillingUnavailable -> "billing_unavailable"
        SubscriptionStatus.Checking -> "checking"
        SubscriptionStatus.Error -> "error"
    }

fun SubscriptionStatus.debugDisplayName(): String =
    when (this) {
        SubscriptionStatus.FreeTrialEligible -> "FreeTrialEligible"
        SubscriptionStatus.FreeNoTrial -> "FreeNoTrial"
        SubscriptionStatus.MonthlyActive -> "MonthlyActive"
        SubscriptionStatus.AnnualActive -> "AnnualActive"
        SubscriptionStatus.GracePeriod -> "GracePeriod"
        SubscriptionStatus.OnHold -> "OnHold"
        SubscriptionStatus.Expired -> "Expired"
        SubscriptionStatus.CanceledUntilExpiry -> "CanceledUntilExpiry"
        SubscriptionStatus.BillingUnavailable -> "BillingUnavailable"
        SubscriptionStatus.Checking -> "Checking"
        SubscriptionStatus.Error -> "Error"
    }

private fun debugOffer(
    plan: SubscriptionPlan,
    status: SubscriptionStatus,
): SubscriptionOffer =
    SubscriptionOffer(
        plan = plan,
        formattedPrice = when (plan) {
            SubscriptionPlan.Monthly -> "R$ 9,90/mês"
            SubscriptionPlan.Annual -> "R$ 99,90/ano"
        },
        billingPeriod = when (plan) {
            SubscriptionPlan.Monthly -> "mensal"
            SubscriptionPlan.Annual -> "anual"
        },
        offerToken = "debug-${plan.basePlanId}",
        priceAmountMicros = when (plan) {
            SubscriptionPlan.Monthly -> 9_900_000
            SubscriptionPlan.Annual -> 99_900_000
        },
        priceCurrencyCode = "BRL",
        hasFreeTrial = status == SubscriptionStatus.FreeTrialEligible,
        trialPeriod = if (status == SubscriptionStatus.FreeTrialEligible) "P7D" else null,
    )

private fun SubscriptionStatus.toAccess(): SubscriptionAccess =
    when (this) {
        SubscriptionStatus.FreeTrialEligible -> SubscriptionAccess.FreeLimited
        SubscriptionStatus.FreeNoTrial -> SubscriptionAccess.FreeLimited
        SubscriptionStatus.MonthlyActive -> SubscriptionAccess.Premium
        SubscriptionStatus.AnnualActive -> SubscriptionAccess.Premium
        SubscriptionStatus.GracePeriod -> SubscriptionAccess.Premium
        SubscriptionStatus.OnHold -> SubscriptionAccess.FreeLimited
        SubscriptionStatus.Expired -> SubscriptionAccess.FreeLimited
        SubscriptionStatus.CanceledUntilExpiry -> SubscriptionAccess.Premium
        SubscriptionStatus.BillingUnavailable -> SubscriptionAccess.BillingUnavailable
        SubscriptionStatus.Checking -> SubscriptionAccess.BillingUnavailable
        SubscriptionStatus.Error -> SubscriptionAccess.BillingUnavailable
    }

private fun SubscriptionStatus.toActivePlan(): SubscriptionPlan? =
    when (this) {
        SubscriptionStatus.MonthlyActive -> SubscriptionPlan.Monthly
        SubscriptionStatus.AnnualActive -> SubscriptionPlan.Annual
        SubscriptionStatus.GracePeriod -> SubscriptionPlan.Monthly
        SubscriptionStatus.CanceledUntilExpiry -> SubscriptionPlan.Monthly
        else -> null
    }
