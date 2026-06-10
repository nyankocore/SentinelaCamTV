package com.sentinela.camtv.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MosaicSlotDao {
    @Query("SELECT * FROM mosaic_slots ORDER BY mosaicIndex ASC, slotIndex ASC")
    fun observeAll(): Flow<List<MosaicSlotEntity>>

    @Query("SELECT * FROM mosaic_slots WHERE mosaicIndex = :mosaicIndex ORDER BY slotIndex ASC")
    fun observeForMosaic(mosaicIndex: Int): Flow<List<MosaicSlotEntity>>

    @Query("SELECT * FROM mosaic_slots ORDER BY mosaicIndex ASC, slotIndex ASC")
    suspend fun allSlotsNow(): List<MosaicSlotEntity>

    @Query("DELETE FROM mosaic_slots")
    suspend fun deleteAll()

    @Query("DELETE FROM mosaic_slots WHERE cameraId = :cameraId")
    suspend fun deleteByCameraId(cameraId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(slots: List<MosaicSlotEntity>)

    @Transaction
    suspend fun replaceAll(slots: List<MosaicSlotEntity>) {
        deleteAll()
        if (slots.isNotEmpty()) {
            insertAll(slots)
        }
    }
}
