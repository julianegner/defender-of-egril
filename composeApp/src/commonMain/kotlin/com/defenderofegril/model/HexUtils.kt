package com.defenderofegril.model

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Hexagon utilities based on Red Blob Games hexagon guide
 * Using axial coordinates for pointy-top hexagons
 * https://www.redblobgames.com/grids/hexagons/
 * 
 * Coordinate System:
 * - Uses axial coordinates (q, r) where q=x and r=y
 * - Pointy-top orientation (vertical points at top and bottom)
 * - Odd rows are offset to the right by half a hexagon width
 * 
 * Visualization:
 *    /\    /\    /\        Row 0 (even - no offset)
 *   /  \  /  \  /  \
 *  |0,0||1,0||2,0|
 *   \  /  \  /  \  /
 *    \/    \/    \/
 *      /\    /\    /\     Row 1 (odd - offset right)
 *     /  \  /  \  /  \
 *    |0,1||1,1||2,1|
 *     \  /  \  /  \  /
 *      \/    \/    \/
 * 
 * Each hexagon has 6 neighbors: E, NE, NW, W, SW, SE
 */

/**
 * Calculate Manhattan distance between two positions on a hexagonal grid
 * For hexagons, we use cube coordinates for distance calculation
 */
fun Position.hexDistanceTo(other: Position): Int {
    // Convert axial (q, r) to cube (x, y, z) coordinates
    // In our case: q = position.x, r = position.y
    val x1 = this.x
    val z1 = this.y
    val y1 = -x1 - z1
    
    val x2 = other.x
    val z2 = other.y
    val y2 = -x2 - z2
    
    return (abs(x1 - x2) + abs(y1 - y2) + abs(z1 - z2)) / 2
}

/**
 * Get the 6 neighbors of a hexagon in axial coordinates (pointy-top)
 * The directions are: E, NE, NW, W, SW, SE
 */
fun Position.getHexNeighbors(): List<Position> {
    // Pointy-top hexagon directions in axial coordinates (q, r)
    val directions = listOf(
        Position(1, 0),   // E
        Position(1, -1),  // NE
        Position(0, -1),  // NW
        Position(-1, 0),  // W
        Position(-1, 1),  // SW
        Position(0, 1)    // SE
    )
    
    return directions.map { dir ->
        Position(this.x + dir.x, this.y + dir.y)
    }
}

/**
 * Convert axial coordinates to pixel position for rendering
 * @param hexSize the size of the hexagon (distance from center to corner)
 * @return Pair of (x, y) pixel coordinates
 */
fun Position.hexToPixel(hexSize: Float): Pair<Float, Float> {
    val x = hexSize * (sqrt(3.0).toFloat() * this.x + sqrt(3.0).toFloat() / 2 * this.y)
    val y = hexSize * (3f / 2f * this.y)
    return Pair(x, y)
}

/**
 * Calculate the offset for even-row or odd-row hexagon rendering
 * For pointy-top hexagons with axial coordinates
 */
fun Position.getHexRowOffset(): Float {
    // For pointy-top hexagons, odd rows are shifted right by half a hex width
    return if (this.y % 2 == 1) sqrt(3.0).toFloat() / 2f else 0f
}
