package com.sentinela.camtv.entitlement

import com.sentinela.camtv.billing.BillingState
import com.sentinela.camtv.billing.SubscriptionAccess
import com.sentinela.camtv.billing.SubscriptionStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class FreeCameraAccessPolicyTest {
    @Test
    fun premiumSeesAllCameras() {
        val visibleIds = FreeCameraAccessPolicy.visibleCameraIds(
            cameraIds = listOf("cam-1", "cam-2", "cam-3"),
            entitlement = EntitlementState(
                billing = BillingState(access = SubscriptionAccess.Premium),
            ),
        )

        assertEquals(listOf("cam-1", "cam-2", "cam-3"), visibleIds)
    }

    @Test
    fun trialSeesAllCameras() {
        val visibleIds = FreeCameraAccessPolicy.visibleCameraIds(
            cameraIds = listOf("cam-1", "cam-2"),
            entitlement = EntitlementState(
                billing = BillingState(access = SubscriptionAccess.Trial),
            ),
        )

        assertEquals(listOf("cam-1", "cam-2"), visibleIds)
    }

    @Test
    fun freeModeUsesSelectedCameraWhenAvailable() {
        val visibleIds = FreeCameraAccessPolicy.visibleCameraIds(
            cameraIds = listOf("cam-1", "cam-2", "cam-3"),
            entitlement = EntitlementState(
                billing = BillingState(access = SubscriptionAccess.FreeLimited),
                freeActiveCameraId = "cam-2",
            ),
        )

        assertEquals(listOf("cam-2"), visibleIds)
    }

    @Test
    fun freeModeFallsBackToFirstCameraWhenSelectionIsMissing() {
        val visibleIds = FreeCameraAccessPolicy.visibleCameraIds(
            cameraIds = listOf("cam-1", "cam-2"),
            entitlement = EntitlementState(
                billing = BillingState(access = SubscriptionAccess.FreeLimited),
                freeActiveCameraId = "cam-9",
            ),
        )

        assertEquals(listOf("cam-1"), visibleIds)
    }

    @Test
    fun gracePeriodKeepsAllCamerasVisible() {
        val visibleIds = FreeCameraAccessPolicy.visibleCameraIds(
            cameraIds = listOf("cam-1", "cam-2", "cam-3"),
            entitlement = EntitlementState(
                billing = BillingState(
                    access = SubscriptionAccess.Premium,
                    status = SubscriptionStatus.GracePeriod,
                ),
            ),
        )

        assertEquals(listOf("cam-1", "cam-2", "cam-3"), visibleIds)
    }

    @Test
    fun onHoldLimitsToActiveFreeCamera() {
        val visibleIds = FreeCameraAccessPolicy.visibleCameraIds(
            cameraIds = listOf("cam-1", "cam-2", "cam-3"),
            entitlement = EntitlementState(
                billing = BillingState(
                    access = SubscriptionAccess.FreeLimited,
                    status = SubscriptionStatus.OnHold,
                ),
                freeActiveCameraId = "cam-3",
            ),
        )

        assertEquals(listOf("cam-3"), visibleIds)
    }

    @Test
    fun expiredLimitsToActiveFreeCamera() {
        val visibleIds = FreeCameraAccessPolicy.visibleCameraIds(
            cameraIds = listOf("cam-1", "cam-2", "cam-3"),
            entitlement = EntitlementState(
                billing = BillingState(
                    access = SubscriptionAccess.FreeLimited,
                    status = SubscriptionStatus.Expired,
                ),
                freeActiveCameraId = "cam-2",
            ),
        )

        assertEquals(listOf("cam-2"), visibleIds)
    }

    @Test
    fun canceledUntilExpiryKeepsAllCamerasVisible() {
        val visibleIds = FreeCameraAccessPolicy.visibleCameraIds(
            cameraIds = listOf("cam-1", "cam-2"),
            entitlement = EntitlementState(
                billing = BillingState(
                    access = SubscriptionAccess.Premium,
                    status = SubscriptionStatus.CanceledUntilExpiry,
                ),
            ),
        )

        assertEquals(listOf("cam-1", "cam-2"), visibleIds)
    }
}
