package com.pillion.domain.usecase

import com.pillion.domain.model.Trip
import com.pillion.domain.repository.TripRepository
import kotlinx.coroutines.flow.Flow

class CreateTripUseCase(
    private val tripRepository: TripRepository,
) {
    suspend operator fun invoke(name: String): Long {
        val now = System.currentTimeMillis()
        return tripRepository.createTrip(
            Trip(
                name = name,
                createdAt = now,
                updatedAt = now,
            )
        )
    }
}

class GetTripsUseCase(
    private val tripRepository: TripRepository,
) {
    operator fun invoke(): Flow<List<Trip>> = tripRepository.getTripsFlow()
}

class GetTripByIdUseCase(
    private val tripRepository: TripRepository,
) {
    suspend operator fun invoke(id: Long): Trip? = tripRepository.getTripById(id)
}

class UpdateTripUseCase(
    private val tripRepository: TripRepository,
) {
    suspend operator fun invoke(trip: Trip) {
        tripRepository.updateTrip(trip.copy(updatedAt = System.currentTimeMillis()))
    }
}

class DeleteTripUseCase(
    private val tripRepository: TripRepository,
) {
    suspend operator fun invoke(id: Long) {
        tripRepository.deleteTrip(id)
    }
}
