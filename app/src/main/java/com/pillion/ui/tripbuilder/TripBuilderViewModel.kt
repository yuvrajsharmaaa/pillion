package com.pillion.ui.tripbuilder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pillion.di.AppContainer
import com.pillion.domain.model.LocationPoint
import com.pillion.domain.model.PlaceType
import com.pillion.domain.model.StageQueryConfig
import com.pillion.domain.model.StageStatus
import com.pillion.domain.model.StageTrigger
import com.pillion.domain.model.Trip
import com.pillion.domain.model.TripStage
import com.pillion.domain.model.TripStageType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditableStage(
    val localId: Long,
    val persistedId: Long,
    val type: TripStageType = TripStageType.RIDE,
    val trigger: StageTrigger = StageTrigger.MANUAL,
    val destinationLabel: String = "",
    val destinationLat: String = "",
    val destinationLng: String = "",
    val scheduledTimeMillis: String = "",
    val placeType: PlaceType = PlaceType.CUSTOM_QUERY,
    val openNow: Boolean = false,
    val customQuery: String = "",
)

data class TripBuilderUiState(
    val tripId: Long,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val tripName: String = "",
    val stages: List<EditableStage> = emptyList(),
    val errorMessage: String? = null,
    val saved: Boolean = false,
)

class TripBuilderViewModel(
    private val appContainer: AppContainer,
    private val tripId: Long,
) : ViewModel() {

    private var loadedTrip: Trip? = null
    private var initialPersistedStageIds: Set<Long> = emptySet()
    private var nextTempStageId: Long = -1L

    private val _uiState = MutableStateFlow(
        TripBuilderUiState(
            tripId = tripId,
            isLoading = true,
        )
    )
    val uiState: StateFlow<TripBuilderUiState> = _uiState.asStateFlow()

    init {
        if (tripId <= 0L) {
            _uiState.update { it.copy(isLoading = false) }
        } else {
            loadTrip()
        }
    }

    private fun loadTrip() {
        viewModelScope.launch {
            val trip = appContainer.getTripByIdUseCase(tripId)
            if (trip == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Trip not found",
                    )
                }
                return@launch
            }

            val stages = appContainer.getTripStagesUseCase(tripId).sortedBy { it.orderIndex }
            loadedTrip = trip
            initialPersistedStageIds = stages.map { it.id }.filter { it > 0L }.toSet()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    tripName = trip.name,
                    stages = stages.map { it.toEditableStage() },
                    errorMessage = null,
                )
            }
        }
    }

    fun setTripName(value: String) {
        _uiState.update { it.copy(tripName = value) }
    }

    fun addStage() {
        _uiState.update { state ->
            state.copy(
                stages = state.stages +
                    EditableStage(
                        localId = nextTempStageId--,
                        persistedId = 0L,
                    )
            )
        }
    }

    fun removeStage(localId: Long) {
        _uiState.update { state ->
            state.copy(stages = state.stages.filterNot { it.localId == localId })
        }
    }

    fun moveStageUp(localId: Long) {
        _uiState.update { state ->
            val index = state.stages.indexOfFirst { it.localId == localId }
            if (index <= 0) return@update state

            val mutable = state.stages.toMutableList()
            mutable.swap(index, index - 1)
            state.copy(stages = mutable)
        }
    }

    fun moveStageDown(localId: Long) {
        _uiState.update { state ->
            val index = state.stages.indexOfFirst { it.localId == localId }
            if (index == -1 || index >= state.stages.lastIndex) return@update state

            val mutable = state.stages.toMutableList()
            mutable.swap(index, index + 1)
            state.copy(stages = mutable)
        }
    }

    fun cycleType(localId: Long) {
        _uiState.update { state ->
            state.copy(stages = state.stages.map { stage ->
                if (stage.localId != localId) stage else stage.copy(type = stage.type.nextEnum())
            })
        }
    }

    fun cycleTrigger(localId: Long) {
        _uiState.update { state ->
            state.copy(stages = state.stages.map { stage ->
                if (stage.localId != localId) stage else stage.copy(trigger = stage.trigger.nextEnum())
            })
        }
    }

    fun cyclePlaceType(localId: Long) {
        _uiState.update { state ->
            state.copy(stages = state.stages.map { stage ->
                if (stage.localId != localId) stage else stage.copy(placeType = stage.placeType.nextEnum())
            })
        }
    }

    fun updateDestinationLabel(localId: Long, value: String) {
        updateStage(localId) { it.copy(destinationLabel = value) }
    }

    fun updateDestinationLat(localId: Long, value: String) {
        updateStage(localId) { it.copy(destinationLat = value) }
    }

    fun updateDestinationLng(localId: Long, value: String) {
        updateStage(localId) { it.copy(destinationLng = value) }
    }

    fun updateScheduledTimeMillis(localId: Long, value: String) {
        updateStage(localId) { it.copy(scheduledTimeMillis = value) }
    }

    fun toggleOpenNow(localId: Long, value: Boolean) {
        updateStage(localId) { it.copy(openNow = value) }
    }

    fun updateCustomQuery(localId: Long, value: String) {
        updateStage(localId) { it.copy(customQuery = value) }
    }

    fun saveTrip() {
        val state = _uiState.value
        val trimmedName = state.tripName.trim()

        if (trimmedName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Trip name is required") }
            return
        }

        if (state.stages.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Add at least one stage") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            val resolvedTripId = if (tripId <= 0L) {
                appContainer.createTripUseCase(trimmedName)
            } else {
                val current = loadedTrip
                if (current != null) {
                    appContainer.updateTripUseCase(current.copy(name = trimmedName))
                }
                tripId
            }

            val domainStages = state.stages.mapIndexed { index, editable ->
                editable.toDomainStage(
                    tripId = resolvedTripId,
                    orderIndex = index,
                )
            }

            domainStages.forEach { stage ->
                appContainer.upsertTripStageUseCase(stage)
            }

            val currentPersistedIds = domainStages.map { it.id }.filter { it > 0L }.toSet()
            val removedStageIds = initialPersistedStageIds - currentPersistedIds
            removedStageIds.forEach { stageId ->
                appContainer.deleteTripStageUseCase(stageId)
            }

            _uiState.update {
                it.copy(
                    isSaving = false,
                    saved = true,
                )
            }
        }
    }

    fun markSavedHandled() {
        _uiState.update { it.copy(saved = false) }
    }

    private fun updateStage(localId: Long, transform: (EditableStage) -> EditableStage) {
        _uiState.update { state ->
            state.copy(stages = state.stages.map { stage ->
                if (stage.localId == localId) transform(stage) else stage
            })
        }
    }

    private fun TripStage.toEditableStage(): EditableStage {
        return EditableStage(
            localId = id,
            persistedId = id,
            type = type,
            trigger = trigger,
            destinationLabel = toLocation?.label.orEmpty(),
            destinationLat = toLocation?.lat?.toString().orEmpty(),
            destinationLng = toLocation?.lng?.toString().orEmpty(),
            scheduledTimeMillis = scheduledTimeMillis?.toString().orEmpty(),
            placeType = queryConfig?.placeType ?: PlaceType.CUSTOM_QUERY,
            openNow = queryConfig?.openNow ?: false,
            customQuery = queryConfig?.customQuery.orEmpty(),
        )
    }

    private fun EditableStage.toDomainStage(tripId: Long, orderIndex: Int): TripStage {
        val destinationLatValue = destinationLat.toDoubleOrNull()
        val destinationLngValue = destinationLng.toDoubleOrNull()

        val destination =
            if (destinationLatValue != null && destinationLngValue != null) {
                LocationPoint(
                    lat = destinationLatValue,
                    lng = destinationLngValue,
                    label = destinationLabel.trim(),
                )
            } else {
                null
            }

        val queryConfig =
            if (customQuery.isNotBlank() || openNow || placeType != PlaceType.CUSTOM_QUERY) {
                StageQueryConfig(
                    placeType = placeType,
                    openNow = openNow,
                    customQuery = customQuery.ifBlank { null },
                )
            } else {
                null
            }

        return TripStage(
            id = persistedId,
            tripId = tripId,
            orderIndex = orderIndex,
            type = type,
            trigger = trigger,
            toLocation = destination,
            scheduledTimeMillis = scheduledTimeMillis.toLongOrNull(),
            queryConfig = queryConfig,
            status = StageStatus.PENDING,
        )
    }

    class Factory(
        private val appContainer: AppContainer,
        private val tripId: Long,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TripBuilderViewModel(appContainer, tripId) as T
        }
    }
}

private fun <T> MutableList<T>.swap(from: Int, to: Int) {
    val first = this[from]
    this[from] = this[to]
    this[to] = first
}

private inline fun <reified T : Enum<T>> T.nextEnum(): T {
    val values = enumValues<T>()
    val nextIndex = (ordinal + 1) % values.size
    return values[nextIndex]
}
