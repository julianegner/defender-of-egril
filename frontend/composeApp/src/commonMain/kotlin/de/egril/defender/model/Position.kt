package de.egril.defender.model

data class Position(val x: Int, val y: Int) {
    fun distanceTo(other: Position): Int {
        // Use hexagonal distance for the game
        return hexDistanceTo(other)
    }
    
    /**
     * Get all 6 hexagonal neighbors
     */
    fun neighbors(): List<Position> {
        return getHexNeighbors()
    }
}
