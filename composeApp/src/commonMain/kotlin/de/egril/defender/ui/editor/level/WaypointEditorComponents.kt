package de.egril.defender.ui.editor.level

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
            Text(
                text = if (isInCircular) "🔴→" else "→",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isInCircular) Color.Red else Color.Black
            )
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
                    Text(
                        text = "⚠",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (isUnconnected) {
                    Text(
                        text = "⚠",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
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
                Text("🗑️", style = MaterialTheme.typography.titleMedium)
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
