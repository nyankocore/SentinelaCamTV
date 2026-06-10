package com.sentinela.camtv.data.mosaic

internal object MosaicSlotMovePolicy {
    fun placeCamera(
        slots: List<MosaicSlot>,
        mosaicIndex: Int,
        slotIndex: Int,
        cameraId: String,
    ): List<MosaicSlot> {
        require(mosaicIndex in 0 until MOSAIC_COUNT)
        require(slotIndex in 0 until MOSAIC_MAX_SLOTS)
        val source = slots.firstOrNull { slot -> slot.cameraId == cameraId }
        val target = slots.firstOrNull { slot ->
            slot.mosaicIndex == mosaicIndex && slot.slotIndex == slotIndex
        }
        if (source == target && source != null) return slots.normalized()

        val result = slots
            .filterNot { slot ->
                slot.cameraId == cameraId ||
                    (slot.mosaicIndex == mosaicIndex && slot.slotIndex == slotIndex)
            }
            .toMutableList()

        if (source != null && target != null) {
            result += target.copy(
                mosaicIndex = source.mosaicIndex,
                slotIndex = source.slotIndex,
            )
        }
        result += MosaicSlot(
            mosaicIndex = mosaicIndex,
            slotIndex = slotIndex,
            cameraId = cameraId,
        )
        return result.normalized()
    }

    fun swapCameras(
        slots: List<MosaicSlot>,
        firstCameraId: String,
        secondCameraId: String,
    ): List<MosaicSlot> {
        if (firstCameraId == secondCameraId) return slots.normalized()
        val first = slots.firstOrNull { slot -> slot.cameraId == firstCameraId } ?: return slots.normalized()
        val second = slots.firstOrNull { slot -> slot.cameraId == secondCameraId } ?: return slots.normalized()
        return slots.map { slot ->
            when (slot.cameraId) {
                firstCameraId -> slot.copy(mosaicIndex = second.mosaicIndex, slotIndex = second.slotIndex)
                secondCameraId -> slot.copy(mosaicIndex = first.mosaicIndex, slotIndex = first.slotIndex)
                else -> slot
            }
        }.normalized()
    }

    private fun List<MosaicSlot>.normalized(): List<MosaicSlot> =
        distinctBy { slot -> slot.cameraId }
            .filter { slot ->
                slot.mosaicIndex in 0 until MOSAIC_COUNT &&
                    slot.slotIndex in 0 until MOSAIC_MAX_SLOTS
            }
            .sortedWith(
                compareBy<MosaicSlot> { slot -> slot.mosaicIndex }
                    .thenBy { slot -> slot.slotIndex }
                    .thenBy { slot -> slot.cameraId },
            )
}
