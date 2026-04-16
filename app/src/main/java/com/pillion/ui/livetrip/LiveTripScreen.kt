package com.pillion.ui.livetrip

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.pillion.domain.model.PlaceSuggestion
import com.pillion.domain.model.StageStatus
import com.pillion.domain.model.TripStage
import com.pillion.location.LocationPermissionHelper
import com.pillion.navigation.MapsIntentHelper
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import kotlinx.coroutines.launch

@Composable
fun LiveTripRoute(
    viewModel: LiveTripViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
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
        onStopTrip = viewModel::stopTrip,
        onNextStage = viewModel::nextStage,
        onCenterOnMe = viewModel::centerOnMyLocation,
        onFocusStage = viewModel::focusStage,
        onQuickCategory = viewModel::onQuickCategorySelected,
        onStageConfiguredSearch = viewModel::onStageConfiguredCategorySelected,
        onPlaceSelected = viewModel::onPlaceSuggestionSelected,
        onDeletePlace = viewModel::deletePlaceSuggestion,
        onDismissArrivalSheet = viewModel::dismissArrivalSheet,
        onToggleArrivalSheet = viewModel::toggleArrivalSheetExpanded,
        onConsumeMapCameraTarget = viewModel::consumeMapCameraTarget,
        onConsumeMapSearchEvent = viewModel::consumePendingExternalMapSearch,
        onConsumeNavigationEvent = viewModel::consumePendingNavigationRequest,
        onConsumeArrivalSheetEvent = viewModel::consumeArrivalSheetForStage,
        onDismissWeakConnectionBanner = viewModel::dismissWeakConnectionBanner,
        onRequestLocationPermission = {
            permissionLauncher.launch(LocationPermissionHelper.locationPermissions)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTripScreen(
    uiState: LiveTripUiState,
    onBack: () -> Unit,
    onStartTrip: () -> Unit,
    onStopTrip: () -> Unit,
    onNextStage: () -> Unit,
    onCenterOnMe: () -> Unit,
    onFocusStage: (Long) -> Unit,
    onQuickCategory: (QuickCategory) -> Unit,
    onStageConfiguredSearch: () -> Unit,
    onPlaceSelected: (Long) -> Unit,
    onDeletePlace: (Long) -> Unit,
    onDismissArrivalSheet: () -> Unit,
    onToggleArrivalSheet: () -> Unit,
    onConsumeMapCameraTarget: () -> Unit,
    onConsumeMapSearchEvent: (Long) -> Unit,
    onConsumeNavigationEvent: (Long) -> Unit,
    onConsumeArrivalSheetEvent: (Long) -> Unit,
    onDismissWeakConnectionBanner: () -> Unit,
    onRequestLocationPermission: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val snackbars = remember { SnackbarHostState() }
    val cameraState = rememberCameraPositionState()
    val scope = rememberCoroutineScope()
    val overlayTopPadding by animateDpAsState(
        targetValue = if (uiState.isArrivalSheetExpanded) 4.dp else 10.dp,
        animationSpec = spring(stiffness = 220f),
        label = "overlayTopPadding",
    )

    LaunchedEffect(uiState.pendingExternalMapSearch) {
        val request = uiState.pendingExternalMapSearch
        if (request != null) {
            val launched = MapsIntentHelper.openSearch(
                context = context,
                lat = request.lat,
                lng = request.lng,
                query = request.query,
            )
            if (!launched) {
                snackbars.showSnackbar("No compatible maps app found on this device.")
            }
            onConsumeMapSearchEvent(request.requestId)
        }
    }

    LaunchedEffect(uiState.pendingNavigationRequest) {
        val request = uiState.pendingNavigationRequest
        if (request != null) {
            val launched = MapsIntentHelper.openNavigation(
                context = context,
                lat = request.lat,
                lng = request.lng,
            )
            if (!launched) {
                snackbars.showSnackbar("No compatible maps app found on this device.")
            }
            onConsumeNavigationEvent(request.requestId)
        }
    }

    LaunchedEffect(uiState.mapCameraTarget) {
        val target = uiState.mapCameraTarget
        if (target != null) {
            cameraState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(target.lat, target.lng), target.zoom),
                900,
            )
            onConsumeMapCameraTarget()
        }
    }

    LaunchedEffect(uiState.showArrivalSheetForStageId) {
        val stageId = uiState.showArrivalSheetForStageId
        if (stageId != null) {
            snackbars.showSnackbar("Arrived near destination. Stage actions are ready.")
            onConsumeArrivalSheetEvent(stageId)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = uiState.trip?.name ?: "Live Trip",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbars) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            MapCanvas(
                uiState = uiState,
                cameraState = cameraState,
                modifier = Modifier.fillMaxSize(),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 12.dp, vertical = overlayTopPadding)
                    .clip(MaterialTheme.shapes.large)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.68f),
                            ),
                        ),
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!uiState.hasLocationPermission) {
                    StatusBanner(
                        text = uiState.locationPermissionRationale,
                        actionLabel = "Grant",
                        onAction = onRequestLocationPermission,
                    )
                }

                if (uiState.weakConnectionBannerVisible) {
                    StatusBanner(
                        text = "Weak connection detected. Location updates may be delayed.",
                        actionLabel = "Dismiss",
                        onAction = onDismissWeakConnectionBanner,
                    )
                }

                ActiveStageCard(
                    uiState = uiState,
                    onOpenActions = onToggleArrivalSheet,
                )
            }

            FloatingControlRail(
                isTripRunning = uiState.isTripRunning,
                onStartTrip = onStartTrip,
                onStopTrip = onStopTrip,
                onNextStage = onNextStage,
                onCenterOnMe = onCenterOnMe,
                onOpenNavigation = {
                    val to = uiState.tripState.activeStage?.toLocation
                    if (to != null) {
                        val launched = MapsIntentHelper.openNavigation(context, to.lat, to.lng)
                        if (!launched) {
                            scope.launch {
                                snackbars.showSnackbar("No compatible maps app found on this device.")
                            }
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp),
            )

            StageTimelineRow(
                stages = uiState.stages,
                activeStageId = uiState.activeStageId,
                selectedStageId = uiState.selectedStageId,
                onFocusStage = onFocusStage,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                            ),
                        ),
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
    }

    if (uiState.isArrivalSheetExpanded) {
        ArrivalActionsBottomSheet(
            uiState = uiState,
            onDismiss = onDismissArrivalSheet,
            onQuickCategory = onQuickCategory,
            onStageConfiguredSearch = onStageConfiguredSearch,
            onPlaceSelected = onPlaceSelected,
            onDeletePlace = onDeletePlace,
        )
    }
}

@Composable
private fun MapCanvas(
    uiState: LiveTripUiState,
    cameraState: CameraPositionState,
    modifier: Modifier = Modifier,
) {
    val properties = MapProperties(isMyLocationEnabled = uiState.hasLocationPermission)

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraState,
        properties = properties,
    ) {
        uiState.latestLocation?.let { location ->
            Marker(
                state = MarkerState(position = LatLng(location.lat, location.lng)),
                title = "You",
                snippet = "Current location",
            )
        }

        uiState.tripState.activeStage?.toLocation?.let { destination ->
            Marker(
                state = MarkerState(position = LatLng(destination.lat, destination.lng)),
                title = "Active destination",
                snippet = destination.label,
            )
        }

        uiState.tripState.nextStage?.toLocation?.let { destination ->
            Marker(
                state = MarkerState(position = LatLng(destination.lat, destination.lng)),
                title = "Next stage",
                snippet = destination.label,
            )
        }

        uiState.placeSuggestions.forEach { suggestion ->
            Marker(
                state = MarkerState(position = suggestion.toLatLng()),
                title = suggestion.name,
                snippet = suggestion.address ?: "Saved place",
            )
        }
    }
}

@Composable
private fun StatusBanner(
    text: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                modifier = Modifier.weight(1f),
                text = text,
                style = MaterialTheme.typography.bodyMedium,
            )
            AssistChip(onClick = onAction, label = { Text(actionLabel) })
        }
    }
}

@Composable
private fun ActiveStageCard(
    uiState: LiveTripUiState,
    onOpenActions: () -> Unit,
) {
    val activeStage = uiState.tripState.activeStage
    val cardScale by animateFloatAsState(
        targetValue = if (activeStage != null) 1f else 0.985f,
        animationSpec = spring(stiffness = 280f),
        label = "activeStageCardScale",
    )

    Card(
        modifier = Modifier.graphicsLayer {
            scaleX = cardScale
            scaleY = cardScale
        },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Active Stage",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            if (activeStage == null) {
                Text("Tap Start to begin this trip.", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(
                    text = stageTitle(activeStage),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Trigger: ${activeStage.trigger.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            uiState.tripState.nextStage?.let { next ->
                Text(
                    text = "Up next: ${stageTitle(next)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            AssistChip(
                onClick = onOpenActions,
                label = { Text("Open arrival actions") },
                leadingIcon = {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArrivalActionsBottomSheet(
    uiState: LiveTripUiState,
    onDismiss: () -> Unit,
    onQuickCategory: (QuickCategory) -> Unit,
    onStageConfiguredSearch: () -> Unit,
    onPlaceSelected: (Long) -> Unit,
    onDeletePlace: (Long) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        val activeStage = uiState.tripState.activeStage

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Arrival Actions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            if (activeStage != null) {
                Text(
                    text = stageTitle(activeStage),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AssistChip(
                    onClick = onStageConfiguredSearch,
                    label = { Text("Search stage-configured places") },
                )
            }

            Text(
                text = "Quick categories",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 4.dp),
            ) {
                items(
                    listOf(
                        QuickCategory.HOTELS,
                        QuickCategory.RESTAURANTS,
                        QuickCategory.PETROL_PUMPS,
                        QuickCategory.CAFES,
                        QuickCategory.CUSTOM,
                    )
                ) { category ->
                    AssistChip(
                        onClick = { onQuickCategory(category) },
                        label = { Text(category.name.replace('_', ' ')) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                    )
                }
            }

            Text(
                text = "Saved places",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )

            if (uiState.placeSuggestions.isEmpty()) {
                Text(
                    text = "No saved places yet for this stage.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                SavedPlacesList(
                    places = uiState.placeSuggestions,
                    onSelect = onPlaceSelected,
                    onDelete = onDeletePlace,
                )
            }
        }
    }
}

@Composable
private fun SavedPlacesList(
    places: List<PlaceSuggestion>,
    onSelect: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 260.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(places, key = { it.id }) { place ->
            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 1.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = place.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = place.address ?: "Lat ${place.lat}, Lng ${place.lng}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = { onSelect(place.id) }) {
                        Icon(Icons.Default.Navigation, contentDescription = "Navigate")
                    }
                    IconButton(onClick = { onDelete(place.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingControlRail(
    isTripRunning: Boolean,
    onStartTrip: () -> Unit,
    onStopTrip: () -> Unit,
    onNextStage: () -> Unit,
    onCenterOnMe: () -> Unit,
    onOpenNavigation: () -> Unit,
    modifier: Modifier = Modifier,
    railPadding: Dp = 10.dp,
    fabModifier: Modifier = Modifier,
    isEmphasized: Boolean = true,
) {
    val spacing by animateDpAsState(
        targetValue = if (isEmphasized) 12.dp else 8.dp,
        animationSpec = tween(durationMillis = 250),
        label = "controlRailSpacing",
    )

    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
            .padding(railPadding),
        verticalArrangement = Arrangement.spacedBy(spacing),
        horizontalAlignment = Alignment.End,
    ) {
        FloatingActionButton(onClick = onCenterOnMe, modifier = fabModifier) {
            Icon(Icons.Default.MyLocation, contentDescription = "Center on my location")
        }

        FloatingActionButton(onClick = onOpenNavigation, modifier = fabModifier) {
            Icon(Icons.Default.Navigation, contentDescription = "Open navigation")
        }

        FloatingActionButton(onClick = onNextStage, modifier = fabModifier) {
            Icon(Icons.Default.SkipNext, contentDescription = "Next stage")
        }

        FloatingActionButton(onClick = if (isTripRunning) onStopTrip else onStartTrip, modifier = fabModifier) {
            Icon(
                imageVector = if (isTripRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = if (isTripRunning) "Stop trip" else "Start trip",
            )
        }
    }
}

@Composable
private fun FloatingControlRail(
    isTripRunning: Boolean,
    onStartTrip: () -> Unit,
    onStopTrip: () -> Unit,
    onNextStage: () -> Unit,
    onCenterOnMe: () -> Unit,
    onOpenNavigation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingControlRail(
        isTripRunning = isTripRunning,
        onStartTrip = onStartTrip,
        onStopTrip = onStopTrip,
        onNextStage = onNextStage,
        onCenterOnMe = onCenterOnMe,
        onOpenNavigation = onOpenNavigation,
        modifier = modifier,
        railPadding = 10.dp,
        fabModifier = Modifier
            .clip(CircleShape)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                shape = CircleShape,
            ),
    )
}

@Composable
private fun StageTimelineRow(
    stages: List<TripStage>,
    activeStageId: Long?,
    selectedStageId: Long?,
    onFocusStage: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (stages.isEmpty()) {
        return
    }

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
    ) {
        items(stages, key = { it.id }) { stage ->
            val isActive = stage.id == activeStageId
            val isSelected = stage.id == selectedStageId
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.03f else 1f,
                animationSpec = spring(stiffness = 320f),
                label = "stagePillScale",
            )
            val containerColor = when {
                isActive -> MaterialTheme.colorScheme.primaryContainer
                stage.status == StageStatus.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
            val contentColor = when {
                isActive -> MaterialTheme.colorScheme.onPrimaryContainer
                stage.status == StageStatus.COMPLETED -> MaterialTheme.colorScheme.onTertiaryContainer
                else -> MaterialTheme.colorScheme.onSurface
            }

            AssistChip(
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
                onClick = { onFocusStage(stage.id) },
                label = {
                    Text(
                        text = stageTitle(stage),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = containerColor,
                    labelColor = contentColor,
                ),
            )
        }
    }
}

private fun stageTitle(stage: TripStage): String {
    val destination = stage.toLocation?.label?.takeIf { it.isNotBlank() } ?: "Stage ${stage.orderIndex + 1}"
    return "${stage.type.name} • $destination"
}

private fun PlaceSuggestion.toLatLng(): LatLng {
    return LatLng(lat, lng)
}
