package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.audio.GlobalSoundManager
import de.egril.defender.audio.SoundEvent
import de.egril.defender.model.*

/**
 * Handles barricade operations for Spike Tower and Spear Tower (level 10+).
 */
class BarricadeSystem(private val state: GameState) {
    
    /**
     * Calculate health points for a new barricade from a tower.
     * HP = tower level - 10 (minimum 1)
     */
    fun calculateBarricadeHP(tower: Defender): Int {
        return maxOf(1, tower.level.value - 10)
    }
    
    /**
     * Check if a tower can build barricades (Spike or Spear, level 10+)
     */
    fun canBuildBarricade(tower: Defender): Boolean {
        return (tower.type == DefenderType.SPIKE_TOWER || tower.type == DefenderType.SPEAR_TOWER) &&
                tower.level.value >= 10
    }
    
    /**
     * Build a new barricade or reinforce an existing one.
     * Returns true if successful.
     */
    fun performBuildBarricade(towerId: Int, barricadePosition: Position): Boolean {
        val tower = state.defenders.find { it.id == towerId } ?: return false
        
        // Check if tower can build barricades
        if (!canBuildBarricade(tower)) return false
        
        // Check if tower has actions remaining
        if (!tower.isReady || tower.actionsRemaining.value <= 0) return false
        
        // Check if position is valid (on path, adjacent to tower)
        if (!state.level.isOnPath(barricadePosition)) return false
        
        val distance = tower.position.value.distanceTo(barricadePosition)
        if (distance != 1) return false  // Must be adjacent
        
        // Calculate HP to add
        val hpToAdd = calculateBarricadeHP(tower)
        
        // Check if there's already a barricade at this position
        val existingBarricade = state.barricades.find { it.position == barricadePosition }
        
        if (existingBarricade != null) {
            // Reinforce existing barricade
            existingBarricade.reinforce(hpToAdd)
        } else {
            // Build new barricade
            val barricade = Barricade(
                position = barricadePosition,
                healthPoints = mutableStateOf(hpToAdd),
                defenderId = towerId
            )
            state.barricades.add(barricade)
        }
        
        // Play sound
        GlobalSoundManager.playSound(SoundEvent.TOWER_PLACED)
        
        // Consume action
        tower.actionsRemaining.value--
        tower.hasBeenUsed.value = true
        
        return true
    }
    
    /**
     * Remove a barricade (player-initiated removal)
     */
    fun removeBarricade(position: Position): Boolean {
        val barricade = state.barricades.find { it.position == position } ?: return false
        state.barricades.remove(barricade)
        return true
    }
    
    /**
     * Check if there's a barricade at a position
     */
    fun getBarricadeAt(position: Position): Barricade? {
        return state.barricades.find { it.position == position }
    }
    
    /**
     * Check if an enemy can attack a barricade (adjacent check)
     */
    fun checkEnemyAdjacentToBarricade(enemyPosition: Position): Barricade? {
        val neighbors = enemyPosition.getHexNeighbors()
        return state.barricades.find { barricade ->
            barricade.position in neighbors && !barricade.isDestroyed()
        }
    }
    
    /**
     * Handle enemy attacking a barricade.
     * Returns the new position for the enemy if barricade is destroyed, null otherwise.
     */
    fun handleEnemyAttackBarricade(enemy: Attacker, barricade: Barricade): Position? {
        // Calculate damage: enemy level (dragons × 5)
        val damage = if (enemy.type.isDragon) {
            enemy.level.value * 5
        } else {
            enemy.level.value
        }
        
        // Apply damage to barricade
        val wasDestroyed = barricade.takeDamage(damage)
        
        if (wasDestroyed) {
            // Remove destroyed barricade
            state.barricades.remove(barricade)
            // Enemy moves to barricade position
            return barricade.position
        }
        
        // Barricade still stands, enemy doesn't move
        return null
    }
}
