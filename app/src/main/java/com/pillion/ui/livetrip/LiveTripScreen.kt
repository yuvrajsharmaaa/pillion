package com.pillion.ui.livetrip

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pillion.domain.model.PlaceSuggestion
import com.pillion.domain.model.TripStage
import com.pillion.location.LocationPermissionHelper
import com.pillion.navigation.MapsIntentHelper

@Composable
fun LiveTripRoute(
    viewModel: LiveTripViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grantResults ->
        val granted = grantResults.values.all { it }
        viewModel.setLocationPermission(granted)
    }

    LaunchedEffect(Unit) {
        viewModel.setLocationPermission(LocationPermissionHelper.hasLocationPermission(context))
    }

    LiveTripScreen(
        uiState = uiState,
        onBack = onBack,
        onStartTrip = viewModel::startTrip,
        onNextStage = viewModel::nextStage,
        onTimeTick = viewModel::processTimeTick,
        onSimulateArrival = viewModel::simulateArrival,
        onSavePlaceSuggestion = viewModel::savePlaceSuggestion,
        onDeletePlaceSuggestion = viewModel::deletePlaceSuggestion,
        onRequestLocationPermission = {
            permissionLauncher.launch(LocationPermissionHelper.locationPermissions)
        },
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LiveTripScreen(
    uiState: LiveTripUiState,
    onBack: () -> Unit,
    onStartTrip: () -> Unit,
    onNextStage: () -> Unit,
    onTimeTick: () -> Unit,
    onSimulateArrival: () -> Unit,
    onSavePlaceSuggestion: (String, Double, Double, String?, String?) -> Unit,
    onDeletePlaceSuggestion: (Long) -> Unit,
    onRequestLocationPermission: () -> Unit,
) {
    val context = LocalContext.current
    var suggestionName by remember { mutableStateOf("") }
    var suggestionLat by remember { mutableStateOf("") }
    var suggestionLng by remember { mutableStateOf("") }
    var suggestionAddress by remember { mutableStateOf("") }
    var suggestionNotes by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = uiState.trip?.name ?: "Live Trip")
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(text = "Back")
                    }
                },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            uiState.errorMessage?.let { message ->
                Text(text = message, color = MaterialTheme.colorScheme.error)
            }

            if (!uiState.hasLocationPermission) {
                Text(
                    text = "Location permission is needed for automatic ON_ARRIVAL stage transitions.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = onRequestLocationPermission) {
                    Text(text = "Grant Location Permission")
                }
            }

            uiState.latestLocation?.let { sample ->
                Text(
                    text = "Live location: ${sample.lat}, ${sample.lng} @ ${sample.speedMps} m/s",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStartTrip) {
                    Text(text = "Start Trip")
                }
                Button(onClick = onNextStage, enabled = uiState.isTripRunning) {
                    Text(text = "Next Stage")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onTimeTick, enabled = uiState.isTripRunning) {
                    Text(text = "Time Tick")
                }
                Button(onClick = onSimulateArrival, enabled = uiState.isTripRunning) {
                    Text(text = "Simulate Arrival")
                }
            }

            Text(
                text = "Trip completed: ${uiState.tripState.isTripCompleted}",
                style = MaterialTheme.typography.bodyLarge,
            )

            StageDetails(
                title = "Active Stage",
                stage = uiState.tripState.activeStage,
            )

            StageDetails(
                title = "Next Stage",
                stage = uiState.tripState.nextStage,
            )

            val activeLocation = uiState.tripState.activeStage?.toLocation
            if (activeLocation != null) {
                Button(
                    onClick = {
                        MapsIntentHelper.openNavigation(
                            context = context,
                            lat = activeLocation.lat,
                            lng = activeLocation.lng,
                        )
                    }
                ) {
                    Text(text = "Open Destination in Google Maps")
                }
            }

            val query = uiState.tripState.activeStage?.queryConfig?.customQuery
            if (!query.isNullOrBlank() && activeLocation != null) {
                Button(
                    onClick = {
                        MapsIntentHelper.openSearch(
                            context = context,
                            lat = activeLocation.lat,
                            lng = activeLocation.lng,
                            query = query,
                        )
                    }
                ) {
                    Text(text = "Search Nearby in Google Maps")
                }
            }

            Text(
                text = "Manual Place Suggestions",
                style = MaterialTheme.typography.titleMedium,
            )

            OutlinedTextField(
                value = suggestionName,
                onValueChange = { suggestionName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Place name") },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = suggestionLat,
                    onValueChange = { suggestionLat = it },
                    modifier = Modifier.weight(1f),
                    label = { Text(text = "Lat") },
                )
                OutlinedTextField(
                    value = suggestionLng,
                    onValueChange = { suggestionLng = it },
                    modifier = Modifier.weight(1f),
                    label = { Text(text = "Lng") },
                )
            }

            OutlinedTextField(
                value = suggestionAddress,
                onValueChange = { suggestionAddress = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Address") },
            )

            OutlinedTextField(
                value = suggestionNotes,
                onValueChange = { suggestionNotes = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Notes") },
            )

            Button(
                onClick = {
                    val fallbackLocation = uiState.tripState.activeStage?.toLocation
                    val lat = suggestionLat.toDoubleOrNull() ?: fallbackLocation?.lat
                    val lng = suggestionLng.toDoubleOrNull() ?: fallbackLocation?.lng
                    if (lat != null && lng != null) {
                        onSavePlaceSuggestion(
                            suggestionName,
                            lat,
                            lng,
                            suggestionAddress,
                            suggestionNotes,
                        )
                        suggestionName = ""
                        suggestionLat = ""
                        suggestionLng = ""
                        suggestionAddress = ""
                        suggestionNotes = ""
                    }
                },
                enabled = uiState.tripState.activeStage != null,
            ) {
                Text(text = "Save Suggestion")
            }

            if (uiState.placeSuggestions.isNotEmpty()) {
                Text(
                    text = "Saved Suggestions",
                    style = MaterialTheme.typography.titleSmall,
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(uiState.placeSuggestions, key = { suggestion -> suggestion.id }) { suggestion ->
                        PlaceSuggestionCard(
                            suggestion = suggestion,
                            onDelete = onDeletePlaceSuggestion,
                        )
                    }
                }
            }

            Text(
                text = "All Stages",
                style = MaterialTheme.typography.titleMedium,
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.tripState.stages.ifEmpty { uiState.stages }, key = { stage -> stage.id * 31 + stage.orderIndex }) { stage ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "${stage.orderIndex + 1}. ${stage.type.name}")
                            Text(text = "Trigger: ${stage.trigger.name}")
                            Text(text = "Status: ${stage.status.name}")
                            Text(text = "To: ${stage.toLocation?.label.orEmpty()}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceSuggestionCard(
    suggestion: PlaceSuggestion,
    onDelete: (Long) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = suggestion.name, style = MaterialTheme.typography.titleSmall)
            Text(text = "${suggestion.lat}, ${suggestion.lng}")
            Text(text = suggestion.address.orEmpty())
            Text(text = suggestion.notes.orEmpty())
            Button(onClick = { onDelete(suggestion.id) }) {
                Text(text = "Delete")
            }
        }
    }
}

@Composable
private fun StageDetails(
    title: String,
    stage: TripStage?,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            if (stage == null) {
                Text(text = "None")
            } else {
                Text(text = "Type: ${stage.type.name}")
                Text(text = "Trigger: ${stage.trigger.name}")
                Text(text = "Status: ${stage.status.name}")
                Text(text = "To: ${stage.toLocation?.label.orEmpty()}")
            }
        }
    }
}
