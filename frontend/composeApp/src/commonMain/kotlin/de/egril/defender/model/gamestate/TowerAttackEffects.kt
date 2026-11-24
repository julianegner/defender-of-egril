package de.egril.defender.model

data class TowerAttackEffect(
    val targetPosition: Position, // Position of the attacked tile
    val turnNumber: Int, // Turn when this attack occurred
)

data class ArrowAttackEffect(
    val sourcePosition: Position, // Tower's tile (source of the arrow)
    val targetPosition: Position, // Target tile
    val turnNumber: Int, // Turn when this attack occurred
)

data class BallistaAttackEffect(
    val sourcePosition: Position, // Ballista tower's tile
    val targetPosition: Position, // Target tile
    val turnNumber: Int, // Turn when this attack occurred
)

data class BowAttackEffect(
    val sourcePosition: Position, // Bow tower's tile
    val targetPosition: Position, // Target tile
    val turnNumber: Int, // Turn when this attack occurred
)

data class SpearAttackEffect(
    val sourcePosition: Position, // Spear tower's tile
    val targetPosition: Position, // Target tile
    val turnNumber: Int, // Turn when this attack occurred
)

data class PikeAttackEffect(
    val sourcePosition: Position, // Pike (spike) tower's tile
    val targetPosition: Position, // Target tile
    val turnNumber: Int, // Turn when this attack occurred
)

data class WizardAttackEffect(
    val sourcePosition: Position, // Wizard tower's tile
    val targetPosition: Position, // Target tile
    val turnNumber: Int, // Turn when this attack occurred
)

data class AlchemyAttackEffect(
    val sourcePosition: Position, // Alchemy tower's tile
    val targetPosition: Position, // Target tile
    val turnNumber: Int, // Turn when this attack occurred
)

data class RocketAttackEffect(
    val sourcePosition: Position,
    val targetPosition: Position,
    val turnNumber: Int,
)

data class SnotlingCannonThrowEffect(
    val sourcePosition: Position,
    val targetPosition: Position,
    val thrownCount: Int,
    val turnNumber: Int,
)
