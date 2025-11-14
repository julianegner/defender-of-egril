package com.defenderofegril.ui.gameplay

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.defenderofegril.model.Position
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Helper for drawing circular segment arcs on neighbor tiles.
 * Uses circular segment calculation as described in: https://en.wikipedia.org/wiki/Circular_segment
 */
object CircularSegmentDrawer {
    
    /**
     * Calculate the angle from the center tile to a neighbor tile in hexagonal grid.
     * Returns angle in radians where 0 is East, PI/2 is North, PI is West, -PI/2 is South.
     */
    fun calculateAngleToNeighbor(centerPos: Position, neighborPos: Position): Float {
        val dx = (neighborPos.x - centerPos.x).toFloat()
        // For hexagonal grid with odd-row offset, adjust for the vertical offset
        val dy = (neighborPos.y - centerPos.y).toFloat()
        
        // For odd-row offset hexagons, we need to adjust x based on row parity
        val adjustedDx = if (centerPos.y % 2 == 0 && neighborPos.y % 2 == 1) {
            dx - 0.5f
        } else if (centerPos.y % 2 == 1 && neighborPos.y % 2 == 0) {
            dx + 0.5f
        } else {
            dx
        }
        
        // Standard atan2 gives angle from positive x-axis, counter-clockwise
        // We need to flip Y because screen coordinates have Y increasing downward
        return atan2(-dy, adjustedDx)
    }
    
    /**
     * Draw a circular arc segment on a neighbor tile.
     * The arc is part of a larger circle centered on the target tile.
     * 
     * @param drawScope The DrawScope to draw in
     * @param color The color of the arc
     * @param radius The radius of the full circle (centered on target tile)
     * @param strokeWidth The width of the arc stroke
     * @param centerPos The position of the central target tile
     * @param neighborPos The position of this neighbor tile
     * @param hexSize The size of a hexagon (for calculating offsets)
     */
    fun drawArcSegment(
        drawScope: DrawScope,
        color: Color,
        radius: Float,
        strokeWidth: Float,
        centerPos: Position,
        neighborPos: Position,
        hexSize: Float
    ) {
        // Calculate the angle from center to this neighbor
        val angleToNeighbor = calculateAngleToNeighbor(centerPos, neighborPos)
        
        // For a hexagonal grid, each neighbor covers approximately 60 degrees (PI/3 radians)
        // We'll draw an arc that spans a bit more to ensure good visual coverage
        val arcSpan = (PI / 2.8).toFloat()  // About 64 degrees - slightly more than 60 to ensure coverage
        val startAngle = (angleToNeighbor - arcSpan / 2) * 180f / PI.toFloat()
        val sweepAngle = arcSpan * 180f / PI.toFloat()
        
        // Calculate the offset from this tile's center to the target tile's center
        // This determines where to center the arc
        val dx = (centerPos.x - neighborPos.x).toFloat()
        val dy = (centerPos.y - neighborPos.y).toFloat()
        
        // Adjust for hexagonal grid offset
        val adjustedDx = if (neighborPos.y % 2 == 0 && centerPos.y % 2 == 1) {
            dx + 0.5f
        } else if (neighborPos.y % 2 == 1 && centerPos.y % 2 == 0) {
            dx - 0.5f
        } else {
            dx
        }
        
        // Convert grid distances to pixel distances
        // For hexagonal grids, horizontal distance is hexWidth per column
        // Vertical distance is verticalSpacing (hexHeight * 0.75) per row
        val hexWidth = hexSize * sqrt(3f)
        val verticalSpacing = hexSize * 2f * 0.75f
        
        val offsetX = adjustedDx * hexWidth
        val offsetY = dy * verticalSpacing
        
        // Center of this tile's drawing area
        val tileCenterX = drawScope.size.width / 2
        val tileCenterY = drawScope.size.height / 2
        
        // Center of the arc circle (offset from this tile's center)
        val arcCenterX = tileCenterX + offsetX
        val arcCenterY = tileCenterY + offsetY
        
        // Draw the arc
        // The arc is defined by a bounding box (top-left, size) and angles
        drawScope.drawArc(
            color = color,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(arcCenterX - radius, arcCenterY - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth)
        )
    }
}
