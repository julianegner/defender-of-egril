package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.audio.GlobalSoundManager
import de.egril.defender.audio.SoundEvent
import de.egril.defender.model.*

/**
 * Handles barricade operations for Spike Tower (level 20+) and Spear Tower (level 10+).
 */
class BarricadeSystem(private val state: GameState) {
    
    /**
     * Calculate health points for a new barricade from a tower.
     * Spike Tower: HP = (tower level - 20) / 2 (minimum 1)
     * Spear Tower: HP = tower level - 10 (minimum 1)
     */
    fun calculateBarricadeHP(tower: Defender): Int {
        return if (tower.type == DefenderType.SPIKE_TOWER) {
            maxOf(1, (tower.level.value - 20) / 2)
        } else {
            maxOf(1, tower.level.value - 10)
        }
    }
    
    /**
     * Check if a tower can build barricades.
     * Spike Tower: level 20+
     * Spear Tower: level 10+
     */
    fun canBuildBarricade(tower: Defender): Boolean {
        return when (tower.type) {
            DefenderType.SPIKE_TOWER -> tower.level.value >= 20
            DefenderType.SPEAR_TOWER -> tower.level.value >= 10
            else -> false
        }
    }
    
    /**
     * Get the range for barricade placement (3 tiles)
     */
    fun getBarricadeRange(): Int = 3
    
    /**
     * Check if a barricade position qualifies as a gate.
     * A gate is a barricade between two buildable tiles (BUILD_AREA or ISLAND) that have towers.
     */
    private fun isGatePosition(barricadePosition: Position): Boolean {
        // Get all 6 hex neighbors
        val neighbors = barricadePosition.getHexNeighbors()
        
        // Count how many neighbors are buildable tiles with towers
        val buildableNeighborsWithTowers = neighbors.count { neighbor ->
            val isBuildable = state.level.isBuildArea(neighbor) || state.level.isBuildIsland(neighbor)
            val hasTower = state.defenders.any { it.position.value == neighbor }
            isBuildable && hasTower
        }
        
        // A gate must be between exactly 2 buildable tiles with towers
        return buildableNeighborsWithTowers >= 2
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
        
        // Check if position is valid (empty path tile within range)
        if (!state.level.isOnPath(barricadePosition)) return false
        
        // Check if position is within 3 tiles range
        val distance = tower.position.value.distanceTo(barricadePosition)
        if (distance > getBarricadeRange()) return false
        
        // Check if position is empty (no attacker, no defender, no existing barricade without reinforcement)
        val hasAttacker = state.attackers.any { !it.isDefeated.value && it.position.value == barricadePosition }
        val hasDefender = state.defenders.any { it.position.value == barricadePosition }
        if (hasAttacker || hasDefender) return false
        
        // Calculate HP to add
        val hpToAdd = calculateBarricadeHP(tower)
        
        // Check if there's already a barricade at this position
        val existingBarricade = state.barricades.find { it.position == barricadePosition }
        
        if (existingBarricade != null) {
            // Reinforce existing barricade
            existingBarricade.reinforce(hpToAdd)
        } else {
            // Check if this should be a gate
            val isGate = isGatePosition(barricadePosition)
            
            // Build new barricade
            val barricade = Barricade(
                id = state.nextBarricadeId.value++,
                position = barricadePosition,
                healthPoints = mutableStateOf(hpToAdd),
                defenderId = towerId,
                isGate = isGate
            )
            state.barricades.add(barricade)
        }
        
        // Play sound
        GlobalSoundManager.playSound(SoundEvent.TOWER_UPGRADED)
        
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
     * Handle enemy attacking a barricade.
     * Returns true if barricade is destroyed, false otherwise.
     * If the barricade falls below 100 HP and has a tower on it, the tower is destroyed.
     */
    fun handleEnemyAttackBarricade(enemy: Attacker, barricade: Barricade, damage: Int): Boolean {
        // Check if barricade had tower support before damage
        val hadTowerSupport = barricade.canSupportTower() && barricade.hasTower()
        
        // Apply damage to barricade
        val wasDestroyed = barricade.takeDamage(damage)
        
        // Add damage effect for visualization
        state.damageEffects.add(
            DamageEffect(
                position = barricade.position,
                damageAmount = damage,
                turnNumber = state.turnNumber.value
            )
        )
        
        // Check if tower base was compromised (fell below 100 HP)
        if (hadTowerSupport && !barricade.canSupportTower() && barricade.hasTower()) {
            // Destroy the tower on this base
            val towerId = barricade.supportedTowerId.value
            if (towerId != null) {
                val tower = state.defenders.find { it.id == towerId }
                if (tower != null) {
                    // Remove tower from game
                    state.defenders.remove(tower)
                    // Clear the reference in barricade
                    barricade.supportedTowerId.value = null
                }
            }
        }
        
        if (wasDestroyed) {
            // If barricade is destroyed and had a tower, remove the tower too
            if (barricade.hasTower()) {
                val towerId = barricade.supportedTowerId.value
                if (towerId != null) {
                    val tower = state.defenders.find { it.id == towerId }
                    if (tower != null) {
                        state.defenders.remove(tower)
                    }
                }
            }
            // Remove destroyed barricade
            state.barricades.remove(barricade)
        }
        
        return wasDestroyed
    }
}
