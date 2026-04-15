package com.pillion.ui.livetrip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pillion.di.AppContainer
import com.pillion.domain.events.TripEvent
import com.pillion.domain.model.PlaceSuggestion
import com.pillion.domain.model.Trip
import com.pillion.domain.model.TripStage
import com.pillion.domain.model.TripState
import com.pillion.domain.statemachine.TripStateMachine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LiveTripUiState(
    val isLoading: Boolean = true,
    val trip: Trip? = null,
    val stages: List<TripStage> = emptyList(),
    val placeSuggestions: List<PlaceSuggestion> = emptyList(),
    val tripState: TripState = TripState(),
    val isTripRunning: Boolean = false,
    val errorMessage: String? = null,
)

class LiveTripViewModel(
    private val appContainer: AppContainer,
    private val tripId: Long,
) : ViewModel() {

    private val stateMachine: TripStateMachine = appContainer.createTripStateMachine()

    private val _uiState = MutableStateFlow(LiveTripUiState())
    val uiState: StateFlow<LiveTripUiState> = _uiState.asStateFlow()

    private var loadedTrip: Trip? = null
    private var latestStages: List<TripStage> = emptyList()
    private var placeSuggestionJob: Job? = null

    init {
        if (tripId <= 0L) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Invalid trip id",
                )
            }
            return
        }

        observeTrip()
        observeStages()
        observeStateMachine()
    }

    private fun observeTrip() {
        viewModelScope.launch {
            val trip = appContainer.getTripByIdUseCase(tripId)
            loadedTrip = trip
            _uiState.update {
                it.copy(
                    isLoading = false,
                    trip = trip,
                    errorMessage = if (trip == null) "Trip not found" else null,
                )
            }
        }
    }

    private fun observeStages() {
        viewModelScope.launch {
            appContainer.observeTripStagesUseCase(tripId).collect { stages ->
                latestStages = stages.sortedBy { it.orderIndex }
                _uiState.update { it.copy(stages = latestStages) }
            }
        }
    }

    private fun observeStateMachine() {
        viewModelScope.launch {
            stateMachine.state.collect { tripState ->
                _uiState.update { it.copy(tripState = tripState) }
                observePlaceSuggestionsForStage(tripState.activeStage?.id)
            }
        }
    }

    private fun observePlaceSuggestionsForStage(stageId: Long?) {
        placeSuggestionJob?.cancel()

        if (stageId == null || stageId <= 0L) {
            _uiState.update { it.copy(placeSuggestions = emptyList()) }
            return
        }

        placeSuggestionJob = viewModelScope.launch {
            appContainer.observePlaceSuggestionsUseCase(stageId).collect { suggestions ->
                _uiState.update { it.copy(placeSuggestions = suggestions) }
            }
        }
    }

    fun startTrip() {
        val trip = loadedTrip ?: return
        if (latestStages.isEmpty()) return

        stateMachine.startTrip(trip, latestStages)
        _uiState.update { it.copy(isTripRunning = true) }
    }

    fun nextStage() {
        stateMachine.onManualAdvance()
    }

    fun processTimeTick() {
        stateMachine.onTimeTick(System.currentTimeMillis())
    }

    fun simulateArrival() {
        val destination = _uiState.value.tripState.activeStage?.toLocation ?: return
        stateMachine.onLocationUpdate(
            lat = destination.lat,
            lng = destination.lng,
            speedMps = 0.5f,
        )
    }

    fun savePlaceSuggestion(
        name: String,
        lat: Double,
        lng: Double,
        address: String?,
        notes: String?,
    ) {
        val activeStage = _uiState.value.tripState.activeStage ?: return
        if (name.isBlank()) return

        viewModelScope.launch {
            appContainer.upsertPlaceSuggestionUseCase(
                PlaceSuggestion(
                    stageId = activeStage.id,
                    name = name.trim(),
                    lat = lat,
                    lng = lng,
                    address = address?.takeIf { it.isNotBlank() },
                    notes = notes?.takeIf { it.isNotBlank() },
                )
            )

            appContainer.tripEventLogger.log(
                TripEvent.PlaceSelected(
                    tripId = tripId,
                    stageId = activeStage.id,
                    placeName = name.trim(),
                )
            )
        }
    }

    fun deletePlaceSuggestion(placeSuggestionId: Long) {
        viewModelScope.launch {
            appContainer.deletePlaceSuggestionUseCase(placeSuggestionId)
        }
    }

    class Factory(
        private val appContainer: AppContainer,
        private val tripId: Long,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LiveTripViewModel(appContainer, tripId) as T
        }
    }
}
