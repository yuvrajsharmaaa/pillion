package com.pillion.ui.triplist

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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pillion.domain.model.Trip

@Composable
fun TripListRoute(
    viewModel: TripListViewModel,
    onCreateTrip: () -> Unit,
    onOpenTrip: (Long) -> Unit,
    onStartTrip: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TripListScreen(
        uiState = uiState,
        onCreateTrip = onCreateTrip,
        onOpenTrip = onOpenTrip,
        onStartTrip = onStartTrip,
        onDeleteTrip = viewModel::deleteTrip,
    )
}

@Composable
private fun TripListScreen(
    uiState: TripListUiState,
    onCreateTrip: () -> Unit,
    onOpenTrip: (Long) -> Unit,
    onStartTrip: (Long) -> Unit,
    onDeleteTrip: (Long) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(text = "Pillion Trips") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateTrip) {
                Text(text = "+")
            }
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Loading trips...",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else if (uiState.trips.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "No trips yet. Tap + to create your first trip.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.trips, key = { it.id }) { trip ->
                    TripCard(
                        trip = trip,
                        onOpenTrip = onOpenTrip,
                        onStartTrip = onStartTrip,
                        onDeleteTrip = onDeleteTrip,
                    )
                }
            }
        }
    }
}

@Composable
private fun TripCard(
    trip: Trip,
    onOpenTrip: (Long) -> Unit,
    onStartTrip: (Long) -> Unit,
    onDeleteTrip: (Long) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = trip.name,
                style = MaterialTheme.typography.titleMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onOpenTrip(trip.id) }) {
                    Text(text = "Edit")
                }
                Button(onClick = { onStartTrip(trip.id) }) {
                    Text(text = "Start")
                }
                Button(onClick = { onDeleteTrip(trip.id) }) {
                    Text(text = "Delete")
                }
            }
        }
    }
}
