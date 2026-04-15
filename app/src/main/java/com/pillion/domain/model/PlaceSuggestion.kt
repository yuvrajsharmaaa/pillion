package com.pillion.domain.model

data class PlaceSuggestion(
    val id: Long = 0L,
    val stageId: Long,
    val name: String,
    val lat: Double,
    val lng: Double,
    val address: String? = null,
    val notes: String? = null,
)
