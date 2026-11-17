package de.egril.defender.model

data class EnemySpawnEffect(
    val position: Position, // Spawn position of the newly appeared enemy
    val turnNumber: Int, // Turn when this spawn occurred
    val attackerType: AttackerType? = null, // Spawned enemy type (used to suppress specific spawn visuals)
    val suppressPortalAnimation: Boolean = false,
)

data class EnemyMoveEffect(
    val position: Position, // Tile that the enemy just vacated (movement trail)
    val turnNumber: Int, // Turn when this movement occurred
)

data class EnemyDeathEffect(
    val position: Position, // Position where the enemy was defeated
    val turnNumber: Int, // Turn when this defeat occurred
    val attackerType: AttackerType, // Type of the defeated enemy (for ghost rendering during animation)
    val attackerLevel: Int, // Level of the defeated enemy (for level badge during animation)
)

data class DragonLevelChangeEffect(
    val position: Position, // Dragon's position when its level changed
    val isLevelUp: Boolean, // true = dragon gained levels (ate units), false = lost levels (took damage)
    val turnNumber: Int, // Turn when this change occurred
)

data class GarokkWarCryEffect(
    val position: Position,
    val turnNumber: Int,
)

data class ShadowSpewEffect(
    val sourcePosition: Position, // Xarithon's tile
    val targetPosition: Position, // Center of the 2×2 target area
    val turnNumber: Int, // Turn when Shadow Spew was activated
)

data class MorvathShadowOrbEffect(
    val sourcePosition: Position, // Morvath's tile
    val targetPosition: Position, // Distant fog tile being added
    val turnNumber: Int, // Turn when the orb was launched
    val attackerId: Int, // Morvath's attacker id
)
