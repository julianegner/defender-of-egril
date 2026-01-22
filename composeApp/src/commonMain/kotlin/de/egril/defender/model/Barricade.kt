package de.egril.defender.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/**
 * Represents a barricade placed by Spike Tower (level 20+) or Spear Tower (level 10+).
 * Barricades block enemy movement and take damage when enemies are adjacent.
 */
data class Barricade(
    val position: Position,
    val healthPoints: MutableState<Int>,
    val defenderId: Int  // Track which defender created this barricade
) {
    /**
     * Check if the barricade is destroyed (health points < 1)
     */
    fun isDestroyed(): Boolean = healthPoints.value < 1
    
    /**
     * Apply damage to the barricade from an attacking enemy.
     * Returns true if the barricade was destroyed by this attack.
     */
    fun takeDamage(damage: Int): Boolean {
        healthPoints.value -= damage
        return isDestroyed()
    }
    
    /**
     * Reinforce the barricade by adding health points.
     */
    fun reinforce(additionalHP: Int) {
        healthPoints.value += additionalHP
    }
}
