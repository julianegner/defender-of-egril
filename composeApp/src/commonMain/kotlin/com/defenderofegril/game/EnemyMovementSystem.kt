package com.defenderofegril.game

import androidx.compose.runtime.mutableStateOf
import com.defenderofegril.model.*

/**
 * Handles enemy spawning, movement, and field effects.
 */
class EnemyMovementSystem(
    private val state: GameState,
    private val pathfinding: PathfindingSystem
) {
    
    fun loadNextWave() {
        if (state.currentWaveIndex.value >= state.level.attackerWaves.size) {
            return
        }
        
        val wave = state.level.attackerWaves[state.currentWaveIndex.value]
        state.attackersToSpawn.addAll(wave.attackers)
        state.currentWaveIndex.value++
        state.spawnCounter.value = 0
    }
    
    fun spawnAttackers() {
        // Use the spawn plan to determine which enemies spawn this turn
        val currentTurn = state.turnNumber.value
        val enemiesToSpawnThisTurn = state.spawnPlan.filter { it.spawnTurn == currentTurn }
        
        if (enemiesToSpawnThisTurn.isEmpty()) return
        
        val spawnPoints = state.level.startPositions
        
        enemiesToSpawnThisTurn.forEachIndexed { index, plannedSpawn ->
            // Use a different spawn point for each enemy (cycle through spawn points)
            val spawnPos = spawnPoints[index % spawnPoints.size]
            
            // Ensure only one Ewhad exists at a time (boss is unique)
            if (plannedSpawn.attackerType == AttackerType.EWHAD) {
                val ewhadExists = state.attackers.any { 
                    it.type == AttackerType.EWHAD && !it.isDefeated.value 
                }
                if (ewhadExists) {
                    // Skip spawning another Ewhad if one already exists
                    return@forEachIndexed
                }
            }
            
            val attacker = Attacker(
                id = state.nextAttackerId.value++,
                type = plannedSpawn.attackerType,
                position = mutableStateOf(spawnPos),
                level = plannedSpawn.level
            )
            state.attackers.add(attacker)
        }
    }
    
    fun findFreeSpawnPosition(): Position? {
        // Try each spawn point to find a free one
        for (spawnPos in state.level.startPositions) {
            if (!state.attackers.any { it.position.value == spawnPos && !it.isDefeated.value }) {
                return spawnPos
            }
        }
        
        // If all spawn points are occupied, try positions along the path near spawn points
        for (spawnPos in state.level.startPositions) {
            val offsets = listOf(
                Position(1, 0), Position(2, 0), Position(3, 0)  // Move along the path to the right
            )
            
            for (offset in offsets) {
                val pos = Position(spawnPos.x + offset.x, spawnPos.y + offset.y)
                if (pos.x >= 0 && pos.x < state.level.gridWidth && 
                    pos.y >= 0 && pos.y < state.level.gridHeight &&
                    state.level.isOnPath(pos) &&
                    !state.attackers.any { it.position.value == pos && !it.isDefeated.value }) {
                    return pos
                }
            }
        }
        
        return null  // No free position found
    }
    
    fun moveAttackers(
        findNearestActiveTower: (Attacker) -> Defender?,
        findPathPositionNearTower: (Position) -> Position
    ) {
        for (attacker in state.attackers) {
            if (attacker.isDefeated.value) continue
            
            // Handle dragon movement separately
            if (attacker.type.isDragon) {
                moveDragon(attacker)
                continue
            }
            
            // Red Witch targets nearest active tower instead of the goal
            val target = if (attacker.type == AttackerType.RED_WITCH) {
                val nearestTower = findNearestActiveTower(attacker)
                if (nearestTower != null) {
                    // Find a path position near the tower
                    findPathPositionNearTower(nearestTower.position)
                } else {
                    state.level.targetPosition
                }
            } else {
                state.level.targetPosition
            }
            
            val path = pathfinding.findPath(attacker.position.value, target, attacker)
            
            if (path.isEmpty()) continue
            
            var remainingSpeed = attacker.type.speed
            var pathIndex = 1 // Skip current position (index 0)
            
            while (remainingSpeed > 0 && pathIndex < path.size) {
                val newPos = path[pathIndex]
                
                // Check if new position is occupied by another alive attacker
                val occupyingAttacker = state.attackers.find {
                    it.id != attacker.id && !it.isDefeated.value && it.position.value == newPos
                }

                if (occupyingAttacker == null) {
                    attacker.position.value = newPos
                    pathIndex++
                } else if (attacker.type == AttackerType.EWHAD) {
                    // Ewhad can swap positions with other units
                    val oldPos = attacker.position.value
                    attacker.position.value = newPos
                    occupyingAttacker.position.value = oldPos
                    pathIndex++
                } else {
                    // Can't move further, stop trying
                    break
                }

                remainingSpeed--

                // Check if reached target
                if (attacker.position.value == target) {
                    state.healthPoints.value--
                    attacker.isDefeated.value = true
                    break
                }
            }
        }
    }
    
    /**
     * Move dragon with special rules:
     * - Turn 1 after spawn: 1 step (walking)
     * - Turn 2+: 5 steps (flying), can move over units and island fields
     */
    private fun moveDragon(dragon: Attacker) {
        dragon.dragonTurnsSinceSpawned.value++
        
        // Determine speed and if flying
        val speed = if (dragon.dragonTurnsSinceSpawned.value == 1) {
            dragon.isFlying.value = false
            1  // Walking on first turn
        } else {
            dragon.isFlying.value = true
            5  // Flying on subsequent turns
        }
        
        val target = state.level.targetPosition
        
        // For flying, we can move directly towards target ignoring path
        if (dragon.isFlying.value) {
            // Get all valid positions to move to (within speed range)
            var currentPos = dragon.position.value
            var remainingSpeed = speed
            
            while (remainingSpeed > 0) {
                // Find next position closer to target
                val neighbors = currentPos.neighbors()
                val nextPos = neighbors.minByOrNull { it.distanceTo(target) } ?: break
                
                // If we're not getting closer, stop
                if (nextPos.distanceTo(target) >= currentPos.distanceTo(target)) {
                    break
                }
                
                currentPos = nextPos
                remainingSpeed--
                
                // Check if reached target
                if (currentPos == target) {
                    state.healthPoints.value--
                    dragon.isDefeated.value = true
                    break
                }
            }
            
            // Check if landing on an enemy unit (eat it)
            val unitAtPosition = state.attackers.find { 
                it.id != dragon.id && !it.isDefeated.value && it.position.value == currentPos
            }
            if (unitAtPosition != null && unitAtPosition.type != AttackerType.EWHAD) {
                // Eat the unit and gain its health
                dragon.currentHealth.value += unitAtPosition.currentHealth.value
                unitAtPosition.isDefeated.value = true
            } else if (unitAtPosition != null && unitAtPosition.type == AttackerType.EWHAD) {
                // Move to adjacent position instead
                val adjacentPositions = currentPos.neighbors()
                val alternatePos = adjacentPositions.firstOrNull { pos ->
                    state.attackers.none { it.position.value == pos && !it.isDefeated.value }
                }
                if (alternatePos != null) {
                    currentPos = alternatePos
                }
            }
            
            dragon.position.value = currentPos
        } else {
            // Walking - follow path normally
            val path = pathfinding.findPath(dragon.position.value, target, dragon)
            if (path.isEmpty()) return
            
            var pathIndex = 1
            var remainingSpeed = speed
            
            while (remainingSpeed > 0 && pathIndex < path.size) {
                val newPos = path[pathIndex]
                
                // Check for unit at position (eat it)
                val unitAtPosition = state.attackers.find {
                    it.id != dragon.id && !it.isDefeated.value && it.position.value == newPos
                }
                
                if (unitAtPosition != null && unitAtPosition.type != AttackerType.EWHAD) {
                    // Eat the unit and gain its health
                    dragon.currentHealth.value += unitAtPosition.currentHealth.value
                    unitAtPosition.isDefeated.value = true
                } else if (unitAtPosition != null && unitAtPosition.type == AttackerType.EWHAD) {
                    // Can't move to Ewhad's position, stop
                    break
                }
                
                dragon.position.value = newPos
                pathIndex++
                remainingSpeed--
                
                // Check if reached target
                if (dragon.position.value == target) {
                    state.healthPoints.value--
                    dragon.isDefeated.value = true
                    break
                }
            }
        }
    }

    fun moveGoblinsAfterSpawn() {
        // Move only goblins that just spawned (those still at spawn points)
        for (attacker in state.attackers) {
            if (attacker.isDefeated.value) continue
            if (attacker.type != AttackerType.GOBLIN) continue

            // Check if goblin is at a spawn point
            if (!state.level.isSpawnPoint(attacker.position.value)) continue

            val target = state.level.targetPosition
            val path = pathfinding.findPath(attacker.position.value, target, attacker)

            if (path.isEmpty() || path.size < 2) continue

            // Move goblin using their speed
            var remainingSpeed = attacker.type.speed
            var pathIndex = 1 // Skip current position (index 0)

            while (remainingSpeed > 0 && pathIndex < path.size) {
                val newPos = path[pathIndex]

                // Check if new position is occupied by another alive attacker
                val isOccupied = state.attackers.any {
                    it.id != attacker.id && !it.isDefeated.value && it.position.value == newPos
                }
                
                if (!isOccupied) {
                    attacker.position.value = newPos
                    pathIndex++
                } else {
                    // Can't move further, stop trying
                    break
                }
                
                remainingSpeed--
                
                // Check if reached target
                if (attacker.position.value == target) {
                    state.healthPoints.value--
                    attacker.isDefeated.value = true
                    break
                }
            }
        }
    }
    
    fun updateFieldEffects() {
        // Decrement turn counters (safe to modify properties)
        state.fieldEffects.forEach { effect ->
            effect.turnsRemaining--
        }
        // Remove expired effects in a separate operation
        state.fieldEffects.removeAll { it.turnsRemaining <= 0 }
    }
}
