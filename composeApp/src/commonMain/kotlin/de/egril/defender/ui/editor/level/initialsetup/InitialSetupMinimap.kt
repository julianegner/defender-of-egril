package de.egril.defender.ui.editor.level.initialsetup

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
import de.egril.defender.editor.TileType
import de.egril.defender.model.Position
import de.egril.defender.ui.settings.AppSettings
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val MINIMAP_HEX_SIZE = 2.0f

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
 * Minimap for selecting positions in initial setup
 */
@Composable
fun InitialSetupMinimap(
    map: EditorMap,
    placementMode: PlacementMode,
    selectedPosition: Position?,
    onTileClick: (Position) -> Unit = {},
    onHoverChange: (Position?) -> Unit = {}
) {
    val isDarkMode = AppSettings.isDarkMode.value

    Canvas(modifier = Modifier.fillMaxSize()
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    val offset = event.changes.firstOrNull()?.position ?: continue
                    
                    when (event.type) {
                        PointerEventType.Move, PointerEventType.Enter -> {
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
                                    val pos = Position(col, row)
                                    if (isValidPlacement(pos, placementMode, map)) {
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
        .pointerInput(placementMode) {
            detectTapGestures { offset ->
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

                var clickedPosition: Position? = null
                var minDistance = Float.MAX_VALUE

                for (row in 0 until map.height) {
                    for (col in 0 until map.width) {
                        val pos = Position(col, row)
                        
                        if (isValidPlacement(pos, placementMode, map)) {
                            val offsetXHex = if (row % 2 == 1) hexWidth / 2 else 0.0f
                            val centerX = offsetXCanvas + col * hexWidth + offsetXHex + hexWidth / 2
                            val centerY = offsetYCanvas + row * verticalSpacing + hexHeight / 2

                            val dx = offset.x - centerX
                            val dy = offset.y - centerY
                            val distance = sqrt(dx * dx + dy * dy)

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

        val hexSize = baseHexSize * mapScale
        val hexWidth = baseHexWidth * mapScale
        val hexHeight = baseHexHeight * mapScale
        val verticalSpacing = baseVerticalSpacing * mapScale

        val scaledMapWidth = totalMapWidth * mapScale
        val scaledMapHeight = totalMapHeight * mapScale
        val offsetXCanvas = (size.width - scaledMapWidth) / 2
        val offsetYCanvas = (size.height - scaledMapHeight) / 2

        for (row in 0 until map.height) {
            for (col in 0 until map.width) {
                val tileType = map.tiles.getOrElse("$col,$row") { TileType.NO_PLAY }
                val pos = Position(col, row)

                val offsetXHex = if (row % 2 == 1) hexWidth / 2 else 0.0f
                val centerX = offsetXCanvas + col * hexWidth + offsetXHex + hexWidth / 2
                val centerY = offsetYCanvas + row * verticalSpacing + hexHeight / 2

                val isValidForPlacement = isValidPlacement(pos, placementMode, map)
                val isSelected = pos == selectedPosition

                val color = when {
                    isSelected -> Color(0xFF00AAFF) // Cyan for selected
                    isValidForPlacement -> if (isDarkMode) Color(0xFF2E5C1A) else Color(0xFF90EE90) // Green for valid
                    tileType == TileType.SPAWN_POINT -> if (isDarkMode) Color(0xFF8B0000) else Color(0xFFDC143C)
                    tileType == TileType.TARGET -> if (isDarkMode) Color(0xFF1E3A8A) else Color(0xFF4169E1)
                    tileType == TileType.PATH -> if (isDarkMode) Color(0xFF3E3528) else Color(0xFF8B4513)
                    tileType == TileType.BUILD_AREA -> if (isDarkMode) Color(0xFF2E5C1A) else Color(0xFF90EE90)
                    tileType == TileType.ISLAND -> if (isDarkMode) Color(0xFF1B4D0E) else Color(0xFF228B22)
                    else -> if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFF808080)
                }

                drawHexagon(centerX, centerY, hexSize, color)
            }
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
