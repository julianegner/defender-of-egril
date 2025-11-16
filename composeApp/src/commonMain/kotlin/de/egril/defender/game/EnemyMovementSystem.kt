package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.audio.GlobalSoundManager
import de.egril.defender.audio.SoundEvent
import de.egril.defender.model.*

/**
 * Handles enemy spawning, movement, and field effects.
 */
class EnemyMovementSystem(
    private val state: GameState,
    private val pathfinding: PathfindingSystem
) {
    
    /**
     * Gets the initial target for a newly spawned attacker.
     * Returns the first waypoint if waypoints exist, otherwise the final target.
     */
    fun getInitialTarget(): Position {
        return if (state.level.waypoints.isNotEmpty()) {
            state.level.waypoints.first().position
        } else {
            state.level.targetPosition
        }
    }
    
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
            val preferredSpawnPoint = spawnPoints[index % spawnPoints.size]
            
            // Find a free position near the preferred spawn point
            val spawnPos = findFreePositionNear(preferredSpawnPoint)
            
            if (spawnPos == null) {
                // No free position found - skip this enemy for now
                // This should rarely happen unless the map is completely congested
                return@forEachIndexed
            }
            
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
                level = mutableStateOf(plannedSpawn.level),
                currentTarget = mutableStateOf(getInitialTarget())
            )
            state.attackers.add(attacker)
        }
        // Play spawn sound
        GlobalSoundManager.playSound(SoundEvent.ENEMY_SPAWN)
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
    
    /**
     * Find a free position for spawning, starting from the preferred spawn point.
     * If the spawn point is occupied, search neighboring path tiles using hex grid neighbors.
     */
    fun findFreePositionNear(preferredSpawnPoint: Position): Position? {
        // First, check if the preferred spawn point is free
        if (!state.attackers.any { it.position.value == preferredSpawnPoint && !it.isDefeated.value }) {
            return preferredSpawnPoint
        }
        
        // BFS to find nearest free position on path
        val visited = mutableSetOf<Position>()
        val queue = mutableListOf(preferredSpawnPoint)
        visited.add(preferredSpawnPoint)
        
        while (queue.isNotEmpty()) {
            val current = queue.removeAt(0)
            
            // Check all hex neighbors
            val neighbors = current.getHexNeighbors()
            for (neighbor in neighbors) {
                // Skip if already visited
                if (neighbor in visited) continue
                
                // Check if position is valid and on path
                if (neighbor.x < 0 || neighbor.x >= state.level.gridWidth ||
                    neighbor.y < 0 || neighbor.y >= state.level.gridHeight) {
                    continue
                }
                
                // Must be on path or a spawn point
                if (!state.level.isOnPath(neighbor) && !state.level.isSpawnPoint(neighbor)) {
                    continue
                }
                
                visited.add(neighbor)
                
                // Check if position is free
                val isOccupied = state.attackers.any { 
                    it.position.value == neighbor && !it.isDefeated.value 
                }
                
                if (!isOccupied) {
                    return neighbor
                }
                
                // Add to queue for further exploration (limit search depth)
                if (visited.size < 20) {
                    queue.add(neighbor)
                }
            }
        }
        
        return null  // No free position found nearby
    }
    
    /**
     * Move dragon with special rules:
     * - Odd turns (1, 3, 5...): 1 step on path (walking)
     * - Even turns (2, 4, 6...): up to 5 steps (flying), can move over obstacles but must end on path
     */
    /**
     * Calculate dragon movement path for the turn-based movement system.
     * Returns a list of positions the dragon should move through.
     * This increments the dragon's turn counter and calculates movement based on alternating walk/fly pattern.
     */
    fun calculateDragonMovementPath(dragon: Attacker): List<Position> {
        val startPos = dragon.position.value
        dragon.dragonTurnsSinceSpawned.value++
        
        // Determine speed and if flying - alternates every turn
        val isOddTurn = dragon.dragonTurnsSinceSpawned.value % 2 == 1
        val speed = if (isOddTurn) {
            dragon.isFlying.value = false
            1  // Walking on odd turns
        } else {
            dragon.isFlying.value = true
            5  // Flying on even turns
        }
        
        println("=== DRAGON MOVEMENT CALCULATION ===")
        println("Dragon ID: ${dragon.id}")
        println("Turns since spawned: ${dragon.dragonTurnsSinceSpawned.value}")
        println("Is odd turn: $isOddTurn")
        println("Is flying: ${dragon.isFlying.value}")
        println("Speed: $speed")
        println("Start position: $startPos")
        
        val target = state.level.targetPosition
        val result = mutableListOf<Position>()
        
        // For flying, calculate the target position using BFS
        if (dragon.isFlying.value) {
            val currentPos = startPos
            val currentDistToTarget = currentPos.distanceTo(target)
            
            // Get all positions on path within flying range (up to 5 tiles away)
            val reachablePathPositions = mutableListOf<Pair<Position, Int>>()
            
            // BFS to find all positions within 5 hexagonal distance
            val visited = mutableSetOf(currentPos)
            val queue = mutableListOf(Pair(currentPos, 0))
            
            while (queue.isNotEmpty()) {
                val (pos, dist) = queue.removeAt(0)
                
                // Check if this position is on path
                if (pos != currentPos && state.level.isOnPath(pos)) {
                    reachablePathPositions.add(Pair(pos, dist))
                }
                
                // Explore neighbors if we haven't reached max flying distance
                if (dist < speed) {
                    for (neighbor in pos.getHexNeighbors()) {
                        // Check bounds
                        if (neighbor.x < 0 || neighbor.x >= state.level.gridWidth ||
                            neighbor.y < 0 || neighbor.y >= state.level.gridHeight) {
                            continue
                        }
                        
                        if (neighbor !in visited) {
                            visited.add(neighbor)
                            queue.add(Pair(neighbor, dist + 1))
                        }
                    }
                }
            }
            
            println("Flying dragon BFS: visited ${visited.size}, found ${reachablePathPositions.size} path positions")
            
            // Choose the path position that gets us closest to target
            val bestPosition = reachablePathPositions.minByOrNull { (pos, _) ->
                pos.distanceTo(target)
            }?.first
            
            val finalPos = if (bestPosition != null) {
                println("  Best position from BFS: $bestPosition")
                bestPosition
            } else {
                // Fallback: if no reachable path positions, use pathfinding to move along path
                println("  BFS failed, using pathfinding fallback")
                val path = pathfinding.findPath(currentPos, target, dragon)
                if (path.size > 1) {
                    val pathIndex = minOf(speed, path.size - 1)
                    println("  Using path index $pathIndex: ${path[pathIndex]}")
                    path[pathIndex]
                } else {
                    println("  Pathfinding also failed, staying in place")
                    currentPos
                }
            }
            
            // For flying, we move directly to the final position
            if (finalPos != currentPos) {
                result.add(finalPos)
            }
        } else {
            // Walking - follow path normally up to 'speed' tiles
            val path = pathfinding.findPath(startPos, target, dragon)
            println("Walking dragon: path size ${path.size}")
            
            if (path.size > 1) {
                val stepsToTake = minOf(speed, path.size - 1)
                for (i in 1..stepsToTake) {
                    result.add(path[i])
                }
            }
        }
        
        println("Movement path: $result")
        println("===================================")
        return result
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
