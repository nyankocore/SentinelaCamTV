package com.sentinela.camtv.data.mosaic

import kotlinx.coroutines.flow.Flow

const val MOSAIC_COUNT = 3
const val MOSAIC_MAX_SLOTS = 15

data class MosaicSlot(
    val mosaicIndex: Int,
    val slotIndex: Int,
    val cameraId: String,
)

interface MosaicLayoutRepository {
    fun observeAllSlots(): Flow<List<MosaicSlot>>
    fun observeSlots(mosaicIndex: Int): Flow<List<MosaicSlot>>
    suspend fun placeCamera(mosaicIndex: Int, slotIndex: Int, cameraId: String)
    suspend fun swapCameras(firstCameraId: String, secondCameraId: String)
    suspend fun removeCameraFromLayout(cameraId: String)
}
