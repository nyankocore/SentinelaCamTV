package com.sentinela.camtv.data.mosaic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MosaicSlotMovePolicyTest {
    @Test
    fun cameraCannotOccupyTwoSlotsGlobally() {
        val result = MosaicSlotMovePolicy.placeCamera(
            slots = listOf(MosaicSlot(0, 0, "cam-1")),
            mosaicIndex = 1,
            slotIndex = 2,
            cameraId = "cam-1",
        )

        assertEquals(listOf(MosaicSlot(1, 2, "cam-1")), result)
    }

    @Test
    fun movingToEmptySlotRemovesSourceSlot() {
        val result = MosaicSlotMovePolicy.placeCamera(
            slots = listOf(
                MosaicSlot(0, 0, "cam-1"),
                MosaicSlot(0, 1, "cam-2"),
            ),
            mosaicIndex = 2,
            slotIndex = 4,
            cameraId = "cam-1",
        )

        assertEquals(
            listOf(
                MosaicSlot(0, 1, "cam-2"),
                MosaicSlot(2, 4, "cam-1"),
            ),
            result,
        )
    }

    @Test
    fun movingToOccupiedSlotSwapsCameras() {
        val result = MosaicSlotMovePolicy.placeCamera(
            slots = listOf(
                MosaicSlot(0, 0, "cam-1"),
                MosaicSlot(1, 3, "cam-2"),
            ),
            mosaicIndex = 1,
            slotIndex = 3,
            cameraId = "cam-1",
        )

        assertEquals(
            listOf(
                MosaicSlot(0, 0, "cam-2"),
                MosaicSlot(1, 3, "cam-1"),
            ),
            result,
        )
    }

    @Test
    fun swapCamerasOnlyChangesTheirSlots() {
        val result = MosaicSlotMovePolicy.swapCameras(
            slots = listOf(
                MosaicSlot(0, 0, "cam-1"),
                MosaicSlot(0, 1, "cam-2"),
                MosaicSlot(2, 2, "cam-3"),
            ),
            firstCameraId = "cam-1",
            secondCameraId = "cam-3",
        )

        assertEquals(
            listOf(
                MosaicSlot(0, 0, "cam-3"),
                MosaicSlot(0, 1, "cam-2"),
                MosaicSlot(2, 2, "cam-1"),
            ),
            result,
        )
    }

    @Test
    fun invalidSlotsAreIgnoredWhenNormalizing() {
        val result = MosaicSlotMovePolicy.placeCamera(
            slots = listOf(
                MosaicSlot(0, 0, "cam-1"),
                MosaicSlot(5, 1, "cam-invalid"),
            ),
            mosaicIndex = 0,
            slotIndex = 2,
            cameraId = "cam-2",
        )

        assertFalse(result.any { slot -> slot.cameraId == "cam-invalid" })
    }
}
