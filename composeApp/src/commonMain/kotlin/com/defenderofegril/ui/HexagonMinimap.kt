package com.defenderofegril.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.defenderofegril.editor.EditorStorage
import com.defenderofegril.editor.TileType
import com.defenderofegril.model.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Configuration options for the hexagon minimap
 */
data class MinimapConfig(
    val showSpawnPoints: Boolean = true,
    val showTarget: Boolean = true,
    val showTowers: Boolean = false,
    val showEnemies: Boolean = false,
    val showViewport: Boolean = false,
    val backgroundColor: Color = Color(0xCC000000),
    val borderColor: Color = Color.White,
    val minimapSizeDp: Float = 120f  // Size in dp for viewport calculations
)

/**
 * Unified hexagon-based minimap composable
 * Can be used for:
 * - Level selection (WorldMapScreen) - shows just the map layout
 * - Save game cards (LoadGameScreen) - shows map layout
 * - In-game minimap (GamePlayScreen) - shows map, units, and viewport
 * - Level editor - shows map layout for selection
 */
@Composable
fun HexagonMinimap(
    level: Level,
    modifier: Modifier = Modifier,
    config: MinimapConfig = MinimapConfig(),
    gameState: GameState? = null,
    scale: Float? = null,
    offsetX: Float? = null,
    offsetY: Float? = null,
    containerSize: IntSize? = null
): String {
    // Get map data from editor storage
    val sequence = remember { EditorStorage.getLevelSequence() }
    val editorLevelId = remember(level.id) {
        if (level.id > 0 && level.id <= sequence.sequence.size) {
            sequence.sequence[level.id - 1]
        } else {
            null
        }
    }
    
    val editorLevel = remember(editorLevelId) { 
        editorLevelId?.let { EditorStorage.getLevel(it) }
    }
    val map = remember(editorLevel?.mapId) { 
        editorLevel?.let { EditorStorage.getMap(it.mapId) }
    }
    
    if (map == null) {
        // Fallback display if map is not found
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Map Preview",
                fontSize = 8.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
        return ""
    }
    
    Box(
        modifier = modifier
            .background(config.backgroundColor)
            .border(2.dp, config.borderColor)
            .padding(4.dp)
    ) {
        HexagonMinimapContent(
            map = map,
            level = level,
            config = config,
            gameState = gameState,
            scale = scale,
            offsetX = offsetX,
            offsetY = offsetY,
            containerSize = containerSize
        )
    }
    
    return map.name
}

@Composable
private fun HexagonMinimapContent(
    map: com.defenderofegril.editor.EditorMap,
    level: Level,
    config: MinimapConfig,
    gameState: GameState?,
    scale: Float?,
    offsetX: Float?,
    offsetY: Float?,
    containerSize: IntSize?
) {
    val hexSize = 6.dp.value
    val hexWidth = sqrt(3.0) * hexSize
    val hexHeight = 2.0 * hexSize
    val verticalSpacing = hexHeight * 0.75
    
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw hexagon map tiles
            for (row in 0 until map.height) {
                for (col in 0 until map.width) {
                    val tileType = map.tiles.getOrElse("$col,$row") { TileType.NO_PLAY }
                    
                    // Calculate hex center position
                    val offsetXHex = if (row % 2 == 1) hexWidth / 2 else 0.0
                    val centerX = (col * hexWidth + offsetXHex + hexWidth / 2).toFloat()
                    val centerY = (row * verticalSpacing + hexHeight / 2).toFloat()
                    
                    // Get color for tile type
                    val color = when (tileType) {
                        TileType.PATH -> Color(0xFF8B4513)
                        TileType.BUILD_AREA -> Color(0xFF90EE90)
                        TileType.ISLAND -> Color(0xFF228B22)
                        TileType.SPAWN_POINT -> if (config.showSpawnPoints) Color(0xFFDC143C) else Color(0xFF8B4513)
                        TileType.TARGET -> if (config.showTarget) Color(0xFF4169E1) else Color(0xFF8B4513)
                        TileType.NO_PLAY -> Color(0xFF808080)
                        TileType.WAYPOINT -> Color(0xFFFFD700)
                    }
                    
                    // Draw hexagon
                    drawHexagon(centerX, centerY, hexSize.toFloat(), color)
                }
            }
            
            // Draw units if gameState is provided and config allows
            if (gameState != null) {
                val cellWidth = size.width / level.gridWidth
                val cellHeight = size.height / level.gridHeight
                
                // Draw defenders (towers)
                if (config.showTowers) {
                    gameState.defenders.forEach { defender ->
                        val x = defender.position.x * cellWidth + cellWidth / 2
                        val y = defender.position.y * cellHeight + cellHeight / 2
                        drawCircle(
                            color = Color(0xFF2196F3),  // Blue - same as ready towers on main map
                            radius = cellWidth.coerceAtMost(cellHeight) / 3,
                            center = Offset(x, y)
                        )
                    }
                }
                
                // Draw attackers (enemies)
                if (config.showEnemies) {
                    gameState.attackers.filter { !it.isDefeated.value }.forEach { attacker ->
                        val x = attacker.position.value.x * cellWidth + cellWidth / 2
                        val y = attacker.position.value.y * cellHeight + cellHeight / 2
                        drawCircle(
                            color = Color.Red,
                            radius = cellWidth.coerceAtMost(cellHeight) / 4,
                            center = Offset(x, y)
                        )
                    }
                }
                
                // Draw spawn points (if not already shown as tiles)
                if (config.showSpawnPoints && !map.tiles.values.contains(TileType.SPAWN_POINT)) {
                    level.startPositions.forEach { spawnPos ->
                        val x = spawnPos.x * cellWidth + cellWidth / 2
                        val y = spawnPos.y * cellHeight + cellHeight / 2
                        drawCircle(
                            color = Color(0xFFFF9800),  // Orange
                            radius = cellWidth.coerceAtMost(cellHeight) / 3,
                            center = Offset(x, y)
                        )
                    }
                }
                
                // Draw target position (if not already shown as tiles)
                if (config.showTarget && !map.tiles.values.contains(TileType.TARGET)) {
                    val targetX = level.targetPosition.x * cellWidth + cellWidth / 2
                    val targetY = level.targetPosition.y * cellHeight + cellHeight / 2
                    drawCircle(
                        color = Color(0xFF4CAF50),  // Green
                        radius = cellWidth.coerceAtMost(cellHeight) / 3,
                        center = Offset(targetX, targetY)
                    )
                }
            }
        }
        
        // Viewport indicator (shows current view) - only if all viewport params are provided
        if (config.showViewport && scale != null && offsetX != null && offsetY != null && containerSize != null) {
            if (containerSize.width > 0 && containerSize.height > 0) {
                val viewportWidthRatio = 1f / scale
                val viewportHeightRatio = 1f / scale

                // Calculate normalized offset (-1 to 1 range)
                val maxOffsetX = (containerSize.width * (scale - 1) / 2).coerceAtLeast(0.01f)
                val maxOffsetY = (containerSize.height * (scale - 1) / 2).coerceAtLeast(0.01f)
                val normalizedOffsetX = -offsetX / maxOffsetX
                val normalizedOffsetY = -offsetY / maxOffsetY

                // Calculate viewport position in minimap
                val viewportX = (normalizedOffsetX * (1f - viewportWidthRatio) / 2f + (1f - viewportWidthRatio) / 2f)
                val viewportY = (normalizedOffsetY * (1f - viewportHeightRatio) / 2f + (1f - viewportHeightRatio) / 2f)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            clip = true
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(viewportWidthRatio)
                            .fillMaxHeight(viewportHeightRatio)
                            .align(Alignment.TopStart)
                            .offset(
                                x = config.minimapSizeDp.dp * viewportX,
                                y = config.minimapSizeDp.dp * viewportY
                            )
                            .border(2.dp, Color.Yellow)  // Yellow border for viewport
                    )
                }
            }
        }
    }
}

/**
 * Draw a hexagon at the specified center position with the given radius and color
 */
private fun DrawScope.drawHexagon(centerX: Float, centerY: Float, radius: Float, color: Color) {
    val path = Path().apply {
        for (i in 0 until 6) {
            val angle = PI * (60.0 * i - 30.0) / 180.0
            val x = centerX + (radius * cos(angle)).toFloat()
            val y = centerY + (radius * sin(angle)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path, color)
}
