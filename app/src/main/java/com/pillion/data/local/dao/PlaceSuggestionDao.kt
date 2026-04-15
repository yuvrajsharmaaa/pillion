package com.pillion.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pillion.data.local.entity.PlaceSuggestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceSuggestionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(placeSuggestion: PlaceSuggestionEntity): Long

    @Update
    suspend fun update(placeSuggestion: PlaceSuggestionEntity)

    @Query("DELETE FROM place_suggestions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM place_suggestions WHERE stageId = :stageId ORDER BY id DESC")
    fun observeForStage(stageId: Long): Flow<List<PlaceSuggestionEntity>>
}
