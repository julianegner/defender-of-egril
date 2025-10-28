package com.defenderofegril.model

/**
 * Represents a trap placed by a dwarven mine
 */
data class Trap(
    val position: Position,
    val damage: Int,
    val mineId: Int  // Track which mine created this trap
)
