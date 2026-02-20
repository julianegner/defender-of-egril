package de.egril.defender.ui.editor.level

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.TileType
import de.egril.defender.model.Position
import de.egril.defender.ui.settings.AppSettings
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Minimap showing spawn points with highlighting
 */
@Composable
fun SpawnPointMinimap(
    map: EditorMap,
    selectedSpawnPoint: Position?
) {
    val isDarkMode = AppSettings.isDarkMode.value

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

                // Determine if this position is the selected spawn point
                val isSelected = pos == selectedSpawnPoint

                // Get color for tile type
                val color = when {
                    isSelected -> Color(0xFF00FF00) // Bright green for selected spawn point
                    tileType == TileType.SPAWN_POINT -> if (isDarkMode) Color(0xFF8B0000) else Color(0xFFDC143C)
                    tileType == TileType.TARGET -> if (isDarkMode) Color(0xFF1E3A8A) else Color(0xFF4169E1)
                    tileType == TileType.PATH -> if (isDarkMode) Color(0xFF3E3528) else Color(0xFF8B4513)
                    tileType == TileType.BUILD_AREA -> if (isDarkMode) Color(0xFF2E5C1A) else Color(0xFF90EE90)
                    @Suppress("DEPRECATION")
                    tileType == TileType.ISLAND -> if (isDarkMode) Color(0xFF2E5C1A) else Color(0xFF90EE90) // Deprecated: same as BUILD_AREA
                    else -> if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFF808080)
                }

                // Draw hexagon
                drawHexagon(centerX, centerY, hexSize, color)
                
                // Draw a border around the selected spawn point
                if (isSelected) {
                    drawHexagonBorder(centerX, centerY, hexSize, Color.Yellow, 2f)
                }
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

/**
 * Helper function to draw a hexagon border (pointy-top orientation)
 */
private fun DrawScope.drawHexagonBorder(centerX: Float, centerY: Float, radius: Float, color: Color, strokeWidth: Float) {
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

    drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth))
}
