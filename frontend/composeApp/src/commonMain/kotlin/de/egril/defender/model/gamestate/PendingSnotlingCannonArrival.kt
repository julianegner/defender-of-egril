package de.egril.defender.model

data class PendingSnotlingCannonArrival(
    val targetPosition: Position,
    val thrownCount: Int,
    val turnNumber: Int,
)
