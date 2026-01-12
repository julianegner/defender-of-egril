package de.egril.defender.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.egril.defender.editor.TileType
import de.egril.defender.model.Position
import de.egril.defender.model.getHexNeighbors
import kotlin.math.sqrt

/**
 * A grid cell that renders tiles with smooth gradient transitions to adjacent tiles.
 * Uses a gradient mask approach where the main tile becomes transparent towards edges,
 * revealing neighbor tiles drawn as background layers.
 * 
 * @param hexSize The size (radius) of the hexagon
 * @param position The grid position of this cell
 * @param tileType The type of tile at this position
 * @param backgroundColor Base background color (fallback if no image)
 * @param borderColor Border color
 * @param borderWidth Border width
 * @param onClick Click callback
 * @param modifier Additional modifiers
 * @param backgroundPainter Painter for the main tile image
 * @param onHover Hover callback
 * @param getNeighborTileType Function to get tile type of neighbor at position
 * @param getNeighborTilePainter Function to get tile painter for neighbor
 * @param content Additional content to overlay on the cell
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GradientBlendedTileCell(
    hexSize: Dp,
    position: Position,
    tileType: TileType,
    backgroundColor: Color,
    borderColor: Color,
    borderWidth: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundPainter: Painter? = null,
    onHover: ((Boolean) -> Unit)? = null,
    getNeighborTileType: (Position) -> TileType?,
    getNeighborTilePainter: (Position, TileType) -> Painter?,
    content: @Composable BoxScope.() -> Unit = { }
) {
    val sqrt3 = sqrt(3.0).toFloat()
    val hexWidth = hexSize.value * sqrt3
    val hexHeight = hexSize.value * 2f

    Box(
        modifier = Modifier
            .width((hexWidth).dp)
            .height((hexHeight).dp)
            .clip(HexagonShape())
            .background(backgroundColor)
            .border(borderWidth, borderColor, HexagonShape())
            .clickable(onClick = onClick)
            .then(
                if (onHover != null) {
                    Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                when (event.type) {
                                    PointerEventType.Move, PointerEventType.Enter -> {
                                        onHover(true)
                                    }
                                    PointerEventType.Exit -> {
                                        onHover(false)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Modifier
                }
            )
            .then(modifier),
        contentAlignment = Alignment.Center
    ) {
        // Draw blended tile background with gradient transitions
        if (backgroundPainter != null && shouldBlendTileType(tileType)) {
            GradientBlendedTileBackground(
                painter = backgroundPainter,
                hexSize = hexSize,
                position = position,
                tileType = tileType,
                getNeighborTileType = getNeighborTileType,
                getNeighborTilePainter = getNeighborTilePainter
            )
        } else if (backgroundPainter != null) {
            // No blending - just draw the tile image normally
            androidx.compose.foundation.Image(
                painter = backgroundPainter,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        }
        
        content()
    }
}

/**
 * Renders the tile background with gradient transitions to neighbor tiles.
 * Neighbors are drawn as background layers, then the main tile is drawn on top
 * with a gradient mask that makes edges transparent (1/3 of hexagon width).
 */
@Composable
private fun GradientBlendedTileBackground(
    painter: Painter,
    hexSize: Dp,
    position: Position,
    tileType: TileType,
    getNeighborTileType: (Position) -> TileType?,
    getNeighborTilePainter: (Position, TileType) -> Painter?
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val hexSizePx = hexSize.toPx()
        
        // Step 1: Draw neighbor tiles as background layers
        val neighbors = position.getHexNeighbors()
        for (neighbor in neighbors) {
            val neighborType = getNeighborTileType(neighbor)
            if (neighborType != null && shouldBlendWithNeighbor(tileType, neighborType)) {
                val neighborPainter = getNeighborTilePainter(neighbor, neighborType)
                if (neighborPainter != null) {
                    // Draw neighbor tile at full opacity as background
                    drawIntoCanvas { canvas ->
                        with(neighborPainter) {
                            draw(size)
                        }
                    }
                }
            }
        }
        
        // Step 2: Draw main tile with gradient mask
        // The gradient makes edges transparent (1/3 of hexagon width)
        drawIntoCanvas { canvas ->
            // Create a radial gradient that is opaque in center and transparent at edges
            // The gradient should affect about 1/3 of the hexagon width (edge zone)
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            
            // Gradient radius: full center is opaque, outer 1/3 fades to transparent
            val opaqueRadius = hexSizePx * 0.55f  // Inner 55% is fully opaque
            val fadeRadius = hexSizePx * 0.85f    // Fades from 55% to 85%
            
            // Create gradient brush using colorStops parameter
            val gradientBrush = Brush.radialGradient(
                0f to Color.White,                    // Fully opaque at center
                0.65f to Color.White,                 // Fully opaque until 65%
                0.85f to Color.White.copy(alpha = 0.5f), // Semi-transparent in fade zone
                1f to Color.Transparent,              // Fully transparent at edges
                center = Offset(centerX, centerY),
                radius = fadeRadius
            )
            
            // Draw the main tile
            with(painter) {
                draw(size)
            }
            
            // Apply gradient mask using DstIn blend mode
            // This makes the tile transparent at edges while keeping center opaque
            drawRect(
                brush = gradientBrush,
                blendMode = BlendMode.DstIn
            )
        }
    }
}

/**
 * Determines if a tile type should have smooth transitions with neighbors.
 * According to requirements: PATH, BUILD_AREA, ISLAND, NO_PLAY tiles.
 */
private fun shouldBlendTileType(tileType: TileType): Boolean {
    return when (tileType) {
        TileType.PATH, TileType.BUILD_AREA, TileType.ISLAND, 
        TileType.NO_PLAY -> true
        TileType.SPAWN_POINT, TileType.TARGET, TileType.RIVER -> false
    }
}

/**
 * Determines if two adjacent tile types should blend with each other.
 */
private fun shouldBlendWithNeighbor(currentType: TileType, neighborType: TileType): Boolean {
    // Don't blend if both tiles are the same type
    if (currentType == neighborType) return false
    
    // Don't blend with spawn points, targets, or rivers
    if (neighborType == TileType.SPAWN_POINT || 
        neighborType == TileType.TARGET || 
        neighborType == TileType.RIVER) return false
    
    // Both tiles should be blendable types
    return shouldBlendTileType(currentType) && shouldBlendTileType(neighborType)
}
