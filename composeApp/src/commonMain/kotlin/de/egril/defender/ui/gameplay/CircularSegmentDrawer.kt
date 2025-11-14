package de.egril.defender.ui.gameplay

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import de.egril.defender.model.Position
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Helper for drawing circular segment arcs on neighbor tiles.
 * Uses circular segment calculation as described in: https://en.wikipedia.org/wiki/Circular_segment
 */
object CircularSegmentDrawer {
    
    /**
     * Calculate the angle from center position to neighbor position in radians.
     * Used for determining the orientation of circular segments.
     * 
     * @param center The central position
     * @param neighbor The neighbor position
     * @return The angle in radians
     */
    fun calculateAngleToNeighbor(center: Position, neighbor: Position): Float {
        val hexSize = 40f
        val hexWidth = hexSize * sqrt(3f)
        val verticalSpacing = hexSize * 2f * 0.75f
        
        // Calculate pixel offsets
        val dx = (neighbor.x - center.x).toFloat()
        val dy = (neighbor.y - center.y).toFloat()
        
        var offsetX = dx * hexWidth
        var offsetY = dy * verticalSpacing
        
        // Adjust for hexagonal offset (even-q vertical layout)
        val neighborRowOffset = if (neighbor.y % 2 == 1) hexWidth * 0.42f else 0f
        val centerRowOffset = if (center.y % 2 == 1) hexWidth * 0.42f else 0f
        offsetX += (neighborRowOffset - centerRowOffset)
        
        // Calculate angle using atan2
        return atan2(offsetY, offsetX)
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
        val dx = (centerPos.x - neighborPos.x).toFloat()
        val dy = (centerPos.y - neighborPos.y).toFloat()

        val hexSize = 40f
        val hexWidth = hexSize * sqrt(3f)
        val verticalSpacing = hexSize * 2f * 0.75f

        var offsetX = (dx * hexWidth)
        var offsetY = (dy * verticalSpacing)

        val neighborRowOffset = if (neighborPos.y % 2 == 1) hexWidth * 0.42f else 0f
        val centerRowOffset = if (centerPos.y % 2 == 1) hexWidth * 0.42f else 0f
        offsetX += (centerRowOffset - neighborRowOffset)

        val tileCenterX = drawScope.size.width / 2
        val tileCenterY = drawScope.size.height / 2

        val arcCenterX = tileCenterX + offsetX
        val arcCenterY = tileCenterY + offsetY

        drawScope.drawCircle(
            color = color, // .copy(alpha = 0.3f),
            radius = radius,
            center = Offset(arcCenterX, arcCenterY),
            style = Stroke(width = strokeWidth)
        )
    }
}
