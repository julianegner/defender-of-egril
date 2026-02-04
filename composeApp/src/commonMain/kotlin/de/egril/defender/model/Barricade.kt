package de.egril.defender.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/**
 * Represents a barricade placed by Spike Tower (level 20+) or Spear Tower (level 10+).
 * Barricades block enemy movement and take damage when enemies are adjacent.
 * When placed between two buildable tiles with towers, it becomes a gate (different visual).
 * Barricades with at least 100 HP can serve as tower bases.
 */
data class Barricade(
    val id: Int,  // Unique identifier for this barricade
    val position: Position,
    val healthPoints: MutableState<Int>,
    val defenderId: Int,  // Track which defender created this barricade
    val isGate: Boolean = false,  // True if barricade is between two buildable tiles with towers
    val supportedTowerId: MutableState<Int?> = mutableStateOf(null)  // ID of tower placed on this barricade (null if no tower)
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
    
    /**
     * Check if this barricade can support a tower (has at least 100 HP)
     */
    fun canSupportTower(): Boolean = healthPoints.value >= 100
    
    /**
     * Check if this barricade currently supports a tower
     */
    fun hasTower(): Boolean = supportedTowerId.value != null
}
