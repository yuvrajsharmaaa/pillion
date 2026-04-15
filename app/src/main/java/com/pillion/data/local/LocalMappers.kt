package com.pillion.data.local

import com.pillion.data.local.entity.PlaceSuggestionEntity
import com.pillion.data.local.entity.TripEntity
import com.pillion.data.local.entity.TripStageEntity
import com.pillion.domain.model.LocationPoint
import com.pillion.domain.model.PlaceSuggestion
import com.pillion.domain.model.PlaceType
import com.pillion.domain.model.StageQueryConfig
import com.pillion.domain.model.StageStatus
import com.pillion.domain.model.StageTrigger
import com.pillion.domain.model.Trip
import com.pillion.domain.model.TripStage
import com.pillion.domain.model.TripStageType

fun TripEntity.toDomain(): Trip =
    Trip(
        id = id,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun Trip.toEntity(): TripEntity =
    TripEntity(
        id = id,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun TripStageEntity.toDomain(): TripStage {
    val hasQueryConfig =
        placeType != null || minRating != null || maxPriceLevel != null || openNow || !customQuery.isNullOrBlank()

    return TripStage(
        id = id,
        tripId = tripId,
        orderIndex = orderIndex,
        type = type.toEnumOrDefault(TripStageType.RIDE),
        fromLocation = toLocationOrNull(fromLat, fromLng, fromLabel),
        toLocation = toLocationOrNull(toLat, toLng, toLabel),
        trigger = trigger.toEnumOrDefault(StageTrigger.MANUAL),
        scheduledTimeMillis = scheduledTimeMillis,
        queryConfig = if (hasQueryConfig) {
            StageQueryConfig(
                placeType = placeType?.toEnumOrDefault(PlaceType.CUSTOM_QUERY) ?: PlaceType.CUSTOM_QUERY,
                minRating = minRating,
                maxPriceLevel = maxPriceLevel,
                openNow = openNow,
                customQuery = customQuery,
            )
        } else {
            null
        },
        status = status.toEnumOrDefault(StageStatus.PENDING),
    )
}

fun TripStage.toEntity(): TripStageEntity =
    TripStageEntity(
        id = id,
        tripId = tripId,
        orderIndex = orderIndex,
        type = type.name,
        fromLat = fromLocation?.lat,
        fromLng = fromLocation?.lng,
        fromLabel = fromLocation?.label,
        toLat = toLocation?.lat,
        toLng = toLocation?.lng,
        toLabel = toLocation?.label,
        trigger = trigger.name,
        scheduledTimeMillis = scheduledTimeMillis,
        placeType = queryConfig?.placeType?.name,
        minRating = queryConfig?.minRating,
        maxPriceLevel = queryConfig?.maxPriceLevel,
        openNow = queryConfig?.openNow ?: false,
        customQuery = queryConfig?.customQuery,
        status = status.name,
    )

fun PlaceSuggestionEntity.toDomain(): PlaceSuggestion =
    PlaceSuggestion(
        id = id,
        stageId = stageId,
        name = name,
        lat = lat,
        lng = lng,
        address = address,
        notes = notes,
    )

fun PlaceSuggestion.toEntity(): PlaceSuggestionEntity =
    PlaceSuggestionEntity(
        id = id,
        stageId = stageId,
        name = name,
        lat = lat,
        lng = lng,
        address = address,
        notes = notes,
    )

private fun toLocationOrNull(lat: Double?, lng: Double?, label: String?): LocationPoint? {
    if (lat == null || lng == null) return null
    return LocationPoint(lat = lat, lng = lng, label = label.orEmpty())
}

private inline fun <reified T : Enum<T>> String.toEnumOrDefault(default: T): T {
    return enumValues<T>().firstOrNull { it.name == this } ?: default
}
