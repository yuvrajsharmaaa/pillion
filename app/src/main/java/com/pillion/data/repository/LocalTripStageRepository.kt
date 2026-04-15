package com.pillion.data.repository

import com.pillion.data.local.dao.TripStageDao
import com.pillion.data.local.toDomain
import com.pillion.data.local.toEntity
import com.pillion.domain.model.TripStage
import com.pillion.domain.repository.TripStageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalTripStageRepository(
    private val tripStageDao: TripStageDao,
) : TripStageRepository {

    override fun getStagesForTripFlow(tripId: Long): Flow<List<TripStage>> =
        tripStageDao.observeForTrip(tripId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getStagesForTrip(tripId: Long): List<TripStage> =
        tripStageDao.getForTrip(tripId).map { it.toDomain() }

    override suspend fun upsertStage(stage: TripStage) {
        if (stage.id == 0L) {
            tripStageDao.insert(stage.toEntity())
        } else {
            tripStageDao.update(stage.toEntity())
        }
    }

    override suspend fun deleteStage(id: Long) {
        tripStageDao.deleteById(id)
    }

    override suspend fun deleteStagesForTrip(tripId: Long) {
        tripStageDao.deleteByTripId(tripId)
    }
}
