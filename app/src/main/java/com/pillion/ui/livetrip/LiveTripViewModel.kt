package com.pillion.ui.livetrip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pillion.di.AppContainer
import com.pillion.domain.events.TripEvent
import com.pillion.domain.model.LocationPoint
import com.pillion.domain.model.PlaceSuggestion
import com.pillion.domain.model.PlaceType
import com.pillion.domain.model.StageStatus
import com.pillion.domain.model.Trip
import com.pillion.domain.model.TripStage
import com.pillion.domain.model.TripState
import com.pillion.domain.statemachine.TripStateMachine
import com.pillion.location.LocationSample
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class TimelineStageStatus {
    PENDING,
    ACTIVE,
    COMPLETED,
    SKIPPED,
}

data class StageTimelineItem(
    val stageId: Long,
    val orderIndex: Int,
    val title: String,
    val triggerLabel: String,
    val status: TimelineStageStatus,
)

enum class CameraTargetReason {
    CURRENT_LOCATION,
    ACTIVE_STAGE,
    SELECTED_STAGE,
}

data class MapCameraTarget(
    val lat: Double,
    val lng: Double,
    val zoom: Float = 15f,
    val reason: CameraTargetReason,
)

enum class QuickCategory {
    HOTELS,
    RESTAURANTS,
    PETROL_PUMPS,
    CAFES,
    CUSTOM,
}

data class ExternalMapSearchRequest(
    val requestId: Long,
    val stageId: Long,
    val lat: Double,
    val lng: Double,
    val query: String,
)

data class NavigationRequest(
    val requestId: Long,
    val lat: Double,
    val lng: Double,
    val label: String,
)

data class LiveTripUiState(
    val isLoading: Boolean = true,
    val trip: Trip? = null,
    val stages: List<TripStage> = emptyList(),
    val timelineItems: List<StageTimelineItem> = emptyList(),
    val activeStageId: Long? = null,
    val upcomingStageId: Long? = null,
    val selectedStageId: Long? = null,
    val placeSuggestions: List<PlaceSuggestion> = emptyList(),
    val hasLocationPermission: Boolean = false,
    val latestLocation: LocationSample? = null,
    val tripState: TripState = TripState(),
    val mapCameraTarget: MapCameraTarget? = null,
    val isArrivalSheetExpanded: Boolean = false,
    val showArrivalSheetForStageId: Long? = null,
    val pendingExternalMapSearch: ExternalMapSearchRequest? = null,
    val pendingNavigationRequest: NavigationRequest? = null,
    val weakConnectionBannerVisible: Boolean = false,
    val locationPermissionRationale: String = "Allow location to automatically switch trip stages when you arrive.",
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
    private var locationJob: Job? = null
    private var timeTickerJob: Job? = null
    private var previousActiveStageId: Long? = null

    init {
        if (tripId <= 0L) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Invalid trip id",
                )
            }
        } else {
            observeTrip()
            observeStages()
            observeStateMachine()
        }
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
                _uiState.update { state ->
                    state.copy(
                        stages = latestStages,
                        timelineItems = toTimelineItems(state.tripState.stages.ifEmpty { latestStages }),
                    )
                }
            }
        }
    }

    private fun observeStateMachine() {
        viewModelScope.launch {
            stateMachine.state.collect { tripState ->
                val activeStageId = tripState.activeStage?.id
                val hasActiveStageChanged = activeStageId != null && activeStageId != previousActiveStageId
                val cameraTargetForActiveStage = tripState.activeStage?.toLocation?.toCameraTarget(
                    reason = CameraTargetReason.ACTIVE_STAGE,
                )

                _uiState.update { state ->
                    state.copy(
                        tripState = tripState,
                        isTripRunning = if (tripState.isTripCompleted) false else state.isTripRunning,
                        activeStageId = activeStageId,
                        upcomingStageId = tripState.nextStage?.id,
                        timelineItems = toTimelineItems(tripState.stages.ifEmpty { latestStages }),
                        selectedStageId = state.selectedStageId ?: activeStageId,
                        showArrivalSheetForStageId = if (hasActiveStageChanged) activeStageId else state.showArrivalSheetForStageId,
                        isArrivalSheetExpanded = if (hasActiveStageChanged) true else state.isArrivalSheetExpanded,
                        mapCameraTarget = if (hasActiveStageChanged) cameraTargetForActiveStage else state.mapCameraTarget,
                    )
                }

                if (tripState.isTripCompleted) {
                    stopRealtimeJobs()
                    _uiState.update {
                        it.copy(
                            isArrivalSheetExpanded = false,
                            showArrivalSheetForStageId = null,
                        )
                    }
                }

                previousActiveStageId = activeStageId
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
        _uiState.update { state ->
            state.copy(
                isTripRunning = true,
                isArrivalSheetExpanded = false,
                showArrivalSheetForStageId = null,
                weakConnectionBannerVisible = false,
            )
        }

        startTimeTickerIfNeeded()
        startLocationTrackingIfAllowed()
    }

    fun stopTrip() {
        stopRealtimeJobs()
        _uiState.update {
            it.copy(
                isTripRunning = false,
                isArrivalSheetExpanded = false,
                showArrivalSheetForStageId = null,
            )
        }
    }

    fun setLocationPermission(granted: Boolean) {
        _uiState.update { it.copy(hasLocationPermission = granted) }

        if (granted) {
            startLocationTrackingIfAllowed()
        } else {
            locationJob?.cancel()
            locationJob = null
        }
    }

    fun nextStage() {
        stateMachine.onManualAdvance()
    }

    fun toggleArrivalSheetExpanded() {
        _uiState.update { it.copy(isArrivalSheetExpanded = !it.isArrivalSheetExpanded) }
    }

    fun dismissArrivalSheet() {
        _uiState.update {
            it.copy(
                isArrivalSheetExpanded = false,
                showArrivalSheetForStageId = null,
            )
        }
    }

    fun consumeArrivalSheetForStage(stageId: Long) {
        _uiState.update { state ->
            if (state.showArrivalSheetForStageId == stageId) {
                state.copy(showArrivalSheetForStageId = null)
            } else {
                state
            }
        }
    }

    fun focusStage(stageId: Long) {
        val stage = _uiState.value.tripState.stages.ifEmpty { latestStages }
            .firstOrNull { it.id == stageId } ?: return

        _uiState.update {
            it.copy(
                selectedStageId = stageId,
                mapCameraTarget = stage.toLocation?.toCameraTarget(CameraTargetReason.SELECTED_STAGE),
            )
        }
    }

    fun centerOnMyLocation() {
        val location = _uiState.value.latestLocation ?: return
        _uiState.update {
            it.copy(
                mapCameraTarget = MapCameraTarget(
                    lat = location.lat,
                    lng = location.lng,
                    zoom = 16f,
                    reason = CameraTargetReason.CURRENT_LOCATION,
                )
            )
        }
    }

    fun consumeMapCameraTarget() {
        _uiState.update { it.copy(mapCameraTarget = null) }
    }

    fun onQuickCategorySelected(category: QuickCategory) {
        val activeStage = _uiState.value.tripState.activeStage ?: return
        val destination = activeStage.toLocation ?: return

        val query = when (category) {
            QuickCategory.HOTELS -> "hotels"
            QuickCategory.RESTAURANTS -> "restaurants"
            QuickCategory.PETROL_PUMPS -> "petrol pump"
            QuickCategory.CAFES -> "cafes"
            QuickCategory.CUSTOM -> activeStage.queryConfig?.customQuery ?: "places"
        }

        enqueueSearchRequest(
            stageId = activeStage.id,
            destination = destination,
            query = query,
        )
    }

    fun onStageConfiguredCategorySelected() {
        val activeStage = _uiState.value.tripState.activeStage ?: return
        val destination = activeStage.toLocation ?: return
        val query = activeStage.queryConfig?.toSearchQuery() ?: return

        enqueueSearchRequest(
            stageId = activeStage.id,
            destination = destination,
            query = query,
        )
    }

    fun onPlaceSuggestionSelected(placeSuggestionId: Long) {
        val suggestion = _uiState.value.placeSuggestions.firstOrNull { it.id == placeSuggestionId } ?: return
        _uiState.update {
            it.copy(
                pendingNavigationRequest = NavigationRequest(
                    requestId = System.nanoTime(),
                    lat = suggestion.lat,
                    lng = suggestion.lng,
                    label = suggestion.name,
                )
            )
        }
    }

    fun consumePendingExternalMapSearch(requestId: Long) {
        _uiState.update { state ->
            if (state.pendingExternalMapSearch?.requestId == requestId) {
                state.copy(pendingExternalMapSearch = null)
            } else {
                state
            }
        }
    }

    fun consumePendingNavigationRequest(requestId: Long) {
        _uiState.update { state ->
            if (state.pendingNavigationRequest?.requestId == requestId) {
                state.copy(pendingNavigationRequest = null)
            } else {
                state
            }
        }
    }

    fun dismissWeakConnectionBanner() {
        _uiState.update { it.copy(weakConnectionBannerVisible = false) }
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

    private fun startLocationTrackingIfAllowed() {
        val state = _uiState.value
        if (!state.isTripRunning || !state.hasLocationPermission || state.tripState.isTripCompleted) return
        if (locationJob?.isActive == true) return

        locationJob = viewModelScope.launch {
            try {
                appContainer.locationUpdatesManager.locationFlow().collect { sample ->
                    _uiState.update {
                        it.copy(
                            latestLocation = sample,
                            weakConnectionBannerVisible = false,
                        )
                    }
                    stateMachine.onLocationUpdate(sample.lat, sample.lng, sample.speedMps)
                }
            } catch (_: SecurityException) {
                _uiState.update {
                    it.copy(errorMessage = "Location permission is required for live arrival triggers")
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(weakConnectionBannerVisible = true)
                }
            }
        }
    }

    private fun startTimeTickerIfNeeded() {
        if (timeTickerJob?.isActive == true) return

        timeTickerJob = viewModelScope.launch {
            while (isActive && _uiState.value.isTripRunning && !_uiState.value.tripState.isTripCompleted) {
                delay(60_000L)
                stateMachine.onTimeTick(System.currentTimeMillis())
            }
        }
    }

    private fun stopRealtimeJobs() {
        locationJob?.cancel()
        locationJob = null

        timeTickerJob?.cancel()
        timeTickerJob = null
    }

    private fun toTimelineItems(stages: List<TripStage>): List<StageTimelineItem> {
        return stages.sortedBy { it.orderIndex }.map { stage ->
            StageTimelineItem(
                stageId = stage.id,
                orderIndex = stage.orderIndex,
                title = stage.type.name.replace('_', ' '),
                triggerLabel = stage.trigger.name.replace('_', ' '),
                status = stage.status.toTimelineStatus(),
            )
        }
    }

    private fun StageStatus.toTimelineStatus(): TimelineStageStatus {
        return when (this) {
            StageStatus.PENDING -> TimelineStageStatus.PENDING
            StageStatus.ACTIVE -> TimelineStageStatus.ACTIVE
            StageStatus.COMPLETED -> TimelineStageStatus.COMPLETED
            StageStatus.SKIPPED -> TimelineStageStatus.SKIPPED
        }
    }

    private fun LocationPoint.toCameraTarget(reason: CameraTargetReason): MapCameraTarget {
        return MapCameraTarget(
            lat = lat,
            lng = lng,
            reason = reason,
        )
    }

    private fun enqueueSearchRequest(
        stageId: Long,
        destination: LocationPoint,
        query: String,
    ) {
        _uiState.update {
            it.copy(
                pendingExternalMapSearch = ExternalMapSearchRequest(
                    requestId = System.nanoTime(),
                    stageId = stageId,
                    lat = destination.lat,
                    lng = destination.lng,
                    query = query,
                )
            )
        }
    }

    private fun com.pillion.domain.model.StageQueryConfig.toSearchQuery(): String {
        return when (placeType) {
            PlaceType.HOTEL -> "hotels"
            PlaceType.FOOD -> "restaurants"
            PlaceType.FUEL -> "petrol pump"
            PlaceType.CAFE -> "cafes"
            PlaceType.MECHANIC -> "motorcycle mechanic"
            PlaceType.CUSTOM_QUERY -> customQuery ?: "places"
        }
    }

    override fun onCleared() {
        stopRealtimeJobs()
        placeSuggestionJob?.cancel()
        super.onCleared()
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
