package com.pillion.di

import android.content.Context
import androidx.room.Room
import com.pillion.data.local.AppDatabase
import com.pillion.data.repository.LocalPlaceSuggestionRepository
import com.pillion.data.repository.LocalTripRepository
import com.pillion.data.repository.LocalTripStageRepository
import com.pillion.domain.events.NoOpTripEventLogger
import com.pillion.domain.events.TripEventLogger
import com.pillion.domain.repository.PlaceSuggestionRepository
import com.pillion.domain.repository.TripRepository
import com.pillion.domain.repository.TripStageRepository
import com.pillion.domain.statemachine.TripStateMachine
import com.pillion.location.LocationUpdatesManager
import com.google.android.gms.location.LocationServices
import com.pillion.domain.usecase.CreateTripUseCase
import com.pillion.domain.usecase.DeletePlaceSuggestionUseCase
import com.pillion.domain.usecase.DeleteTripStageUseCase
import com.pillion.domain.usecase.DeleteTripUseCase
import com.pillion.domain.usecase.GetTripByIdUseCase
import com.pillion.domain.usecase.GetTripStagesUseCase
import com.pillion.domain.usecase.GetTripsUseCase
import com.pillion.domain.usecase.ObservePlaceSuggestionsUseCase
import com.pillion.domain.usecase.ObserveTripStagesUseCase
import com.pillion.domain.usecase.UpdateTripUseCase
import com.pillion.domain.usecase.UpsertPlaceSuggestionUseCase
import com.pillion.domain.usecase.UpsertTripStageUseCase

class AppContainer(context: Context) {
    private val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    private val database: AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "pillion.db",
        )
            .fallbackToDestructiveMigration()
            .build()

    val tripRepository: TripRepository = LocalTripRepository(database.tripDao())
    val tripStageRepository: TripStageRepository = LocalTripStageRepository(database.tripStageDao())
    val placeSuggestionRepository: PlaceSuggestionRepository =
        LocalPlaceSuggestionRepository(database.placeSuggestionDao())

    val createTripUseCase = CreateTripUseCase(tripRepository)
    val getTripsUseCase = GetTripsUseCase(tripRepository)
    val getTripByIdUseCase = GetTripByIdUseCase(tripRepository)
    val updateTripUseCase = UpdateTripUseCase(tripRepository)
    val deleteTripUseCase = DeleteTripUseCase(tripRepository)

    val observeTripStagesUseCase = ObserveTripStagesUseCase(tripStageRepository)
    val getTripStagesUseCase = GetTripStagesUseCase(tripStageRepository)
    val upsertTripStageUseCase = UpsertTripStageUseCase(tripStageRepository)
    val deleteTripStageUseCase = DeleteTripStageUseCase(tripStageRepository)

    val observePlaceSuggestionsUseCase = ObservePlaceSuggestionsUseCase(placeSuggestionRepository)
    val upsertPlaceSuggestionUseCase = UpsertPlaceSuggestionUseCase(placeSuggestionRepository)
    val deletePlaceSuggestionUseCase = DeletePlaceSuggestionUseCase(placeSuggestionRepository)

    val tripEventLogger: TripEventLogger = NoOpTripEventLogger
    val locationUpdatesManager = LocationUpdatesManager(fusedLocationProviderClient)

    fun createTripStateMachine(): TripStateMachine =
        TripStateMachine(eventLogger = tripEventLogger)
}
