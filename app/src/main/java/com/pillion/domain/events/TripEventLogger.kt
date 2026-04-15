package com.pillion.domain.events

sealed interface TripEvent {
    data class TripStarted(
        val tripId: Long,
    ) : TripEvent

    data class StageChanged(
        val tripId: Long,
        val fromStageId: Long?,
        val toStageId: Long?,
    ) : TripEvent

    data class PlaceSelected(
        val tripId: Long,
        val stageId: Long,
        val placeName: String,
    ) : TripEvent
}

interface TripEventLogger {
    fun log(event: TripEvent)
}

object NoOpTripEventLogger : TripEventLogger {
    override fun log(event: TripEvent) = Unit
}
