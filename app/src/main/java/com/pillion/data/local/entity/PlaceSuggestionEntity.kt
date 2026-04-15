package com.pillion.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "place_suggestions",
    foreignKeys = [
        ForeignKey(
            entity = TripStageEntity::class,
            parentColumns = ["id"],
            childColumns = ["stageId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["stageId"])]
)
data class PlaceSuggestionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val stageId: Long,
    val name: String,
    val lat: Double,
    val lng: Double,
    val address: String?,
    val notes: String?,
)
