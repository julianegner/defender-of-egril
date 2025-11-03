package com.defenderofegril.game

import androidx.compose.runtime.mutableStateOf
import com.defenderofegril.model.*
import kotlin.math.min

class GameEngine(private val state: GameState) {
    
    companion object {
        // LASTING damage is applied at half the initial damage per turn
        private const val LASTING_DAMAGE_DIVISOR = 2
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
        
        // Reset actions to reflect new action count from upgrade
        defender.resetActions()
        
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
                
                // Ensure only one Ewhad exists at a time (boss is unique)
                if (type == AttackerType.EWHAD) {
                    val ewhadExists = state.attackers.any { 
                        it.type == AttackerType.EWHAD && !it.isDefeated.value 
                    }
                    if (ewhadExists) {
                        // Skip spawning another Ewhad if one already exists
                        return@repeat
                    }
                }
                
                // Calculate level for initial enemies
                val enemyLevel = 1 + (state.currentWaveIndex.value - 1)
                
                val attacker = Attacker(
                    id = state.nextAttackerId.value++,
                    type = type,
                    position = mutableStateOf(spawnPos),
                    level = enemyLevel
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
            AttackType.AREA -> areaAttack(defender, target.position.value)
            AttackType.LASTING -> lastingAttack(defender, target.position.value)
            AttackType.NONE -> return false  // Mines and special structures can't attack
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
        if (defender.type.attackType == AttackType.AREA || defender.type.attackType == AttackType.LASTING) {
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
            AttackType.AREA -> areaAttack(defender, targetPosition)
            AttackType.LASTING -> lastingAttack(defender, targetPosition)
            AttackType.NONE -> return false  // Mines and special structures can't attack
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
        
        // Check and activate traps
        checkAndActivateTraps()
        
        // Apply damage over time effects
        applyLastingEffects()
        
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
                val path = findPath(currentPos, target, attacker)
                
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
                val path = findPath(currentPos, target, attacker)
                
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
        
        // Check and activate traps after all movements
        checkAndActivateTraps()
        
        // Apply damage over time effects
        applyLastingEffects()
        
        // Update field effects
        updateFieldEffects()

        // Process special enemy abilities
        processEnemyAbilities()

        // Remove defeated attackers and give rewards
        processDefeatedAttackers()
        
        // Check if we should load next wave
        if (state.attackersToSpawn.isEmpty() && state.attackers.isEmpty()) {
            loadNextWave()
        }
        
        // Advance building timers and re-enable towers
        advanceBuildTimers()
        updateTowerDisableStatus()
        
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
    
    private fun areaAttack(defender: Defender, targetPosition: Position) {
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

        // Damage all enemies in affected positions (except those immune to fireballs)
        val targets = state.attackers.filter { 
            !it.isDefeated.value && affectedPositions.contains(it.position.value)
        }
        
        for (target in targets) {
            // Check immunity to fireball (Red Demons)
            if (target.canBeDamagedByFireball()) {
                target.currentHealth.value -= defender.damage
                if (target.currentHealth.value <= 0) {
                    target.isDefeated.value = true
                }
            }
        }

        // Clear existing fireball effects from this defender
        state.fieldEffects.removeAll {
            it.type == FieldEffectType.FIREBALL && it.defenderId == defender.id
        }

        // Remove acid effects from affected positions (fire burns away the acid)
        state.fieldEffects.removeAll {
            it.type == FieldEffectType.ACID && it.position in affectedPositions
        }

        // Add new fireball effects (visual only, last for 1 turn to show affected area)
        for (pos in affectedPositions) {
            state.fieldEffects.add(
                FieldEffect(
                    position = pos,
                    type = FieldEffectType.FIREBALL,
                    damage = defender.damage,
                    turnsRemaining = 1,  // Visual effect lasts 1 turn
                    defenderId = defender.id
                )
            )
        }
    }
    
    private fun lastingAttack(defender: Defender, targetPosition: Position) {
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
            // Check immunity to acid (Blue Demons)
            if (target.canBeDamagedByAcid()) {
                // Initial damage is same as DOT tick damage (not full damage)
                target.currentHealth.value -= defender.damage / LASTING_DAMAGE_DIVISOR
                // Mark for additional rounds of DOT based on tower level
                defender.dotRoundsRemaining[target.id] = defender.dotDuration

                if (target.currentHealth.value  <= 0) {
                    target.isDefeated.value = true
                }
            }
        }

        // Create field effects for acid DOT on all affected positions
        // Don't remove existing acid effects - they should persist until they expire
        
        // Get all positions with active fireball effects (fire burns away acid)
        val fireballPositions = state.fieldEffects
            .filter { it.type == FieldEffectType.FIREBALL }
            .mapTo(mutableSetOf()) { it.position }
        
        for (pos in affectedPositions) {
            // Skip this position if there's an active fireball
            if (pos in fireballPositions) continue
            
            // Find if there's an enemy at this position
            val enemyAtPos = targets.find { it.position.value == pos }

            // Check if there's already an acid effect at this position
            val existingEffect = state.fieldEffects.find {
                it.type == FieldEffectType.ACID && it.position == pos
            }

            val newDuration = defender.dotDuration

            if (existingEffect != null) {
                // If existing effect has more turns, keep it; otherwise replace it
                if (newDuration > existingEffect.turnsRemaining) {
                    state.fieldEffects.remove(existingEffect)
                    state.fieldEffects.add(
                        FieldEffect(
                            position = pos,
                            type = FieldEffectType.ACID,
                            damage = defender.damage / LASTING_DAMAGE_DIVISOR,
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
                        type = FieldEffectType.ACID,
                        damage = defender.damage / LASTING_DAMAGE_DIVISOR,
                        turnsRemaining = newDuration,
                        defenderId = defender.id,
                        attackerId = enemyAtPos?.id
                    )
                )
            }
        }
    }
    
    private fun applyLastingEffects() {
        // Apply LASTING damage from acid puddles on the ground
        val acidEffects = state.fieldEffects.filter { it.type == FieldEffectType.ACID }

        for (effect in acidEffects) {
            // Find all enemies standing in the acid
            val enemiesInAcid = state.attackers.filter {
                !it.isDefeated.value && it.position.value == effect.position
            }

            for (attacker in enemiesInAcid) {
                // Check immunity to acid (Blue Demons)
                if (attacker.canBeDamagedByAcid()) {
                    attacker.currentHealth.value -= effect.damage
                    if (attacker.currentHealth.value <= 0) {
                        attacker.isDefeated.value = true
                    }
                }
            }
        }
    }
    
    private fun moveAttackers() {
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
            
            val path = findPath(attacker.position.value, target, attacker)
            
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
            val path = findPath(dragon.position.value, target, dragon)
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

    private fun moveGoblinsAfterSpawn() {
        // Move only goblins that just spawned (those still at spawn points)
        for (attacker in state.attackers) {
            if (attacker.isDefeated.value) continue
            if (attacker.type != AttackerType.GOBLIN) continue

            // Check if goblin is at a spawn point
            if (!state.level.isSpawnPoint(attacker.position.value)) continue

            val target = state.level.targetPosition
            val path = findPath(attacker.position.value, target, attacker)

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
    
    // A* pathfinding algorithm with danger awareness
    private fun findPath(start: Position, goal: Position, attacker: Attacker? = null): List<Position> {
        if (start == goal) return listOf(start)
        
        val openSet = mutableSetOf(start)
        val cameFrom = mutableMapOf<Position, Position>()
        val gScore = mutableMapOf(start to 0)
        val fScore = mutableMapOf(start to start.distanceTo(goal))
        
        var iterations = 0
        val maxIterations = 1000 // Prevent infinite loops
        
        while (openSet.isNotEmpty() && iterations < maxIterations) {
            iterations++
            
            // Select the position with the lowest fScore
            // If multiple positions have the same fScore, prefer the one closest to the goal (heuristic tiebreaker)
            val current = openSet.minWithOrNull(compareBy<Position> { fScore[it] ?: Int.MAX_VALUE }
                .thenBy { it.distanceTo(goal) }) ?: break
            
            if (current == goal) {
                return reconstructPath(cameFrom, current)
            }
            
            openSet.remove(current)
            
            for (neighbor in getNeighbors(current)) {
                val moveCost = calculateMoveCost(neighbor, attacker)
                val tentativeGScore = (gScore[current] ?: Int.MAX_VALUE) + moveCost
                
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
        
        // No path found or max iterations reached, return simple path towards goal
        return listOf(start, moveTowards(start, goal))
    }
    
    /**
     * Calculate the cost of moving to a position, considering dangers like acid and tower coverage.
     * Returns higher costs for dangerous positions to encourage safer paths.
     */
    private fun calculateMoveCost(position: Position, attacker: Attacker?): Int {
        var cost = 1  // Base movement cost
        
        // If no attacker info, use basic cost (for compatibility)
        if (attacker == null) return cost
        
        val attackerHealth = attacker.currentHealth.value
        
        // Check for acid field effects at this position
        val acidEffect = state.fieldEffects.find { 
            it.type == FieldEffectType.ACID && it.position == position 
        }
        if (acidEffect != null) {
            // Acid applies effect.damage each turn a unit stands in it
            // For pathfinding cost calculation, we assume 1 turn of exposure:
            // - Units move through cells one at a time during their movement phase
            // - Even if blocked, they won't choose to stay in acid (will seek alternate paths)
            // - This provides a reasonable heuristic for path cost without over-penalizing
            // Note: The high cost (1000) for lethal acid ensures it's only chosen as last resort
            val acidDamage = acidEffect.damage
            
            // If acid would defeat the unit, add very high cost (but not impossible)
            if (acidDamage >= attackerHealth) {
                cost += 1000  // Very high cost, avoid if possible
            } else {
                // Add cost proportional to the damage (encourage avoiding acid)
                cost += acidDamage * 10
            }
        }
        
        // Check for tower coverage at this position
        var maxTowerDamage = 0
        var totalTowerThreat = 0
        
        for (defender in state.defenders) {
            if (!defender.isReady) continue
            
            val distance = defender.position.distanceTo(position)
            
            // Check if position is in tower range
            if (distance >= defender.type.minRange && distance <= defender.range) {
                val potentialDamage = when (defender.type.attackType) {
                    AttackType.LASTING -> {
                        // DOT damage over multiple turns
                        val dotDamagePerTurn = defender.damage / LASTING_DAMAGE_DIVISOR
                        dotDamagePerTurn * defender.dotDuration
                    }
                    else -> defender.damage
                }
                
                maxTowerDamage = maxOf(maxTowerDamage, potentialDamage)
                totalTowerThreat += potentialDamage
            }
        }
        
        if (totalTowerThreat > 0) {
            // If tower damage would defeat the unit, add high cost
            if (maxTowerDamage >= attackerHealth) {
                cost += 500  // High cost for lethal positions
            } else {
                // Add moderate cost for tower coverage (prefer paths outside tower range)
                // Use total threat to account for multiple overlapping towers
                cost += (totalTowerThreat / 10).coerceAtMost(100)
            }
        }
        
        return cost
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
        
        val candidate = when {
            dx != 0 -> Position(from.x + dx.coerceIn(-1, 1), from.y)
            dy != 0 -> Position(from.x, from.y + dy.coerceIn(-1, 1))
            else -> from
        }
        
        // Ensure the candidate position is valid (on path and within bounds)
        if (candidate.x >= 0 && candidate.x < state.level.gridWidth &&
            candidate.y >= 0 && candidate.y < state.level.gridHeight &&
            state.level.isOnPath(candidate)) {
            return candidate
        }
        
        // If not valid, just return current position (don't move)
        return from
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
    
    /**
     * Process special enemy abilities:
     * - Evil Mage: summons demons
     * - Ewhad: summons demons and undead
     * - Green Witch: heals adjacent units
     * - Red Witch: disables towers
     */
    private fun processEnemyAbilities() {
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
                        repeat(attacker.level) {
                            spawnDemonNear(attacker, AttackerType.BLUE_DEMON, state.turnNumber.value)
                        }
                        // Summon red demons (level / 2)
                        repeat(attacker.level / 2) {
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
                        repeat(attacker.level * 2) {
                            spawnDemonNear(attacker, AttackerType.BLUE_DEMON, state.turnNumber.value)
                        }
                        repeat(attacker.level) {
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
                            val healAmount = minOf(attacker.level, adjacentEnemy.maxHealth - adjacentEnemy.currentHealth.value)
                            adjacentEnemy.currentHealth.value += healAmount
                        }
                    }
                }
                AttackerType.RED_WITCH -> {
                    // Disable nearby tower (instead of moving to target)
                    disableNearestTower(attacker)
                }
                else -> {}
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
        
        val demon = Attacker(
            id = state.nextAttackerId.value++,
            type = demonType,
            position = mutableStateOf(spawnPos),
            level = level
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
            tower.level.value <= witch.level
        }
        
        if (eligibleTowers.isEmpty()) return
        
        // Find closest tower
        val nearestTower = eligibleTowers.minByOrNull { tower ->
            tower.position.distanceTo(witch.position.value)
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
    private fun updateTowerDisableStatus() {
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
    private fun findNearestActiveTower(witch: Attacker): Defender? {
        val eligibleTowers = state.defenders.filter { tower ->
            tower.isReady && !tower.isDisabled.value && tower.level.value <= witch.level
        }
        
        if (eligibleTowers.isEmpty()) return null
        
        return eligibleTowers.minByOrNull { tower ->
            tower.position.distanceTo(witch.position.value)
        }
    }
    
    /**
     * Find a position on the path near the target tower for Red Witch to move towards
     */
    private fun findPathPositionNearTower(towerPosition: Position): Position {
        // Find all path positions adjacent to the tower
        val adjacentPathPositions = towerPosition.getHexNeighbors().filter { pos ->
            pos.x >= 0 && pos.x < state.level.gridWidth &&
            pos.y >= 0 && pos.y < state.level.gridHeight &&
            state.level.isOnPath(pos)
        }
        
        // Return the first adjacent path position, or tower position if none found
        return adjacentPathPositions.firstOrNull() ?: towerPosition
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
    
    /**
     * Cheat code to spawn a dragon from a random dwarven mine
     */
    fun spawnDragonCheat(): Boolean {
        // Find any dwarven mine on the map
        val mine = state.defenders.find { it.type == DefenderType.DWARVEN_MINE }
        
        if (mine != null) {
            spawnDragonFromMine(mine)
            return true
        }
        
        return false
    }
    
    /**
     * Perform the Dig action for a dwarven mine
     */
    fun performMineDig(mineId: Int): DigOutcome? {
        val mine = state.defenders.find { it.id == mineId && it.type == DefenderType.DWARVEN_MINE } ?: return null
        
        if (!mine.isReady || mine.actionsRemaining.value <= 0) return null
        
        // Roll for outcome
        val outcome = DigOutcome.roll()
        
        // Process outcome
        when (outcome) {
            DigOutcome.DRAGON -> {
                // Dragon awakens - destroy mine and spawn dragon
                spawnDragonFromMine(mine)
            }
            else -> {
                // Add coins
                state.coins.value += outcome.coins
                mine.coinsGenerated.value += outcome.coins
            }
        }
        
        // Consume action
        mine.actionsRemaining.value--
        mine.hasBeenUsed.value = true
        
        return outcome
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
        
        // Consume action
        mine.actionsRemaining.value--
        mine.hasBeenUsed.value = true
        
        return true
    }
    
    /**
     * Spawn a dragon when a mine is destroyed
     */
    private fun spawnDragonFromMine(mine: Defender) {
        // Spawn dragon first to get its ID
        val dragonHealth = 500 + mine.coinsGenerated.value
        val dragon = Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.DRAGON,
            position = mutableStateOf(Position(0, 0)), // Temporary position
            level = 1,
            currentHealth = mutableStateOf(dragonHealth),
            spawnedFromLairId = null  // Will be set after lair is created
        )
        
        // Replace mine with dragon's lair and link to dragon
        val lairDefender = Defender(
            id = state.nextDefenderId.value++,
            type = DefenderType.DRAGONS_LAIR,
            position = mine.position,
            buildTimeRemaining = mutableStateOf(0),
            dragonId = mutableStateOf(dragon.id)
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
            // Remove the unit if it's not Ewhad
            if (unitAtPosition != null) {
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
    fun checkAndActivateTraps() {
        val trapsToRemove = mutableListOf<Trap>()
        
        for (trap in state.traps) {
            val enemyAtPosition = state.attackers.find { 
                it.position.value == trap.position && !it.isDefeated.value 
            }
            
            if (enemyAtPosition != null) {
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
        processDefeatedAttackers()
    }
}
