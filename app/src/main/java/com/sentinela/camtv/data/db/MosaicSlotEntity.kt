package com.sentinela.camtv.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "mosaic_slots",
    primaryKeys = ["mosaicIndex", "slotIndex"],
    indices = [
        Index(value = ["cameraId"], unique = true),
    ],
    foreignKeys = [
        ForeignKey(
            entity = CameraEntity::class,
            parentColumns = ["id"],
            childColumns = ["cameraId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class MosaicSlotEntity(
    val mosaicIndex: Int,
    val slotIndex: Int,
    val cameraId: String,
)
