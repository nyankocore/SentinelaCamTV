package com.sentinela.camtv.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugBillingRepositoryTest {
    @Test
    fun premiumDebugModeUnlocksFullAccess() {
        val repository = DebugBillingRepository("premium")

        repository.start()

        assertEquals(SubscriptionAccess.Premium, repository.state.value.access)
        assertEquals(SubscriptionStatus.MonthlyActive, repository.state.value.status)
        assertEquals(SubscriptionPlan.Monthly, repository.state.value.activePlan)
        assertNotNull(repository.state.value.monthlyOffer)
        assertNotNull(repository.state.value.annualOffer)
    }

    @Test
    fun trialDebugModeMarksFreeTrialEligible() {
        val repository = DebugBillingRepository("trial")

        repository.start()

        assertEquals(SubscriptionAccess.FreeLimited, repository.state.value.access)
        assertEquals(SubscriptionStatus.FreeTrialEligible, repository.state.value.status)
        assertTrue(repository.state.value.monthlyOffer?.hasFreeTrial == true)
        assertTrue(repository.state.value.annualOffer?.hasFreeTrial == true)
    }

    @Test
    fun freeNoTrialDebugModeKeepsFreeLimitWithoutTrialText() {
        val repository = DebugBillingRepository("free_no_trial")

        repository.start()

        assertEquals(SubscriptionAccess.FreeLimited, repository.state.value.access)
        assertEquals(SubscriptionStatus.FreeNoTrial, repository.state.value.status)
        assertFalse(repository.state.value.monthlyOffer?.hasFreeTrial == true)
    }

    @Test
    fun unknownDebugModeUsesFreeLimit() {
        val repository = DebugBillingRepository("anything")

        repository.start()

        assertEquals(SubscriptionAccess.FreeLimited, repository.state.value.access)
    }

    @Test
    fun annualDebugModeUnlocksAnnualPlan() {
        val repository = DebugBillingRepository("annual_active")

        repository.start()

        assertEquals(SubscriptionAccess.Premium, repository.state.value.access)
        assertEquals(SubscriptionStatus.AnnualActive, repository.state.value.status)
        assertEquals(SubscriptionPlan.Annual, repository.state.value.activePlan)
    }

    @Test
    fun gracePeriodKeepsFullAccess() {
        val repository = DebugBillingRepository("grace_period")

        repository.start()

        assertEquals(SubscriptionAccess.Premium, repository.state.value.access)
        assertEquals(SubscriptionStatus.GracePeriod, repository.state.value.status)
    }

    @Test
    fun onHoldUsesFreeLimit() {
        val repository = DebugBillingRepository("on_hold")

        repository.start()

        assertEquals(SubscriptionAccess.FreeLimited, repository.state.value.access)
        assertEquals(SubscriptionStatus.OnHold, repository.state.value.status)
    }

    @Test
    fun expiredUsesFreeLimit() {
        val repository = DebugBillingRepository("expired")

        repository.start()

        assertEquals(SubscriptionAccess.FreeLimited, repository.state.value.access)
        assertEquals(SubscriptionStatus.Expired, repository.state.value.status)
    }

    @Test
    fun canceledUntilExpiryKeepsPremiumAccess() {
        val repository = DebugBillingRepository("canceled_until_expiry")

        repository.start()

        assertEquals(SubscriptionAccess.Premium, repository.state.value.access)
        assertEquals(SubscriptionStatus.CanceledUntilExpiry, repository.state.value.status)
    }

    @Test
    fun checkingAndErrorDoNotUnlockAccess() {
        val checking = DebugBillingRepository("checking")
        val error = DebugBillingRepository("error")
        val unavailable = DebugBillingRepository("billing_unavailable")

        assertEquals(SubscriptionAccess.BillingUnavailable, checking.state.value.access)
        assertEquals(SubscriptionStatus.Checking, checking.state.value.status)
        assertEquals(SubscriptionAccess.BillingUnavailable, error.state.value.access)
        assertEquals(SubscriptionStatus.Error, error.state.value.status)
        assertEquals(SubscriptionAccess.BillingUnavailable, unavailable.state.value.access)
        assertEquals(SubscriptionStatus.BillingUnavailable, unavailable.state.value.status)
    }

    @Test
    fun purchaseInDebugSimulatesPremium() {
        val repository = DebugBillingRepository("anything")

        repository.simulatePurchase(SubscriptionPlan.Annual)

        assertEquals(SubscriptionAccess.Premium, repository.state.value.access)
        assertEquals(SubscriptionPlan.Annual, repository.state.value.activePlan)
    }
}
