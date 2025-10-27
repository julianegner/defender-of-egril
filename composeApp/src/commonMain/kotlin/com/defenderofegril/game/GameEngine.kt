package com.defenderofegril.game

import androidx.compose.runtime.mutableStateOf
import com.defenderofegril.model.*
import kotlin.math.min

class GameEngine(private val state: GameState) {
    
    companion object {
        // DOT damage is applied at half the initial damage per turn
        private const val DOT_DAMAGE_DIVISOR = 2
    }

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
        
        // Reset actions if tower is ready
        if (defender.isReady) {
            defender.resetActions()
        }
        
        return true
    }
    
    fun upgradeDefender(defenderId: Int): Boolean {
        val defender = state.defenders.find { it.id == defenderId } ?: return false
        if (!state.canUpgradeDefender(defender)) return false
        
        state.coins.value -= defender.upgradeCost
        defender.level.value++
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
        
        // Can only sell if tower is ready and has actions remaining
        if (!defender.isReady) return false
        if (defender.actionsRemaining.value <= 0) return false
        
        // Refund 75% of total cost
        val refund = (defender.totalCost * 0.75).toInt()
        state.coins.value += refund
        state.defenders.remove(defender)
        
        return true
    }
    
    fun startFirstPlayerTurn() {
        if (state.phase.value != GamePhase.INITIAL_BUILDING) return
        state.phase.value = GamePhase.PLAYER_TURN
        state.turnNumber.value = 1  // Start at turn 1 when game begins
        
        // Load first wave
        if (state.currentWaveIndex.value == 0 && state.attackersToSpawn.isEmpty()) {
            loadNextWave()
        }
        
        // Spawn initial enemies immediately
        spawnInitialEnemies()
        
        // Reset all defender actions
        resetDefenderActions()
    }
    
    private fun spawnInitialEnemies() {
        // Spawn 6 enemies initially (2x the number of spawn points)
        val enemiesToSpawn = minOf(6, state.attackersToSpawn.size)
        val spawnPoints = state.level.startPositions
        
        repeat(enemiesToSpawn) { index ->
            if (state.attackersToSpawn.isNotEmpty()) {
                // Use a different spawn point for each enemy (cycle through spawn points)
                val spawnPos = spawnPoints[index % spawnPoints.size]
                val type = state.attackersToSpawn.removeAt(0)
                val attacker = Attacker(
                    id = state.nextAttackerId.value++,
                    type = type,
                    position = mutableStateOf(spawnPos)
                )
                state.attackers.add(attacker)
            }
        }

        // Move goblins immediately after initial spawning (this is not during enemy turn)
        moveGoblinsAfterSpawn()
    }
    
    fun defenderAttack(defenderId: Int, targetId: Int): Boolean {
        val defender = state.defenders.find { it.id == defenderId } ?: return false
        val target = state.attackers.find { it.id == targetId && !it.isDefeated.value } ?: return false
        
        if (!defender.canAttack(target)) return false
        
        // Mark defender as used
        defender.hasBeenUsed.value = true
        
        // Perform attack based on type
        when (defender.type.attackType) {
            AttackType.MELEE, AttackType.RANGED -> singleTargetAttack(defender, target)
            AttackType.AOE -> aoeAttack(defender, target.position.value)
            AttackType.DOT -> dotAttack(defender, target.position.value)
        }

        defender.actionsRemaining.value--

        // Process defeated attackers immediately to give coins
        processDefeatedAttackers()

        return true
    }

    fun defenderAttackPosition(defenderId: Int, targetPosition: Position): Boolean {
        val defender = state.defenders.find { it.id == defenderId } ?: return false

        // Check if defender can reach the target position
        val distance = defender.position.distanceTo(targetPosition)
        if (distance < defender.type.minRange || distance > defender.range) return false
        if (!defender.isReady || defender.actionsRemaining.value <= 0) return false
        
        // Mark defender as used
        defender.hasBeenUsed.value = true

        // For AOE and DOT attacks, target position must be on the path
        if (defender.type.attackType == AttackType.AOE || defender.type.attackType == AttackType.DOT) {
            if (!state.level.isOnPath(targetPosition)) return false
        } else {
            // For single-target attacks, there must be an enemy at the position
            val target = state.attackers.find { it.position.value == targetPosition && !it.isDefeated.value }
            if (target == null) return false
        }

        // Perform attack based on type
        when (defender.type.attackType) {
            AttackType.MELEE, AttackType.RANGED -> {
                // Single target attack requires an enemy
                val target = state.attackers.find { it.position.value == targetPosition && !it.isDefeated.value }
                if (target != null) {
                    singleTargetAttack(defender, target)
                } else {
                    return false
                }
            }
            AttackType.AOE -> aoeAttack(defender, targetPosition)
            AttackType.DOT -> dotAttack(defender, targetPosition)
        }
        
        defender.actionsRemaining.value--
        
        // Process defeated attackers immediately to give coins
        processDefeatedAttackers()
        
        return true
    }
    
    fun endPlayerTurn() {
        if (state.phase.value != GamePhase.PLAYER_TURN && state.phase.value != GamePhase.ENEMY_TURN) return
        
        // Only increment turn and process if we're actually starting enemy turn
        if (state.phase.value == GamePhase.PLAYER_TURN) {
            state.turnNumber.value++
        }
        
        state.phase.value = GamePhase.ENEMY_TURN
        
        // Spawn new attackers
        spawnAttackers()
        
        // Move attackers
        moveAttackers()
        
        // Apply damage over time effects
        applyDotEffects()
        
        // Update field effects
        updateFieldEffects()

        // Remove defeated attackers and give rewards
        processDefeatedAttackers()
        
        // Check if we should load next wave
        if (state.attackersToSpawn.isEmpty() && state.attackers.isEmpty()) {
            loadNextWave()
        }
        
        // Advance building timers and start next player turn
        advanceBuildTimers()
        state.phase.value = GamePhase.PLAYER_TURN
        resetDefenderActions()
    }
    
    /**
     * Calculate all movement steps for attackers during enemy turn without applying them.
     * Returns a list of movement steps, where each step contains all movements that should happen together.
     * Uses a simulated approach to handle collisions between units moving simultaneously.
     */
    fun calculateEnemyTurnMovements(): List<List<Pair<Int, Position>>> {
        val allMovementSteps = mutableListOf<List<Pair<Int, Position>>>()
        
        // Get all non-defeated attackers
        val movingAttackers = state.attackers.filter { !it.isDefeated.value }.toMutableList()
        
        if (movingAttackers.isEmpty()) return allMovementSteps
        
        // Track current positions for collision detection during simulation
        val currentPositions = mutableMapOf<Int, Position>()
        movingAttackers.forEach { currentPositions[it.id] = it.position.value }
        
        // Find the maximum speed to know how many steps to simulate
        val maxSpeed = movingAttackers.maxOfOrNull { it.type.speed } ?: 0
        
        // Simulate movement step by step
        for (stepIndex in 0 until maxSpeed) {
            val movementsInThisStep = mutableListOf<Pair<Int, Position>>()
            val positionsToOccupy = mutableSetOf<Position>()
            
            for (attacker in movingAttackers) {
                val currentPos = currentPositions[attacker.id] ?: continue
                
                // Check if this attacker has more moves left
                if (stepIndex >= attacker.type.speed) continue
                
                val target = state.level.targetPosition
                val path = findPath(currentPos, target)
                
                if (path.size < 2) continue  // No movement possible
                
                val newPos = path[1]  // Next position in path
                
                // Check if this position is already occupied or will be occupied by another unit in this step
                val isOccupied = currentPositions.any { (id, pos) ->
                    id != attacker.id && pos == newPos
                } || positionsToOccupy.contains(newPos)
                
                if (!isOccupied) {
                    movementsInThisStep.add(Pair(attacker.id, newPos))
                    positionsToOccupy.add(newPos)
                    currentPositions[attacker.id] = newPos
                } else {
                    // If optimal path is blocked, try to find an alternative position
                    val alternativePos = findAlternativePosition(currentPos, target, attacker.id, currentPositions, positionsToOccupy)
                    if (alternativePos != null) {
                        movementsInThisStep.add(Pair(attacker.id, alternativePos))
                        positionsToOccupy.add(alternativePos)
                        currentPositions[attacker.id] = alternativePos
                    }
                    // If no alternative found, unit stays in place for this step
                }
            }
            
            if (movementsInThisStep.isNotEmpty()) {
                allMovementSteps.add(movementsInThisStep)
            }
        }
        
        return allMovementSteps
    }
    
    /**
     * Apply a single movement step for the given attacker.
     */
    fun applyMovement(attackerId: Int, newPosition: Position) {
        val attacker = state.attackers.find { it.id == attackerId } ?: return
        if (attacker.isDefeated.value) return
        
        // Check if position is occupied by another alive attacker
        val isOccupied = state.attackers.any {
            it.id != attacker.id && !it.isDefeated.value && it.position.value == newPosition
        }
        
        // Only move if position is not occupied
        if (!isOccupied) {
            attacker.position.value = newPosition
            
            // Check if reached target
            if (newPosition == state.level.targetPosition) {
                state.healthPoints.value--
                attacker.isDefeated.value = true
            }
        }
    }
    
    /**
     * Prepare for enemy turn: set phase but don't spawn yet.
     * Spawning happens after movements to ensure spawn points are clear.
     */
    fun startEnemyTurn() {
        if (state.phase.value != GamePhase.PLAYER_TURN) return
        
        state.turnNumber.value++
        state.phase.value = GamePhase.ENEMY_TURN
    }
    
    /**
     * Spawn new attackers during enemy turn.
     * Called after movements to ensure spawn points are clear.
     */
    fun spawnEnemyTurnAttackers() {
        spawnAttackers()
    }
    
    /**
     * Calculate movement steps for newly spawned units (those at spawn points).
     * This moves them away from spawn points to make room for future spawns.
     * Uses a simulated approach to handle collisions between units moving simultaneously.
     */
    fun calculateNewlySpawnedMovements(): List<List<Pair<Int, Position>>> {
        val allMovementSteps = mutableListOf<List<Pair<Int, Position>>>()
        
        // Find attackers at spawn points
        val newlySpawned = state.attackers.filter { attacker ->
            !attacker.isDefeated.value && state.level.isSpawnPoint(attacker.position.value)
        }.toMutableList()
        
        if (newlySpawned.isEmpty()) return allMovementSteps
        
        // Track current positions for collision detection during simulation
        val currentPositions = mutableMapOf<Int, Position>()
        newlySpawned.forEach { currentPositions[it.id] = it.position.value }
        
        // Find the maximum speed to know how many steps to simulate
        val maxSpeed = newlySpawned.maxOfOrNull { it.type.speed } ?: 0
        
        // Simulate movement step by step
        for (stepIndex in 0 until maxSpeed) {
            val movementsInThisStep = mutableListOf<Pair<Int, Position>>()
            val positionsToOccupy = mutableSetOf<Position>()
            
            for (attacker in newlySpawned) {
                val currentPos = currentPositions[attacker.id] ?: continue
                
                // Check if this attacker has more moves left
                if (stepIndex >= attacker.type.speed) continue
                
                val target = state.level.targetPosition
                val path = findPath(currentPos, target)
                
                if (path.size < 2) continue  // No movement possible
                
                val newPos = path[1]  // Next position in path
                
                // Check if this position is already occupied or will be occupied by another unit in this step
                val isOccupied = state.attackers.any {
                    it.id != attacker.id && !it.isDefeated.value && it.position.value == newPos
                } || currentPositions.any { (id, pos) ->
                    id != attacker.id && pos == newPos
                } || positionsToOccupy.contains(newPos)
                
                if (!isOccupied) {
                    movementsInThisStep.add(Pair(attacker.id, newPos))
                    positionsToOccupy.add(newPos)
                    currentPositions[attacker.id] = newPos
                } else {
                    // If optimal path is blocked, try to find an alternative position
                    // This is crucial for clearing spawn points
                    val alternativePos = findAlternativePosition(currentPos, target, attacker.id, currentPositions, positionsToOccupy)
                    if (alternativePos != null) {
                        movementsInThisStep.add(Pair(attacker.id, alternativePos))
                        positionsToOccupy.add(alternativePos)
                        currentPositions[attacker.id] = alternativePos
                    }
                    // If no alternative found, unit stays in place for this step
                }
            }
            
            if (movementsInThisStep.isNotEmpty()) {
                allMovementSteps.add(movementsInThisStep)
            }
        }
        
        return allMovementSteps
    }
    
    /**
     * Find an alternative position when the optimal path is blocked.
     * Tries to find any adjacent position that moves the unit closer to (or at least not further from) the target.
     * Prioritizes positions on the path.
     */
    private fun findAlternativePosition(
        currentPos: Position,
        target: Position,
        attackerId: Int,
        currentPositions: Map<Int, Position>,
        positionsToOccupy: Set<Position>
    ): Position? {
        val currentDistance = currentPos.distanceTo(target)
        
        // Get all hex neighbors
        val neighbors = currentPos.getHexNeighbors()
        
        // Filter valid positions (on the map, on path, not blocked by islands)
        val validNeighbors = neighbors.filter { neighbor ->
            neighbor.x >= 0 && neighbor.x < state.level.gridWidth &&
            neighbor.y >= 0 && neighbor.y < state.level.gridHeight &&
            state.level.isOnPath(neighbor) &&
            !state.level.isBuildIsland(neighbor)
        }
        
        // Find positions that are not occupied
        val availableNeighbors = validNeighbors.filter { neighbor ->
            val isOccupied = state.attackers.any {
                it.id != attackerId && !it.isDefeated.value && it.position.value == neighbor
            } || currentPositions.any { (id, pos) ->
                id != attackerId && pos == neighbor
            } || positionsToOccupy.contains(neighbor)
            
            !isOccupied
        }
        
        if (availableNeighbors.isEmpty()) return null
        
        // Prioritize positions that move closer to the target
        val movingCloser = availableNeighbors.filter { it.distanceTo(target) < currentDistance }
        if (movingCloser.isNotEmpty()) {
            return movingCloser.minByOrNull { it.distanceTo(target) }
        }
        
        // If no closer positions, accept same distance (lateral movement to clear spawn point)
        val sameDist = availableNeighbors.filter { it.distanceTo(target) == currentDistance }
        if (sameDist.isNotEmpty()) {
            return sameDist.first()
        }
        
        // As a last resort for spawn points, even moving away is better than staying
        // This ensures spawn points are always cleared
        if (state.level.isSpawnPoint(currentPos)) {
            return availableNeighbors.minByOrNull { it.distanceTo(target) }
        }
        
        return null
    }
    
    /**
     * Complete enemy turn: apply effects and start player turn.
     */
    fun completeEnemyTurn() {
        if (state.phase.value != GamePhase.ENEMY_TURN) return
        
        // Apply damage over time effects
        applyDotEffects()
        
        // Update field effects
        updateFieldEffects()

        // Remove defeated attackers and give rewards
        processDefeatedAttackers()
        
        // Check if we should load next wave
        if (state.attackersToSpawn.isEmpty() && state.attackers.isEmpty()) {
            loadNextWave()
        }
        
        // Advance building timers and start next player turn
        advanceBuildTimers()
        state.phase.value = GamePhase.PLAYER_TURN
        resetDefenderActions()
    }
    
    private fun resetDefenderActions() {
        state.defenders.forEach { it.resetActions() }
    }
    
    private fun advanceBuildTimers() {
        state.defenders.forEach { defender ->
            if (defender.buildTimeRemaining.value > 0) {
                defender.buildTimeRemaining.value--
                if (defender.buildTimeRemaining.value == 0) {
                    defender.resetActions()
                }
            }
        }
    }
    
    private fun isPositionOccupied(position: Position): Boolean {
        return state.defenders.any { it.position == position } ||
               state.attackers.any { it.position.value == position }
    }
    
    private fun loadNextWave() {
        if (state.currentWaveIndex.value >= state.level.attackerWaves.size) {
            return
        }
        
        val wave = state.level.attackerWaves[state.currentWaveIndex.value]
        state.attackersToSpawn.addAll(wave.attackers)
        state.currentWaveIndex.value++
        state.spawnCounter.value = 0
    }
    
    private fun spawnAttackers() {
        if (state.attackersToSpawn.isEmpty()) return
        
        state.spawnCounter.value++
        val wave = state.level.attackerWaves.getOrNull(state.currentWaveIndex.value - 1) ?: return
        
        if (state.spawnCounter.value >= wave.spawnDelay) {
            // Spawn 6 enemies per turn (2x the number of spawn points)
            val spawnPoints = state.level.startPositions
            val enemiesToSpawn = minOf(6, state.attackersToSpawn.size)
            
            repeat(enemiesToSpawn) { index ->
                if (state.attackersToSpawn.isEmpty()) return@repeat

                // Use a different spawn point for each enemy (cycle through spawn points)
                val spawnPos = spawnPoints[index % spawnPoints.size]

                val type = state.attackersToSpawn.removeAt(0)
                val attacker = Attacker(
                    id = state.nextAttackerId.value++,
                    type = type,
                    position = mutableStateOf(spawnPos)
                )
                state.attackers.add(attacker)
            }

            state.spawnCounter.value = 0

            // NOTE: Goblin movement after spawning will be handled by the animation system
            // instead of calling moveGoblinsAfterSpawn() here
        }
    }
    
    private fun findFreeSpawnPosition(): Position? {
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
    
    private fun singleTargetAttack(defender: Defender, target: Attacker) {
        target.currentHealth.value -= defender.damage
        if (target.currentHealth.value <= 0) {
            target.isDefeated.value = true
        }
    }
    
    private fun aoeAttack(defender: Defender, targetPosition: Position) {
        // Calculate affected positions - target and all neighbors that are on the path
        val affectedPositions = mutableSetOf(targetPosition)
        affectedPositions.addAll(
            targetPosition.getHexNeighbors().filter { neighbor ->
                neighbor.x >= 0 && neighbor.x < state.level.gridWidth &&
                neighbor.y >= 0 && neighbor.y < state.level.gridHeight &&
                state.level.isOnPath(neighbor)
            }
        )

        // Only include target position if it's on the path
        if (!state.level.isOnPath(targetPosition)) {
            affectedPositions.remove(targetPosition)
        }

        // Damage all enemies in affected positions
        val targets = state.attackers.filter { 
            !it.isDefeated.value && affectedPositions.contains(it.position.value)
        }
        
        for (target in targets) {
            target.currentHealth.value -= defender.damage
            if (target.currentHealth.value <= 0) {
                target.isDefeated.value = true
            }
        }

        // Clear existing fireball effects from this defender
        state.fieldEffects.removeAll {
            it.type == FieldEffectType.FIREBALL_AOE && it.defenderId == defender.id
        }

        // Remove acid effects from affected positions (fire burns away the acid)
        state.fieldEffects.removeAll {
            it.type == FieldEffectType.ACID_DOT && it.position in affectedPositions
        }

        // Add new fireball effects (visual only, last for 1 turn to show affected area)
        for (pos in affectedPositions) {
            state.fieldEffects.add(
                FieldEffect(
                    position = pos,
                    type = FieldEffectType.FIREBALL_AOE,
                    damage = defender.damage,
                    turnsRemaining = 1,  // Visual effect lasts 1 turn
                    defenderId = defender.id
                )
            )
        }
    }
    
    private fun dotAttack(defender: Defender, targetPosition: Position) {
        // Calculate affected positions - target and all neighbors that are on the path
        val affectedPositions = mutableSetOf(targetPosition)
        affectedPositions.addAll(
            targetPosition.getHexNeighbors().filter { neighbor ->
                neighbor.x >= 0 && neighbor.x < state.level.gridWidth &&
                neighbor.y >= 0 && neighbor.y < state.level.gridHeight &&
                state.level.isOnPath(neighbor)
            }
        )

        // Only include target position if it's on the path
        if (!state.level.isOnPath(targetPosition)) {
            affectedPositions.remove(targetPosition)
        }
        
        // Apply initial damage and DOT to all enemies in affected positions
        val targets = state.attackers.filter {
            !it.isDefeated.value && affectedPositions.contains(it.position.value)
        }

        for (target in targets) {
            // Initial damage is same as DOT tick damage (not full damage)
            target.currentHealth.value -= defender.damage / DOT_DAMAGE_DIVISOR
            // Mark for additional rounds of DOT based on tower level
            defender.dotRoundsRemaining[target.id] = defender.dotDuration

            if (target.currentHealth.value  <= 0) {
                target.isDefeated.value = true
            }
        }

        // Create field effects for acid DOT on all affected positions
        // Don't remove existing acid effects - they should persist until they expire
        
        // Get all positions with active fireball effects (fire burns away acid)
        val fireballPositions = state.fieldEffects
            .mapNotNullTo(mutableSetOf()) { if (it.type == FieldEffectType.FIREBALL_AOE) it.position else null }
        
        for (pos in affectedPositions) {
            // Skip this position if there's an active fireball
            if (pos in fireballPositions) continue
            
            // Find if there's an enemy at this position
            val enemyAtPos = targets.find { it.position.value == pos }

            // Check if there's already an acid effect at this position
            val existingEffect = state.fieldEffects.find {
                it.type == FieldEffectType.ACID_DOT && it.position == pos
            }

            val newDuration = defender.dotDuration

            if (existingEffect != null) {
                // If existing effect has more turns, keep it; otherwise replace it
                if (newDuration > existingEffect.turnsRemaining) {
                    state.fieldEffects.remove(existingEffect)
                    state.fieldEffects.add(
                        FieldEffect(
                            position = pos,
                            type = FieldEffectType.ACID_DOT,
                            damage = defender.damage / DOT_DAMAGE_DIVISOR,
                            turnsRemaining = newDuration,
                            defenderId = defender.id,
                            attackerId = enemyAtPos?.id
                        )
                    )
                }
                // If existing has equal or more turns, do nothing (keep existing)
            } else {
                // No existing effect, add new one
                state.fieldEffects.add(
                    FieldEffect(
                        position = pos,
                        type = FieldEffectType.ACID_DOT,
                        damage = defender.damage / DOT_DAMAGE_DIVISOR,
                        turnsRemaining = newDuration,
                        defenderId = defender.id,
                        attackerId = enemyAtPos?.id
                    )
                )
            }
        }
    }
    
    private fun applyDotEffects() {
        // Apply DOT damage from acid puddles on the ground
        val acidEffects = state.fieldEffects.filter { it.type == FieldEffectType.ACID_DOT }

        for (effect in acidEffects) {
            // Find all enemies standing in the acid
            val enemiesInAcid = state.attackers.filter {
                !it.isDefeated.value && it.position.value == effect.position
            }

            for (attacker in enemiesInAcid) {
                attacker.currentHealth.value -= effect.damage
                if (attacker.currentHealth.value <= 0) {
                    attacker.isDefeated.value = true
                    /*
                    todo from branch
            val toRemove = mutableListOf<Int>()
            for ((attackerId, rounds) in defender.dotRoundsRemaining) {
                val attacker = state.attackers.find { it.id == attackerId }
                if (attacker != null && !attacker.isDefeated.value) {
                    attacker.currentHealth.value -= defender.damage / 2
                    if (attacker.currentHealth.value <= 0) {
                        attacker.isDefeated.value = true
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
                     */
                }
            }
        }
    }
    
    private fun moveAttackers() {
        for (attacker in state.attackers) {
            if (attacker.isDefeated.value) continue
            
            val target = state.level.targetPosition
            val path = findPath(attacker.position.value, target)
            
            if (path.isEmpty()) continue
            
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

    private fun moveGoblinsAfterSpawn() {
        // Move only goblins that just spawned (those still at spawn points)
        for (attacker in state.attackers) {
            if (attacker.isDefeated.value) continue
            if (attacker.type != AttackerType.GOBLIN) continue

            // Check if goblin is at a spawn point
            if (!state.level.isSpawnPoint(attacker.position.value)) continue

            val target = state.level.targetPosition
            val path = findPath(attacker.position.value, target)

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
    
    private fun updateFieldEffects() {
        // Decrement turn counters (safe to modify properties)
        state.fieldEffects.forEach { effect ->
            effect.turnsRemaining--
        }
        // Remove expired effects in a separate operation
        state.fieldEffects.removeAll { it.turnsRemaining <= 0 }
    }

    private fun processDefeatedAttackers() {
        val defeated = state.attackers.filter { it.isDefeated.value && it.position.value != state.level.targetPosition }
        for (attacker in defeated) {
            state.coins.value += attacker.type.reward
        }
        state.attackers.removeAll { it.isDefeated.value }
    }
    
    // Cheat code support for testing
    fun addCoins(amount: Int) {
        state.coins.value += amount
    }
    
    fun spawnEnemy(type: AttackerType, level: Int = 1) {
        // Find a free spawn position
        val spawnPos = findFreeSpawnPosition() ?: return
        
        // Create the enemy with scaled health based on level
        val scaledHealth = type.health * level
        val attacker = Attacker(
            id = state.nextAttackerId.value++,
            type = type,
            position = mutableStateOf(spawnPos),
            currentHealth = mutableStateOf(scaledHealth)
        )
        
        // Add to attackers list
        state.attackers.add(attacker)
        
        // Move goblins immediately after spawning (if it's a goblin)
        if (type == AttackerType.GOBLIN) {
            moveGoblinsAfterSpawn()
        }
    }
}
