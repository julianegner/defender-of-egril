package de.egril.defender.ui.worldmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import de.egril.defender.model.LevelStatus
import de.egril.defender.model.Position
import de.egril.defender.model.WorldLevel
import de.egril.defender.ui.mouseWheelZoom
import de.egril.defender.ui.settings.AppSettings
import de.egril.defender.utils.isPlatformMobile
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Hexagonal World Map View that displays levels as hex tiles with connections
 */
@Composable
fun HexWorldMapView(
    worldLevels: List<WorldLevel>,
    onLevelClicked: (WorldMapLevelInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkMode = AppSettings.isDarkMode.value
    
    // Generate the world map from levels
    val worldMap = remember(worldLevels) {
        WorldMapGenerator.generateWorldMap(worldLevels)
    }
    
    // Pan and zoom state
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    
    // Use rememberUpdatedState to avoid capturing stale offset values in gesture handlers
    val currentOffsetX by rememberUpdatedState(offsetX)
    val currentOffsetY by rememberUpdatedState(offsetY)
    val currentScale by rememberUpdatedState(scale)
    
    // Helper function to find level at screen position
    fun findLevelAtPosition(screenX: Float, screenY: Float): WorldMapLevelInfo? {
        val hexSize = 60f
        val hexWidth = hexSize * sqrt(3.0).toFloat()
        val hexHeightVal = hexHeight(hexSize)
        val verticalSpacing = hexHeightVal * 0.75f
        
        // Calculate map dimensions
        val mapPixelWidth = worldMap.width * hexWidth + hexWidth / 2
        val mapPixelHeight = worldMap.height * verticalSpacing + hexHeightVal / 4
        
        // Calculate the offset to center the map (before scale)
        val baseCenterOffsetX = (containerSize.width - mapPixelWidth) / 2
        val baseCenterOffsetY = (containerSize.height - mapPixelHeight) / 2
        
        // Transform screen coordinates to map coordinates (account for scale and pan)
        val mapX = (screenX - containerSize.width / 2 - currentOffsetX) / currentScale + containerSize.width / 2 - baseCenterOffsetX
        val mapY = (screenY - containerSize.height / 2 - currentOffsetY) / currentScale + containerSize.height / 2 - baseCenterOffsetY
        
        // Find if any level tile contains this point
        for (levelInfo in worldMap.levels) {
            val pos = levelInfo.position
            val offsetXHex = if (pos.y % 2 == 1) hexWidth / 2 else 0f
            val centerX = pos.x * hexWidth + offsetXHex + hexWidth / 2
            val centerY = pos.y * verticalSpacing + hexHeightVal / 2
            
            // Check if click is within hexagon (approximate with circle for simplicity)
            val dx = mapX - centerX
            val dy = mapY - centerY
            val distance = sqrt(dx * dx + dy * dy)
            
            if (distance < hexSize * 0.9f) {
                return levelInfo
            }
        }
        return null
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDarkMode) Color(0xFF1A1A2E) else Color(0xFF87CEEB)) // Sky background
            .onSizeChanged { containerSize = it }
            .mouseWheelZoom(
                containerSize = containerSize,
                scale = scale,
                offsetX = offsetX,
                offsetY = offsetY,
                onScaleChange = { scale = it.coerceIn(0.5f, 2f) },
                onOffsetChange = { x, y -> offsetX = x; offsetY = y }
            )
            .pointerInput(worldMap) {
                detectTapGestures { offset ->
                    val level = findLevelAtPosition(offset.x, offset.y)
                    if (level != null) {
                        onLevelClicked(level)
                    }
                }
            }
            .pointerInput(Unit) {
                var dragStartOffsetX = 0f
                var dragStartOffsetY = 0f
                var cumulativeX = 0f
                var cumulativeY = 0f
                
                detectDragGestures(
                    onDragStart = {
                        dragStartOffsetX = currentOffsetX
                        dragStartOffsetY = currentOffsetY
                        cumulativeX = 0f
                        cumulativeY = 0f
                    },
                    onDrag = { _, dragAmount ->
                        // Accumulate incremental drag amounts
                        cumulativeX += dragAmount.x
                        cumulativeY += dragAmount.y
                        // Apply the cumulative offset from the drag start position
                        offsetX = dragStartOffsetX + cumulativeX
                        offsetY = dragStartOffsetY + cumulativeY
                    }
                )
            }
            .then(
                if (isPlatformMobile) {
                    Modifier.pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(0.5f, 2f)
                            scale = newScale
                            if (newScale > 1f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            }
                        }
                    }
                } else {
                    Modifier
                }
            )
    ) {
        // Draw the hex map
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
        ) {
            val hexSize = 60f
            val hexWidth = hexSize * sqrt(3.0).toFloat()
            val hexHeight = hexSize * 2f
            val verticalSpacing = hexHeight * 0.75f
            
            // Calculate offset to center the map
            val mapPixelWidth = worldMap.width * hexWidth + hexWidth / 2
            val mapPixelHeight = worldMap.height * verticalSpacing + hexHeight / 4
            val centerOffsetX = (size.width - mapPixelWidth) / 2
            val centerOffsetY = (size.height - mapPixelHeight) / 2
            
            // Draw path connections first (behind tiles)
            for ((from, to) in worldMap.pathConnections) {
                val fromCenter = getHexCenter(from, hexSize, hexWidth, verticalSpacing, centerOffsetX, centerOffsetY)
                val toCenter = getHexCenter(to, hexSize, hexWidth, verticalSpacing, centerOffsetX, centerOffsetY)
                drawLine(
                    color = if (isDarkMode) Color(0xFF5C4033) else Color(0xFF8B4513),
                    start = fromCenter,
                    end = toCenter,
                    strokeWidth = 4f
                )
            }
            
            // Draw all tiles
            for ((pos, tile) in worldMap.tiles) {
                val center = getHexCenter(pos, hexSize, hexWidth, verticalSpacing, centerOffsetX, centerOffsetY)
                
                val tileColor = getTileColor(tile, isDarkMode)
                val borderColor = getTileBorderColor(tile, isDarkMode)
                
                drawHexagon(center.x, center.y, hexSize, tileColor)
                drawHexagonBorder(center.x, center.y, hexSize, borderColor)
                
                // Draw special indicators for level tiles
                if (tile.type == WorldMapTileType.LEVEL) {
                    val levelInfo = worldMap.levels.find { it.levelId == tile.levelId }
                    if (levelInfo != null) {
                        // Draw status indicator
                        drawLevelStatusIndicator(center, hexSize, levelInfo.status, isDarkMode)
                        
                        // Draw special symbol for final level, or tower icon for regular levels
                        if (tile.isFinalLevel) {
                            drawFinalLevelSymbol(center, hexSize)
                        } else {
                            drawTowerSymbol(center, hexSize, levelInfo.status, isDarkMode)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Calculate the center position of a hexagon in screen coordinates
 */
private fun getHexCenter(
    pos: Position,
    hexSize: Float,
    hexWidth: Float,
    verticalSpacing: Float,
    centerOffsetX: Float,
    centerOffsetY: Float
): Offset {
    val offsetXHex = if (pos.y % 2 == 1) hexWidth / 2 else 0f
    val centerX = centerOffsetX + pos.x * hexWidth + offsetXHex + hexWidth / 2
    val centerY = centerOffsetY + pos.y * verticalSpacing + hexHeight(hexSize) / 2
    return Offset(centerX, centerY)
}

private fun hexHeight(hexSize: Float) = hexSize * 2f

/**
 * Get the fill color for a tile based on its type
 */
private fun getTileColor(tile: WorldMapTile, isDarkMode: Boolean): Color {
    return when (tile.type) {
        WorldMapTileType.LEVEL -> {
            when {
                tile.isFinalLevel -> if (isDarkMode) Color(0xFF4A0080) else Color(0xFF9B59B6)  // Purple for final
                tile.isTutorialLevel -> if (isDarkMode) Color(0xFF1A5276) else Color(0xFF3498DB)  // Blue for tutorial
                else -> if (isDarkMode) Color(0xFF1E5128) else Color(0xFF27AE60)  // Green for regular levels
            }
        }
        WorldMapTileType.PATH -> if (isDarkMode) Color(0xFF5C4033) else Color(0xFFD4A574)  // Brown path
        WorldMapTileType.MOUNTAIN -> if (isDarkMode) Color(0xFF4A4A4A) else Color(0xFF95A5A6)  // Grey mountain
        WorldMapTileType.RIVER -> if (isDarkMode) Color(0xFF1A3F5C) else Color(0xFF3498DB)  // Blue river
        WorldMapTileType.LAKE -> if (isDarkMode) Color(0xFF1A4A6E) else Color(0xFF2980B9)  // Darker blue lake
        WorldMapTileType.FOREST -> if (isDarkMode) Color(0xFF1E4620) else Color(0xFF27AE60)  // Green forest
        WorldMapTileType.EMPTY -> if (isDarkMode) Color(0xFF2D2D44) else Color(0xFF98D8C8)  // Light green/grey empty
    }
}

/**
 * Get the border color for a tile
 */
private fun getTileBorderColor(tile: WorldMapTile, isDarkMode: Boolean): Color {
    return when (tile.type) {
        WorldMapTileType.LEVEL -> if (isDarkMode) Color(0xFFFFD700) else Color(0xFFD4AC0D)  // Gold border for levels
        WorldMapTileType.PATH -> if (isDarkMode) Color(0xFF8B4513) else Color(0xFFA0522D)  // Brown border
        else -> Color.Transparent
    }
}

/**
 * Draw a filled hexagon
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

/**
 * Draw hexagon border
 */
private fun DrawScope.drawHexagonBorder(centerX: Float, centerY: Float, radius: Float, color: Color) {
    if (color == Color.Transparent) return
    
    val path = Path().apply {
        for (i in 0 until 6) {
            val angle = PI * (60.0 * i - 30.0) / 180.0
            val x = centerX + (radius * cos(angle)).toFloat()
            val y = centerY + (radius * sin(angle)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path, color, style = Stroke(width = 2f))
}

/**
 * Draw status indicator (checkmark, X, or lock)
 */
private fun DrawScope.drawLevelStatusIndicator(
    center: Offset,
    hexSize: Float,
    status: LevelStatus,
    isDarkMode: Boolean
) {
    val indicatorSize = hexSize * 0.25f
    val indicatorY = center.y + hexSize * 0.5f
    
    when (status) {
        LevelStatus.WON -> {
            // Draw green checkmark at bottom of tile (same position as lock)
            drawCircle(
                color = Color(0xFF2ECC71),
                radius = indicatorSize,
                center = Offset(center.x, indicatorY)
            )
            
            // Draw checkmark
            val path = Path().apply {
                moveTo(center.x - indicatorSize * 0.4f, indicatorY)
                lineTo(center.x - indicatorSize * 0.1f, indicatorY + indicatorSize * 0.3f)
                lineTo(center.x + indicatorSize * 0.4f, indicatorY - indicatorSize * 0.3f)
            }
            drawPath(path, Color.White, style = Stroke(width = 2f))
        }
        LevelStatus.LOCKED -> {
            // Draw lock icon at the bottom of the tile
            val lockSize = indicatorSize
            
            // Draw lock background circle
            drawCircle(
                color = Color(0xFF7F8C8D),
                radius = lockSize,
                center = Offset(center.x, indicatorY)
            )
            
            // Draw lock body (rectangle)
            drawRect(
                color = Color.White,
                topLeft = Offset(center.x - lockSize * 0.35f, indicatorY - lockSize * 0.1f),
                size = androidx.compose.ui.geometry.Size(lockSize * 0.7f, lockSize * 0.55f)
            )
            // Draw lock shackle (arc)
            drawArc(
                color = Color.White,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(center.x - lockSize * 0.25f, indicatorY - lockSize * 0.55f),
                size = androidx.compose.ui.geometry.Size(lockSize * 0.5f, lockSize * 0.45f),
                style = Stroke(width = 2f)
            )
        }
        LevelStatus.UNLOCKED -> {
            // No special indicator for unlocked but not yet won
        }
    }
}

/**
 * Draw a simple tower symbol on level tiles
 */
private fun DrawScope.drawTowerSymbol(
    center: Offset,
    hexSize: Float,
    status: LevelStatus,
    isDarkMode: Boolean
) {
    val towerSize = hexSize * 0.4f
    val towerColor = Color.White
    
    // Draw simple tower shape (trapezoid)
    val path = Path().apply {
        val topWidth = towerSize * 0.4f
        val bottomWidth = towerSize * 0.6f
        val height = towerSize * 0.6f
        
        moveTo(center.x - bottomWidth / 2, center.y + height / 2)
        lineTo(center.x + bottomWidth / 2, center.y + height / 2)
        lineTo(center.x + topWidth / 2, center.y - height / 2)
        lineTo(center.x - topWidth / 2, center.y - height / 2)
        close()
    }
    
    drawPath(path, towerColor.copy(alpha = 0.8f))
    drawPath(path, towerColor, style = Stroke(width = 1.5f))
    
    // Add battlements
    val battlement = towerSize * 0.1f
    val topWidth = towerSize * 0.4f
    val top = center.y - towerSize * 0.6f / 2
    for (i in 0..2) {
        val x = center.x - topWidth / 2 + (topWidth / 3) * i
        drawRect(
            color = towerColor,
            topLeft = Offset(x, top - battlement),
            size = androidx.compose.ui.geometry.Size(battlement, battlement)
        )
    }
}

/**
 * Draw special symbol for the final level (Ewhad + Wizard Tower)
 */
private fun DrawScope.drawFinalLevelSymbol(center: Offset, hexSize: Float) {
    val symbolSize = hexSize * 0.5f
    
    // Draw a star/special symbol for the final level
    val starPoints = 5
    val outerRadius = symbolSize
    val innerRadius = symbolSize * 0.5f
    
    val path = Path().apply {
        for (i in 0 until starPoints * 2) {
            val radius = if (i % 2 == 0) outerRadius else innerRadius
            val angle = PI / 2 + (PI * i / starPoints)
            val x = center.x + (radius * cos(angle)).toFloat()
            val y = center.y - (radius * sin(angle)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    
    drawPath(path, Color(0xFFFFD700))  // Gold star
    drawPath(path, Color(0xFFB8860B), style = Stroke(width = 2f))  // Dark gold border
    
    // Add a small wizard hat in the center
    val hatPath = Path().apply {
        val hatHeight = symbolSize * 0.4f
        val hatWidth = symbolSize * 0.3f
        moveTo(center.x, center.y - hatHeight / 2)
        lineTo(center.x - hatWidth / 2, center.y + hatHeight / 3)
        lineTo(center.x + hatWidth / 2, center.y + hatHeight / 3)
        close()
    }
    drawPath(hatPath, Color(0xFF4A0080))  // Purple wizard hat
}
