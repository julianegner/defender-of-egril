package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.audio.GlobalSoundManager
import de.egril.defender.audio.SoundEvent
import de.egril.defender.model.*

/**
 * Handles mine operations including digging, trap building, and dragon spawning.
 */
class MineOperations(private val state: GameState) {
    
    fun performMineDig(mineId: Int): DigOutcome? {
        val mine = state.defenders.find { it.id == mineId && it.type == DefenderType.DWARVEN_MINE } ?: return null
        
        if (!mine.isReady || mine.actionsRemaining.value <= 0) return null
        
        // Roll for outcome
        val outcome = DigOutcome.roll()
        
        // Play dig sound
        GlobalSoundManager.playSound(SoundEvent.MINE_DIG)
        
        // Process outcome
        when (outcome) {
            DigOutcome.DRAGON -> {
                // Dragon awakens - destroy mine and spawn dragon
                spawnDragonFromMine(mine)
                GlobalSoundManager.playSound(SoundEvent.MINE_DRAGON_SPAWN)
            }
            else -> {
                // Add coins
                state.coins.value += outcome.coins
                mine.coinsGenerated.value += outcome.coins
                if (outcome.coins > 0) {
                    GlobalSoundManager.playSound(SoundEvent.MINE_COIN_FOUND)
                }
            }
        }
        
        // Consume action
        mine.actionsRemaining.value--
        mine.hasBeenUsed.value = true
        
        return outcome
    }
    
    /**
     * Perform a forced dig action with a specific outcome (for cheat codes)
     */
    fun performMineDigWithOutcome(outcomeType: DigOutcome): DigOutcome? {
        // Find any dwarven mine on the map
        val mine = state.defenders.find { it.type == DefenderType.DWARVEN_MINE } ?: return null
        
        if (!mine.isReady || mine.actionsRemaining.value <= 0) return null
        
        // Play dig sound
        GlobalSoundManager.playSound(SoundEvent.MINE_DIG)
        
        // Process outcome
        when (outcomeType) {
            DigOutcome.DRAGON -> {
                // Dragon awakens - destroy mine and spawn dragon
                spawnDragonFromMine(mine)
                GlobalSoundManager.playSound(SoundEvent.MINE_DRAGON_SPAWN)
            }
            else -> {
                // Add coins
                state.coins.value += outcomeType.coins
                mine.coinsGenerated.value += outcomeType.coins
                if (outcomeType.coins > 0) {
                    GlobalSoundManager.playSound(SoundEvent.MINE_COIN_FOUND)
                }
            }
        }
        
        // Consume action
        mine.actionsRemaining.value--
        mine.hasBeenUsed.value = true
        
        return outcomeType
    }
    
    /**
     * Build a trap at the specified position
     */
    fun performMineBuildTrap(mineId: Int, trapPosition: Position): Boolean {
        val mine = state.defenders.find { it.id == mineId && it.type == DefenderType.DWARVEN_MINE } ?: return false
        
        if (!mine.isReady || mine.actionsRemaining.value <= 0) return false
        
        // Check if position is within range
        val distance = mine.position.distanceTo(trapPosition)
        if (distance > mine.range) return false
        
        // Check if position is on the path
        if (!state.level.isOnPath(trapPosition)) return false
        
        // Check if there's already a trap at this position
        if (state.traps.any { it.position == trapPosition }) return false
        
        // Check if there's an enemy unit at this position
        if (state.attackers.any { it.position.value == trapPosition && !it.isDefeated.value }) return false
        
        // Create trap with current mine damage
        val trap = Trap(
            position = trapPosition,
            damage = mine.trapDamage,
            mineId = mineId
        )
        
        state.traps.add(trap)
        
        // Play trap built sound
        GlobalSoundManager.playSound(SoundEvent.MINE_TRAP_BUILT)
        
        // Consume action
        mine.actionsRemaining.value--
        mine.hasBeenUsed.value = true
        
        return true
    }
    
    /**
     * Spawn a dragon when a mine is destroyed
     */
    private fun spawnDragonFromMine(mine: Defender) {
        // Get a random dragon name
        val dragonName = DragonNames.getRandomName()
        
        // Spawn dragon first to get its ID
        var dragonHealth = 500 + mine.coinsGenerated.value
        
        // Find the closest target position from the mine
        val closestTarget = state.level.targetPositions.minByOrNull { 
            it.distanceTo(mine.position) 
        } ?: state.level.targetPositions.first()
        
        val dragon = Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.DRAGON,
            position = mutableStateOf(Position(0, 0)), // Temporary position
            level = mutableStateOf(1),
            currentHealth = mutableStateOf(dragonHealth),
            spawnedFromLairId = null,  // Will be set after lair is created
            dragonName = dragonName,
            currentTarget = mutableStateOf(if (state.level.waypoints.isNotEmpty()) {
                state.level.waypoints.first().position
            } else {
                closestTarget
            })
        )
        
        // Update dragon level based on initial health
        dragon.updateDragonLevel()
        
        // Replace mine with dragon's lair and link to dragon
        val lairDefender = Defender(
            id = state.nextDefenderId.value++,
            type = DefenderType.DRAGONS_LAIR,
            position = mine.position,
            buildTimeRemaining = mutableStateOf(0),
            dragonId = mutableStateOf(dragon.id),
            dragonName = dragonName
        )
        state.defenders.remove(mine)
        state.defenders.add(lairDefender)
        
        // Update dragon's spawnedFromLairId
        val dragonWithLair = dragon.copy(spawnedFromLairId = lairDefender.id)
        
        // Find closest position on path to mine
        val pathPositions = state.level.pathCells
        val closestPathPos = pathPositions.minByOrNull { it.distanceTo(mine.position) } ?: return
        
        // Check if there's a unit at that position
        val unitAtPosition = state.attackers.find { 
            it.position.value == closestPathPos && !it.isDefeated.value 
        }
        
        val spawnPosition = if (unitAtPosition != null && unitAtPosition.type == AttackerType.EWHAD) {
            // If Ewhad is there, find an adjacent path tile
            val adjacentPathPositions = pathPositions.filter { 
                it.distanceTo(closestPathPos) == 1 
            }
            adjacentPathPositions.minByOrNull { it.distanceTo(mine.position) } ?: closestPathPos
        } else {
            // Remove the unit if it's not Ewhad - dragon eats it!
            if (unitAtPosition != null) {
                println("Dragon eating ${unitAtPosition.type} on spawn at $closestPathPos, gaining ${unitAtPosition.currentHealth.value} HP")
                dragonHealth += unitAtPosition.currentHealth.value
                dragon.currentHealth.value = dragonHealth  // Update dragon health
                dragon.updateDragonLevel()  // Update level based on health
                unitAtPosition.isDefeated.value = true
            }
            closestPathPos
        }
        
        // Set dragon's actual spawn position
        dragonWithLair.position.value = spawnPosition
        state.attackers.add(dragonWithLair)
    }
    
    /**
     * Check and activate traps when enemies move
     */
    fun checkAndActivateTraps(processDefeated: () -> Unit) {
        val trapsToRemove = mutableListOf<Trap>()
        
        for (trap in state.traps) {
            val enemyAtPosition = state.attackers.find { 
                it.position.value == trap.position && !it.isDefeated.value 
            }
            
            if (enemyAtPosition != null) {
                // Play trap trigger sound
                GlobalSoundManager.playSound(SoundEvent.TRAP_TRIGGERED)
                
                // Deal damage to enemy
                enemyAtPosition.currentHealth.value -= trap.damage
                
                // Check if defeated
                if (enemyAtPosition.currentHealth.value <= 0) {
                    enemyAtPosition.isDefeated.value = true
                }
                
                // Mark trap for removal
                trapsToRemove.add(trap)
            }
        }
        
        // Remove activated traps
        state.traps.removeAll(trapsToRemove)
        
        // Process defeated attackers to give rewards
        processDefeated()
    }
}
