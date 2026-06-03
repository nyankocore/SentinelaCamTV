package com.sentinela.camtv.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DebugBillingRepositoryTest {
    @Test
    fun premiumDebugModeUnlocksFullAccess() {
        val repository = DebugBillingRepository("premium")

        repository.start()

        assertEquals(SubscriptionAccess.Premium, repository.state.value.access)
        assertNotNull(repository.state.value.monthlyOffer)
        assertNotNull(repository.state.value.annualOffer)
    }

    @Test
    fun trialDebugModeUnlocksFullAccess() {
        val repository = DebugBillingRepository("trial")

        repository.start()

        assertEquals(SubscriptionAccess.Trial, repository.state.value.access)
    }

    @Test
    fun unknownDebugModeUsesFreeLimit() {
        val repository = DebugBillingRepository("anything")

        repository.start()

        assertEquals(SubscriptionAccess.FreeLimited, repository.state.value.access)
    }

    @Test
    fun purchaseInDebugSimulatesPremium() {
        val repository = DebugBillingRepository("anything")

        repository.simulatePurchase(SubscriptionPlan.Annual)

        assertEquals(SubscriptionAccess.Premium, repository.state.value.access)
        assertEquals(SubscriptionPlan.Annual, repository.state.value.activePlan)
    }
}
