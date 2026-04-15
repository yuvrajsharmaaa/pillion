package com.pillion.data.repository

import com.pillion.data.local.dao.TripDao
import com.pillion.data.local.toDomain
import com.pillion.data.local.toEntity
import com.pillion.domain.model.Trip
import com.pillion.domain.repository.TripRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalTripRepository(
    private val tripDao: TripDao,
) : TripRepository {

    override suspend fun createTrip(trip: Trip): Long = tripDao.insert(trip.toEntity())

    override fun getTripsFlow(): Flow<List<Trip>> =
        tripDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getTripById(id: Long): Trip? = tripDao.getById(id)?.toDomain()

    override suspend fun updateTrip(trip: Trip) {
        tripDao.update(trip.toEntity())
    }

    override suspend fun deleteTrip(id: Long) {
        tripDao.deleteById(id)
    }
}
