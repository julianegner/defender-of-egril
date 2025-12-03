package de.egril.defender.model

/**
 * Flow direction for river tiles in a hexagonal grid
 * Using hexagonal neighbor directions
 */
enum class RiverFlow {
    NORTH_EAST,   // +1x, -1y (for even rows) or same x, -1y (for odd rows)
    EAST,         // +1x, 0y
    SOUTH_EAST,   // +1x, +1y (for even rows) or same x, +1y (for odd rows)
    SOUTH_WEST,   // -1x, +1y (for even rows) or same x, +1y (for odd rows)
    WEST,         // -1x, 0y
    NORTH_WEST,   // -1x, -1y (for even rows) or same x, -1y (for odd rows)
    NONE,         // No flow (still water)
    MAELSTROM     // Whirlpool/vortex
}

/**
 * Represents a river tile with flow direction and speed
 */
data class RiverTile(
    val position: Position,
    val flowDirection: RiverFlow = RiverFlow.NONE,
    val flowSpeed: Int = 1  // 1 or 2 (shown as one or two arrows)
) {
    init {
        require(flowSpeed in 1..2) { "Flow speed must be 1 or 2" }
    }
}
