package de.egril.defender.model

data class DamageEffect(
    val position: Position,
    val damageAmount: Int,
    val turnNumber: Int, // Track which turn this damage occurred for display timing
)

data class BombExplosionEffect(
    val center: Position, // Center of the explosion
    val affectedPositions: List<Position>, // All affected tile positions
    val turnNumber: Int, // Turn when this explosion occurred
)

data class CoinGainEffect(
    val position: Position, // Position of the defeated enemy that awarded coins
    val amount: Int, // Amount of coins gained
    val turnNumber: Int, // Turn when this coin gain occurred
)
