package de.egril.defender.model

/**
 * Types of traps that can be placed
 */
enum class TrapType {
    DWARVEN,    // Damage trap from dwarven mine
    MAGICAL     // Teleport trap from wizard tower (level 10+)
}

/**
 * Represents a trap placed by a dwarven mine or wizard tower
 */
data class Trap(
    val position: Position,
    val damage: Int,
    val defenderId: Int,  // Track which defender created this trap (mine or wizard)
    val type: TrapType = TrapType.DWARVEN  // Type of trap
)
