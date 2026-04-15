package com.pillion.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pillion.data.local.entity.TripStageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripStageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stage: TripStageEntity): Long

    @Update
    suspend fun update(stage: TripStageEntity)

    @Query("DELETE FROM trip_stages WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM trip_stages WHERE tripId = :tripId")
    suspend fun deleteByTripId(tripId: Long)

    @Query("SELECT * FROM trip_stages WHERE tripId = :tripId ORDER BY orderIndex ASC, id ASC")
    fun observeForTrip(tripId: Long): Flow<List<TripStageEntity>>

    @Query("SELECT * FROM trip_stages WHERE tripId = :tripId ORDER BY orderIndex ASC, id ASC")
    suspend fun getForTrip(tripId: Long): List<TripStageEntity>
}
