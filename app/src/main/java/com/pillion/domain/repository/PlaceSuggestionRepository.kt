package com.pillion.domain.repository

import com.pillion.domain.model.PlaceSuggestion
import kotlinx.coroutines.flow.Flow

interface PlaceSuggestionRepository {
    fun getPlaceSuggestionsForStageFlow(stageId: Long): Flow<List<PlaceSuggestion>>
    suspend fun upsertSuggestion(place: PlaceSuggestion)
    suspend fun deleteSuggestion(id: Long)
}
