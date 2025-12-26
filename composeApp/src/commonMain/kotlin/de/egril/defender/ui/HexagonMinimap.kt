package de.egril.defender.ui

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
import de.egril.defender.editor.EditorStorage
import de.egril.defender.editor.TileType
import de.egril.defender.model.*
import de.egril.defender.ui.getLocalizedName
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
    containerSize: IntSize? = null,
    contentSize: IntSize? = null
): String {
    // Get map data from editor storage
    val sequence = remember { EditorStorage.getLevelSequence() }
    val editorLevelId = remember(level.editorLevelId) {
        // Use the stored editor level ID if available, otherwise fall back to old behavior for backwards compatibility
        level.editorLevelId ?: if (level.id > 0 && level.id <= sequence.sequence.size) {
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
            containerSize = containerSize,
            contentSize = contentSize
        )
    }
    
    val locale = com.hyperether.resources.currentLanguage.value
    return map.getLocalizedName(locale)
}

/**
 * Hexagon minimap variant that accepts an EditorMap directly (for level editor)
 */
@Composable
fun HexagonMinimapFromEditorMap(
    map: de.egril.defender.editor.EditorMap,
    modifier: Modifier = Modifier,
    config: MinimapConfig = MinimapConfig(),
    scale: Float? = null,
    offsetX: Float? = null,
    offsetY: Float? = null,
    containerSize: IntSize? = null,
    contentSize: IntSize? = null
) {
    val dummyLevel = remember(map.id) {
        Level(
            id = 0,
            name = map.name,
            gridWidth = map.width,
            gridHeight = map.height,
            startPositions = emptyList(),
            targetPositions = listOf(Position(0, 0)),
            pathCells = emptySet(),
            buildIslands = emptySet(),
            attackerWaves = emptyList()
        )
    }


    Box(
        modifier = modifier
            .background(config.backgroundColor)
            .border(2.dp, config.borderColor)
            .padding(4.dp)
    ) {
        HexagonMinimapContent(
            map = map,
            level = dummyLevel,
            config = config,
            gameState = null,
            scale = scale,
            offsetX = offsetX,
            offsetY = offsetY,
            containerSize = containerSize,
            contentSize = contentSize
        )
    }
}

@Composable
private fun HexagonMinimapContent(
    map: de.egril.defender.editor.EditorMap,
    level: Level,
    config: MinimapConfig,
    gameState: GameState?,
    scale: Float?,
    offsetX: Float?,
    offsetY: Float?,
    containerSize: IntSize?,
    contentSize: IntSize?
) {
    val isDarkMode = de.egril.defender.ui.settings.AppSettings.isDarkMode.value
    
    // Cache tile type checks for performance
    val hasSpawnTile = remember(map.tiles) { map.tiles.values.contains(TileType.SPAWN_POINT) }
    val hasTargetTile = remember(map.tiles) { map.tiles.values.contains(TileType.TARGET) }
    
    Box(modifier = Modifier.fillMaxSize()) {
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
                    
                    // Calculate hex center position
                    val offsetXHex = if (row % 2 == 1) hexWidth / 2 else 0.0f
                    val centerX = offsetXCanvas + col * hexWidth + offsetXHex + hexWidth / 2
                    val centerY = offsetYCanvas + row * verticalSpacing + hexHeight / 2
                    
                    // Get color for tile type
                    val color = when (tileType) {
                        TileType.PATH -> if (isDarkMode) Color(0xFF3E3528) else Color(0xFF8B4513)
                        TileType.BUILD_AREA -> if (isDarkMode) Color(0xFF2E5C1A) else Color(0xFF90EE90)
                        TileType.ISLAND -> if (isDarkMode) Color(0xFF1B4D0E) else Color(0xFF228B22)
                        TileType.SPAWN_POINT -> if (config.showSpawnPoints) {
                            if (isDarkMode) Color(0xFF8B0000) else Color(0xFFDC143C)
                        } else {
                            if (isDarkMode) Color(0xFF3E3528) else Color(0xFF8B4513)
                        }
                        TileType.TARGET -> if (config.showTarget) {
                            if (isDarkMode) Color(0xFF1E3A8A) else Color(0xFF4169E1)
                        } else {
                            if (isDarkMode) Color(0xFF3E3528) else Color(0xFF8B4513)
                        }
                        TileType.NO_PLAY -> if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFF808080)
                        TileType.RIVER -> if (isDarkMode) Color(0xFF1E3A5F) else Color(0xFF4682B4)  // Steel blue for water
                    }
                    
                    // Draw hexagon
                    drawHexagon(centerX, centerY, hexSize, color)
                }
            }
            
            // Draw units if gameState is provided and config allows
            // Note: Units use the game's grid coordinate system (gameState.level.gridWidth x gridHeight)
            // Units must be positioned using the same hexagonal offset logic as the map tiles
            if (gameState != null) {
                // Helper function to calculate hexagon center position for a given grid position
                fun getHexCenterPosition(position: Position): Offset {
                    val row = position.y
                    val col = position.x
                    
                    // Calculate hex center position using the same logic as map tile rendering
                    val offsetXHex = if (row % 2 == 1) hexWidth / 2 else 0.0f
                    val centerX = offsetXCanvas + col * hexWidth + offsetXHex + hexWidth / 2
                    val centerY = offsetYCanvas + row * verticalSpacing + hexHeight / 2
                    
                    return Offset(centerX, centerY)
                }
                
                // Draw defenders (towers) - including those on rafts
                if (config.showTowers) {
                    gameState.defenders.forEach { defender ->
                        val center = getHexCenterPosition(defender.position.value)
                        drawCircle(
                            color = Color(0xFF2196F3),  // Blue - same as ready towers on main map
                            radius = hexSize / 2,
                            center = center
                        )
                    }
                }
                
                // Draw rafts specifically (to ensure they're visible even if towers config is off)
                gameState.rafts.filter { it.isActive }.forEach { raft ->
                    val center = getHexCenterPosition(raft.currentPosition.value)
                    drawCircle(
                        color = Color(0xFF2196F3),  // Blue - same color as towers
                        radius = hexSize / 2,
                        center = center
                    )
                }
                
                // Draw attackers (enemies)
                if (config.showEnemies) {
                    gameState.attackers.filter { !it.isDefeated.value }.forEach { attacker ->
                        val center = getHexCenterPosition(attacker.position.value)
                        drawCircle(
                            color = Color.Red,
                            radius = hexSize / 2.5f,
                            center = center
                        )
                    }
                }
                
                // Draw spawn points (if not already shown as tiles)
                if (config.showSpawnPoints && !hasSpawnTile) {
                    level.startPositions.forEach { spawnPos ->
                        val center = getHexCenterPosition(spawnPos)
                        drawCircle(
                            color = Color(0xFFFF9800),  // Orange
                            radius = hexSize / 2,
                            center = center
                        )
                    }
                }
                
                // Draw target positions (if not already shown as tiles)
                if (config.showTarget && !hasTargetTile) {
                    level.targetPositions.forEach { targetPos ->
                        val center = getHexCenterPosition(targetPos)
                        drawCircle(
                            color = Color(0xFF4CAF50),  // Green
                            radius = hexSize / 2,
                            center = center
                        )
                    }
                }
            }
        }
        
        // Viewport indicator (shows current view) - only if all viewport params are provided
        if (config.showViewport && scale != null && offsetX != null && offsetY != null && containerSize != null && contentSize != null) {
            if (containerSize.width > 0 && containerSize.height > 0 && contentSize.width > 0 && contentSize.height > 0) {
                // Calculate what portion of the map is actually visible
                // scaledContentSize is the size of the map at the current zoom level
                val scaledContentWidth = contentSize.width * scale
                val scaledContentHeight = contentSize.height * scale
                
                // Calculate viewport ratio - what fraction of the map is visible
                // When zoomed in, less of the map is visible (smaller ratio)
                // When zoomed out, more of the map is visible (larger ratio, but clamped to 1.0)
                val viewportWidthRatio = (containerSize.width.toFloat() / scaledContentWidth).coerceAtMost(1f)
                val viewportHeightRatio = (containerSize.height.toFloat() / scaledContentHeight).coerceAtMost(1f)

                // Calculate viewport position
                // The viewport can be panned within the bounds of (scaledContentSize - containerSize)
                // maxOffset is how far we can pan in each direction from center
                val maxOffsetX = ((scaledContentWidth - containerSize.width) / 2).coerceAtLeast(0.01f)
                val maxOffsetY = ((scaledContentHeight - containerSize.height) / 2).coerceAtLeast(0.01f)
                
                // Normalize the current offset to 0-1 range (0 = left/top edge, 1 = right/bottom edge)
                // offsetX is positive when panned left (showing right side), negative when panned right (showing left side)
                // So we negate it to get the correct direction
                val normalizedOffsetX = (-offsetX / maxOffsetX).coerceIn(-1f, 1f)
                val normalizedOffsetY = (-offsetY / maxOffsetY).coerceIn(-1f, 1f)

                // Calculate viewport position in minimap
                // The viewport box can move within the area: (1.0 - viewportRatio)
                // normalizedOffset ranges from -1 (left/top) to 1 (right/bottom)
                // We map this to 0 (left/top of minimap) to (1 - viewportRatio) (right/bottom of minimap)
                val viewportX = ((normalizedOffsetX + 1f) / 2f) * (1f - viewportWidthRatio)
                val viewportY = ((normalizedOffsetY + 1f) / 2f) * (1f - viewportHeightRatio)

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
