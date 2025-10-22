package com.defenderofegril.model

data class Position(val x: Int, val y: Int) {
    /**
     * Calculate hexagonal distance using offset coordinates.
     * In offset coordinates (odd-row offset), hexagons are arranged so that:
     * - Each hex has 6 neighbors
     * - Odd rows are shifted right by 0.5 units
     * 
     * We convert offset to axial (cube) coordinates for distance calculation.
     */
    fun distanceTo(other: Position): Int {
        // Convert offset coordinates to axial coordinates
        val q1 = x - (y - (y and 1)) / 2
        val r1 = y
        
        val q2 = other.x - (other.y - (other.y and 1)) / 2
        val r2 = other.y
        
        // In axial coordinates, the third coordinate s = -q - r
        // Distance in hexagonal grid (cube coordinates)
        return (kotlin.math.abs(q1 - q2) + kotlin.math.abs(r1 - r2) + kotlin.math.abs((q1 + r1) - (q2 + r2))) / 2
    }
}
