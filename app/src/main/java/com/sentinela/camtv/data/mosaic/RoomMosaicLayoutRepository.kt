package com.sentinela.camtv.data.mosaic

import com.sentinela.camtv.data.db.MosaicSlotDao
import com.sentinela.camtv.data.db.MosaicSlotEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomMosaicLayoutRepository(
    private val mosaicSlotDao: MosaicSlotDao,
) : MosaicLayoutRepository {
    override fun observeAllSlots(): Flow<List<MosaicSlot>> =
        mosaicSlotDao.observeAll().map { entities -> entities.map(::toDomain) }

    override fun observeSlots(mosaicIndex: Int): Flow<List<MosaicSlot>> =
        mosaicSlotDao.observeForMosaic(mosaicIndex.coerceIn(0, MOSAIC_COUNT - 1))
            .map { entities -> entities.map(::toDomain) }

    override suspend fun placeCamera(
        mosaicIndex: Int,
        slotIndex: Int,
        cameraId: String,
    ) {
        val updated = MosaicSlotMovePolicy.placeCamera(
            slots = mosaicSlotDao.allSlotsNow().map(::toDomain),
            mosaicIndex = mosaicIndex,
            slotIndex = slotIndex,
            cameraId = cameraId,
        )
        mosaicSlotDao.replaceAll(updated.map(::toEntity))
    }

    override suspend fun swapCameras(
        firstCameraId: String,
        secondCameraId: String,
    ) {
        val updated = MosaicSlotMovePolicy.swapCameras(
            slots = mosaicSlotDao.allSlotsNow().map(::toDomain),
            firstCameraId = firstCameraId,
            secondCameraId = secondCameraId,
        )
        mosaicSlotDao.replaceAll(updated.map(::toEntity))
    }

    override suspend fun removeCameraFromLayout(cameraId: String) {
        mosaicSlotDao.deleteByCameraId(cameraId)
    }

    private fun toDomain(entity: MosaicSlotEntity): MosaicSlot =
        MosaicSlot(
            mosaicIndex = entity.mosaicIndex,
            slotIndex = entity.slotIndex,
            cameraId = entity.cameraId,
        )

    private fun toEntity(slot: MosaicSlot): MosaicSlotEntity =
        MosaicSlotEntity(
            mosaicIndex = slot.mosaicIndex,
            slotIndex = slot.slotIndex,
            cameraId = slot.cameraId,
        )
}
