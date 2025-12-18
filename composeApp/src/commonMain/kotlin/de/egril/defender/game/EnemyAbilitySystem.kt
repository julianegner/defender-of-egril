package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*
import kotlin.math.min

/**
 * Handles special enemy abilities like summoning demons, healing, disabling towers, and building bridges.
 */
class EnemyAbilitySystem(private val state: GameState) {
    
    private val bridgeSystem = BridgeSystem(state)
    
    fun processEnemyAbilities() {
        // Create a snapshot of attackers to avoid ConcurrentModificationException
        // when spawning new demons during iteration
        val attackersSnapshot = state.attackers.toList()
        
        for (attacker in attackersSnapshot) {
            if (attacker.isDefeated.value) continue
            
            // Decrement summon cooldown
            if (attacker.summonCooldown.value > 0) {
                attacker.summonCooldown.value--
            }
            
            when (attacker.type) {
                AttackerType.EVIL_MAGE -> {
                    // Only summon if cooldown is 0
                    if (attacker.summonCooldown.value == 0) {
                        // Summon 1 blue demon per level
                        repeat(attacker.level.value) {
                            spawnDemonNear(attacker, AttackerType.BLUE_DEMON, state.turnNumber.value)
                        }
                        // Summon red demons (level / 2)
                        repeat(attacker.level.value / 2) {
                            spawnDemonNear(attacker, AttackerType.RED_DEMON, state.turnNumber.value)
                        }
                        // Set cooldown to 3 turns (summons every 3 turns)
                        attacker.summonCooldown.value = 3
                    }
                }
                AttackerType.EWHAD -> {
                    // Only summon if cooldown is 0
                    if (attacker.summonCooldown.value == 0) {
                        // Ewhad spawns double the demons of a regular evil mage
                        repeat(attacker.level.value * 2) {
                            spawnDemonNear(attacker, AttackerType.BLUE_DEMON, state.turnNumber.value)
                        }
                        repeat(attacker.level.value) {
                            spawnDemonNear(attacker, AttackerType.RED_DEMON, state.turnNumber.value)
                        }
                        // Additional 3 undead
                        repeat(3) {
                            spawnUndeadNear(attacker, 10 + state.turnNumber.value)
                        }
                        // Set cooldown to 3 turns (summons every 3 turns)
                        attacker.summonCooldown.value = 3
                    }
                }
                AttackerType.GREEN_WITCH -> {
                    // Heal adjacent units
                    val adjacentPositions = attacker.position.value.getHexNeighbors()
                    for (adjacent in adjacentPositions) {
                        val adjacentEnemy = state.attackers.find { 
                            !it.isDefeated.value && it.id != attacker.id && it.position.value == adjacent 
                        }
                        if (adjacentEnemy != null) {
                            val healAmount = min(attacker.level.value, adjacentEnemy.maxHealth - adjacentEnemy.currentHealth.value)
                            adjacentEnemy.currentHealth.value += healAmount
                        }
                    }
                }
                AttackerType.RED_WITCH -> {
                    // Disable nearby tower (instead of moving to target)
                    disableNearestTower(attacker)
                }
                else -> {
                    // Check if this unit should build a bridge
                    // Units build bridges when adjacent to rivers blocking their path
                    if (attacker.type.canBuildBridge && !attacker.isBuildingBridge.value) {
                        if (bridgeSystem.shouldAutoBuildBridge(attacker)) {
                            bridgeSystem.autoBuildBridge(attacker)
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Spawn a demon near the given attacker (1-2 cells away)
     */
    private fun spawnDemonNear(summoner: Attacker, demonType: AttackerType, level: Int) {
        val summonerPos = summoner.position.value
        
        // Try to find a free position 1-2 cells away
        val possiblePositions = mutableListOf<Position>()
        
        // Get positions 1 cell away
        possiblePositions.addAll(summonerPos.getHexNeighbors())
        
        // Get positions 2 cells away
        for (neighbor in summonerPos.getHexNeighbors()) {
            possiblePositions.addAll(neighbor.getHexNeighbors())
        }
        
        // Filter valid positions (on path, not occupied, within bounds)
        val validPositions = possiblePositions.filter { pos ->
            pos.x >= 0 && pos.x < state.level.gridWidth &&
            pos.y >= 0 && pos.y < state.level.gridHeight &&
            state.level.isOnPath(pos) &&
            !state.attackers.any { it.position.value == pos && !it.isDefeated.value }
        }.distinct()
        
        if (validPositions.isEmpty()) return
        
        // Pick a random position
        val spawnPos = validPositions.random()
        
        // Inherit the summoner's current target so demons follow the same waypoint chain
        val inheritedTarget = summoner.currentTarget?.value ?: if (state.level.waypoints.isNotEmpty()) {
            // Use the first waypoint's next target, not the waypoint position itself
            state.level.waypoints.first().nextTarget
        } else {
            state.level.targetPositions.first()
        }
        
        val demon = Attacker(
            id = state.nextAttackerId.value++,
            type = demonType,
            position = mutableStateOf(spawnPos),
            level = mutableStateOf(level),
            currentTarget = mutableStateOf(inheritedTarget)
        )
        state.attackers.add(demon)
    }
    
    /**
     * Spawn an undead (skeleton) near the given attacker
     */
    private fun spawnUndeadNear(summoner: Attacker, level: Int) {
        spawnDemonNear(summoner, AttackerType.SKELETON, level)
    }
    
    /**
     * Red Witch disables the nearest active tower of her level or less
     */
    private fun disableNearestTower(witch: Attacker) {
        // Find nearest tower that:
        // - Is ready (not building)
        // - Is not already disabled
        // - Has level <= witch level
        val eligibleTowers = state.defenders.filter { tower ->
            tower.isReady && 
            !tower.isDisabled.value && 
            tower.level.value <= witch.level.value
        }
        
        if (eligibleTowers.isEmpty()) return
        
        // Find closest tower
        val nearestTower = eligibleTowers.minByOrNull { tower ->
            tower.position.value.distanceTo(witch.position.value)
        }
        
        if (nearestTower != null) {
            // Disable for 3 turns
            nearestTower.isDisabled.value = true
            nearestTower.disabledTurnsRemaining.value = 3
        }
    }
    
    /**
     * Update tower disable status - decrement timers and re-enable towers
     */
    fun updateTowerDisableStatus() {
        for (tower in state.defenders) {
            if (tower.isDisabled.value) {
                tower.disabledTurnsRemaining.value--
                if (tower.disabledTurnsRemaining.value <= 0) {
                    tower.isDisabled.value = false
                }
            }
        }
    }
    
    /**
     * Find the nearest active tower for Red Witch to target
     */
    fun findNearestActiveTower(witch: Attacker): Defender? {
        val eligibleTowers = state.defenders.filter { tower ->
            tower.isReady && !tower.isDisabled.value && tower.level.value <= witch.level.value
        }
        
        if (eligibleTowers.isEmpty()) return null
        
        return eligibleTowers.minByOrNull { tower ->
            tower.position.value.distanceTo(witch.position.value)
        }
    }
    
    /**
     * Find a position on the path near the target tower for Red Witch to move towards
     */
    fun findPathPositionNearTower(towerPosition: Position): Position {
        // Find all path positions adjacent to the tower
        val adjacentPathPositions = towerPosition.getHexNeighbors().filter { pos ->
            pos.x >= 0 && pos.x < state.level.gridWidth &&
            pos.y >= 0 && pos.y < state.level.gridHeight &&
            state.level.isOnPath(pos)
        }
        
        // Return the first adjacent path position, or tower position if none found
        return adjacentPathPositions.firstOrNull() ?: towerPosition
    }
}
