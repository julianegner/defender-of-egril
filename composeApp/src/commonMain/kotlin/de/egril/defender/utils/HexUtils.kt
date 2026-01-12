package de.egril.defender.utils

// Kotlin
import androidx.compose.ui.geometry.Offset
import de.egril.defender.model.Position
import de.egril.defender.ui.HexagonalGridConstants
import kotlin.math.sqrt

/**
 * Converts a screen position to hex grid Position.
 * This function matches the actual rendering layout from HexagonalMapView.kt
 *
 * @param pointerPos Screen position (Offset)
 * @param offsetX Current pan offset X
 * @param offsetY Current pan offset Y
 * @param zoomLevel Current zoom level
 * @param hexSize Size of hexagon (in dp or px, must match rendering)
 * @return Position? - null if out of bounds
 */
fun screenToHexGridPosition(
    pointerPos: Offset,
    offsetX: Float,
    offsetY: Float,
    zoomLevel: Float,
    hexSize: Float
): Position? {
    // Adjust for pan and zoom to get content-space coordinates
    val px = (pointerPos.x - offsetX) / zoomLevel
    val py = (pointerPos.y - offsetY) / zoomLevel

    // Hex geometry (matching HexagonalMapView.kt)
    val sqrt3 = sqrt(3f)
    val hexWidth = hexSize * sqrt3
    val hexHeight = (hexSize * 2f)
    val verticalSpacing = hexHeight * 0.75f
    
    // Layout spacing (using HexagonalGridConstants to match HexagonalMapView.kt)
    val rowSpacing = -hexHeight + verticalSpacing + HexagonalGridConstants.VERTICAL_SPACING_ADJUSTMENT  // -27.0 for hexSize=40
    val colSpacing = HexagonalGridConstants.HORIZONTAL_SPACING
    val oddRowOffset = hexWidth * HexagonalGridConstants.ODD_ROW_OFFSET_RATIO
    
    // Calculate row (y) from vertical position
    // Formula (accounting for Row.offset modifier and Arrangement.spacedBy):
    // Each row is placed at: y * (hexHeight + rowSpacing)
    // Then offset by: -(y-1) = -y + 1
    // Hex center is at: hexHeight/2
    // Combined: yPixel = y * (hexHeight + rowSpacing) - y + 1 + hexHeight/2
    //                  = y * (hexHeight + rowSpacing - 1) + 1 + hexHeight/2
    // Reverse: y = (yPixel - 1 - hexHeight/2) / (hexHeight + rowSpacing - 1)
    val yApprox = (py - 1f - hexHeight / 2f) / (hexHeight + rowSpacing - 1f)
    val y = kotlin.math.round(yApprox).toInt()
    
    // Calculate column (x) from horizontal position, accounting for odd row offset
    // Formula (even row): xPixel = x * (hexWidth + colSpacing) + hexWidth/2
    // Formula (odd row):  xPixel = oddRowOffset + x * (hexWidth + colSpacing) + hexWidth/2
    val halfHexWidth = hexWidth * 0.5f

    val xApprox = if (y % 2 == 0) {
        (px - halfHexWidth) / (hexWidth + colSpacing)
    } else {
        (px - oddRowOffset - halfHexWidth) / (hexWidth + colSpacing)
    }
    val x = kotlin.math.round(xApprox).toInt()
    
    return Position(x, y)
}
