package com.pillion.domain.usecase

import com.pillion.domain.model.TripStage
import com.pillion.domain.repository.TripStageRepository
import kotlinx.coroutines.flow.Flow

class ObserveTripStagesUseCase(
    private val stageRepository: TripStageRepository,
) {
    operator fun invoke(tripId: Long): Flow<List<TripStage>> = stageRepository.getStagesForTripFlow(tripId)
}

class GetTripStagesUseCase(
    private val stageRepository: TripStageRepository,
) {
    suspend operator fun invoke(tripId: Long): List<TripStage> = stageRepository.getStagesForTrip(tripId)
}

class UpsertTripStageUseCase(
    private val stageRepository: TripStageRepository,
) {
    suspend operator fun invoke(stage: TripStage) {
        stageRepository.upsertStage(stage)
    }
}

class DeleteTripStageUseCase(
    private val stageRepository: TripStageRepository,
) {
    suspend operator fun invoke(stageId: Long) {
        stageRepository.deleteStage(stageId)
    }
}
