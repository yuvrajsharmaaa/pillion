package com.pillion.ui.triplist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pillion.di.AppContainer
import com.pillion.domain.model.Trip
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TripListUiState(
    val isLoading: Boolean = true,
    val trips: List<Trip> = emptyList(),
)

class TripListViewModel(
    private val appContainer: AppContainer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripListUiState())
    val uiState: StateFlow<TripListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            appContainer.getTripsUseCase().collect { trips ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        trips = trips,
                    )
                }
            }
        }
    }

    fun deleteTrip(tripId: Long) {
        viewModelScope.launch {
            appContainer.deleteTripUseCase(tripId)
        }
    }

    class Factory(
        private val appContainer: AppContainer,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TripListViewModel(appContainer) as T
        }
    }
}
