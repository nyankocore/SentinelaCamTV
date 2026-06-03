package com.sentinela.camtv.entitlement

object FreeCameraAccessPolicy {
    fun visibleCameraIds(
        cameraIds: List<String>,
        entitlement: EntitlementState,
    ): List<String> {
        if (entitlement.hasFullAccess) return cameraIds
        if (cameraIds.isEmpty()) return emptyList()

        val activeId = entitlement.freeActiveCameraId
        return listOf(
            if (activeId != null && activeId in cameraIds) {
                activeId
            } else {
                cameraIds.first()
            },
        )
    }
}
