package de.egril.defender.model

data class TowerConstructionEffect(
    val position: Position, // Position of the tower that finished building
    val turnNumber: Int, // Turn when construction completed
)

data class TrapTriggerEffect(
    val position: Position, // Position of the trap that was triggered
    val turnNumber: Int, // Turn when this trap triggered
)

data class MineDigEffect(
    val position: Position, // Position of the mine that was dug
    val turnNumber: Int, // Turn when digging occurred
)
