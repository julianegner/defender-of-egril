package com.defenderofegril.game

import androidx.compose.runtime.mutableStateOf
import com.defenderofegril.audio.GlobalSoundManager
import com.defenderofegril.audio.SoundEvent
import com.defenderofegril.model.*

/**
 * Manages tower/defender placement, upgrades, undo, and selling operations.
 */
class TowerManager(private val state: GameState) {
    
    fun placeDefender(type: DefenderType, position: Position): Boolean {
        if (!state.canPlaceDefender(type)) return false
        if (isPositionOccupied(position)) return false
        // Cannot place on spawn points or target
        if (state.level.isSpawnPoint(position) || position == state.level.targetPosition) return false
        // Can place in build areas (which now includes path cells)
        if (!state.level.isBuildArea(position)) return false
        
        val buildTime = if (state.phase.value == GamePhase.INITIAL_BUILDING) 0 else type.buildTime
        
        val defender = Defender(
            id = state.nextDefenderId.value++,
            type = type,
            position = position,
            buildTimeRemaining = mutableStateOf(buildTime),
            placedOnTurn = state.turnNumber.value
        )
        state.defenders.add(defender)
        state.coins.value -= type.baseCost
        
        // Play tower placed sound
        GlobalSoundManager.playSound(SoundEvent.TOWER_PLACED)
        
        // Reset actions if tower is ready
        if (defender.isReady) {
            defender.resetActions()
        }
        
        return true
    }
    
    fun upgradeDefender(defenderId: Int): Boolean {
        val defender = state.defenders.find { it.id == defenderId } ?: return false
        if (!state.canUpgradeDefender(defender)) return false
        
        // Store the old actionsPerTurn before upgrade
        val oldActionsPerTurn = defender.actionsPerTurnCalculated
        
        state.coins.value -= defender.upgradeCost
        defender.level.value++
        
        // Play tower upgraded sound
        GlobalSoundManager.playSound(SoundEvent.TOWER_UPGRADED)
        
        // Calculate the new actionsPerTurn after upgrade
        val newActionsPerTurn = defender.actionsPerTurnCalculated
        
        // Only add the increase in actionsPerTurn to actionsRemaining
        val actionIncrease = newActionsPerTurn - oldActionsPerTurn
        if (actionIncrease > 0 && defender.isReady) {
            defender.actionsRemaining.value += actionIncrease
        }
        
        return true
    }
    
    fun undoTower(defenderId: Int): Boolean {
        val defender = state.defenders.find { it.id == defenderId } ?: return false
        
        // Can only undo if placed on current turn and not used
        if (defender.placedOnTurn != state.turnNumber.value) return false
        if (defender.hasBeenUsed.value) return false
        
        // Refund full cost
        state.coins.value += defender.totalCost
        state.defenders.remove(defender)
        
        return true
    }
    
    fun sellTower(defenderId: Int): Boolean {
        val defender = state.defenders.find { it.id == defenderId } ?: return false
        
        // Cannot sell dragon's lair
        if (!defender.canSell) return false
        
        // Can only sell if tower is ready and has actions remaining
        if (!defender.isReady) return false
        if (defender.actionsRemaining.value <= 0) return false
        
        // Refund 75% of total cost
        val refund = (defender.totalCost * 0.75).toInt()
        state.coins.value += refund
        state.defenders.remove(defender)
        
        // Play tower sold sound
        GlobalSoundManager.playSound(SoundEvent.TOWER_SOLD)
        
        return true
    }
    
    private fun isPositionOccupied(position: Position): Boolean {
        return state.defenders.any { it.position == position } ||
               state.attackers.any { it.position.value == position }
    }
}
