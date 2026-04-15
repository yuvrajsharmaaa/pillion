package com.pillion.domain.model

enum class TripStageType {
    RIDE,
    ARRIVAL_PLACES,
    TIME_PLACES,
    CUSTOM,
}

enum class StageTrigger {
    ON_ARRIVAL,
    AT_TIME,
    MANUAL,
}

enum class StageStatus {
    PENDING,
    ACTIVE,
    COMPLETED,
    SKIPPED,
}

data class TripStage(
    val id: Long = 0L,
    val tripId: Long,
    val orderIndex: Int,
    val type: TripStageType,
    val fromLocation: LocationPoint? = null,
    val toLocation: LocationPoint? = null,
    val trigger: StageTrigger,
    val scheduledTimeMillis: Long? = null,
    val queryConfig: StageQueryConfig? = null,
    val status: StageStatus = StageStatus.PENDING,
)
