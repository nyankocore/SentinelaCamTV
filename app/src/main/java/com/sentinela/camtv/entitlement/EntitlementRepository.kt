package com.sentinela.camtv.entitlement

import android.app.Activity
import com.sentinela.camtv.billing.BillingState
import com.sentinela.camtv.billing.SubscriptionPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

data class EntitlementState(
    val billing: BillingState = BillingState(),
    val freeActiveCameraId: String? = null,
) {
    val hasFullAccess: Boolean
        get() = billing.hasFullAccess

    val freeLimitActive: Boolean
        get() = !hasFullAccess
}

interface EntitlementRepository {
    val billingState: StateFlow<BillingState>
    fun observeEntitlement(): Flow<EntitlementState>
    fun start()
    fun refresh()
    fun launchPurchase(activity: Activity, plan: SubscriptionPlan): Result<Unit>
    suspend fun setFreeActiveCameraId(cameraId: String?)
}
