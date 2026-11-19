@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui.editor.worldmap

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.ConnectionType
import de.egril.defender.editor.EditorStorage
import de.egril.defender.editor.WorldMapData
import de.egril.defender.editor.WorldMapLocationData
import de.egril.defender.editor.WorldMapPathData
import de.egril.defender.editor.WorldMapPoint
import de.egril.defender.ui.icon.CheckmarkIcon
import de.egril.defender.ui.settings.AppSettings
import defender_of_egril.composeapp.generated.resources.*
import defender_of_egril.composeapp.generated.resources.Res

/**
 * World Map Position Editor content - allows placing locations on the world map visually.
 * Uses the new WorldMapData model with locations and curved paths.
 */
@Composable
fun WorldMapPositionEditorContent() {
    var worldMapData by remember { mutableStateOf(EditorStorage.getWorldMapData()) }
    var allLevels by remember { mutableStateOf(EditorStorage.getAllLevels()) }
    var selectedLocationId by remember { mutableStateOf<String?>(null) }
    var hoveredPosition by remember { mutableStateOf<WorldMapPoint?>(null) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var showAddLocationDialog by remember { mutableStateOf(false) }
    var showEditPathDialog by remember { mutableStateOf<WorldMapPathData?>(null) }
    var showAddConnectionDialog by remember { mutableStateOf(false) }
    var showEditLocationDialog by remember { mutableStateOf<WorldMapLocationData?>(null) }

    // Connection creation mode: when active, clicking locations will create a connection
    var connectionCreationMode by remember { mutableStateOf(false) }
    var connectionFromLocation by remember { mutableStateOf<String?>(null) }

    // Waypoint edit mode: when active, allows adding/dragging waypoints. Otherwise, clicking selects connections
    var waypointEditMode by remember { mutableStateOf(true) } // Default to waypoint edit mode

    // Waypoint interaction state - store path IDs instead of path object for reliable updates
    var selectedPathIds by remember { mutableStateOf<Pair<String, String>?>(null) } // (fromLocationId, toLocationId)
    var draggingWaypointIndex by remember { mutableStateOf<Int?>(null) }
    var hoveredPathId by remember { mutableStateOf<Pair<String, String>?>(null) }

    val isDarkMode = AppSettings.isDarkMode.value

    // Previous saved version - set when the user saves, to allow resetting
    var previousWorldMapData by remember { mutableStateOf<WorldMapData?>(null) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    // Reload data
    LaunchedEffect(Unit) {
        worldMapData = EditorStorage.getWorldMapData()
        allLevels = EditorStorage.getAllLevels()
    }

    var worldmapSavedSuccess by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(8.dp),
    ) {
        // Title row with Save and Reset buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.world_map_positions),
                style = MaterialTheme.typography.titleMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (previousWorldMapData != null) {
                    OutlinedButton(
                        onClick = { showResetConfirmDialog = true },
                    ) {
                        Text(stringResource(Res.string.reset_worldmap))
                    }
                }
                Button(
                    onClick = {
                        previousWorldMapData = EditorStorage.getWorldMapData()
                        EditorStorage.saveWorldMapData(worldMapData)
                        worldmapSavedSuccess = true
                    },
                ) {
                    Text(stringResource(Res.string.save_worldmap))
                }
            }
        }

        // Save success message
        if (worldmapSavedSuccess) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.worldmap_saved_success),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    TextButton(onClick = { worldmapSavedSuccess = false }) {
                        CheckmarkIcon(size = 16.dp, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Instructions
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Text(
                text = stringResource(Res.string.world_map_position_instructions),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(8.dp),
            )
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Left side: World Map with clickable positions
            Card(
                modifier =
                    Modifier
                        .weight(2f)
                        .fillMaxHeight(),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .onSizeChanged { containerSize = it },
                ) {
                    WorldMapCanvas(
                        worldMapData = worldMapData,
                        allLevels = allLevels,
                        selectedLocationId = selectedLocationId,
                        hoveredPosition = hoveredPosition,
                        containerSize = containerSize,
                        isDarkMode = isDarkMode,
                        selectedPathIds = selectedPathIds,
                        draggingWaypointIndex = draggingWaypointIndex,
                        hoveredPathId = hoveredPathId,
                        connectionCreationMode = connectionCreationMode,
                        connectionFromLocation = connectionFromLocation,
                        waypointEditMode = waypointEditMode,
                        onLocationClick = { locationId ->
                            // Handle location click based on mode
                            if (connectionCreationMode) {
                                // Connection creation mode
                                if (connectionFromLocation == null) {
                                    // First location selected
                                    connectionFromLocation = locationId
                                } else if (connectionFromLocation != locationId) {
                                    // Second location selected - create connection
                                    val newPath =
                                        WorldMapPathData(
                                            fromLocationId = connectionFromLocation!!,
                                            toLocationId = locationId,
                                            controlPoints = emptyList(),
                                            type = ConnectionType.ROAD,
                                        )
                                    worldMapData = worldMapData.withUpdatedPath(newPath)
                                    connectionCreationMode = false
                                    connectionFromLocation = null
                                } else {
                                    // Same location clicked - cancel
                                    connectionFromLocation = null
                                }
                            } else {
                                // Edit mode - open edit dialog for location
                                val location = worldMapData.locations.find { it.id == locationId }
                                if (location != null) {
                                    showEditLocationDialog = location
                                }
                            }
                        },
                        onPositionClick = { point ->
                            // Update position for selected location
                            if (selectedLocationId != null) {
                                val location = worldMapData.locations.find { it.id == selectedLocationId }
                                if (location != null) {
                                    val updatedLocation = location.copy(position = point)
                                    worldMapData = worldMapData.withUpdatedLocation(updatedLocation)
                                }
                            }
                        },
                        onPathClick = { path, addWaypoint ->
                            // Double-check current waypoint edit mode to prevent stale closure issues
                            if (addWaypoint && waypointEditMode) {
                                // Save the path with the new waypoint
                                worldMapData = worldMapData.withUpdatedPath(path)
                                selectedPathIds = path.fromLocationId to path.toLocationId
                            } else if (!addWaypoint || !waypointEditMode) {
                                // Select the connection and open edit dialog
                                showEditPathDialog = path
                            }
                        },
                        onWaypointDragStart = { fromId, toId, waypointIndex ->
                            selectedPathIds = fromId to toId
                            draggingWaypointIndex = waypointIndex
                        },
                        onWaypointDrag = { fromId, toId, waypointIndex, newPosition ->
                            if (selectedPathIds == (fromId to toId) && draggingWaypointIndex == waypointIndex) {
                                // Find the path in worldMapData and update it
                                val pathIndex =
                                    worldMapData.paths.indexOfFirst {
                                        it.fromLocationId == fromId &&
                                            it.toLocationId == toId
                                    }
                                if (pathIndex >= 0) {
                                    val currentPath = worldMapData.paths[pathIndex]
                                    val updatedControlPoints = currentPath.controlPoints.toMutableList()
                                    if (waypointIndex < updatedControlPoints.size) {
                                        updatedControlPoints[waypointIndex] = newPosition
                                        val updatedPath = currentPath.copy(controlPoints = updatedControlPoints)

                                        // Update worldMapData with the new path
                                        val updatedPaths = worldMapData.paths.toMutableList()
                                        updatedPaths[pathIndex] = updatedPath
                                        worldMapData = worldMapData.copy(paths = updatedPaths)
                                    }
                                }
                            }
                        },
                        onWaypointDragEnd = {
                            // Waypoint drag updates are already applied in-memory via onWaypointDrag
                            draggingWaypointIndex = null
                            selectedPathIds = null
                        },
                        onPathHover = { pathId ->
                            hoveredPathId = pathId
                        },
                        onHoverChange = { point ->
                            hoveredPosition = point
                        },
                    )

                    // Hover tooltip
                    if (hoveredPosition != null) {
                        Card(
                            modifier =
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                                ),
                        ) {
                            Text(
                                text = "Position: ${hoveredPosition!!.x}, ${hoveredPosition!!.y}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(8.dp),
                            )
                        }
                    }

                    // Connection mode indicator
                    if (connectionCreationMode) {
                        Card(
                            modifier =
                                Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f),
                                ),
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "Connection Mode Active",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondary,
                                )
                                if (connectionFromLocation != null) {
                                    val fromLoc = worldMapData.locations.find { it.id == connectionFromLocation }
                                    Text(
                                        text = "From: ${fromLoc?.name ?: connectionFromLocation}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondary,
                                    )
                                    Text(
                                        text = "Click destination location",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondary,
                                    )
                                } else {
                                    Text(
                                        text = "Click source location",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondary,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Right side: Locations and Paths lists
            Card(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                ) {
                    // Add Location button
                    Button(
                        onClick = { showAddLocationDialog = true },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    ) {
                        Text(stringResource(Res.string.add_location))
                    }

                    // Connection creation mode button
                    Button(
                        onClick = {
                            connectionCreationMode = !connectionCreationMode
                            if (!connectionCreationMode) {
                                connectionFromLocation = null
                            }
                        },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    if (connectionCreationMode) {
                                        MaterialTheme.colorScheme.secondary
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                            ),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    ) {
                        Text(if (connectionCreationMode) "Cancel Connection Mode" else "Create Connection (Click Mode)")
                    }

                    // Waypoint edit mode toggle button
                    Button(
                        onClick = {
                            waypointEditMode = !waypointEditMode
                            // Clear any active waypoint drag state when switching modes
                            if (!waypointEditMode) {
                                selectedPathIds = null
                                draggingWaypointIndex = null
                            }
                        },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    if (waypointEditMode) {
                                        MaterialTheme.colorScheme.tertiary
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                            ),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    ) {
                        Text(if (waypointEditMode) "Waypoint Edit Mode: ON" else "Waypoint Edit Mode: OFF")
                    }

                    // Auto-connect based on dependencies button
                    Button(
                        onClick = {
                            // Auto-create connections based on level dependencies
                            val newPaths = mutableListOf<WorldMapPathData>()
                            for (toLocation in worldMapData.locations) {
                                for (toLevelId in toLocation.levelIds) {
                                    val toLevel = allLevels.find { it.id == toLevelId }
                                    if (toLevel != null && toLevel.prerequisites.isNotEmpty()) {
                                        // Find which location has any of the prerequisite levels
                                        for (fromLocation in worldMapData.locations) {
                                            if (fromLocation.id != toLocation.id) {
                                                val hasPrereq = fromLocation.levelIds.any { it in toLevel.prerequisites }
                                                if (hasPrereq) {
                                                    // Check if connection already exists
                                                    val exists =
                                                        worldMapData.paths.any {
                                                            it.fromLocationId == fromLocation.id && it.toLocationId == toLocation.id
                                                        }
                                                    if (!exists) {
                                                        newPaths.add(
                                                            WorldMapPathData(
                                                                fromLocationId = fromLocation.id,
                                                                toLocationId = toLocation.id,
                                                                controlPoints = emptyList(),
                                                                type = ConnectionType.ROAD,
                                                            ),
                                                        )
                                                    }
                                                    break
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            // Add new paths to worldMapData in memory
                            var updatedData = worldMapData
                            for (path in newPaths) {
                                updatedData = updatedData.withUpdatedPath(path)
                            }
                            worldMapData = updatedData
                        },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    ) {
                        Text(stringResource(Res.string.auto_connect_by_dependencies))
                    }

                    Text(
                        text = stringResource(Res.string.locations),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(worldMapData.locations) { location ->
                            LocationListItem(
                                location = location,
                                worldMapData = worldMapData,
                                allLevels = allLevels,
                                isSelected = location.id == selectedLocationId,
                                onClick = {
                                    selectedLocationId = if (selectedLocationId == location.id) null else location.id
                                },
                                onEdit = {
                                    showEditLocationDialog = location
                                },
                                onDelete = {
                                    worldMapData =
                                        worldMapData.copy(
                                            locations = worldMapData.locations.filter { it.id != location.id },
                                            paths =
                                                worldMapData.paths.filter {
                                                    it.fromLocationId != location.id && it.toLocationId != location.id
                                                },
                                        )
                                    if (selectedLocationId == location.id) {
                                        selectedLocationId = null
                                    }
                                },
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Connections (${worldMapData.paths.size})",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Button(
                            onClick = { showAddConnectionDialog = true },
                            modifier = Modifier.padding(start = 8.dp),
                        ) {
                            Text("+")
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f).padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(worldMapData.paths) { path ->
                            PathListItem(
                                path = path,
                                locations = worldMapData.locations,
                                allLevels = allLevels,
                                onClick = { showEditPathDialog = path },
                                onDelete = {
                                    worldMapData =
                                        worldMapData.copy(
                                            paths =
                                                worldMapData.paths.filter {
                                                    !(it.fromLocationId == path.fromLocationId && it.toLocationId == path.toLocationId)
                                                },
                                        )
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    // Reset to Previous Version Confirmation Dialog
    if (showResetConfirmDialog && previousWorldMapData != null) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text(stringResource(Res.string.reset_worldmap_confirm_title)) },
            text = { Text(stringResource(Res.string.reset_worldmap_confirm_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        val previous = previousWorldMapData!!
                        EditorStorage.saveWorldMapData(previous)
                        worldMapData = previous
                        previousWorldMapData = null
                        showResetConfirmDialog = false
                        worldmapSavedSuccess = false
                    },
                ) {
                    Text(stringResource(Res.string.yes))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetConfirmDialog = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }

    // Add Location Dialog
    if (showAddLocationDialog) {
        AddLocationDialog(
            allLevels = allLevels,
            existingLocations = worldMapData.locations,
            onDismiss = { showAddLocationDialog = false },
            onConfirm = { newLocation ->
                worldMapData = worldMapData.withUpdatedLocation(newLocation)
                showAddLocationDialog = false
            },
        )
    }

    // Edit Path Dialog
    if (showEditPathDialog != null) {
        EditPathDialog(
            path = showEditPathDialog!!,
            onDismiss = { showEditPathDialog = null },
            onConfirm = { updatedPath ->
                worldMapData = worldMapData.withUpdatedPath(updatedPath)
                showEditPathDialog = null
            },
        )
    }

    // Add Connection Dialog
    if (showAddConnectionDialog) {
        AddConnectionDialog(
            locations = worldMapData.locations,
            onDismiss = { showAddConnectionDialog = false },
            onConfirm = { newPath ->
                worldMapData = worldMapData.withUpdatedPath(newPath)
                showAddConnectionDialog = false
            },
        )
    }

    // Edit Location Dialog
    if (showEditLocationDialog != null) {
        EditLocationDialog(
            location = showEditLocationDialog!!,
            allLevels = allLevels,
            existingLocations = worldMapData.locations,
            onDismiss = { showEditLocationDialog = null },
            onConfirm = { updatedLocation ->
                worldMapData = worldMapData.withUpdatedLocation(updatedLocation)
                showEditLocationDialog = null
            },
        )
    }
}
