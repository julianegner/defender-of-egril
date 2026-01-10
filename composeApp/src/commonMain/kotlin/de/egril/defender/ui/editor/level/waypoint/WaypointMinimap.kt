package de.egril.defender.ui.editor.level.waypoint

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.EditorWaypoint
import de.egril.defender.editor.TileType
import de.egril.defender.model.Position
import de.egril.defender.ui.settings.AppSettings
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// Minimap configuration
private const val MINIMAP_HEX_SIZE = 2.0f  // Doubled from original 1.0f for better visibility
private const val CONNECTION_STROKE_WIDTH = 2.0f  // Stroke width for waypoint connections

/**
 * Color palette for different target destinations.
 * Supports up to 8 distinct targets. If there are more than 8 targets,
 * colors will wrap around using modulo operator (e.g., 9th target uses same color as 1st).
 */
private val TARGET_COLORS = listOf(
    Color(0xFFFFD700), // Gold
    Color(0xFF00FFFF), // Cyan
    Color(0xFFFF00FF), // Magenta
    Color(0xFF00FF00), // Lime
    Color(0xFFFF6600), // Orange
    Color(0xFF9966FF), // Purple
    Color(0xFFFF0066), // Pink
    Color(0xFF66FF99), // Mint
)

/**
 * Determines which final target position a waypoint ultimately leads to by following the chain.
 * Made internal for testing purposes.
 */
internal fun findUltimateTarget(
    waypoint: EditorWaypoint,
    allWaypoints: List<EditorWaypoint>,
    targets: List<Position>
): Position? {
    val visited = mutableSetOf<Position>()
    var current = waypoint.nextTargetPosition
    
    // Follow the chain until we reach a target or detect a loop
    while (true) {
        // Check if we reached a target
        if (targets.contains(current)) {
            return current
        }
        
        // Check for circular dependency
        if (visited.contains(current)) {
            return null // Circular dependency detected
        }
        visited.add(current)
        
        // Find the next waypoint in the chain
        val nextWaypoint = allWaypoints.find { it.position == current }
        if (nextWaypoint == null) {
            // No more waypoints, current position might be the target
            return if (targets.contains(current)) current else null
        }
        
        current = nextWaypoint.nextTargetPosition
    }
}

/**
 * Minimap showing waypoint positions with highlighting
 */
@Composable
fun WaypointMinimap(
    map: EditorMap,
    selectedSource: Position?,
    selectedTarget: Position?,
    existingWaypoints: List<EditorWaypoint>,
    onTileClick: (Position) -> Unit = {},
    onHoverChange: (Position?) -> Unit = {}
) {
    val isDarkMode = AppSettings.isDarkMode.value
    
    // Get all target positions from the map
    val targets = map.getTargets()
    
    // Create a map of waypoint position to its ultimate target and color
    val waypointTargetColors = existingWaypoints.associate { waypoint ->
        val ultimateTarget = findUltimateTarget(waypoint, existingWaypoints, targets)
        val targetIndex = ultimateTarget?.let { targets.indexOf(it) } ?: -1
        val color = if (targetIndex >= 0) {
            TARGET_COLORS[targetIndex % TARGET_COLORS.size]
        } else {
            Color.Gray // For waypoints that don't lead to a valid target
        }
        waypoint.position to color
    }

    Canvas(modifier = Modifier.Companion.fillMaxSize()
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    val offset = event.changes.firstOrNull()?.position ?: continue
                    
                    when (event.type) {
                        PointerEventType.Move, PointerEventType.Enter -> {
                            // Calculate hovered position
                            val mapWidth = map.width
                            val mapHeight = map.height
                            val baseHexSize = MINIMAP_HEX_SIZE
                            val baseHexWidth = (sqrt(3.0) * baseHexSize).toFloat()
                            val baseHexHeight = 2.0f * baseHexSize
                            val baseVerticalSpacing = baseHexHeight * 0.75f
                            val totalMapWidth = (mapWidth) * baseHexWidth + baseHexWidth / 2
                            val totalMapHeight = (mapHeight - 1) * baseVerticalSpacing + baseHexHeight
                            val padding = 4f
                            val scaleX = (size.width - padding * 2) / totalMapWidth
                            val scaleY = (size.height - padding * 2) / totalMapHeight
                            val mapScale = minOf(scaleX, scaleY)
                            val hexWidth = baseHexWidth * mapScale
                            val hexHeight = baseHexHeight * mapScale
                            val verticalSpacing = baseVerticalSpacing * mapScale
                            val scaledMapWidth = totalMapWidth * mapScale
                            val scaledMapHeight = totalMapHeight * mapScale
                            val offsetXCanvas = (size.width - scaledMapWidth) / 2
                            val offsetYCanvas = (size.height - scaledMapHeight) / 2

                            var hoveredPosition: Position? = null
                            var minDistance = Float.MAX_VALUE

                            for (row in 0 until map.height) {
                                for (col in 0 until map.width) {
                                    val tileType = map.tiles.getOrElse("$col,$row") { TileType.NO_PLAY }
                                    if (tileType == TileType.SPAWN_POINT || 
                                        tileType == TileType.PATH ||
                                        tileType == TileType.TARGET) {
                                        val pos = Position(col, row)
                                        val offsetXHex = if (row % 2 == 1) hexWidth / 2 else 0.0f
                                        val centerX = offsetXCanvas + col * hexWidth + offsetXHex + hexWidth / 2
                                        val centerY = offsetYCanvas + row * verticalSpacing + hexHeight / 2
                                        val dx = offset.x - centerX
                                        val dy = offset.y - centerY
                                        val distance = sqrt(dx * dx + dy * dy)
                                        val hitRadius = hexHeight / 2
                                        if (distance < hitRadius && distance < minDistance) {
                                            minDistance = distance
                                            hoveredPosition = pos
                                        }
                                    }
                                }
                            }
                            onHoverChange(hoveredPosition)
                        }
                        PointerEventType.Exit -> {
                            onHoverChange(null)
                        }
                    }
                }
            }
        }
        .pointerInput(Unit) {
            detectTapGestures { offset ->
                // Calculate map dimensions in hex coordinates
                val mapWidth = map.width
                val mapHeight = map.height

                // Calculate the size needed for the hex grid (doubled)
                val baseHexSize = MINIMAP_HEX_SIZE
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

                // Find which hex was clicked
                var clickedPosition: Position? = null
                var minDistance = Float.MAX_VALUE

                for (row in 0 until map.height) {
                    for (col in 0 until map.width) {
                        val tileType = map.tiles.getOrElse("$col,$row") { TileType.NO_PLAY }
                        
                        // Only consider valid waypoint-related tiles
                        if (tileType == TileType.SPAWN_POINT || 
                            tileType == TileType.PATH ||
                            tileType == TileType.TARGET) {
                            
                            val pos = Position(col, row)
                            
                            // Calculate hex center position
                            val offsetXHex = if (row % 2 == 1) hexWidth / 2 else 0.0f
                            val centerX = offsetXCanvas + col * hexWidth + offsetXHex + hexWidth / 2
                            val centerY = offsetYCanvas + row * verticalSpacing + hexHeight / 2

                            // Check if click is within this hex 
                            // For hexagons, we use a slightly larger radius than hexSize
                            // to account for the circumscribed circle
                            val dx = offset.x - centerX
                            val dy = offset.y - centerY
                            val distance = sqrt(dx * dx + dy * dy)

                            // Use hexHeight/2 as the hit radius for better accuracy
                            val hitRadius = hexHeight / 2
                            if (distance < hitRadius && distance < minDistance) {
                                minDistance = distance
                                clickedPosition = pos
                            }
                        }
                    }
                }

                clickedPosition?.let { onTileClick(it) }
            }
        }
    ) {
        // Calculate map dimensions in hex coordinates
        val mapWidth = map.width
        val mapHeight = map.height

        // Calculate the size needed for the hex grid (doubled)
        val baseHexSize = MINIMAP_HEX_SIZE
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
                
                // Check if this position is a defined waypoint
                val isDefinedWaypoint = existingWaypoints.any { it.position == pos }

                // Get color for tile type
                val color = when {
                    isSelected && pos == selectedSource -> Color(0xFF40E0D0) // Turquoise for selected source
                    isSelected && pos == selectedTarget -> Color(0xFF00AAFF) // Cyan for selected target
                    isDefinedWaypoint -> if (isDarkMode) Color(0xFF9A7B00) else Color(0xFFFFD700) // Yellow for waypoints
                    tileType == TileType.SPAWN_POINT -> if (isDarkMode) Color(0xFF8B0000) else Color(0xFFDC143C)
                    tileType == TileType.TARGET -> if (isDarkMode) Color(0xFF1E3A8A) else Color(0xFF4169E1)
                    tileType == TileType.PATH -> if (isDarkMode) Color(0xFF3E3528) else Color(0xFF8B4513)
                    tileType == TileType.BUILD_AREA -> if (isDarkMode) Color(0xFF2E5C1A) else Color(0xFF90EE90)
                    tileType == TileType.ISLAND -> if (isDarkMode) Color(0xFF1B4D0E) else Color(0xFF228B22)
                    else -> if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFF808080)
                }

                // Draw hexagon
                drawHexagon(centerX, centerY, hexSize, color)
            }
        }
        
        // Draw connection lines for ALL waypoints (after tiles are drawn)
        // This ensures connections are always visible, even if waypoint positions aren't on the tile grid
        for (waypoint in existingWaypoints) {
            val sourcePos = waypoint.position
            val targetPos = waypoint.nextTargetPosition
            
            // Get the color for this waypoint based on its ultimate target
            val connectionColor = waypointTargetColors[sourcePos] ?: Color.Gray
            
            // Calculate source position
            val sourceCol = sourcePos.x
            val sourceRow = sourcePos.y
            val sourceOffsetXHex = if (sourceRow % 2 == 1) hexWidth / 2 else 0.0f
            val sourceCenterX = offsetXCanvas + sourceCol * hexWidth + sourceOffsetXHex + hexWidth / 2
            val sourceCenterY = offsetYCanvas + sourceRow * verticalSpacing + hexHeight / 2
            
            // Calculate target position
            val targetCol = targetPos.x
            val targetRow = targetPos.y
            val targetOffsetXHex = if (targetRow % 2 == 1) hexWidth / 2 else 0.0f
            val targetCenterX = offsetXCanvas + targetCol * hexWidth + targetOffsetXHex + hexWidth / 2
            val targetCenterY = offsetYCanvas + targetRow * verticalSpacing + hexHeight / 2
            
            // Draw connection line
            drawLine(
                color = connectionColor,
                start = Offset(sourceCenterX, sourceCenterY),
                end = Offset(targetCenterX, targetCenterY),
                strokeWidth = CONNECTION_STROKE_WIDTH
            )
        }
    }
}

/**
 * Helper function to draw a hexagon (pointy-top orientation)
 */
private fun DrawScope.drawHexagon(centerX: Float, centerY: Float, radius: Float, color: Color) {
    val path = Path()
    for (i in 0..5) {
        val angle = PI * (60.0 * i - 30.0) / 180.0
        val x = centerX + (radius * cos(angle)).toFloat()
        val y = centerY + (radius * sin(angle)).toFloat()

        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()

    drawPath(path, color)
}
