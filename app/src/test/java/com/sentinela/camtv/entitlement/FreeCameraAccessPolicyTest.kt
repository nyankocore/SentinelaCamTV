package com.sentinela.camtv.entitlement

import com.sentinela.camtv.billing.BillingState
import com.sentinela.camtv.billing.SubscriptionAccess
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
}
