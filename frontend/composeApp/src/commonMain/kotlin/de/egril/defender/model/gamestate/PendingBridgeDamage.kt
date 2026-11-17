package de.egril.defender.model

/**
 * Bridge damage that is intentionally deferred until the tower attack animation has fully resolved.
 * This avoids reducing bridge health before the projectile/impact visual has finished.
 */
data class PendingBridgeDamage(
    val position: Position,
    val damage: Int,
)
