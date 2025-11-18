package de.egril.defender.ui.editor.level

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.EditorWaypoint
import de.egril.defender.editor.TileType
import de.egril.defender.editor.WaypointChain
import de.egril.defender.editor.WaypointValidationResult
import de.egril.defender.model.Position
import de.egril.defender.ui.icon.*
import defender_of_egril.composeapp.generated.resources.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Tree view display for waypoint chains showing the hierarchical structure
 */
@Composable
fun WaypointTreeView(
    validationResult: WaypointValidationResult,
    map: EditorMap?,
    onDeleteConnection: (Position) -> Unit,
    onConnectWaypoint: (Position) -> Unit
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
                onDeleteConnection = onDeleteConnection,
                onConnectWaypoint = onConnectWaypoint
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
    onDeleteConnection: (Position) -> Unit,
    onConnectWaypoint: (Position) -> Unit
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
            // waypoints
            chain.positions.forEachIndexed { index, pos ->
                WaypointChainNode(
                    position = pos,
                    isSpawn = spawnPoints.contains(pos),
                    isTarget = false,
                    isInCircular = circularDeps.contains(pos),
                    isUnconnected = unconnectedWaypoints.contains(pos),
                    indentLevel = index,
                    onDelete = { onDeleteConnection(pos) },
                    onConnect = { onConnectWaypoint(pos) }
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
                    indentLevel = chain.positions.size,
                    onDelete = null,  // Can't delete target
                    onConnect = { onConnectWaypoint(chain.endPosition) }
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
    onDelete: (() -> Unit)?,
    onConnect: (() -> Unit)? = null
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WarningIcon(size = 12.dp)
                    Text(
                        text = stringResource(Res.string.circular_dependency_warning),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Red
                    )
                }
            }
            if (isUnconnected) {
                if (!isSpawn) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WarningIcon(size = 12.dp)
                        Text(
                            text = stringResource(Res.string.unconnected_waypoint_warning),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                // Connect button (if unconnected)
                if (onConnect != null) {
                    Button(
                        onClick = onConnect,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.connect),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
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
    onAdd: (Position, Position) -> Unit,
    preselectedSource: Position? = null
) {
    val waypointTiles = remember(map) { map?.getWaypoints() ?: emptyList() }
    val spawnPoints = remember(map) { map?.getSpawnPoints() ?: emptyList() }
    val target = remember(map) { map?.getTarget() }
    
    var selectedSource by remember { mutableStateOf(preselectedSource) }
    var selectedTarget by remember { mutableStateOf<Position?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showConnectedSources by remember { mutableStateOf(false) }
    
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
    
    val validTargets = remember(waypointTiles, target) {
        val targets = waypointTiles.toMutableList()
        if (target != null) targets.add(target)
        targets.distinct().sortedWith(compareBy({ it.y }, { it.x }))
    }
    
    // Build map of positions that are already connected to targets
    val connectedToTarget = remember(existingWaypoints, target) {
        val connected = mutableSetOf<Position>()
        
        // Build a map for quick lookup
        val waypointMap = existingWaypoints.associateBy { it.position }
        
        // For each position, check if it eventually leads to target
        fun leadsToTarget(pos: Position, visited: MutableSet<Position> = mutableSetOf()): Boolean {
            if (pos == target) return true
            if (pos in visited) return false // Circular reference
            visited.add(pos)
            
            val waypoint = waypointMap[pos] ?: return false
            return leadsToTarget(waypoint.nextTargetPosition, visited)
        }
        
        for (pos in validTargets) {
            if (pos == target || leadsToTarget(pos)) {
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
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Minimap showing spawn points, waypoints, and targets
                if (map != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .background(Color(0xCC000000))
                            .border(2.dp, Color.White)
                            .padding(4.dp)
                    ) {
                        WaypointMinimap(
                            map = map,
                            spawnPoints = spawnPoints,
                            waypointTiles = waypointTiles,
                            target = target,
                            selectedSource = selectedSource,
                            selectedTarget = selectedTarget,
                            existingWaypoints = existingWaypoints
                        )
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
                            val isConnectedToTarget = connectedToTarget.contains(pos)
                            FilterChip(
                                selected = selectedTarget == pos,
                                onClick = {
                                    selectedTarget = pos
                                    errorMessage = null
                                },
                                label = { 
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isTarget) "T(${pos.x},${pos.y})" else "W(${pos.x},${pos.y})",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        // Show green checkmark if this position is the target or connected to target
                                        if (isConnectedToTarget) {
                                            CheckmarkIcon(size = 12.dp, tint = Color.Green)
                                        }
                                    }
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

/**
 * Minimap showing waypoint positions with highlighting
 */
@Composable
private fun WaypointMinimap(
    map: EditorMap,
    spawnPoints: List<Position>,
    waypointTiles: List<Position>,
    target: Position?,
    selectedSource: Position?,
    selectedTarget: Position?,
    existingWaypoints: List<EditorWaypoint>
) {
    val isDarkMode = de.egril.defender.ui.settings.AppSettings.isDarkMode.value
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Calculate map dimensions in hex coordinates
        val mapWidth = map.width
        val mapHeight = map.height
        
        // Calculate the size needed for the hex grid
        val baseHexSize = 1.0f
        val baseHexWidth = (sqrt(3.0) * baseHexSize).toFloat()
        val baseHexHeight = 2.0f * baseHexSize
        val baseVerticalSpacing = baseHexHeight * 0.75f
        
        // Calculate total map dimensions in base units
        val totalMapWidth = (mapWidth) * baseHexWidth + baseHexWidth / 2
        val totalMapHeight = (mapHeight - 1) * baseVerticalSpacing + baseHexHeight
        
        // Scale to fit in canvas with some padding
        val padding = 4f
        val scaleX = (size.width - padding * 2) / totalMapWidth
        val scaleY = (size.height - padding * 2) / totalMapHeight
        val mapScale = minOf(scaleX, scaleY)
        
        // Calculate actual hex dimensions after scaling
        val hexSize = baseHexSize * mapScale
        val hexWidth = baseHexWidth * mapScale
        val hexHeight = baseHexHeight * mapScale
        val verticalSpacing = baseVerticalSpacing * mapScale
        
        // Center the map in the canvas
        val scaledMapWidth = totalMapWidth * mapScale
        val scaledMapHeight = totalMapHeight * mapScale
        val offsetXCanvas = (size.width - scaledMapWidth) / 2
        val offsetYCanvas = (size.height - scaledMapHeight) / 2
        
        // Draw hexagon map tiles
        for (row in 0 until map.height) {
            for (col in 0 until map.width) {
                val tileType = map.tiles.getOrElse("$col,$row") { TileType.NO_PLAY }
                val pos = Position(col, row)
                
                // Calculate hex center position
                val offsetXHex = if (row % 2 == 1) hexWidth / 2 else 0.0f
                val centerX = offsetXCanvas + col * hexWidth + offsetXHex + hexWidth / 2
                val centerY = offsetYCanvas + row * verticalSpacing + hexHeight / 2
                
                // Determine if this position is highlighted
                val isSelected = pos == selectedSource || pos == selectedTarget
                
                // Get color for tile type
                val color = when {
                    isSelected && pos == selectedSource -> Color(0xFF40E0D0) // Turquoise for selected source
                    isSelected && pos == selectedTarget -> Color(0xFF00AAFF) // Cyan for selected target
                    tileType == TileType.SPAWN_POINT -> if (isDarkMode) Color(0xFF8B0000) else Color(0xFFDC143C)
                    tileType == TileType.TARGET -> if (isDarkMode) Color(0xFF1E3A8A) else Color(0xFF4169E1)
                    tileType == TileType.WAYPOINT -> if (isDarkMode) Color(0xFF9A7B00) else Color(0xFFFFD700)
                    tileType == TileType.PATH -> if (isDarkMode) Color(0xFF3E3528) else Color(0xFF8B4513)
                    tileType == TileType.BUILD_AREA -> if (isDarkMode) Color(0xFF2E5C1A) else Color(0xFF90EE90)
                    tileType == TileType.ISLAND -> if (isDarkMode) Color(0xFF1B4D0E) else Color(0xFF228B22)
                    else -> if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFF808080)
                }
                
                // Draw hexagon
                drawHexagon(centerX, centerY, hexSize, color)
                
                // Draw connection lines for existing waypoints
                val waypoint = existingWaypoints.find { it.position == pos }
                if (waypoint != null) {
                    val targetPos = waypoint.nextTargetPosition
                    val targetCol = targetPos.x
                    val targetRow = targetPos.y
                    if (targetCol in 0 until mapWidth && targetRow in 0 until mapHeight) {
                        val targetOffsetXHex = if (targetRow % 2 == 1) hexWidth / 2 else 0.0f
                        val targetCenterX = offsetXCanvas + targetCol * hexWidth + targetOffsetXHex + hexWidth / 2
                        val targetCenterY = offsetYCanvas + targetRow * verticalSpacing + hexHeight / 2
                        
                        // Draw arrow line
                        drawLine(
                            color = Color.Yellow,
                            start = Offset(centerX, centerY),
                            end = Offset(targetCenterX, targetCenterY),
                            strokeWidth = 1.5f
                        )
                    }
                }
            }
        }
    }
}

/**
 * Helper function to draw a hexagon
 */
private fun DrawScope.drawHexagon(centerX: Float, centerY: Float, radius: Float, color: Color) {
    val path = Path()
    for (i in 0..5) {
        val angle = (PI / 3.0 * i).toFloat()
        val x = centerX + radius * cos(angle)
        val y = centerY + radius * sin(angle)
        
        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()
    
    drawPath(path, color)
}
