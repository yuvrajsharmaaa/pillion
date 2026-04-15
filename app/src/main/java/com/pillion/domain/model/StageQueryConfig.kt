package com.pillion.domain.model

enum class PlaceType {
    HOTEL,
    FOOD,
    FUEL,
    CAFE,
    MECHANIC,
    CUSTOM_QUERY,
}

data class StageQueryConfig(
    val placeType: PlaceType = PlaceType.CUSTOM_QUERY,
    val minRating: Float? = null,
    val maxPriceLevel: Int? = null,
    val openNow: Boolean = false,
    val customQuery: String? = null,
)
