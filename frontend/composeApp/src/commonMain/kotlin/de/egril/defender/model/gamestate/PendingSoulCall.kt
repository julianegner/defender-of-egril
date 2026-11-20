package de.egril.defender.model

data class PendingSoulCall(
    val position: Position,
    val attackerType: AttackerType,
    val level: Int,
    val reviveTurn: Int,
    val dragonName: String? = null,
    val currentTarget: Position? = null,
)
