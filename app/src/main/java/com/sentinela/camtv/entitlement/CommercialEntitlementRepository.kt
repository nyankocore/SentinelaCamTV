package com.sentinela.camtv.entitlement

import android.app.Activity
import com.sentinela.camtv.billing.BillingRepository
import com.sentinela.camtv.billing.BillingState
import com.sentinela.camtv.billing.SubscriptionAccess
import com.sentinela.camtv.billing.SubscriptionPlan
import com.sentinela.camtv.preferences.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class CommercialEntitlementRepository(
    private val billingRepository: BillingRepository,
    private val settingsRepository: SettingsRepository,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) : EntitlementRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var observingBilling = false

    override val billingState: StateFlow<BillingState> = billingRepository.state

    override fun observeEntitlement(): Flow<EntitlementState> =
        combine(
            billingRepository.state,
            settingsRepository.observePreferences(),
        ) { billing, preferences ->
            val effectiveBilling = billing.effectiveWithGracePeriod(
                graceUntilEpochMillis = preferences.premiumGraceUntilEpochMillis,
            )
            EntitlementState(
                billing = effectiveBilling,
                freeActiveCameraId = preferences.freeActiveCameraId,
            )
        }

    override fun start() {
        billingRepository.start()
        startGracePeriodObserver()
    }

    override fun refresh() {
        billingRepository.refresh()
        startGracePeriodObserver()
    }

    override fun launchPurchase(activity: Activity, plan: SubscriptionPlan): Result<Unit> =
        billingRepository.launchPurchase(activity, plan)

    override suspend fun setFreeActiveCameraId(cameraId: String?) {
        settingsRepository.setFreeActiveCameraId(cameraId)
    }

    private fun startGracePeriodObserver() {
        if (observingBilling) return
        observingBilling = true
        scope.launch {
            billingRepository.state.collect { billing ->
                if (billing.hasFullAccess) {
                    settingsRepository.setPremiumGraceUntilEpochMillis(
                        clockMillis() + PREMIUM_GRACE_PERIOD_MILLIS,
                    )
                }
            }
        }
    }

    private fun BillingState.effectiveWithGracePeriod(
        graceUntilEpochMillis: Long,
    ): BillingState {
        if (access != SubscriptionAccess.BillingUnavailable) return this
        if (clockMillis() > graceUntilEpochMillis) return this
        return copy(
            access = SubscriptionAccess.Premium,
            loading = false,
            message = message ?: "Assinatura mantida temporariamente enquanto a Google Play responde.",
        )
    }

    private companion object {
        const val PREMIUM_GRACE_PERIOD_MILLIS = 24L * 60L * 60L * 1000L
    }
}
