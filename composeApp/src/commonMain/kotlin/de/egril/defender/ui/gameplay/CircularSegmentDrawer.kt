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
 * Helper for drawing circular arc segments on neighbor tiles.
 * Draws only the portion of a ring that overlaps with a specific tile.
 */
object CircularSegmentDrawer {
    
    /**
     * Draw a circular arc segment on a neighbor tile.
     * The arc is part of a larger circle centered on the target tile.
     * Only the portion of the circle that passes through this tile is drawn.
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
        // Calculate hexagonal grid dimensions
        val sqrt3 = sqrt(3f)
        val hexWidth = hexSize * sqrt3
        val verticalSpacing = hexSize * 2f * 0.75f
        
        // Calculate direction from neighbor to center (for positioning the arc center)
        val dx = (centerPos.x - neighborPos.x).toFloat()
        val dy = (centerPos.y - neighborPos.y).toFloat()
        
        // Calculate offset in pixels, accounting for row offsets
        // Horizontal spacing has -10dp overlap (from HexagonalMapView.kt line 292)
        val horizontalSpacing = hexWidth - 10f
        var offsetX = dx * horizontalSpacing
        var offsetY = dy * verticalSpacing
        
        val neighborRowOffset = if (neighborPos.y % 2 == 1) hexWidth * 0.42f else 0f
        val centerRowOffset = if (centerPos.y % 2 == 1) hexWidth * 0.42f else 0f
        offsetX += (centerRowOffset - neighborRowOffset)
        
        // Calculate tile center
        val tileCenterX = drawScope.size.width / 2
        val tileCenterY = drawScope.size.height / 2
        
        // Calculate where the center of the target tile is relative to this tile
        val arcCenterX = tileCenterX + offsetX
        val arcCenterY = tileCenterY + offsetY
        
        // Calculate the angle from center TO this neighbor tile (opposite of offset direction)
        val angleRad = atan2(-offsetY, -offsetX)
        val angleDeg = (angleRad * 180 / PI).toFloat()
        
        // Calculate arc span - cover approximately 60 degrees (one hexagon face) plus overlap
        val arcSpan = 70f
        val startAngle = angleDeg - arcSpan / 2
        
        // Draw the arc segment
        drawScope.drawArc(
            color = color,
            startAngle = startAngle,
            sweepAngle = arcSpan,
            useCenter = false,
            topLeft = Offset(arcCenterX - radius, arcCenterY - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth)
        )
    }
}
