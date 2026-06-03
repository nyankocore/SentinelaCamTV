package com.sentinela.camtv.billing

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

interface BillingRepository {
    val state: StateFlow<BillingState>
    fun start()
    fun refresh()
    fun launchPurchase(activity: Activity, plan: SubscriptionPlan): Result<Unit>
}
