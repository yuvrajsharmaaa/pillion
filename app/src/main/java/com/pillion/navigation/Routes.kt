package com.pillion.navigation

object Routes {
    const val TripList = "trip_list"
    const val TripBuilderPattern = "trip_builder/{tripId}"
    const val LiveTripPattern = "live_trip/{tripId}"

    fun tripBuilder(tripId: Long): String = "trip_builder/$tripId"
    fun liveTrip(tripId: Long): String = "live_trip/$tripId"
}
