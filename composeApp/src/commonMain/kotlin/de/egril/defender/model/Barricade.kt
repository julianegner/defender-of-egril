package de.egril.defender.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/**
 * Represents a barricade placed by Spike Tower (level 20+) or Spear Tower (level 10+).
 * Barricades block enemy movement and take damage when enemies are adjacent.
 * When placed between two buildable tiles with towers, it becomes a gate (different visual).
 */
data class Barricade(
    val position: Position,
    val healthPoints: MutableState<Int>,
    val defenderId: Int,  // Track which defender created this barricade
    val isGate: Boolean = false  // True if barricade is between two buildable tiles with towers
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
