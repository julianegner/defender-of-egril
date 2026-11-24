package de.egril.defender.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/** Gargoyle balance values. Health and damage scale with the gargoyle's level. */
object GargoyleRules {
    const val HEALTH_PER_LEVEL = 500
    const val DAMAGE_PER_LEVEL = 50
    const val ATTACKS_PER_TURN = 2
    const val ATTACK_RANGE = 2
    const val ACTIVE_TURNS = 3

    /** Distance at which an evil wizard or evil mage converts a gargoyle into an active enemy. */
    const val ACTIVATION_RANGE = 2

    /** Towers whose attacks consume their action but never reduce a gargoyle's health. */
    val NO_DAMAGE_DEFENDERS: Set<DefenderType> = setOf(DefenderType.SPIKE_TOWER, DefenderType.SPEAR_TOWER, DefenderType.BOW_TOWER)
}

/**
 * A pre-placed, player-independent guardian. While inactive it sits on its tile, attacks enemies
 * at the end of the player turn and can be targeted by towers. When an evil wizard/mage comes
 * within [GargoyleRules.ACTIVATION_RANGE], it becomes an enemy attacker ([activeAttackerId]) for
 * [GargoyleRules.ACTIVE_TURNS] enemy turns, keeping its current health.
 */
data class Gargoyle(
    val id: Int,
    val position: MutableState<Position>,
    val level: MutableState<Int>,
    val currentHealth: MutableState<Int>,
    val isActive: MutableState<Boolean> = mutableStateOf(false),
    val activeTurnsRemaining: MutableState<Int> = mutableStateOf(0),
    val activeAttackerId: MutableState<Int?> = mutableStateOf(null),
) {
    val maxHealth: Int get() = GargoyleRules.HEALTH_PER_LEVEL * level.value

    val attackDamage: Int get() = GargoyleRules.DAMAGE_PER_LEVEL * level.value
}
