package com.pillion.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pillion.data.local.dao.PlaceSuggestionDao
import com.pillion.data.local.dao.TripDao
import com.pillion.data.local.dao.TripStageDao
import com.pillion.data.local.entity.PlaceSuggestionEntity
import com.pillion.data.local.entity.TripEntity
import com.pillion.data.local.entity.TripStageEntity

@Database(
    entities = [
        TripEntity::class,
        TripStageEntity::class,
        PlaceSuggestionEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun tripStageDao(): TripStageDao
    abstract fun placeSuggestionDao(): PlaceSuggestionDao
}
