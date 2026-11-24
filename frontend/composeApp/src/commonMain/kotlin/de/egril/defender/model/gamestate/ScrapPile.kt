package de.egril.defender.model

data class ScrapPile(
    val position: Position,
    val ownerAttackerId: Int,
    val hatchTurn: Int,
)
