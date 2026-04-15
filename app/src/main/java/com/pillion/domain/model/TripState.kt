package com.pillion.domain.model

data class TripState(
    val activeStage: TripStage? = null,
    val nextStage: TripStage? = null,
    val stages: List<TripStage> = emptyList(),
    val isTripCompleted: Boolean = false,
)
