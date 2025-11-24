package de.egril.defender.ui.editor.level.waypoint

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.EditorWaypoint
import de.egril.defender.editor.TileType
import de.egril.defender.model.Position
import de.egril.defender.ui.icon.CheckmarkIcon
import de.egril.defender.ui.icon.Number1Icon
import de.egril.defender.ui.icon.Number2Icon
import de.egril.defender.ui.settings.AppSettings
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.add_waypoint
import defender_of_egril.composeapp.generated.resources.cancel
import defender_of_egril.composeapp.generated.resources.click_to_connect_waypoints
import defender_of_egril.composeapp.generated.resources.select_on_map
import defender_of_egril.composeapp.generated.resources.show_connected
import defender_of_egril.composeapp.generated.resources.waypoint_exists_error
import defender_of_egril.composeapp.generated.resources.waypoint_source
import defender_of_egril.composeapp.generated.resources.waypoint_target

/**
 * Quick add dialog that shows map positions for easier waypoint creation
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickAddWaypointDialog(
    map: EditorMap?,
    existingWaypoints: List<EditorWaypoint>,
    onDismiss: () -> Unit,
    onAdd: (Position, Position) -> Unit,
    preselectedSource: Position? = null
) {
    val waypointTiles = remember(map) { map?.getWaypoints() ?: emptyList() }
    val spawnPoints = remember(map) { map?.getSpawnPoints() ?: emptyList() }
    val targets = remember(map) { map?.getTargets() ?: emptyList() }

    var selectedSource by remember { mutableStateOf<Position?>(null) }
    selectedSource = preselectedSource
    var selectedTarget by remember { mutableStateOf<Position?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showConnectedSources by remember { mutableStateOf(false) }
    var hoveredPosition by remember { mutableStateOf<Position?>(null) }
    
    val isDarkMode = AppSettings.isDarkMode.value

    // Build set of positions that already have connections from them
    val connectedSources = remember(existingWaypoints) {
        existingWaypoints.map { it.position }.toSet()
    }

    val validSources = remember(spawnPoints, waypointTiles, showConnectedSources, connectedSources) {
        val allSources = (spawnPoints + waypointTiles).distinct()
        val filtered = if (showConnectedSources) {
            allSources
        } else {
            allSources.filter { it !in connectedSources }
        }
        filtered.sortedWith(compareBy({ it.y }, { it.x }))
    }

    val validTargets = remember(waypointTiles, targets) {
        (waypointTiles + targets).distinct().sortedWith(compareBy({ it.y }, { it.x }))
    }

    // Build map of positions that are already connected to targets
    val connectedToTarget = remember(existingWaypoints, targets) {
        val connected = mutableSetOf<Position>()
        val targetSet = targets.toSet()

        // Build a map for quick lookup
        val waypointMap = existingWaypoints.associateBy { it.position }

        // For each position, check if it eventually leads to a target
        fun leadsToTarget(pos: Position, visited: MutableSet<Position> = mutableSetOf()): Boolean {
            if (pos in targetSet) return true
            if (pos in visited) return false // Circular reference
            visited.add(pos)

            val waypoint = waypointMap[pos] ?: return false
            return leadsToTarget(waypoint.nextTargetPosition, visited)
        }

        for (pos in validTargets) {
            if (pos in targetSet || leadsToTarget(pos)) {
                connected.add(pos)
            }
        }

        connected
    }

    val waypointExistsErrorMsg = stringResource(Res.string.waypoint_exists_error)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(Res.string.select_on_map))
        },
        text = {
            Column(
                modifier = Modifier.Companion.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Minimap showing spawn points, waypoints, and targets
                if (map != null) {
                    Row(
                        modifier = Modifier.Companion.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Minimap
                        Box(
                            modifier = Modifier.Companion
                                .weight(1f)
                                .height(250.dp)
                                .background(Color(0xCC000000))
                                .border(2.dp, Color.Companion.White)
                                .padding(4.dp)
                        ) {
                            WaypointMinimap(
                                map = map,
                                selectedSource = selectedSource,
                                selectedTarget = selectedTarget,
                                existingWaypoints = existingWaypoints,
                                onTileClick = { clickedPos ->
                                    // Determine if clicked position is a valid source or target
                                    val isValidSource = validSources.contains(clickedPos)
                                    val isValidTarget = validTargets.contains(clickedPos)
                                    
                                    when {
                                        // If no source is selected yet, select this position as source if valid
                                        selectedSource == null && isValidSource -> {
                                            selectedSource = clickedPos
                                            errorMessage = null
                                        }
                                        // If source is selected but no target, select as target if valid
                                        selectedSource != null && selectedTarget == null && isValidTarget -> {
                                            selectedTarget = clickedPos
                                            errorMessage = null
                                        }
                                        // If both are selected, clicking a valid source resets and selects new source
                                        selectedSource != null && selectedTarget != null && isValidSource -> {
                                            selectedSource = clickedPos
                                            selectedTarget = null
                                            errorMessage = null
                                        }
                                        // If both selected and clicking a valid target, just update target
                                        selectedSource != null && selectedTarget != null && isValidTarget -> {
                                            selectedTarget = clickedPos
                                            errorMessage = null
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
                            modifier = Modifier.width(110.dp),
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
                            LegendItem("Selected Src", Color(0xFF40E0D0))
                            LegendItem("Selected Tgt", Color(0xFF00AAFF))
                            
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
                                        val typeLabel = when (tileType) {
                                            TileType.SPAWN_POINT -> "Spawn"
                                            TileType.WAYPOINT -> "Waypoint"
                                            TileType.TARGET -> "Target"
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
                        }
                    }
                }

                // Instructions
                Text(
                    text = stringResource(Res.string.click_to_connect_waypoints),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Source selection section
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Number1Icon(size = 18.dp)
                        Text(
                            text = stringResource(Res.string.waypoint_source),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )
                        // Checkbox to show/hide already connected sources
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Checkbox(
                                checked = showConnectedSources,
                                onCheckedChange = { showConnectedSources = it }
                            )
                            Text(
                                text = stringResource(Res.string.show_connected),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        validSources.forEach { pos ->
                            val isSpawn = spawnPoints.contains(pos)
                            FilterChip(
                                modifier = Modifier.height(28.dp),
                                selected = selectedSource == pos,
                                onClick = {
                                    selectedSource = pos
                                    errorMessage = null
                                },
                                label = { WaypointLabel(pos, isSpawn, false, false) },
                                leadingIcon = if (selectedSource == pos) {
                                    { CheckmarkIcon(size = 14.dp) }
                                } else null
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Target selection section
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Number2Icon(size = 18.dp)
                        Text(
                            text = stringResource(Res.string.waypoint_target),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        validTargets.forEach { pos ->
                            val isTarget = targets.contains(pos)
                            val isConnectedToTarget = connectedToTarget.contains(pos)
                            FilterChip(
                                modifier = Modifier.height(28.dp),
                                selected = selectedTarget == pos,
                                onClick = {
                                    selectedTarget = pos
                                    errorMessage = null
                                },
                                label = { WaypointLabel(pos, false, isTarget, isConnectedToTarget) },
                                leadingIcon = if (selectedTarget == pos) {
                                    { CheckmarkIcon(size = 14.dp) }
                                } else null
                            )
                        }
                    }
                }

                // Error message
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedSource == null || selectedTarget == null) {
                        errorMessage = "Please select both source and target"
                        return@Button
                    }

                    val exists = existingWaypoints.any { it.position == selectedSource }
                    if (exists) {
                        errorMessage = waypointExistsErrorMsg
                        return@Button
                    }

                    onAdd(selectedSource!!, selectedTarget!!)
                }
            ) {
                Text(stringResource(Res.string.add_waypoint))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

/**
 * Legend item showing a color swatch and label
 */
@Composable
private fun LegendItem(label: String, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color)
                .border(0.5.dp, MaterialTheme.colorScheme.outline)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

/*
                                label = {
                                    Text(
                                        text = (if (isSpawn) "S" else "W") + "(${pos.x},${pos.y})",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
 */
@Composable
private fun WaypointLabel(
    pos: Position,
    isSpawn: Boolean,
    isTarget: Boolean,
    isConnectedToTarget: Boolean
){
    val waypointPrefix = if (isTarget) "T" else if (isSpawn) "S" else "W"

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$waypointPrefix(${pos.x},${pos.y})",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isSpawn || isTarget) FontWeight.Bold else FontWeight.Normal
        )
        // Show green checkmark if this position is the target or connected to target
        if (isConnectedToTarget) {
            CheckmarkIcon(size = 12.dp, tint = Color.Green)
        }
    }
}
