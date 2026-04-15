package com.pillion.domain.repository

import com.pillion.domain.model.Trip
import kotlinx.coroutines.flow.Flow

interface TripRepository {
    suspend fun createTrip(trip: Trip): Long
    fun getTripsFlow(): Flow<List<Trip>>
    suspend fun getTripById(id: Long): Trip?
    suspend fun updateTrip(trip: Trip)
    suspend fun deleteTrip(id: Long)
}
