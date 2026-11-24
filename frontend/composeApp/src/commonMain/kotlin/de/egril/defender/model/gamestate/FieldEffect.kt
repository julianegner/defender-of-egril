package de.egril.defender.model

enum class FieldEffectType {
    FIREBALL, // Visual effect for wizard fireball area
    ACID, // Visual effect for alchemy acid with duration
    WEB, // Araxxa's spreading spider web area
    BURNING_TILE, // Ignis-Va death: burning ground that disables nearby towers
    SHADOW_FOG, // Morvath's shadow veil that hides tile information
}

data class FieldEffect(
    val position: Position,
    val type: FieldEffectType,
    val damage: Int,
    var turnsRemaining: Int,
    val defenderId: Int, // Track which tower created this effect
    val attackerId: Int? = null, // For DOT effects, track which enemy has the effect
)
