package com.pillion.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "trip_stages",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["tripId"]),
        Index(value = ["tripId", "orderIndex"]),
    ]
)
data class TripStageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val tripId: Long,
    val orderIndex: Int,
    val type: String,
    val fromLat: Double?,
    val fromLng: Double?,
    val fromLabel: String?,
    val toLat: Double?,
    val toLng: Double?,
    val toLabel: String?,
    val trigger: String,
    val scheduledTimeMillis: Long?,
    val placeType: String?,
    val minRating: Float?,
    val maxPriceLevel: Int?,
    val openNow: Boolean,
    val customQuery: String?,
    val status: String,
)
