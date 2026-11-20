package de.egril.defender.model

enum class HealingEffectType {
    GREEN_WITCH, // Visual effect for green witch healing
}

data class HealingEffect(
    val position: Position,
    val type: HealingEffectType,
    val healAmount: Int,
    val turnNumber: Int, // Track which turn this healing occurred for display timing
)
