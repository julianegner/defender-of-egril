package com.defenderofegril.game

import com.defenderofegril.model.*
import kotlin.math.min

class GameEngine(private val state: GameState) {
    
    fun placeDefender(type: DefenderType, position: Position): Boolean {
        if (!state.canPlaceDefender(type)) return false
        if (isPositionOccupied(position)) return false
        // Cannot place on spawn points or target
        if (state.level.isSpawnPoint(position) || position == state.level.targetPosition) return false
        // Can place in build areas (which now includes path cells)
        if (!state.level.isBuildArea(position)) return false
        
        val buildTime = if (state.phase == GamePhase.INITIAL_BUILDING) 0 else type.buildTime
        
        val defender = Defender(
            id = state.nextDefenderId++,
            type = type,
            position = position,
            buildTimeRemaining = buildTime
        )
        state.defenders.add(defender)
        state.coins -= type.baseCost
        
        // Reset actions if tower is ready
        if (defender.isReady) {
            defender.resetActions()
        }
        
        return true
    }
    
    fun upgradeDefender(defenderId: Int): Boolean {
        val defender = state.defenders.find { it.id == defenderId } ?: return false
        if (!state.canUpgradeDefender(defender)) return false
        
        state.coins -= defender.upgradeCost
        defender.level++
        return true
    }
    
    fun startFirstPlayerTurn() {
        if (state.phase != GamePhase.INITIAL_BUILDING) return
        state.phase = GamePhase.PLAYER_TURN
        state.turnNumber = 1  // Start at turn 1 when game begins
        
        // Load first wave
        if (state.currentWaveIndex == 0 && state.attackersToSpawn.isEmpty()) {
            loadNextWave()
        }
        
        // Spawn initial enemies immediately
        spawnInitialEnemies()
        
        // Reset all defender actions
        resetDefenderActions()
    }
    
    private fun spawnInitialEnemies() {
        // Spawn first enemies immediately at different spawn points
        val enemiesToSpawn = minOf(3, state.attackersToSpawn.size)
        val spawnPoints = state.level.startPositions.toMutableList()
        
        repeat(enemiesToSpawn) { index ->
            if (state.attackersToSpawn.isNotEmpty() && spawnPoints.isNotEmpty()) {
                // Use a different spawn point for each enemy
                val spawnPos = spawnPoints[index % spawnPoints.size]
                val type = state.attackersToSpawn.removeAt(0)
                val attacker = Attacker(
                    id = state.nextAttackerId++,
                    type = type,
                    position = spawnPos
                )
                state.attackers.add(attacker)
            }
        }
    }
    
    fun defenderAttack(defenderId: Int, targetId: Int): Boolean {
        val defender = state.defenders.find { it.id == defenderId } ?: return false
        val target = state.attackers.find { it.id == targetId && !it.isDefeated } ?: return false
        
        if (!defender.canAttack(target)) return false
        
        // Perform attack based on type
        when (defender.type.attackType) {
            AttackType.MELEE, AttackType.RANGED -> singleTargetAttack(defender, target)
            AttackType.AOE -> aoeAttack(defender, target)
            AttackType.DOT -> dotAttack(defender, target)
        }
        
        defender.actionsRemaining--
        
        // Process defeated attackers immediately to give coins
        processDefeatedAttackers()
        
        return true
    }
    
    fun endPlayerTurn() {
        if (state.phase != GamePhase.PLAYER_TURN && state.phase != GamePhase.ENEMY_TURN) return
        
        // Only increment turn and process if we're actually starting enemy turn
        if (state.phase == GamePhase.PLAYER_TURN) {
            state.turnNumber++
        }
        
        state.phase = GamePhase.ENEMY_TURN
        
        // Spawn new attackers
        spawnAttackers()
        
        // Move attackers
        moveAttackers()
        
        // Apply damage over time effects
        applyDotEffects()
        
        // Remove defeated attackers and give rewards
        processDefeatedAttackers()
        
        // Check if we should load next wave
        if (state.attackersToSpawn.isEmpty() && state.attackers.isEmpty()) {
            loadNextWave()
        }
        
        // Advance building timers and start next player turn
        advanceBuildTimers()
        state.phase = GamePhase.PLAYER_TURN
        resetDefenderActions()
    }
    
    private fun resetDefenderActions() {
        state.defenders.forEach { it.resetActions() }
    }
    
    private fun advanceBuildTimers() {
        state.defenders.forEach { defender ->
            if (defender.buildTimeRemaining > 0) {
                defender.buildTimeRemaining--
                if (defender.buildTimeRemaining == 0) {
                    defender.resetActions()
                }
            }
        }
    }
    
    private fun isPositionOccupied(position: Position): Boolean {
        return state.defenders.any { it.position == position } ||
               state.attackers.any { it.position == position }
    }
    
    private fun loadNextWave() {
        if (state.currentWaveIndex >= state.level.attackerWaves.size) {
            return
        }
        
        val wave = state.level.attackerWaves[state.currentWaveIndex]
        state.attackersToSpawn.addAll(wave.attackers)
        state.currentWaveIndex++
        state.spawnCounter = 0
    }
    
    private fun spawnAttackers() {
        if (state.attackersToSpawn.isEmpty()) return
        
        state.spawnCounter++
        val wave = state.level.attackerWaves.getOrNull(state.currentWaveIndex - 1) ?: return
        
        if (state.spawnCounter >= wave.spawnDelay) {
            // Find a free position near the start position
            val spawnPosition = findFreeSpawnPosition() ?: return
            
            val type = state.attackersToSpawn.removeAt(0)
            val attacker = Attacker(
                id = state.nextAttackerId++,
                type = type,
                position = spawnPosition
            )
            state.attackers.add(attacker)
            state.spawnCounter = 0
        }
    }
    
    private fun findFreeSpawnPosition(): Position? {
        // Try each spawn point to find a free one
        for (spawnPos in state.level.startPositions) {
            if (!state.attackers.any { it.position == spawnPos && !it.isDefeated }) {
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
                    !state.attackers.any { it.position == pos && !it.isDefeated }) {
                    return pos
                }
            }
        }
        
        return null  // No free position found
    }
    
    private fun singleTargetAttack(defender: Defender, target: Attacker) {
        target.currentHealth -= defender.damage
        if (target.currentHealth <= 0) {
            target.isDefeated = true
        }
    }
    
    private fun aoeAttack(defender: Defender, primaryTarget: Attacker) {
        // Damage primary target and nearby enemies
        val targets = state.attackers.filter { 
            !it.isDefeated && it.position.distanceTo(primaryTarget.position) <= 1 
        }
        
        for (target in targets) {
            target.currentHealth -= defender.damage
            if (target.currentHealth <= 0) {
                target.isDefeated = true
            }
        }
    }
    
    private fun dotAttack(defender: Defender, target: Attacker) {
        // Apply initial damage
        target.currentHealth -= defender.damage
        // Mark for 3 more rounds of DOT
        defender.dotRoundsRemaining[target.id] = 3
        
        if (target.currentHealth <= 0) {
            target.isDefeated = true
        }
    }
    
    private fun applyDotEffects() {
        for (defender in state.defenders) {
            if (defender.type.attackType != AttackType.DOT) continue
            
            val toRemove = mutableListOf<Int>()
            for ((attackerId, rounds) in defender.dotRoundsRemaining) {
                val attacker = state.attackers.find { it.id == attackerId }
                if (attacker != null && !attacker.isDefeated) {
                    attacker.currentHealth -= defender.damage / 2
                    if (attacker.currentHealth <= 0) {
                        attacker.isDefeated = true
                    }
                    
                    if (rounds <= 1) {
                        toRemove.add(attackerId)
                    } else {
                        defender.dotRoundsRemaining[attackerId] = rounds - 1
                    }
                } else {
                    toRemove.add(attackerId)
                }
            }
            
            toRemove.forEach { defender.dotRoundsRemaining.remove(it) }
        }
    }
    
    private fun moveAttackers() {
        for (attacker in state.attackers) {
            if (attacker.isDefeated) continue
            
            val target = state.level.targetPosition
            val path = findPath(attacker.position, target)
            
            if (path.isEmpty()) continue
            
            var remainingSpeed = attacker.type.speed
            var pathIndex = 1 // Skip current position (index 0)
            
            while (remainingSpeed > 0 && pathIndex < path.size) {
                val newPos = path[pathIndex]
                
                // Check if new position is occupied by another alive attacker
                val isOccupied = state.attackers.any { 
                    it.id != attacker.id && !it.isDefeated && it.position == newPos 
                }
                
                if (!isOccupied) {
                    attacker.position = newPos
                    pathIndex++
                } else {
                    // Can't move further, stop trying
                    break
                }
                
                remainingSpeed--
                
                // Check if reached target
                if (attacker.position == target) {
                    state.healthPoints--
                    attacker.isDefeated = true
                    break
                }
            }
        }
    }
    
    // A* pathfinding algorithm
    private fun findPath(start: Position, goal: Position): List<Position> {
        if (start == goal) return listOf(start)
        
        val openSet = mutableSetOf(start)
        val cameFrom = mutableMapOf<Position, Position>()
        val gScore = mutableMapOf(start to 0)
        val fScore = mutableMapOf(start to start.distanceTo(goal))
        
        while (openSet.isNotEmpty()) {
            val current = openSet.minByOrNull { fScore[it] ?: Int.MAX_VALUE } ?: break
            
            if (current == goal) {
                return reconstructPath(cameFrom, current)
            }
            
            openSet.remove(current)
            
            for (neighbor in getNeighbors(current)) {
                val tentativeGScore = (gScore[current] ?: Int.MAX_VALUE) + 1
                
                if (tentativeGScore < (gScore[neighbor] ?: Int.MAX_VALUE)) {
                    cameFrom[neighbor] = current
                    gScore[neighbor] = tentativeGScore
                    fScore[neighbor] = tentativeGScore + neighbor.distanceTo(goal)
                    
                    if (!openSet.contains(neighbor)) {
                        openSet.add(neighbor)
                    }
                }
            }
        }
        
        // No path found, return simple path towards goal
        return listOf(start, moveTowards(start, goal))
    }
    
    private fun reconstructPath(cameFrom: Map<Position, Position>, current: Position): List<Position> {
        val path = mutableListOf(current)
        var node = current
        while (cameFrom.containsKey(node)) {
            node = cameFrom[node]!!
            path.add(0, node)
        }
        return path
    }
    
    private fun getNeighbors(pos: Position): List<Position> {
        // Use hexagonal neighbors instead of square grid
        return pos.getHexNeighbors().filter { neighbor ->
            neighbor.x >= 0 && neighbor.x < state.level.gridWidth &&
            neighbor.y >= 0 && neighbor.y < state.level.gridHeight &&
            state.level.isOnPath(neighbor) &&
            !isBlocked(neighbor)
        }
    }
    
    private fun isBlocked(pos: Position): Boolean {
        // Check if position has a build island (these block enemies)
        return state.level.isBuildIsland(pos)
    }
    
    private fun moveTowards(from: Position, to: Position): Position {
        val dx = to.x - from.x
        val dy = to.y - from.y
        
        return when {
            dx != 0 -> Position(from.x + dx.coerceIn(-1, 1), from.y)
            dy != 0 -> Position(from.x, from.y + dy.coerceIn(-1, 1))
            else -> from
        }
    }
    
    private fun processDefeatedAttackers() {
        val defeated = state.attackers.filter { it.isDefeated && it.position != state.level.targetPosition }
        for (attacker in defeated) {
            state.coins += attacker.type.reward
        }
        state.attackers.removeAll { it.isDefeated }
    }
    
    // Cheat code support for testing
    fun addCoins(amount: Int) {
        state.coins += amount
    }
}
