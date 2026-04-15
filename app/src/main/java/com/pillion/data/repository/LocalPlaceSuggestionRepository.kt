package com.pillion.data.repository

import com.pillion.data.local.dao.PlaceSuggestionDao
import com.pillion.data.local.toDomain
import com.pillion.data.local.toEntity
import com.pillion.domain.model.PlaceSuggestion
import com.pillion.domain.repository.PlaceSuggestionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalPlaceSuggestionRepository(
    private val placeSuggestionDao: PlaceSuggestionDao,
) : PlaceSuggestionRepository {

    override fun getPlaceSuggestionsForStageFlow(stageId: Long): Flow<List<PlaceSuggestion>> =
        placeSuggestionDao.observeForStage(stageId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun upsertSuggestion(place: PlaceSuggestion) {
        if (place.id == 0L) {
            placeSuggestionDao.insert(place.toEntity())
        } else {
            placeSuggestionDao.update(place.toEntity())
        }
    }

    override suspend fun deleteSuggestion(id: Long) {
        placeSuggestionDao.deleteById(id)
    }
}
