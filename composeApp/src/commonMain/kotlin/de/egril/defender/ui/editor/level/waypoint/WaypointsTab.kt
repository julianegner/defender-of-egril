package de.egril.defender.ui.editor.level.waypoint

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.EditorLevel
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.EditorWaypoint
import de.egril.defender.editor.WaypointValidationResult
import de.egril.defender.model.Position
import de.egril.defender.ui.editor.ConfirmationDialog
import de.egril.defender.ui.icon.MapIcon
import de.egril.defender.ui.icon.WarningIcon
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.add_waypoint
import defender_of_egril.composeapp.generated.resources.available_waypoint_tiles
import defender_of_egril.composeapp.generated.resources.circular_dependency_detected
import defender_of_egril.composeapp.generated.resources.confirm_remove_all_waypoints
import defender_of_egril.composeapp.generated.resources.no_waypoints_configured
import defender_of_egril.composeapp.generated.resources.remove_all_waypoints
import defender_of_egril.composeapp.generated.resources.select_on_map
import defender_of_egril.composeapp.generated.resources.unconnected_waypoint_warning
import defender_of_egril.composeapp.generated.resources.waypoint_list_view
import defender_of_egril.composeapp.generated.resources.waypoint_source
import defender_of_egril.composeapp.generated.resources.waypoint_target
import defender_of_egril.composeapp.generated.resources.waypoint_tree_view
import defender_of_egril.composeapp.generated.resources.waypoint_validation_error
import defender_of_egril.composeapp.generated.resources.waypoint_validation_success
import defender_of_egril.composeapp.generated.resources.waypoints_description

/**
 * Tab 4: Waypoints Configuration
 */
@Composable
fun WaypointsTab(
    waypoints: MutableList<EditorWaypoint>,
    onWaypointsChange: (MutableList<EditorWaypoint>) -> Unit,
    map: EditorMap?,
    isValid: Boolean
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showQuickAddDialog by remember { mutableStateOf(false) }
    var preselectedSource by remember { mutableStateOf<Position?>(null) }
    var showRemoveAllDialog by remember { mutableStateOf(false) }
    var showTreeView by remember { mutableStateOf(true) }  // Default to tree view

    // Get waypoint tiles, spawn points, and target from the map
    val waypointTiles = remember(map) { map?.getWaypoints() ?: emptyList() }
    val spawnPoints = remember(map) { map?.getSpawnPoints() ?: emptyList() }
    val target = remember(map) { map?.getTarget() }

    // Perform detailed validation
    val validationResult = remember(waypoints, target, spawnPoints) {
        if (target != null) {
            // Create a temporary EditorLevel to use validation
            val tempLevel = EditorLevel(
                id = "temp",
                mapId = "",
                title = "",
                startCoins = 0,
                startHealthPoints = 0,
                enemySpawns = emptyList(),
                availableTowers = emptySet(),
                waypoints = waypoints.toList()
            )
            tempLevel.validateWaypointsDetailed(target, spawnPoints)
        } else {
            WaypointValidationResult(isValid = true)
        }
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

        // Available waypoint tiles info
        item {
            Text(
                text = stringResource(Res.string.available_waypoint_tiles).replace("%d", waypointTiles.size.toString()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

        // Action buttons row
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // First row: Add and Remove
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showAddDialog = true },
                        enabled = waypointTiles.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(Res.string.add_waypoint))
                    }

                    Button(
                        onClick = { showRemoveAllDialog = true },
                        enabled = waypoints.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (waypoints.isNotEmpty())
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(Res.string.remove_all_waypoints))
                    }
                }

                // Second row: Quick Add button
                Button(
                    onClick = { showQuickAddDialog = true },
                    enabled = waypointTiles.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MapIcon(size = 16.dp)
                        Text(stringResource(Res.string.select_on_map))
                    }
                }
            }
        }

        // View toggle button
        if (waypoints.isNotEmpty()) {
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

        // Display waypoints in tree view or list view
        if (waypoints.isEmpty()) {
            item {
                Text(
                    text = stringResource(Res.string.no_waypoints_configured),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        } else if (showTreeView) {
            // Tree view - shows hierarchical chain structure
            item {
                WaypointTreeView(
                    validationResult = validationResult,
                    map = map,
                    onDeleteConnection = { position ->
                        val newWaypoints = waypoints.toMutableList().apply {
                            removeAll { it.position == position }
                        }
                        onWaypointsChange(newWaypoints)
                    },
                    onConnectWaypoint = { position ->
                        preselectedSource = position
                        showQuickAddDialog = true
                    }
                )
            }
        } else {
            // List view - original simple list
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(Res.string.waypoint_source),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "→",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Text(
                        text = stringResource(Res.string.waypoint_target),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(40.dp)) // Space for delete button
                }
            }

            items(waypoints) { waypoint ->
                WaypointConnectionCard(
                    waypoint = waypoint,
                    spawnPoints = spawnPoints,
                    waypointTiles = waypointTiles,
                    target = target,
                    isInCircular = validationResult.circularDependencies.contains(waypoint.position) ||
                            validationResult.circularDependencies.contains(waypoint.nextTargetPosition),
                    isUnconnected = validationResult.unconnectedWaypoints.contains(waypoint.position) ||
                            validationResult.unconnectedWaypoints.contains(waypoint.nextTargetPosition),
                    onDelete = {
                        val newWaypoints = waypoints.toMutableList().apply { remove(waypoint) }
                        onWaypointsChange(newWaypoints)
                    }
                )
            }
        }
    }

    // Add waypoint dialog
    if (showAddDialog) {
        AddWaypointDialog(
            waypointTiles = waypointTiles,
            spawnPoints = spawnPoints,
            target = target,
            existingWaypoints = waypoints,
            onDismiss = { showAddDialog = false },
            onAdd = { source, targetPos ->
                val newWaypoint = EditorWaypoint(source, targetPos)
                val newWaypoints = waypoints.toMutableList().apply { add(newWaypoint) }
                onWaypointsChange(newWaypoints)
                showAddDialog = false
            }
        )
    }

    // Quick add waypoint dialog (map-based selection)
    if (showQuickAddDialog) {
        QuickAddWaypointDialog(
            map = map,
            existingWaypoints = waypoints,
            onDismiss = {
                showQuickAddDialog = false
                preselectedSource = null
            },
            onAdd = { source, targetPos ->
                val newWaypoint = EditorWaypoint(source, targetPos)
                val newWaypoints = waypoints.toMutableList().apply { add(newWaypoint) }
                onWaypointsChange(newWaypoints)
                showQuickAddDialog = false
                preselectedSource = null
            },
            preselectedSource = preselectedSource
        )
    }

    // Remove all confirmation dialog
    if (showRemoveAllDialog) {
        ConfirmationDialog(
            title = stringResource(Res.string.remove_all_waypoints),
            message = stringResource(Res.string.confirm_remove_all_waypoints),
            onDismiss = { showRemoveAllDialog = false },
            onConfirm = {
                onWaypointsChange(mutableListOf())
                showRemoveAllDialog = false
            }
        )
    }
}
