package de.egril.defender.ui.editor.level.initialsetup

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.TileType
import de.egril.defender.model.Position
import de.egril.defender.ui.settings.AppSettings
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val MINIMAP_HEX_RADIUS = 2.0f

/**
 * Placement mode for initial setup elements
 */
enum class PlacementMode {
    DEFENDER,    // Towers: BUILD_AREA or ISLAND
    ATTACKER,    // Enemies: PATH or SPAWN_POINT
    TRAP,        // Traps: PATH
    BARRICADE    // Barricades: PATH
}

/**
 * Checks if a position is valid for the given placement mode
 */
fun isValidPlacement(position: Position, mode: PlacementMode, map: EditorMap): Boolean {
    val tileType = map.getTileType(position.x, position.y)
    return when (mode) {
        PlacementMode.DEFENDER -> tileType == TileType.BUILD_AREA || tileType == TileType.ISLAND
        PlacementMode.ATTACKER -> tileType == TileType.PATH || tileType == TileType.SPAWN_POINT
        PlacementMode.TRAP -> tileType == TileType.PATH
        PlacementMode.BARRICADE -> tileType == TileType.PATH
    }
}

/**
 * Data class to hold hexagon geometry calculations
 */
private data class HexGeometry(
    val hexSize: Float,
    val hexWidth: Float,
    val hexHeight: Float,
    val verticalSpacing: Float,
    val offsetXCanvas: Float,
    val offsetYCanvas: Float
)

/**
 * Calculate hexagon geometry for the minimap
 */
private fun calculateHexGeometry(
    mapWidth: Int,
    mapHeight: Int,
    canvasWidth: Float,
    canvasHeight: Float
): HexGeometry {
    val baseHexSize = MINIMAP_HEX_RADIUS
    val baseHexWidth = (sqrt(3.0) * baseHexSize).toFloat()
    val baseHexHeight = 2.0f * baseHexSize
    val baseVerticalSpacing = baseHexHeight * 0.75f
    val totalMapWidth = (mapWidth) * baseHexWidth + baseHexWidth / 2
    val totalMapHeight = (mapHeight - 1) * baseVerticalSpacing + baseHexHeight
    val padding = 4f
    val scaleX = (canvasWidth - padding * 2) / totalMapWidth
    val scaleY = (canvasHeight - padding * 2) / totalMapHeight
    val mapScale = minOf(scaleX, scaleY)
    val hexSize = baseHexSize * mapScale
    val hexWidth = baseHexWidth * mapScale
    val hexHeight = baseHexHeight * mapScale
    val verticalSpacing = baseVerticalSpacing * mapScale
    val scaledMapWidth = totalMapWidth * mapScale
    val scaledMapHeight = totalMapHeight * mapScale
    val offsetXCanvas = (canvasWidth - scaledMapWidth) / 2
    val offsetYCanvas = (canvasHeight - scaledMapHeight) / 2
    
    return HexGeometry(hexSize, hexWidth, hexHeight, verticalSpacing, offsetXCanvas, offsetYCanvas)
}

/**
 * Minimap for selecting positions in initial setup
 * Enhanced to show existing placements and support removal
 */
@Composable
fun InitialSetupMinimap(
    map: EditorMap,
    placementMode: PlacementMode?,
    existingDefenders: List<de.egril.defender.editor.InitialDefender> = emptyList(),
    existingAttackers: List<de.egril.defender.editor.InitialAttacker> = emptyList(),
    existingTraps: List<de.egril.defender.editor.InitialTrap> = emptyList(),
    existingBarricades: List<de.egril.defender.editor.InitialBarricade> = emptyList(),
    selectedElement: de.egril.defender.ui.editor.level.initialsetup.SelectedElement? = null,
    onTileClick: (Position) -> Unit = {}
) {
    val isDarkMode = AppSettings.isDarkMode.value
    var hoveredPosition by remember { mutableStateOf<Position?>(null) }

    Canvas(modifier = Modifier.fillMaxSize()
        .pointerInput(placementMode, existingDefenders, existingAttackers, existingTraps, existingBarricades) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    val offset = event.changes.firstOrNull()?.position ?: continue
                    
                    when (event.type) {
                        PointerEventType.Move, PointerEventType.Enter -> {
                            val geometry = calculateHexGeometry(map.width, map.height, size.width.toFloat(), size.height.toFloat())
                            val hitRadius = geometry.hexHeight / 2
                            val hitRadiusSquared = hitRadius * hitRadius

                            var newHoveredPosition: Position? = null
                            var minDistanceSquared = Float.MAX_VALUE

                            for (row in 0 until map.height) {
                                for (col in 0 until map.width) {
                                    val pos = Position(col, row)
                                    val offsetXHex = if (row % 2 == 1) geometry.hexWidth / 2 else 0.0f
                                    val centerX = geometry.offsetXCanvas + col * geometry.hexWidth + offsetXHex + geometry.hexWidth / 2
                                    val centerY = geometry.offsetYCanvas + row * geometry.verticalSpacing + geometry.hexHeight / 2
                                    val dx = offset.x - centerX
                                    val dy = offset.y - centerY
                                    val distanceSquared = dx * dx + dy * dy
                                    if (distanceSquared < hitRadiusSquared && distanceSquared < minDistanceSquared) {
                                        minDistanceSquared = distanceSquared
                                        newHoveredPosition = pos
                                    }
                                }
                            }
                            hoveredPosition = newHoveredPosition
                        }
                        PointerEventType.Exit -> {
                            hoveredPosition = null
                        }
                    }
                }
            }
        }
        .pointerInput(placementMode, existingDefenders, existingAttackers, existingTraps, existingBarricades) {
            detectTapGestures { offset ->
                val geometry = calculateHexGeometry(map.width, map.height, size.width.toFloat(), size.height.toFloat())
                val hitRadius = geometry.hexHeight / 2
                val hitRadiusSquared = hitRadius * hitRadius

                var clickedPosition: Position? = null
                var minDistanceSquared = Float.MAX_VALUE

                for (row in 0 until map.height) {
                    for (col in 0 until map.width) {
                        val pos = Position(col, row)
                        val offsetXHex = if (row % 2 == 1) geometry.hexWidth / 2 else 0.0f
                        val centerX = geometry.offsetXCanvas + col * geometry.hexWidth + offsetXHex + geometry.hexWidth / 2
                        val centerY = geometry.offsetYCanvas + row * geometry.verticalSpacing + geometry.hexHeight / 2

                        val dx = offset.x - centerX
                        val dy = offset.y - centerY
                        val distanceSquared = dx * dx + dy * dy

                        if (distanceSquared < hitRadiusSquared && distanceSquared < minDistanceSquared) {
                            minDistanceSquared = distanceSquared
                            clickedPosition = pos
                        }
                    }
                }

                clickedPosition?.let { onTileClick(it) }
            }
        }
    ) {
        val geometry = calculateHexGeometry(map.width, map.height, size.width, size.height)

        // Draw tiles
        for (row in 0 until map.height) {
            for (col in 0 until map.width) {
                val tileType = map.tiles.getOrElse("$col,$row") { TileType.NO_PLAY }
                val pos = Position(col, row)

                val offsetXHex = if (row % 2 == 1) geometry.hexWidth / 2 else 0.0f
                val centerX = geometry.offsetXCanvas + col * geometry.hexWidth + offsetXHex + geometry.hexWidth / 2
                val centerY = geometry.offsetYCanvas + row * geometry.verticalSpacing + geometry.hexHeight / 2

                val isValidForPlacement = placementMode?.let { isValidPlacement(pos, it, map) } ?: false
                val isHovered = pos == hoveredPosition
                val hasDefender = existingDefenders.any { it.position == pos }
                val hasAttacker = existingAttackers.any { it.position == pos }
                val hasTrap = existingTraps.any { it.position == pos }
                val hasBarricade = existingBarricades.any { it.position == pos }
                val isSelected = when (selectedElement) {
                    is de.egril.defender.ui.editor.level.initialsetup.SelectedElement.Defender -> selectedElement.defender.position == pos
                    is de.egril.defender.ui.editor.level.initialsetup.SelectedElement.Attacker -> selectedElement.attacker.position == pos
                    is de.egril.defender.ui.editor.level.initialsetup.SelectedElement.Trap -> selectedElement.trap.position == pos
                    is de.egril.defender.ui.editor.level.initialsetup.SelectedElement.Barricade -> selectedElement.barricade.position == pos
                    null -> false
                }
                
                // Validation checks for placement conflicts
                val hasConflict = when (placementMode) {
                    PlacementMode.DEFENDER -> hasDefender
                    PlacementMode.TRAP, PlacementMode.BARRICADE -> hasTrap || hasBarricade
                    else -> false
                }

                val color = when {
                    isSelected -> Color(0xFFFFD700) // Gold for selected element
                    hasConflict && isHovered && placementMode != null -> Color(0xFFFF4444) // Red for invalid placement
                    isHovered && isValidForPlacement -> Color(0xFF00FFFF) // Cyan for valid hover
                    isValidForPlacement && placementMode != null -> if (isDarkMode) Color(0xFF2E5C1A) else Color(0xFF90EE90) // Green for valid
                    tileType == TileType.SPAWN_POINT -> if (isDarkMode) Color(0xFF8B0000) else Color(0xFFDC143C)
                    tileType == TileType.TARGET -> if (isDarkMode) Color(0xFF1E3A8A) else Color(0xFF4169E1)
                    tileType == TileType.PATH -> if (isDarkMode) Color(0xFF3E3528) else Color(0xFF8B4513)
                    tileType == TileType.BUILD_AREA -> if (isDarkMode) Color(0xFF2E5C1A) else Color(0xFF90EE90)
                    tileType == TileType.ISLAND -> if (isDarkMode) Color(0xFF1B4D0E) else Color(0xFF228B22)
                    else -> if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFF808080)
                }

                drawHexagon(centerX, centerY, geometry.hexSize, color)
            }
        }
        
        // Draw existing placements on top
        val iconSize = geometry.hexSize * 1.2f
        
        // Draw defenders (towers)
        existingDefenders.forEach { defender ->
            val offsetXHex = if (defender.position.y % 2 == 1) geometry.hexWidth / 2 else 0.0f
            val centerX = geometry.offsetXCanvas + defender.position.x * geometry.hexWidth + offsetXHex + geometry.hexWidth / 2
            val centerY = geometry.offsetYCanvas + defender.position.y * geometry.verticalSpacing + geometry.hexHeight / 2
            
            // Draw blue circle for tower
            drawCircle(
                color = Color(0xFF2196F3),
                radius = iconSize / 2,
                center = Offset(centerX, centerY)
            )
        }
        
        // Draw attackers (enemies)
        existingAttackers.forEach { attacker ->
            val offsetXHex = if (attacker.position.y % 2 == 1) geometry.hexWidth / 2 else 0.0f
            val centerX = geometry.offsetXCanvas + attacker.position.x * geometry.hexWidth + offsetXHex + geometry.hexWidth / 2
            val centerY = geometry.offsetYCanvas + attacker.position.y * geometry.verticalSpacing + geometry.hexHeight / 2
            
            // Draw red circle for enemy
            drawCircle(
                color = Color(0xFFFF0000),
                radius = iconSize / 2,
                center = Offset(centerX, centerY)
            )
        }
        
        // Draw traps
        existingTraps.forEach { trap ->
            val offsetXHex = if (trap.position.y % 2 == 1) geometry.hexWidth / 2 else 0.0f
            val centerX = geometry.offsetXCanvas + trap.position.x * geometry.hexWidth + offsetXHex + geometry.hexWidth / 2
            val centerY = geometry.offsetYCanvas + trap.position.y * geometry.verticalSpacing + geometry.hexHeight / 2
            
            // Draw triangle for trap
            val path = Path().apply {
                moveTo(centerX, centerY - iconSize / 2)
                lineTo(centerX + iconSize / 2, centerY + iconSize / 2)
                lineTo(centerX - iconSize / 2, centerY + iconSize / 2)
                close()
            }
            drawPath(
                path = path,
                color = if (trap.type == "MAGICAL") Color(0xFF9C27B0) else Color(0xFF795548)
            )
        }
        
        // Draw barricades
        existingBarricades.forEach { barricade ->
            val offsetXHex = if (barricade.position.y % 2 == 1) geometry.hexWidth / 2 else 0.0f
            val centerX = geometry.offsetXCanvas + barricade.position.x * geometry.hexWidth + offsetXHex + geometry.hexWidth / 2
            val centerY = geometry.offsetYCanvas + barricade.position.y * geometry.verticalSpacing + geometry.hexHeight / 2
            
            // Draw square for barricade
            drawRect(
                color = Color(0xFF8D6E63),
                topLeft = Offset(centerX - iconSize / 2, centerY - iconSize / 2),
                size = Size(iconSize, iconSize)
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
