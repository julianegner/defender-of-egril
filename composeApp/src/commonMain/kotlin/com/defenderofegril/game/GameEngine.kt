package com.defenderofegril.game

import androidx.compose.runtime.mutableStateOf
import com.defenderofegril.model.*

/**
 * Main game engine that coordinates all game systems.
 * Delegates to specialized subsystems for better organization and maintainability.
 */
class GameEngine(private val state: GameState) {
    
    // Specialized subsystems
    private val towerManager = TowerManager(state)
    private val pathfinding = PathfindingSystem(state)
    private val combatSystem = CombatSystem(state)
    private val enemyMovement = EnemyMovementSystem(state, pathfinding)
    private val enemyAbilities = EnemyAbilitySystem(state)
    private val mineOperations = MineOperations(state)
    
    // Tower Management - delegated to TowerManager
    fun placeDefender(type: DefenderType, position: Position): Boolean =
        towerManager.placeDefender(type, position)
    
    fun upgradeDefender(defenderId: Int): Boolean =
        towerManager.upgradeDefender(defenderId)
    
    fun undoTower(defenderId: Int): Boolean =
        towerManager.undoTower(defenderId)
    
    fun sellTower(defenderId: Int): Boolean =
        towerManager.sellTower(defenderId)
    
    // Combat System - delegated to CombatSystem
    fun defenderAttack(defenderId: Int, targetId: Int): Boolean =
        combatSystem.defenderAttack(defenderId, targetId) { combatSystem.processDefeatedAttackers() }
    
    fun defenderAttackPosition(defenderId: Int, targetPosition: Position): Boolean =
        combatSystem.defenderAttackPosition(defenderId, targetPosition) { combatSystem.processDefeatedAttackers() }
    
    // Mine Operations - delegated to MineOperations
    fun performMineDig(mineId: Int): DigOutcome? =
        mineOperations.performMineDig(mineId)
    
    fun performMineDigWithOutcome(outcomeType: DigOutcome): DigOutcome? =
        mineOperations.performMineDigWithOutcome(outcomeType)
    
    fun performMineBuildTrap(mineId: Int, trapPosition: Position): Boolean =
        mineOperations.performMineBuildTrap(mineId, trapPosition)
    
    fun checkAndActivateTraps() {
        mineOperations.checkAndActivateTraps { combatSystem.processDefeatedAttackers() }
    }
    
    // Turn Management
    fun startFirstPlayerTurn() {
        if (state.phase.value != GamePhase.INITIAL_BUILDING) return
        state.phase.value = GamePhase.PLAYER_TURN
        state.turnNumber.value = 1  // Start at turn 1 when game begins
        
        // Load first wave
        if (state.currentWaveIndex.value == 0 && state.attackersToSpawn.isEmpty()) {
            enemyMovement.loadNextWave()
        }
        
        // Spawn initial enemies immediately
        spawnInitialEnemies()
        
        // Reset all defender actions
        resetDefenderActions()
    }
    
    private fun spawnInitialEnemies() {
        // Spawn all enemies scheduled for turn 1 from the spawn plan
        val turn1Spawns = state.spawnPlan.filter { it.spawnTurn == 1 }
        val spawnPoints = state.level.startPositions
        
        turn1Spawns.forEachIndexed { index, plannedSpawn ->
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

        // Move goblins immediately after initial spawning (this is not during enemy turn)
        enemyMovement.moveGoblinsAfterSpawn()
    }
    
    fun endPlayerTurn() {
        if (state.phase.value != GamePhase.PLAYER_TURN && state.phase.value != GamePhase.ENEMY_TURN) return
        
        // Only increment turn and process if we're actually starting enemy turn
        if (state.phase.value == GamePhase.PLAYER_TURN) {
            state.turnNumber.value++
        }
        
        state.phase.value = GamePhase.ENEMY_TURN
        
        // Spawn new attackers
        enemyMovement.spawnAttackers()
        
        // Move attackers
        enemyMovement.moveAttackers(
            findNearestActiveTower = { witch -> enemyAbilities.findNearestActiveTower(witch) },
            findPathPositionNearTower = { pos -> enemyAbilities.findPathPositionNearTower(pos) }
        )
        
        // Check and activate traps
        checkAndActivateTraps()
        
        // Apply damage over time effects
        combatSystem.applyLastingEffects()
        
        // Update field effects
        enemyMovement.updateFieldEffects()

        // Remove defeated attackers and give rewards
        combatSystem.processDefeatedAttackers()
        
        // Check if we should load next wave
        if (state.attackersToSpawn.isEmpty() && state.attackers.isEmpty()) {
            enemyMovement.loadNextWave()
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
                val path = pathfinding.findPath(currentPos, target, attacker)
                
                if (path.size < 2) continue  // No movement possible
                
                val newPos = path[1]  // Next position in path
                
                // Check if this position is already occupied or will be occupied by another unit in this step
                // Exception: Allow multiple units to move to the target position (they get defeated immediately)
                val isOccupied = if (newPos == state.level.targetPosition) {
                    false  // Target position can accommodate multiple units
                } else {
                    currentPositions.any { (id, pos) ->
                        id != attacker.id && pos == newPos
                    } || positionsToOccupy.contains(newPos)
                }
                
                if (!isOccupied) {
                    movementsInThisStep.add(Pair(attacker.id, newPos))
                    if (newPos != state.level.targetPosition) {
                        // Only mark non-target positions as occupied
                        positionsToOccupy.add(newPos)
                    }
                    currentPositions[attacker.id] = newPos
                } else {
                    // If optimal path is blocked, try to find an alternative position
                    val alternativePos = findAlternativePosition(currentPos, target, attacker.id, currentPositions, positionsToOccupy)
                    if (alternativePos != null) {
                        movementsInThisStep.add(Pair(attacker.id, alternativePos))
                        if (alternativePos != state.level.targetPosition) {
                            positionsToOccupy.add(alternativePos)
                        }
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
        // Exception: Allow movement to target position even if occupied (units get defeated immediately)
        val isOccupied = if (newPosition == state.level.targetPosition) {
            false  // Target can accommodate multiple units
        } else {
            state.attackers.any {
                it.id != attacker.id && !it.isDefeated.value && it.position.value == newPosition
            }
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
        enemyMovement.spawnAttackers()
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
                val path = pathfinding.findPath(currentPos, target, attacker)
                
                if (path.size < 2) continue  // No movement possible
                
                val newPos = path[1]  // Next position in path
                
                // Check if this position is already occupied or will be occupied by another unit in this step
                // Exception: Allow multiple units to move to the target position (they get defeated immediately)
                val isOccupied = if (newPos == state.level.targetPosition) {
                    false  // Target position can accommodate multiple units
                } else {
                    state.attackers.any {
                        it.id != attacker.id && !it.isDefeated.value && it.position.value == newPos
                    } || currentPositions.any { (id, pos) ->
                        id != attacker.id && pos == newPos
                    } || positionsToOccupy.contains(newPos)
                }
                
                if (!isOccupied) {
                    movementsInThisStep.add(Pair(attacker.id, newPos))
                    if (newPos != state.level.targetPosition) {
                        // Only mark non-target positions as occupied
                        positionsToOccupy.add(newPos)
                    }
                    currentPositions[attacker.id] = newPos
                } else {
                    // If optimal path is blocked, try to find an alternative position
                    // This is crucial for clearing spawn points
                    val alternativePos = findAlternativePosition(currentPos, target, attacker.id, currentPositions, positionsToOccupy)
                    if (alternativePos != null) {
                        movementsInThisStep.add(Pair(attacker.id, alternativePos))
                        if (alternativePos != state.level.targetPosition) {
                            positionsToOccupy.add(alternativePos)
                        }
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
        combatSystem.applyLastingEffects()
        
        // Update field effects
        enemyMovement.updateFieldEffects()

        // Process special enemy abilities
        enemyAbilities.processEnemyAbilities()

        // Remove defeated attackers and give rewards
        combatSystem.processDefeatedAttackers()
        
        // Check if we should load next wave
        if (state.attackersToSpawn.isEmpty() && state.attackers.isEmpty()) {
            enemyMovement.loadNextWave()
        }
        
        // Advance building timers and re-enable towers
        advanceBuildTimers()
        enemyAbilities.updateTowerDisableStatus()
        
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
    
    // Cheat code support for testing
    fun addCoins(amount: Int) {
        state.coins.value += amount
    }
    
    fun spawnEnemy(type: AttackerType, level: Int = 1) {
        // Find a free spawn position
        val spawnPos = enemyMovement.findFreeSpawnPosition() ?: return
        
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
            enemyMovement.moveGoblinsAfterSpawn()
        }
    }
    
    /**
     * Cheat code to spawn a dragon from a random dwarven mine
     */
    fun spawnDragonCheat(): Boolean {
        // Find any dwarven mine on the map
        val mine = state.defenders.find { it.type == DefenderType.DWARVEN_MINE }
        
        if (mine != null) {
            // Use the mine operations to spawn the dragon
            mineOperations.performMineDigWithOutcome(DigOutcome.DRAGON)
            return true
        }
        
        return false
    }
}
