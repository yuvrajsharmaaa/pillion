package com.pillion.domain.repository

import com.pillion.domain.model.TripStage
import kotlinx.coroutines.flow.Flow

interface TripStageRepository {
    fun getStagesForTripFlow(tripId: Long): Flow<List<TripStage>>
    suspend fun getStagesForTrip(tripId: Long): List<TripStage>
    suspend fun upsertStage(stage: TripStage)
    suspend fun deleteStage(id: Long)
    suspend fun deleteStagesForTrip(tripId: Long)
}
