package com.pillion.ui.tripbuilder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun TripBuilderRoute(
    viewModel: TripBuilderViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) {
            viewModel.markSavedHandled()
            onBack()
        }
    }

    TripBuilderScreen(
        uiState = uiState,
        onBack = onBack,
        onTripNameChanged = viewModel::setTripName,
        onAddStage = viewModel::addStage,
        onSave = viewModel::saveTrip,
        onRemoveStage = viewModel::removeStage,
        onMoveStageUp = viewModel::moveStageUp,
        onMoveStageDown = viewModel::moveStageDown,
        onCycleType = viewModel::cycleType,
        onCycleTrigger = viewModel::cycleTrigger,
        onCyclePlaceType = viewModel::cyclePlaceType,
        onDestinationLabelChanged = viewModel::updateDestinationLabel,
        onDestinationLatChanged = viewModel::updateDestinationLat,
        onDestinationLngChanged = viewModel::updateDestinationLng,
        onScheduledTimeChanged = viewModel::updateScheduledTimeMillis,
        onOpenNowChanged = viewModel::toggleOpenNow,
        onCustomQueryChanged = viewModel::updateCustomQuery,
    )
}

@Composable
private fun TripBuilderScreen(
    uiState: TripBuilderUiState,
    onBack: () -> Unit,
    onTripNameChanged: (String) -> Unit,
    onAddStage: () -> Unit,
    onSave: () -> Unit,
    onRemoveStage: (Long) -> Unit,
    onMoveStageUp: (Long) -> Unit,
    onMoveStageDown: (Long) -> Unit,
    onCycleType: (Long) -> Unit,
    onCycleTrigger: (Long) -> Unit,
    onCyclePlaceType: (Long) -> Unit,
    onDestinationLabelChanged: (Long, String) -> Unit,
    onDestinationLatChanged: (Long, String) -> Unit,
    onDestinationLngChanged: (Long, String) -> Unit,
    onScheduledTimeChanged: (Long, String) -> Unit,
    onOpenNowChanged: (Long, Boolean) -> Unit,
    onCustomQueryChanged: (Long, String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Trip Builder") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(text = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onSave, enabled = !uiState.isSaving) {
                        Text(text = if (uiState.isSaving) "Saving..." else "Save")
                    }
                },
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            uiState.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            OutlinedTextField(
                value = uiState.tripName,
                onValueChange = onTripNameChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Trip name") },
            )

            Button(onClick = onAddStage) {
                Text(text = "Add Stage")
            }

            if (uiState.stages.isEmpty()) {
                Text(text = "No stages yet. Add one to continue.")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(uiState.stages, key = { _, stage -> stage.localId }) { index, stage ->
                        StageCard(
                            index = index,
                            stage = stage,
                            onRemoveStage = onRemoveStage,
                            onMoveStageUp = onMoveStageUp,
                            onMoveStageDown = onMoveStageDown,
                            onCycleType = onCycleType,
                            onCycleTrigger = onCycleTrigger,
                            onCyclePlaceType = onCyclePlaceType,
                            onDestinationLabelChanged = onDestinationLabelChanged,
                            onDestinationLatChanged = onDestinationLatChanged,
                            onDestinationLngChanged = onDestinationLngChanged,
                            onScheduledTimeChanged = onScheduledTimeChanged,
                            onOpenNowChanged = onOpenNowChanged,
                            onCustomQueryChanged = onCustomQueryChanged,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StageCard(
    index: Int,
    stage: EditableStage,
    onRemoveStage: (Long) -> Unit,
    onMoveStageUp: (Long) -> Unit,
    onMoveStageDown: (Long) -> Unit,
    onCycleType: (Long) -> Unit,
    onCycleTrigger: (Long) -> Unit,
    onCyclePlaceType: (Long) -> Unit,
    onDestinationLabelChanged: (Long, String) -> Unit,
    onDestinationLatChanged: (Long, String) -> Unit,
    onDestinationLngChanged: (Long, String) -> Unit,
    onScheduledTimeChanged: (Long, String) -> Unit,
    onOpenNowChanged: (Long, Boolean) -> Unit,
    onCustomQueryChanged: (Long, String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "Stage ${index + 1}", style = MaterialTheme.typography.titleMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onCycleType(stage.localId) }) {
                    Text(text = "Type: ${stage.type.name}")
                }
                Button(onClick = { onCycleTrigger(stage.localId) }) {
                    Text(text = "Trigger: ${stage.trigger.name}")
                }
            }

            OutlinedTextField(
                value = stage.destinationLabel,
                onValueChange = { onDestinationLabelChanged(stage.localId, it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Destination label") },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = stage.destinationLat,
                    onValueChange = { onDestinationLatChanged(stage.localId, it) },
                    modifier = Modifier.weight(1f),
                    label = { Text(text = "Lat") },
                )
                OutlinedTextField(
                    value = stage.destinationLng,
                    onValueChange = { onDestinationLngChanged(stage.localId, it) },
                    modifier = Modifier.weight(1f),
                    label = { Text(text = "Lng") },
                )
            }

            OutlinedTextField(
                value = stage.scheduledTimeMillis,
                onValueChange = { onScheduledTimeChanged(stage.localId, it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Scheduled time millis (AT_TIME)") },
            )

            Button(onClick = { onCyclePlaceType(stage.localId) }) {
                Text(text = "Place type: ${stage.placeType.name}")
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Checkbox(
                    checked = stage.openNow,
                    onCheckedChange = { onOpenNowChanged(stage.localId, it) },
                )
                Text(text = "Open now")
            }

            OutlinedTextField(
                value = stage.customQuery,
                onValueChange = { onCustomQueryChanged(stage.localId, it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Custom query") },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onMoveStageUp(stage.localId) }) {
                    Text(text = "Up")
                }
                Button(onClick = { onMoveStageDown(stage.localId) }) {
                    Text(text = "Down")
                }
                Button(onClick = { onRemoveStage(stage.localId) }) {
                    Text(text = "Delete")
                }
            }
        }
    }
}
