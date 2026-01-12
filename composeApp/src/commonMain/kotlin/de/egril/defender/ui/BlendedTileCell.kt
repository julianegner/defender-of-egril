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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
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
 * A grid cell that renders tiles with smooth transitions to adjacent tiles.
 * Shows blended edges where adjacent tiles of different types meet.
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
 * @param getNeighborTileType Function to get tile type of neighbor at position (returns null if out of bounds)
 * @param getNeighborTilePainter Function to get tile painter for neighbor (returns null if no image)
 * @param content Additional content to overlay on the cell
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BlendedTileCell(
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
        // Draw blended tile background with smooth transitions
        if (backgroundPainter != null) {
            BlendedTileBackground(
                painter = backgroundPainter,
                hexSize = hexSize,
                position = position,
                tileType = tileType,
                getNeighborTileType = getNeighborTileType,
                getNeighborTilePainter = getNeighborTilePainter
            )
        }
        
        content()
    }
}

/**
 * Renders the tile background with smooth transitions to adjacent tiles.
 * Uses alpha gradients to blend neighbor tiles at hexagon edges.
 */
@Composable
private fun BlendedTileBackground(
    painter: Painter,
    hexSize: Dp,
    position: Position,
    tileType: TileType,
    getNeighborTileType: (Position) -> TileType?,
    getNeighborTilePainter: (Position, TileType) -> Painter?
) {
    val sqrt3 = sqrt(3.0).toFloat()
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val hexSizePx = hexSize.toPx()
        val hexWidthPx = hexSizePx * sqrt3
        val hexHeightPx = hexSizePx * 2f
        
        // Create hexagon clipping path
        val hexPath = createHexagonPath(size)
        
        clipPath(hexPath) {
            // Draw main tile image
            drawIntoCanvas { canvas ->
                with(painter) {
                    draw(size)
                }
            }
            
            // Only blend tiles that should have smooth transitions
            if (shouldBlendTileType(tileType)) {
                // Get all 6 neighbors
                val neighbors = position.getHexNeighbors()
                
                for (neighbor in neighbors) {
                    val neighborType = getNeighborTileType(neighbor)
                    if (neighborType != null && shouldBlendWithNeighbor(tileType, neighborType)) {
                        val neighborPainter = getNeighborTilePainter(neighbor, neighborType)
                        if (neighborPainter != null) {
                            // Calculate edge direction and draw neighbor with alpha gradient
                            drawNeighborBlend(
                                neighborPainter = neighborPainter,
                                currentPos = position,
                                neighborPos = neighbor,
                                hexSizePx = hexSizePx,
                                size = size
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Draws a neighbor tile with alpha gradient blending at the edge.
 * The gradient makes the neighbor tile visible near the shared edge and transparent towards center.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNeighborBlend(
    neighborPainter: Painter,
    currentPos: Position,
    neighborPos: Position,
    hexSizePx: Float,
    size: Size
) {
    // Calculate direction vector from current to neighbor
    val dx = neighborPos.x - currentPos.x
    val dy = neighborPos.y - currentPos.y
    
    // Calculate the angle to the neighbor for gradient orientation
    // For hexagonal grid with odd-row offset, we need to consider the actual hex layout
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    
    // Determine edge direction based on neighbor offset
    // This creates a linear gradient perpendicular to the edge
    val edgeAngle = getEdgeAngle(dx, dy, currentPos.y % 2 == 1)
    
    // Create radial gradient brush centered on the edge between hexagons
    // The gradient should be transparent at center and opaque towards the edge
    val gradientRadius = hexSizePx * 0.6f  // Blend zone extends about 60% from center
    val edgeDistance = hexSizePx * 0.7f    // Edge is about 70% from center
    
    // Calculate gradient center point (towards the neighbor)
    val gradientCenterX = centerX + kotlin.math.cos(edgeAngle) * edgeDistance
    val gradientCenterY = centerY + kotlin.math.sin(edgeAngle) * edgeDistance
    
    val brush = Brush.radialGradient(
        colors = listOf(
            Color.Transparent,
            Color.White.copy(alpha = 0.5f),
            Color.White
        ),
        center = Offset(gradientCenterX, gradientCenterY),
        radius = gradientRadius
    )
    
    // Draw neighbor tile with gradient mask
    // Use graphicsLayer for compositing instead of saveLayer
    drawIntoCanvas { canvas ->
        // Draw neighbor tile first
        with(neighborPainter) {
            draw(size)
        }
        
        // Apply gradient mask using DST_IN blend mode
        drawRect(
            brush = brush,
            blendMode = BlendMode.DstIn
        )
    }
}

/**
 * Calculate the angle (in radians) from the current hex to the neighbor hex.
 * Used to orient the gradient for edge blending.
 */
private fun getEdgeAngle(dx: Int, dy: Int, isOddRow: Boolean): Float {
    // Map neighbor offset to approximate angle
    // For pointy-top hexagons, the 6 edges are at specific angles
    return when {
        // East
        dx == 1 && dy == 0 -> 0f
        // North-East
        (dx == 0 && dy == -1 && !isOddRow) || (dx == 1 && dy == -1 && isOddRow) -> 
            -kotlin.math.PI.toFloat() / 3f
        // North-West
        (dx == -1 && dy == -1 && !isOddRow) || (dx == 0 && dy == -1 && isOddRow) -> 
            -2f * kotlin.math.PI.toFloat() / 3f
        // West
        dx == -1 && dy == 0 -> kotlin.math.PI.toFloat()
        // South-West
        (dx == -1 && dy == 1 && !isOddRow) || (dx == 0 && dy == 1 && isOddRow) -> 
            2f * kotlin.math.PI.toFloat() / 3f
        // South-East
        (dx == 0 && dy == 1 && !isOddRow) || (dx == 1 && dy == 1 && isOddRow) -> 
            kotlin.math.PI.toFloat() / 3f
        else -> 0f
    }
}

/**
 * Determines if a tile type should have smooth transitions with neighbors.
 * According to requirements: PATH, BUILD_AREA, ISLAND, NO_PLAY, and RIVER tiles.
 */
private fun shouldBlendTileType(tileType: TileType): Boolean {
    return when (tileType) {
        TileType.PATH, TileType.BUILD_AREA, TileType.ISLAND, 
        TileType.NO_PLAY, TileType.RIVER -> true
        TileType.SPAWN_POINT, TileType.TARGET -> false
    }
}

/**
 * Determines if two adjacent tile types should blend with each other.
 * Special case: River tiles can show riverbank at edges to non-river tiles.
 */
private fun shouldBlendWithNeighbor(currentType: TileType, neighborType: TileType): Boolean {
    // Don't blend if both tiles are the same type
    if (currentType == neighborType) return false
    
    // Don't blend with spawn points or targets
    if (neighborType == TileType.SPAWN_POINT || neighborType == TileType.TARGET) return false
    
    // Both tiles should be blendable types
    return shouldBlendTileType(currentType) && shouldBlendTileType(neighborType)
}

/**
 * Creates a hexagon path for the given size.
 * Used for clipping the blended tile rendering.
 */
private fun createHexagonPath(size: Size): Path {
    val path = Path()
    val width = size.width
    val height = size.height
    val centerX = width / 2f
    val centerY = height / 2f
    val radius = minOf(width, height) / 2f
    val sqrt3 = sqrt(3.0).toFloat()

    // Top point
    path.moveTo(centerX, centerY - radius)
    // Top-right
    path.lineTo(centerX + radius * sqrt3 / 2f, centerY - radius / 2f)
    // Bottom-right
    path.lineTo(centerX + radius * sqrt3 / 2f, centerY + radius / 2f)
    // Bottom point
    path.lineTo(centerX, centerY + radius)
    // Bottom-left
    path.lineTo(centerX - radius * sqrt3 / 2f, centerY + radius / 2f)
    // Top-left
    path.lineTo(centerX - radius * sqrt3 / 2f, centerY - radius / 2f)
    // Close the path
    path.close()
    
    return path
}
