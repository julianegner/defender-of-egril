package de.egril.defender.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/**
 * Types of bridges that can be built by enemies
 */
enum class BridgeType(
    val displayName: String,
    val maxSpan: Int  // Maximum number of river tiles this bridge can span
) {
    WOODEN("Wooden Bridge", maxSpan = 1),    // Built by Orks
    STONE("Stone Bridge", maxSpan = 2),       // Built by Ogres
    MAGICAL("Magical Bridge", maxSpan = 1)    // Built by Evil Wizards and Ewhad (temporary)
}

/**
 * Represents a bridge built by an enemy unit
 * 
 * Bridges are built over river tiles and allow enemies to cross.
 * - Wooden bridges (Ork): Span 1 river tile, have HP equal to the Ork's HP
 * - Stone bridges (Ogre): Span 1-2 river tiles, have HP equal to the Ogre's HP
 * - Magical bridges (Evil Wizard/Ewhad): Span 1 river tile, no HP, last 3 turns
 * 
 * Bridges do not count towards the enemy count for winning.
 * Units on bridges when destroyed are also destroyed.
 */
data class Bridge(
    val id: Int,
    val type: BridgeType,
    val positions: List<Position>,  // River positions covered by this bridge (1-2 tiles)
    val currentHealth: MutableState<Int> = mutableStateOf(0),  // 0 for magical bridges
    val isDestroyed: MutableState<Boolean> = mutableStateOf(false),
    val turnsRemaining: MutableState<Int> = mutableStateOf(0),  // For magical bridges (3 turns), 0 for others
    val createdByAttackerId: Int,  // ID of the attacker that created this bridge
    val createdOnTurn: Int  // Turn number when bridge was created
) {
    val maxHealth: Int = currentHealth.value
    
    /**
     * Check if this bridge is still active
     */
    val isActive: Boolean get() = !isDestroyed.value && (type != BridgeType.MAGICAL || turnsRemaining.value > 0)
    
    /**
     * Check if a position is covered by this bridge
     */
    fun coversPosition(position: Position): Boolean = positions.contains(position)
    
    /**
     * Decrement turns remaining for magical bridges
     * Returns true if bridge expired
     */
    fun decrementTurn(): Boolean {
        if (type == BridgeType.MAGICAL && turnsRemaining.value > 0) {
            turnsRemaining.value--
            if (turnsRemaining.value <= 0) {
                isDestroyed.value = true
                return true
            }
        }
        return false
    }
    
    /**
     * Take damage and return true if bridge is destroyed
     */
    fun takeDamage(damage: Int): Boolean {
        if (type == BridgeType.MAGICAL) {
            // Magical bridges can't be damaged
            return false
        }
        
        currentHealth.value = maxOf(0, currentHealth.value - damage)
        if (currentHealth.value <= 0) {
            isDestroyed.value = true
            return true
        }
        return false
    }
}
