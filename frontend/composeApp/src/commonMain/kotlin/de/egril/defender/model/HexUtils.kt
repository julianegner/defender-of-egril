package de.egril.defender.model

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
 * For hexagons with odd-row offset (pointy-top), we need to convert properly
 */
fun Position.hexDistanceTo(other: Position): Int {
    // For odd-row offset coordinates (pointy-top), convert to cube coordinates
    // Reference: https://www.redblobgames.com/grids/hexagons/#conversions-offset
    
    val col1 = this.x
    val row1 = this.y
    val q1 = col1 - (row1 - (row1 and 1)) / 2
    val r1 = row1
    
    val col2 = other.x
    val row2 = other.y
    val q2 = col2 - (row2 - (row2 and 1)) / 2
    val r2 = row2
    
    // Convert axial to cube coordinates
    val x1 = q1
    val z1 = r1
    val y1 = -x1 - z1
    
    val x2 = q2
    val z2 = r2
    val y2 = -x2 - z2
    
    return (abs(x1 - x2) + abs(y1 - y2) + abs(z1 - z2)) / 2
}

/**
 * Get the 6 neighbors of a hexagon in odd-row offset coordinates (pointy-top)
 * The directions are: E, NE, NW, W, SW, SE
 */
fun Position.getHexNeighbors(): List<Position> {
    // For odd-row offset (pointy-top), neighbor offsets depend on whether row is even or odd
    // Reference: https://www.redblobgames.com/grids/hexagons/#neighbors-offset
    
    val parity = this.y and 1  // 0 for even rows, 1 for odd rows
    
    val directions = if (parity == 0) {
        // Even rows
        listOf(
            Position(1, 0),   // E
            Position(0, -1),  // NE
            Position(-1, -1), // NW
            Position(-1, 0),  // W
            Position(-1, 1),  // SW
            Position(0, 1)    // SE
        )
    } else {
        // Odd rows
        listOf(
            Position(1, 0),   // E
            Position(1, -1),  // NE
            Position(0, -1),  // NW
            Position(-1, 0),  // W
            Position(0, 1),   // SW
            Position(1, 1)    // SE
        )
    }
    
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

/**
 * Get all hexagons within a given radius from this position.
 * Uses hexagonal distance calculation (hexDistanceTo).
 * @param radius The maximum distance (in hex steps) from this position
 * @param gridWidth Maximum grid width (to filter out-of-bounds positions)
 * @param gridHeight Maximum grid height (to filter out-of-bounds positions)
 * @return Set of positions within the given radius (excluding this position)
 */
fun Position.getHexNeighborsWithinRadius(radius: Int, gridWidth: Int, gridHeight: Int): Set<Position> {
    if (radius <= 0) return emptySet()
    
    val result = mutableSetOf<Position>()
    
    // Check all positions within a bounding box and filter by hex distance
    for (dy in -radius..radius) {
        for (dx in -radius - 1..radius + 1) {  // Add 1 to account for hex offset
            val pos = Position(this.x + dx, this.y + dy)
            if (pos != this && 
                pos.x >= 0 && pos.x < gridWidth &&
                pos.y >= 0 && pos.y < gridHeight &&
                this.hexDistanceTo(pos) <= radius) {
                result.add(pos)
            }
        }
    }
    
    return result
}
