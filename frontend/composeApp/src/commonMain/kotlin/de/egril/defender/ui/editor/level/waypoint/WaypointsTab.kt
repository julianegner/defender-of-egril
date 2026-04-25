package de.egril.defender.ui.editor.level.waypoint

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.EditorLevel
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.EditorWaypoint
import de.egril.defender.editor.TileType
import de.egril.defender.editor.WaypointValidationResult
import de.egril.defender.model.Position
import de.egril.defender.ui.editor.ConfirmationDialog
import de.egril.defender.ui.icon.RightArrowIcon
import de.egril.defender.ui.icon.WarningIcon
import de.egril.defender.ui.settings.AppSettings
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.circular_dependency_detected
import defender_of_egril.composeapp.generated.resources.confirm_remove_all_waypoints
import defender_of_egril.composeapp.generated.resources.no_waypoints_configured
import defender_of_egril.composeapp.generated.resources.remove_all_waypoints
import defender_of_egril.composeapp.generated.resources.unconnected_waypoint_warning
import defender_of_egril.composeapp.generated.resources.waypoint_list_view
import defender_of_egril.composeapp.generated.resources.waypoint_tree_view
import defender_of_egril.composeapp.generated.resources.waypoint_validation_error
import defender_of_egril.composeapp.generated.resources.waypoint_validation_success
import defender_of_egril.composeapp.generated.resources.waypoints_description
import de.egril.defender.config.LogConfig

/**
 * Tab 4: Waypoints Configuration
 * Redesigned to include map directly on the page with click-based waypoint creation
 */
@Composable
fun WaypointsTab(
    waypoints: List<EditorWaypoint>,
    onWaypointsChange: (List<EditorWaypoint>) -> Unit,
    map: EditorMap?,
    isValid: Boolean
) {
    var showRemoveAllDialog by remember { mutableStateOf(false) }
    var showTreeView by remember { mutableStateOf(true) }  // Default to tree view
    var selectedSource by remember { mutableStateOf<Position?>(null) }
    var hoveredPosition by remember { mutableStateOf<Position?>(null) }
    
    // Local state to track current waypoints for immediate updates within this component
    // This ensures onClick handler sees updates immediately, not after recomposition
    var localWaypoints by remember(waypoints) { mutableStateOf(waypoints.toList()) }
    
    val isDarkMode = AppSettings.isDarkMode.value

    // Get path tiles, spawn points, and targets from the map
    val pathTiles = remember(map) { map?.getPathCells()?.toList() ?: emptyList() }
    val spawnPoints = remember(map) { map?.getSpawnPoints() ?: emptyList() }
    val targets = remember(map) { map?.getTargets() ?: emptyList() }
    
    // Extract existing waypoint positions from the waypoints list
    val existingWaypointPositions = remember(localWaypoints) {
        localWaypoints.map { it.position }.toSet().toList()
    }

    // Perform detailed validation
    val validationResult = remember(localWaypoints, targets, spawnPoints) {
        if (targets.isNotEmpty()) {
            // Create a temporary EditorLevel to use validation
            val tempLevel = EditorLevel(
                id = "temp",
                mapId = "",
                title = "",
                startCoins = 0,
                startHealthPoints = 0,
                enemySpawns = emptyList(),
                availableTowers = emptySet(),
                waypoints = localWaypoints
            )
            tempLevel.validateWaypointsDetailed(targets, spawnPoints)
        } else {
            WaypointValidationResult(isValid = true)
        }
    }
    
    // Valid sources: spawn points + existing waypoint positions
    val validSources = remember(spawnPoints, existingWaypointPositions) {
        (spawnPoints + existingWaypointPositions).distinct()
    }
    
    // Valid targets: path tiles + existing waypoint positions + targets
    val validTargets = remember(pathTiles, existingWaypointPositions, targets) {
        (pathTiles + existingWaypointPositions + targets).distinct()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Description
        item {
            Text(
                text = stringResource(Res.string.waypoints_description),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Minimap section with legend
        if (map != null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Minimap
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(400.dp)  // Larger map as requested
                            .background(Color(0xCC000000))
                            .border(2.dp, Color.White)
                            .padding(4.dp)
                    ) {
                        WaypointMinimap(
                            map = map,
                            selectedSource = selectedSource,
                            selectedTarget = null,  // No separate target selection in new UX
                            existingWaypoints = localWaypoints,
                            onTileClick = { clickedPos ->
                                // New click-based UX: 
                                // 1. Click a spawn point or existing waypoint to select it as source
                                // 2. Click any path tile, waypoint, or target to create connection
                                val isValidSource = validSources.contains(clickedPos)
                                val isValidTarget = validTargets.contains(clickedPos)
                                
                                when {
                                    // No source selected: select this as source if valid
                                    selectedSource == null && isValidSource -> {
                                        selectedSource = clickedPos
                                    }
                                    // Source selected: create connection to target
                                    selectedSource != null && isValidTarget && selectedSource != clickedPos -> {
                                        // Create waypoint from source to target
                                        val newWaypoint = EditorWaypoint(selectedSource!!, clickedPos)
                                        if (LogConfig.ENABLE_UI_LOGGING) {
                                        println("=== CREATING WAYPOINT ===")
                                        }
                                        if (LogConfig.ENABLE_UI_LOGGING) {
                                        println("Source: $selectedSource, Target: $clickedPos")
                                        }
                                        if (LogConfig.ENABLE_UI_LOGGING) {
                                        println("Current waypoints before: ${localWaypoints.map { "(${it.position.x},${it.position.y})->(${it.nextTargetPosition.x},${it.nextTargetPosition.y})" }}")
                                        }
                                        
                                        // Create a NEW mutableList from LOCAL waypoints to accumulate changes
                                        val newWaypoints = localWaypoints.toMutableList()
                                        
                                        // Check if waypoint already exists at this position and replace it
                                        val existingIndex = newWaypoints.indexOfFirst { it.position == selectedSource }
                                        if (existingIndex >= 0) {
                                            println("Replacing existing waypoint at index $existingIndex")
                                            newWaypoints[existingIndex] = newWaypoint
                                        } else {
                                            if (LogConfig.ENABLE_UI_LOGGING) {
                                            println("Adding new waypoint (no existing waypoint at position $selectedSource)")
                                            }
                                            newWaypoints.add(newWaypoint)
                                        }
                                        
                                        if (LogConfig.ENABLE_UI_LOGGING) {
                                        println("New waypoints after: ${newWaypoints.map { "(${it.position.x},${it.position.y})->(${it.nextTargetPosition.x},${it.nextTargetPosition.y})" }}")
                                        }
                                        
                                        // Update local state immediately so next click sees the change
                                        localWaypoints = newWaypoints.toList()
                                        // Also notify parent
                                        onWaypointsChange(newWaypoints)
                                        // Set the clicked position as the new source for chaining
                                        // Only if it's not a target (targets are endpoints, not sources)
                                        selectedSource = if (targets.contains(clickedPos)) null else clickedPos
                                    }
                                    // Clicked on source again: deselect
                                    selectedSource != null && clickedPos == selectedSource -> {
                                        selectedSource = null
                                    }
                                }
                            },
                            onHoverChange = { pos ->
                                hoveredPosition = pos
                            }
                        )
                    }
                    
                    // Legend and hover info
                    Column(
                        modifier = Modifier.width(130.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Legend title
                        Text(
                            text = "Legend",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        // Legend items
                        LegendItem("Spawn", if (isDarkMode) Color(0xFF8B0000) else Color(0xFFDC143C))
                        LegendItem("Target", if (isDarkMode) Color(0xFF1E3A8A) else Color(0xFF4169E1))
                        LegendItem("Waypoint", if (isDarkMode) Color(0xFF9A7B00) else Color(0xFFFFD700))
                        LegendItem("Selected", Color(0xFF40E0D0))
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Hover position display
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x44000000))
                                .border(1.dp, MaterialTheme.colorScheme.outline)
                                .padding(6.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "Hover:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                if (hoveredPosition != null) {
                                    val pos = hoveredPosition!!
                                    val tileType = map.tiles.getOrElse("${pos.x},${pos.y}") { TileType.NO_PLAY }
                                    val isDefinedWaypoint = localWaypoints.any { it.position == pos }
                                    val typeLabel = when {
                                        isDefinedWaypoint -> "Waypoint"
                                        tileType == TileType.SPAWN_POINT -> "Spawn"
                                        tileType == TileType.TARGET -> "Target"
                                        tileType == TileType.PATH -> "Path"
                                        else -> "Unknown"
                                    }
                                    Text(
                                        text = typeLabel,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "(${pos.x}, ${pos.y})",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                } else {
                                    Text(
                                        text = "—",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Instructions
                        Text(
                            text = "Click a spawn/waypoint, then click on the map to connect",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Validation status with detailed messages
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (validationResult.isValid) {
                        Text(
                            text = stringResource(Res.string.waypoint_validation_success),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Green
                        )
                    } else {
                        Text(
                            text = stringResource(Res.string.waypoint_validation_error),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Red
                        )
                    }
                }

                // Show specific validation issues
                if (validationResult.circularDependencies.isNotEmpty()) {
                    Text(
                        text = stringResource(Res.string.circular_dependency_detected),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Red,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (validationResult.unconnectedWaypoints.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        WarningIcon(size = 14.dp)
                        Text(
                            text = "${stringResource(Res.string.unconnected_waypoint_warning)}: ${validationResult.unconnectedWaypoints.size} waypoint(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Action buttons row (only Remove All button remains)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showRemoveAllDialog = true },
                    enabled = localWaypoints.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (localWaypoints.isNotEmpty())
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(Res.string.remove_all_waypoints))
                }
            }
        }

        // View toggle button
        if (localWaypoints.isNotEmpty()) {
            item {
                Button(
                    onClick = { showTreeView = !showTreeView },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (showTreeView)
                            stringResource(Res.string.waypoint_list_view)
                        else
                            stringResource(Res.string.waypoint_tree_view)
                    )
                }
            }
        }

        // Display spawn points and waypoints in tree view or list view
        if (showTreeView) {
            // Tree view - shows hierarchical chain structure
            item {
                WaypointTreeView(
                    validationResult = validationResult,
                    map = map,
                    onDeleteConnection = { position ->
                        val newWaypoints = localWaypoints.toMutableList().apply {
                            removeAll { it.position == position }
                        }
                        localWaypoints = newWaypoints.toList()  // Update local state
                        onWaypointsChange(newWaypoints)
                    },
                    onConnectWaypoint = {
                        // No action needed - users can click on the map directly
                    }
                )
            }
        } else {
            // List view - shows spawn points and waypoints
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Source",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    RightArrowIcon(
                        size = 14.dp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Text(
                        text = "Target",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(40.dp)) // Space for delete button
                }
            }
            
            // Show spawn points
            items(spawnPoints) { spawnPoint ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Spawn (${spawnPoint.x}, ${spawnPoint.y})",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    RightArrowIcon(
                        size = 14.dp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    val waypointFromSpawn = localWaypoints.firstOrNull { it.position == spawnPoint }
                    if (waypointFromSpawn != null) {
                        Text(
                            text = "(${waypointFromSpawn.nextTargetPosition.x}, ${waypointFromSpawn.nextTargetPosition.y})",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Text(
                            text = "—",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.width(40.dp))
                }
            }

            // Show waypoints
            items(localWaypoints) { waypoint ->
                // Skip waypoints that are at spawn points (already shown above)
                if (!spawnPoints.contains(waypoint.position)) {
                    WaypointConnectionCard(
                        waypoint = waypoint,
                        spawnPoints = spawnPoints,
                        validWaypointPositions = existingWaypointPositions,
                        targets = targets,
                        isInCircular = validationResult.circularDependencies.contains(waypoint.position) ||
                                validationResult.circularDependencies.contains(waypoint.nextTargetPosition),
                        isUnconnected = validationResult.unconnectedWaypoints.contains(waypoint.position) ||
                                validationResult.unconnectedWaypoints.contains(waypoint.nextTargetPosition),
                        onDelete = {
                            val newWaypoints = localWaypoints.toMutableList().apply { remove(waypoint) }
                            localWaypoints = newWaypoints.toList()  // Update local state
                            onWaypointsChange(newWaypoints)
                        }
                    )
                }
            }
            
            // Show message if no waypoints at all
            if (localWaypoints.isEmpty()) {
                item {
                    Text(
                        text = stringResource(Res.string.no_waypoints_configured),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }

    // Remove all confirmation dialog
    if (showRemoveAllDialog) {
        ConfirmationDialog(
            title = stringResource(Res.string.remove_all_waypoints),
            message = stringResource(Res.string.confirm_remove_all_waypoints),
            onDismiss = { showRemoveAllDialog = false },
            onConfirm = {
                localWaypoints = emptyList()  // Update local state immediately
                onWaypointsChange(emptyList())
                selectedSource = null  // Clear selection when removing all waypoints
                showRemoveAllDialog = false
            }
        )
    }
}

/**
 * Helper composable for legend items
 */
@Composable
private fun LegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(12.dp)
                .height(12.dp)
                .background(color)
                .border(1.dp, Color.White)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontSize = MaterialTheme.typography.bodySmall.fontSize * 0.9
        )
    }
}
