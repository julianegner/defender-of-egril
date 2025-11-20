package de.egril.defender.ui.gameplay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntSize
import de.egril.defender.model.Position
import de.egril.defender.model.getHexNeighbors
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Overlay that draws target rings centered on the selected target tile.
 * Uses the Red Blob Games hexagon ring algorithm to calculate ring positions.
 * https://www.redblobgames.com/grids/hexagons/#rings
 * 
 * This overlay draws 3 concentric rings centered on the target tile, with arc segments
 * only visible on neighboring path tiles.
 */
@Composable
fun TargetRingsOverlay(
    targetPosition: Position,
    pathNeighbors: List<Position>,
    color: Color,
    hexSize: Float,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    containerSize: IntSize,
    contentSize: IntSize,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        // Calculate the center of the target tile in window coordinates
        val centerOffset = calculateTileCenterInWindow(
            position = targetPosition,
            hexSize = hexSize,
            scale = scale,
            offsetX = offsetX,
            offsetY = offsetY,
            containerSize = containerSize,
            contentSize = contentSize
        )
        
        // Draw 3 concentric rings with arc segments on path tiles
        val radii = listOf(
            TargetCircleConstants.OUTER_CIRCLE_1_RADIUS,
            TargetCircleConstants.OUTER_CIRCLE_2_RADIUS,
            TargetCircleConstants.OUTER_CIRCLE_3_RADIUS
        )
        
        radii.forEach { radius ->
            // For each path neighbor, draw the arc segment of this ring
            pathNeighbors.forEach { neighbor ->
                drawArcSegmentForNeighbor(
                    centerPos = targetPosition,
                    neighborPos = neighbor,
                    centerOffset = centerOffset,
                    radius = radius * scale,
                    color = color,
                    strokeWidth = TargetCircleConstants.OUTER_CIRCLE_STROKE_WIDTH * scale,
                    hexSize = hexSize,
                    scale = scale
                )
            }
        }
    }
}

/**
 * Calculate the center of a tile in window coordinates.
 * Takes into account hexagonal grid layout, pan, zoom, and row offset.
 * 
 * The hexagonal grid uses the following layout (from HexagonalMapView.kt):
 * - Tiles are arranged in rows with hexWidth horizontal spacing (minus overlap)
 * - Odd rows are offset by hexWidth * 0.42f to the right
 * - Rows have verticalSpacing between them
 * - The entire content is then transformed via graphicsLayer (scale + translate)
 */
private fun calculateTileCenterInWindow(
    position: Position,
    hexSize: Float,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    containerSize: IntSize,
    contentSize: IntSize
): Offset {
    val sqrt3 = sqrt(3f)
    val hexWidth = hexSize * sqrt3
    val hexHeight = hexSize * 2f
    val verticalSpacing = hexHeight * 0.75f
    
    // Calculate position in content coordinates (before transformation)
    // X position: tile column * hexWidth + rowOffset + center of hex
    val rowOffset = if (position.y % 2 == 1) hexWidth * 0.42f else 0f
    val horizontalSpacing = hexWidth - 10f  // From Row arrangement in HexagonalMapView
    val contentX = position.x * horizontalSpacing + rowOffset + hexWidth / 2
    
    // Y position: tile row * verticalSpacing + vertical adjustment + center of hex
    // Note: Row arrangement has (-hexHeight + verticalSpacing - 7f).dp spacing
    val rowSpacing = verticalSpacing - 7f
    val contentY = position.y * rowSpacing + hexHeight / 2
    
    // Apply transformation: scale around origin, then translate
    // The graphicsLayer transformation is: scale * content + translation
    val scaledX = contentX * scale
    val scaledY = contentY * scale
    
    // Translation is relative to the container center
    // Final window coordinates
    val windowX = scaledX + offsetX + containerSize.width / 2
    val windowY = scaledY + offsetY + containerSize.height / 2
    
    return Offset(windowX, windowY)
}

/**
 * Draw an arc segment of a ring on a neighbor tile.
 * The arc is part of a circle centered on the target tile.
 */
private fun DrawScope.drawArcSegmentForNeighbor(
    centerPos: Position,
    neighborPos: Position,
    centerOffset: Offset,
    radius: Float,
    color: Color,
    strokeWidth: Float,
    hexSize: Float,
    scale: Float
) {
    // Calculate the direction from center to neighbor
    val dx = (neighborPos.x - centerPos.x).toFloat()
    val dy = (neighborPos.y - centerPos.y).toFloat()
    
    val sqrt3 = sqrt(3f)
    val hexWidth = hexSize * sqrt3
    val verticalSpacing = hexSize * 2f * 0.75f
    
    // Calculate offset for hexagonal grid
    var offsetDx = dx * hexWidth
    var offsetDy = dy * verticalSpacing
    
    // Adjust for row offset (odd/even rows)
    val neighborRowOffset = if (neighborPos.y % 2 == 1) hexWidth * 0.42f else 0f
    val centerRowOffset = if (centerPos.y % 2 == 1) hexWidth * 0.42f else 0f
    offsetDx += (neighborRowOffset - centerRowOffset)
    
    // Calculate angle from center to neighbor (in radians)
    val angleRad = atan2(offsetDy, offsetDx)
    val angleDeg = (angleRad * 180 / PI).toFloat()
    
    // Calculate arc span (approximately 60 degrees for hexagon, with some overlap)
    val arcSpan = 64f
    val startAngle = angleDeg - arcSpan / 2
    
    // Draw the arc segment
    drawArc(
        color = color,
        startAngle = startAngle,
        sweepAngle = arcSpan,
        useCenter = false,
        topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
        style = Stroke(width = strokeWidth)
    )
}
