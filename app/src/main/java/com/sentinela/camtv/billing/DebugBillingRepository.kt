package com.sentinela.camtv.billing

import android.app.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DebugBillingRepository(
    debugEntitlement: String,
) : BillingRepository {
    private val debugAccess = when (debugEntitlement.lowercase()) {
        "premium" -> SubscriptionAccess.Premium
        "trial" -> SubscriptionAccess.Trial
        "unavailable" -> SubscriptionAccess.BillingUnavailable
        else -> SubscriptionAccess.FreeLimited
    }

    private val _state = MutableStateFlow(
        BillingState(
            access = debugAccess,
            monthlyOffer = SubscriptionOffer(
                plan = SubscriptionPlan.Monthly,
                formattedPrice = "R$ 9,90",
                billingPeriod = "mensal",
                offerToken = "debug-monthly",
            ),
            annualOffer = SubscriptionOffer(
                plan = SubscriptionPlan.Annual,
                formattedPrice = "R$ 99,90",
                billingPeriod = "anual",
                offerToken = "debug-annual",
            ),
            message = "Acesso de teste: ${debugAccess.name}",
        ),
    )

    override val state: StateFlow<BillingState> = _state

    override fun start() = Unit

    override fun refresh() = Unit

    override fun launchPurchase(activity: Activity, plan: SubscriptionPlan): Result<Unit> {
        simulatePurchase(plan)
        return Result.success(Unit)
    }

    internal fun simulatePurchase(plan: SubscriptionPlan) {
        _state.value = _state.value.copy(
            access = SubscriptionAccess.Premium,
            activePlan = plan,
            message = "Assinatura simulada no debug.",
        )
    }
}
