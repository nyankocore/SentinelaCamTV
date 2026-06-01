package com.sentinela.camtv.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraSourceCompatibilityTest {
    @Test
    fun legacyRoomSourceTypeNameIsPreserved() {
        assertEquals(
            CameraSourceType.INTELBRAS_DVR_CHANNEL,
            CameraSourceType.valueOf("INTELBRAS_DVR_CHANNEL"),
        )
    }
}
