package com.pillion.domain.usecase

import com.pillion.domain.model.PlaceSuggestion
import com.pillion.domain.repository.PlaceSuggestionRepository
import kotlinx.coroutines.flow.Flow

class ObservePlaceSuggestionsUseCase(
    private val placeSuggestionRepository: PlaceSuggestionRepository,
) {
    operator fun invoke(stageId: Long): Flow<List<PlaceSuggestion>> =
        placeSuggestionRepository.getPlaceSuggestionsForStageFlow(stageId)
}

class UpsertPlaceSuggestionUseCase(
    private val placeSuggestionRepository: PlaceSuggestionRepository,
) {
    suspend operator fun invoke(placeSuggestion: PlaceSuggestion) {
        placeSuggestionRepository.upsertSuggestion(placeSuggestion)
    }
}

class DeletePlaceSuggestionUseCase(
    private val placeSuggestionRepository: PlaceSuggestionRepository,
) {
    suspend operator fun invoke(placeSuggestionId: Long) {
        placeSuggestionRepository.deleteSuggestion(placeSuggestionId)
    }
}
