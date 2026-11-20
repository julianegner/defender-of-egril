package de.egril.defender.model

/**
 * Spell targeting mode state
 */
data class SpellTargetingState(
    val activeSpell: SpellType,
    val validTargets: Set<Any> = emptySet(), // Can be Position, Attacker, or Defender depending on spell type
)
