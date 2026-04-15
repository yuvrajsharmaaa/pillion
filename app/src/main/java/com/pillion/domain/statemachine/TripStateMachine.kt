package com.pillion.domain.statemachine

import com.pillion.domain.events.NoOpTripEventLogger
import com.pillion.domain.events.TripEvent
import com.pillion.domain.events.TripEventLogger
import com.pillion.domain.model.StageStatus
import com.pillion.domain.model.StageTrigger
import com.pillion.domain.model.Trip
import com.pillion.domain.model.TripStage
import com.pillion.domain.model.TripState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TripStateMachine(
    private val arrivalDistanceMeters: Double = 1500.0,
    private val maxArrivalSpeedMps: Float = 4.0f,
    private val eventLogger: TripEventLogger = NoOpTripEventLogger,
) {
    private var trip: Trip? = null
    private var stages: List<TripStage> = emptyList()
    private var activeIndex: Int = -1

    private val _state = MutableStateFlow(TripState())
    val state: StateFlow<TripState> = _state.asStateFlow()

    fun startTrip(trip: Trip, stages: List<TripStage>) {
        val sortedStages = stages.sortedBy { it.orderIndex }
        this.trip = trip
        this.stages = sortedStages.mapIndexed { index, stage ->
            stage.copy(status = if (index == 0) StageStatus.ACTIVE else StageStatus.PENDING)
        }
        activeIndex = if (this.stages.isNotEmpty()) 0 else -1

        emitState()
        eventLogger.log(TripEvent.TripStarted(trip.id))
    }

    fun onLocationUpdate(lat: Double, lng: Double, speedMps: Float) {
        val current = currentStageOrNull() ?: return
        if (current.trigger != StageTrigger.ON_ARRIVAL) return

        val destination = current.toLocation ?: return
        val distance = distanceMeters(lat, lng, destination.lat, destination.lng)
        if (distance <= arrivalDistanceMeters && speedMps <= maxArrivalSpeedMps) {
            advance()
        }
    }

    fun onTimeTick(nowMillis: Long) {
        val current = currentStageOrNull() ?: return
        if (current.trigger != StageTrigger.AT_TIME) return

        val scheduled = current.scheduledTimeMillis ?: return
        if (nowMillis >= scheduled) {
            advance()
        }
    }

    fun onManualAdvance() {
        if (currentStageOrNull() == null) return
        advance()
    }

    private fun currentStageOrNull(): TripStage? {
        if (activeIndex !in stages.indices) return null
        return stages[activeIndex]
    }

    private fun advance() {
        val currentTrip = trip ?: return
        val previous = currentStageOrNull()

        if (activeIndex !in stages.indices) return

        val updated = stages.toMutableList()
        updated[activeIndex] = updated[activeIndex].copy(status = StageStatus.COMPLETED)

        val nextIndex = activeIndex + 1
        activeIndex = if (nextIndex in updated.indices) {
            updated[nextIndex] = updated[nextIndex].copy(status = StageStatus.ACTIVE)
            nextIndex
        } else {
            -1
        }

        stages = updated
        emitState()

        eventLogger.log(
            TripEvent.StageChanged(
                tripId = currentTrip.id,
                fromStageId = previous?.id,
                toStageId = currentStageOrNull()?.id,
            )
        )
    }

    private fun emitState() {
        val active = currentStageOrNull()
        val next = if (activeIndex >= 0 && activeIndex + 1 in stages.indices) stages[activeIndex + 1] else null

        _state.value = TripState(
            activeStage = active,
            nextStage = next,
            stages = stages,
            isTripCompleted = stages.isNotEmpty() && active == null,
        )
    }
}
