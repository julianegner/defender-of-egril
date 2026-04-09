package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.audio.GlobalSoundManager
import de.egril.defender.audio.SoundEvent
import de.egril.defender.model.*
import de.egril.defender.model.DifficultyModifiers
import de.egril.defender.config.LogConfig

/**
 * Manages tower/defender placement, upgrades, undo, and selling operations.
 */
class TowerManager(private val state: GameState) {
    
    fun placeDefender(type: DefenderType, position: Position, instantDeploy: Boolean = false): Boolean {
        if (!state.canPlaceDefender(type)) return false
        
        // Check if position is on a barricade with at least 100 HP (tower base)
        val barricadeAtPosition = state.barricades.find { it.position == position }
        val isOnTowerBase = barricadeAtPosition != null && barricadeAtPosition.canSupportTower()
        
        // If placing on a tower base, allow placement even if position would normally be occupied
        if (!isOnTowerBase) {
            if (isPositionOccupied(position)) return false
        } else {
            // On tower base, check if there's already a tower on this barricade
            if (barricadeAtPosition.hasTower()) return false
        }
        
        // Cannot place on spawn points or any target
        if (state.level.isSpawnPoint(position) || state.level.isTargetPosition(position)) return false
        
        // Check if position is on a river tile (for raft placement)
        val isRiverPlacement = state.level.isRiverTile(position)
        
        // Cannot place Dwarven Mines on rafts (river tiles)
        if (type == DefenderType.DWARVEN_MINE && isRiverPlacement) {
            // Show info message
            state.infoState.value = state.infoState.value.showInfo(InfoType.MINE_ON_RIVER_WARNING)
            return false
        }
        
        // Cannot place barges on still water (NONE or MAELSTROM) river tiles — silently ignore like NO_PLAY
        if (isRiverPlacement) {
            val riverTile = state.level.getRiverTile(position)
            if (riverTile != null && (riverTile.flowDirection == RiverFlow.NONE || riverTile.flowDirection == RiverFlow.MAELSTROM)) {
                return false
            }
        }
        
        // Can place in build areas OR on river tiles (for rafts, except mines) OR on tower bases
        if (!state.level.isBuildArea(position) && !isRiverPlacement && !isOnTowerBase) return false
        
        val buildTime = if (state.phase.value == GamePhase.INITIAL_BUILDING || instantDeploy) 0 else type.buildTime
        
        // Get initial tower level based on difficulty
        val initialLevel = DifficultyModifiers.getInitialTowerLevel(state.difficulty)
        
        val defender = Defender(
            id = state.nextDefenderId.value++,
            type = type,
            position = mutableStateOf(position),
            level = mutableStateOf(initialLevel),
            buildTimeRemaining = mutableStateOf(buildTime),
            placedOnTurn = state.turnNumber.value
        )
        
        // If placed on tower base, link the tower and barricade
        if (isOnTowerBase) {
            defender.towerBaseBarricadeId.value = barricadeAtPosition.id
            barricadeAtPosition.supportedTowerId.value = defender.id
        }
        
        state.defenders.add(defender)
        state.coins.value -= type.baseCost
        
        // Create a raft if placed on river tile (and not on tower base)
        if (isRiverPlacement && !isOnTowerBase) {
            val raftId = createRaft(defender)
            defender.raftId.value = raftId
        }
        
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
        val oldLevel = defender.level.value
        
        state.coins.value -= defender.upgradeCost
        defender.level.value++
        
        // Play tower upgraded sound
        GlobalSoundManager.playSound(SoundEvent.TOWER_UPGRADED)
        
        // Check if spike tower just reached level 10 (spike barbs ability)
        if (defender.type == DefenderType.SPIKE_TOWER && 
            oldLevel < 10 && 
            defender.level.value >= 10 &&
            !defender.hasShownSpikeBarbsTutorial.value) {
            // Show spike barbs tutorial
            state.infoState.value = state.infoState.value.showInfo(InfoType.SPIKE_BARBS_INFO)
            defender.hasShownSpikeBarbsTutorial.value = true
        }
        
        // Check if spike tower just reached level 20 or spear tower just reached level 10 (barricade ability)
        val showBarricadeTutorial = when (defender.type) {
            DefenderType.SPIKE_TOWER -> oldLevel < 20 && defender.level.value >= 20
            DefenderType.SPEAR_TOWER -> oldLevel < 10 && defender.level.value >= 10
            else -> false
        }
        
        if (showBarricadeTutorial && !defender.hasShownBarricadeTutorial.value) {
            // Show barricade tutorial
            state.infoState.value = state.infoState.value.showInfo(InfoType.BARRICADE_INFO)
            defender.hasShownBarricadeTutorial.value = true
        }
        
        // Check if wizard tower just reached level 10 for the first time
        if (defender.type == DefenderType.WIZARD_TOWER && 
            oldLevel < 10 && 
            defender.level.value >= 10 &&
            !defender.hasShownMagicalTrapTutorial.value) {
            // Show magical trap tutorial
            state.infoState.value = state.infoState.value.showInfo(InfoType.MAGICAL_TRAP_INFO)
            defender.hasShownMagicalTrapTutorial.value = true
        }
        
        // Check if wizard or alchemy tower just reached level 20 for the first time (extended area attack)
        if ((defender.type == DefenderType.WIZARD_TOWER || defender.type == DefenderType.ALCHEMY_TOWER) && 
            oldLevel < 20 && 
            defender.level.value >= 20 &&
            !defender.hasShownExtendedAreaTutorial.value) {
            // Show extended area attack tutorial
            state.infoState.value = state.infoState.value.showInfo(InfoType.EXTENDED_AREA_INFO)
            defender.hasShownExtendedAreaTutorial.value = true
        }
        
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
        
        // If tower is on a tower base, clear the barricade's tower reference
        if (defender.isOnTowerBase) {
            val barricadeId = defender.towerBaseBarricadeId.value
            if (barricadeId != null) {
                val barricade = state.barricades.find { it.id == barricadeId }
                if (barricade != null) {
                    barricade.supportedTowerId.value = null
                }
            }
        }
        
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
        
        // If tower is on a tower base, clear the barricade's tower reference
        if (defender.isOnTowerBase) {
            val barricadeId = defender.towerBaseBarricadeId.value
            if (barricadeId != null) {
                val barricade = state.barricades.find { it.id == barricadeId }
                if (barricade != null) {
                    barricade.supportedTowerId.value = null
                }
            }
        }
        
        // Refund 75% of total cost
        val refund = (defender.totalCost * 0.75).toInt()
        state.coins.value += refund
        state.defenders.remove(defender)
        
        // Play tower sold sound
        GlobalSoundManager.playSound(SoundEvent.TOWER_SOLD)
        
        return true
    }
    
    private fun isPositionOccupied(position: Position): Boolean {
        return state.defenders.any { it.position.value == position } ||
               state.attackers.any { it.position.value == position }
    }
    
    /**
     * Create a raft for a defender placed on a river tile.
     * Returns the raft ID.
     */
    private fun createRaft(defender: Defender): Int {
        val raftId = state.nextRaftId.value++
        val raft = Raft(
            id = raftId,
            defenderId = defender.id,
            currentPosition = mutableStateOf(defender.position.value)
        )
        state.rafts.add(raft)
        if (LogConfig.ENABLE_TOWER_LOGGING) {
        println("Created raft $raftId for tower ${defender.type} at ${defender.position.value}")
        }
        return raftId
    }
}
