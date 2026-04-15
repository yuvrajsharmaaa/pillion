package com.pillion.domain.statemachine

import com.pillion.domain.model.LocationPoint
import com.pillion.domain.model.StageStatus
import com.pillion.domain.model.StageTrigger
import com.pillion.domain.model.Trip
import com.pillion.domain.model.TripStage
import com.pillion.domain.model.TripStageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripStateMachineTest {

    @Test
    fun startTrip_setsFirstStageActive() {
        val machine = TripStateMachine()
        val trip = sampleTrip()
        val stages = listOf(sampleManualStage(id = 1L, orderIndex = 0), sampleManualStage(id = 2L, orderIndex = 1))

        machine.startTrip(trip, stages)

        val state = machine.state.value
        assertNotNull(state.activeStage)
        assertEquals(1L, state.activeStage?.id)
        assertEquals(StageStatus.ACTIVE, state.activeStage?.status)
        assertFalse(state.isTripCompleted)
    }

    @Test
    fun onManualAdvance_movesToNextStage() {
        val machine = TripStateMachine()
        val trip = sampleTrip()
        val stages = listOf(sampleManualStage(id = 1L, orderIndex = 0), sampleManualStage(id = 2L, orderIndex = 1))

        machine.startTrip(trip, stages)
        machine.onManualAdvance()

        val state = machine.state.value
        assertEquals(2L, state.activeStage?.id)
        assertEquals(StageStatus.ACTIVE, state.activeStage?.status)
        assertEquals(1L, state.stages.first().id)
        assertEquals(StageStatus.COMPLETED, state.stages.first().status)
    }

    @Test
    fun onArrival_advancesWhenNearAndSlow() {
        val machine = TripStateMachine(arrivalDistanceMeters = 2000.0, maxArrivalSpeedMps = 3.0f)
        val trip = sampleTrip()
        val arrivalStage = TripStage(
            id = 11L,
            tripId = trip.id,
            orderIndex = 0,
            type = TripStageType.RIDE,
            trigger = StageTrigger.ON_ARRIVAL,
            toLocation = LocationPoint(lat = 28.6139, lng = 77.2090, label = "Delhi"),
            status = StageStatus.PENDING,
        )
        val nextStage = sampleManualStage(id = 12L, orderIndex = 1)

        machine.startTrip(trip, listOf(arrivalStage, nextStage))

        machine.onLocationUpdate(lat = 28.7000, lng = 77.2500, speedMps = 2.0f)
        assertEquals(11L, machine.state.value.activeStage?.id)

        machine.onLocationUpdate(lat = 28.6139, lng = 77.2090, speedMps = 1.5f)
        assertEquals(12L, machine.state.value.activeStage?.id)
    }

    @Test
    fun onTimeTick_advancesWhenScheduledTimeReached() {
        val machine = TripStateMachine()
        val trip = sampleTrip()
        val now = 1_000_000L

        val timeStage = TripStage(
            id = 21L,
            tripId = trip.id,
            orderIndex = 0,
            type = TripStageType.TIME_PLACES,
            trigger = StageTrigger.AT_TIME,
            scheduledTimeMillis = now,
            status = StageStatus.PENDING,
        )
        val nextStage = sampleManualStage(id = 22L, orderIndex = 1)

        machine.startTrip(trip, listOf(timeStage, nextStage))
        machine.onTimeTick(nowMillis = now - 1)
        assertEquals(21L, machine.state.value.activeStage?.id)

        machine.onTimeTick(nowMillis = now)
        assertEquals(22L, machine.state.value.activeStage?.id)

        machine.onManualAdvance()
        assertTrue(machine.state.value.isTripCompleted)
    }

    private fun sampleTrip(): Trip =
        Trip(
            id = 99L,
            name = "Weekend Ride",
            createdAt = 1L,
            updatedAt = 1L,
        )

    private fun sampleManualStage(id: Long, orderIndex: Int): TripStage =
        TripStage(
            id = id,
            tripId = 99L,
            orderIndex = orderIndex,
            type = TripStageType.CUSTOM,
            trigger = StageTrigger.MANUAL,
            status = StageStatus.PENDING,
        )
}
