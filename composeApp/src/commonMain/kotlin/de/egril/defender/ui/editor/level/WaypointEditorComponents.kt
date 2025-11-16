package de.egril.defender.ui.editor.level

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.EditorWaypoint
import de.egril.defender.editor.WaypointChain
import de.egril.defender.editor.WaypointValidationResult
import de.egril.defender.model.Position
import de.egril.defender.ui.icon.*
import defender_of_egril.composeapp.generated.resources.*

/**
 * Tree view display for waypoint chains showing the hierarchical structure
 */
@Composable
fun WaypointTreeView(
    validationResult: WaypointValidationResult,
    map: EditorMap?,
    onDeleteConnection: (Position) -> Unit
) {
    val spawnPoints = remember(map) { map?.getSpawnPoints() ?: emptyList() }
    val target = remember(map) { map?.getTarget() }
    
    if (validationResult.waypointChains.isEmpty()) {
        Text(
            text = stringResource(Res.string.no_waypoints_configured),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        return
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(Res.string.waypoint_chains_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        
        validationResult.waypointChains.forEach { chain ->
            WaypointChainCard(
                chain = chain,
                spawnPoints = spawnPoints,
                target = target,
                circularDeps = validationResult.circularDependencies,
                unconnectedWaypoints = validationResult.unconnectedWaypoints,
                onDeleteConnection = onDeleteConnection
            )
        }
    }
}

/**
 * Card displaying a single waypoint chain
 */
@Composable
private fun WaypointChainCard(
    chain: WaypointChain,
    spawnPoints: List<Position>,
    target: Position?,
    circularDeps: Set<Position>,
    unconnectedWaypoints: Set<Position>,
    onDeleteConnection: (Position) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (chain.hasCircularDependency) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Start position (spawn point or waypoint)
            WaypointChainNode(
                position = chain.startPosition,
                isSpawn = spawnPoints.contains(chain.startPosition),
                isTarget = false,
                isInCircular = circularDeps.contains(chain.startPosition),
                isUnconnected = unconnectedWaypoints.contains(chain.startPosition),
                indentLevel = 0,
                onDelete = { onDeleteConnection(chain.startPosition) }
            )
            
            // Intermediate waypoints
            chain.positions.forEachIndexed { index, pos ->
                WaypointChainNode(
                    position = pos,
                    isSpawn = false,
                    isTarget = false,
                    isInCircular = circularDeps.contains(pos),
                    isUnconnected = unconnectedWaypoints.contains(pos),
                    indentLevel = index + 1,
                    onDelete = { onDeleteConnection(pos) }
                )
            }
            
            // End position (target or incomplete)
            if (chain.endPosition != null) {
                WaypointChainNode(
                    position = chain.endPosition,
                    isSpawn = false,
                    isTarget = chain.endPosition == target,
                    isInCircular = circularDeps.contains(chain.endPosition),
                    isUnconnected = unconnectedWaypoints.contains(chain.endPosition),
                    indentLevel = chain.positions.size + 1,
                    onDelete = null  // Can't delete target
                )
            }
        }
    }
}

/**
 * Single node in the waypoint chain tree
 */
@Composable
private fun WaypointChainNode(
    position: Position,
    isSpawn: Boolean,
    isTarget: Boolean,
    isInCircular: Boolean,
    isUnconnected: Boolean,
    indentLevel: Int,
    onDelete: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (indentLevel * 16).dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Arrow indicator (except for first node)
        if (indentLevel > 0) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isInCircular) {
                    RedCircleIcon(size = 12.dp)
                }
                RightArrowIcon(
                    size = 14.dp,
                    tint = if (isInCircular) Color.Red else Color.Black
                )
            }
        }
        
        // Position and type
        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.waypoint_position_format).format(position.x, position.y),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                // Warning icons
                if (isInCircular) {
                    WarningIcon(
                        size = 14.dp
                    )
                }
                if (isUnconnected) {
                    WarningIcon(
                        size = 14.dp
                    )
                }
            }
            
            // Type label
            if (isSpawn) {
                Text(
                    text = stringResource(Res.string.spawn_point_text),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (isTarget) {
                Text(
                    text = stringResource(Res.string.target_text),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Green
                )
            } else {
                Text(
                    text = "WAYPOINT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            // Warning messages
            if (isInCircular) {
                Text(
                    text = stringResource(Res.string.circular_dependency_warning),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Red
                )
            }
            if (isUnconnected) {
                Text(
                    text = stringResource(Res.string.unconnected_waypoint_warning),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        
        // Delete button (if allowed)
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                TrashIcon(size = 20.dp)
            }
        }
    }
}

/**
 * Mode selector for waypoint connection
 */
enum class WaypointConnectionMode {
    SELECT_SOURCE,
    SELECT_TARGET,
    INACTIVE
}

/**
 * State for interactive waypoint connection on map
 */
data class WaypointConnectionState(
    val mode: WaypointConnectionMode = WaypointConnectionMode.INACTIVE,
    val selectedSource: Position? = null
)

/**
 * Quick add dialog that shows map positions for easier waypoint creation
 */
@Composable
fun QuickAddWaypointDialog(
    map: EditorMap?,
    existingWaypoints: List<EditorWaypoint>,
    onDismiss: () -> Unit,
    onAdd: (Position, Position) -> Unit
) {
    val waypointTiles = remember(map) { map?.getWaypoints() ?: emptyList() }
    val spawnPoints = remember(map) { map?.getSpawnPoints() ?: emptyList() }
    val target = remember(map) { map?.getTarget() }
    
    var selectedSource by remember { mutableStateOf<Position?>(null) }
    var selectedTarget by remember { mutableStateOf<Position?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val validSources = remember(spawnPoints, waypointTiles) {
        (spawnPoints + waypointTiles).distinct().sortedWith(compareBy({ it.y }, { it.x }))
    }
    
    val validTargets = remember(waypointTiles, target) {
        val targets = waypointTiles.toMutableList()
        if (target != null) targets.add(target)
        targets.distinct().sortedWith(compareBy({ it.y }, { it.x }))
    }
    
    val waypointExistsErrorMsg = stringResource(Res.string.waypoint_exists_error)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(stringResource(Res.string.select_on_map)) 
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Number1Icon(size = 18.dp)
                        Text(
                            text = stringResource(Res.string.waypoint_source),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(validSources) { pos ->
                            val isSpawn = spawnPoints.contains(pos)
                            FilterChip(
                                selected = selectedSource == pos,
                                onClick = {
                                    selectedSource = pos
                                    errorMessage = null
                                },
                                label = { 
                                    Text(
                                        text = if (isSpawn) "S(${pos.x},${pos.y})" else "W(${pos.x},${pos.y})",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
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
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(validTargets) { pos ->
                            val isTarget = target == pos
                            FilterChip(
                                selected = selectedTarget == pos,
                                onClick = {
                                    selectedTarget = pos
                                    errorMessage = null
                                },
                                label = { 
                                    Text(
                                        text = if (isTarget) "T(${pos.x},${pos.y})" else "W(${pos.x},${pos.y})",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
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
