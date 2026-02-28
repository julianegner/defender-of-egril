package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.audio.GlobalSoundManager
import de.egril.defender.audio.SoundEvent
import de.egril.defender.model.*
import de.egril.defender.config.LogConfig

/**
 * Main game engine that coordinates all game systems.
 * Delegates to specialized subsystems for better organization and maintainability.
 */
class GameEngine(private val state: GameState) {
    
    // Specialized subsystems
    private val towerManager = TowerManager(state)
    private val pathfinding = PathfindingSystem(state)
    private val bridgeSystem = BridgeSystem(state)
    private val combatSystem = CombatSystem(
        state, 
        bridgeSystem,
        getEffectiveLevel = { defender -> getEffectiveLevel(defender) },
        getEffectiveRange = { defender -> getEffectiveRange(defender) }
    )
    private val enemyMovement = EnemyMovementSystem(state, pathfinding)
    private val enemyAbilities = EnemyAbilitySystem(state)
    private val mineOperations = MineOperations(state)
    private val raftSystem = RaftSystem(state)
    private val barricadeSystem = BarricadeSystem(state)  // Add barricade system
    
    // Callback for dragon level changes (stored to set on new dragons)
    private var dragonLevelChangeCallback: ((oldLevel: Int, newLevel: Int) -> Unit)? = null
    
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
    
    fun performWizardPlaceMagicalTrap(wizardId: Int, trapPosition: Position): Boolean =
        mineOperations.performWizardPlaceMagicalTrap(wizardId, trapPosition)
    
    /**
     * Perform wizard mana generation action
     * Generates base 5 mana + (wizard level / 5) bonus mana
     * Consumes one wizard action
     * Returns true if mana was generated successfully
     */
    fun performWizardGenerateMana(wizardId: Int): Boolean {
        // Find the wizard tower
        val wizard = state.defenders.find { it.id == wizardId } ?: return false
        
        // Verify it's a wizard tower
        if (wizard.type != DefenderType.WIZARD_TOWER) return false
        
        // Check if wizard has actions remaining
        if (wizard.actionsRemaining.value <= 0) return false
        
        // Check if mana is already at max
        if (state.currentMana.value >= state.maxMana.value) return false
        
        // Calculate mana generation amount
        val manaAmount = 5 + (wizard.level.value / 5)
        
        // Add mana (capped at max)
        val newMana = minOf(state.currentMana.value + manaAmount, state.maxMana.value)
        val actualManaGenerated = newMana - state.currentMana.value
        state.currentMana.value = newMana
        
        // Consume one action
        wizard.actionsRemaining.value -= 1
        
        // Log mana generation
        if (LogConfig.ENABLE_SPELL_LOGGING) {
            println("=== SPELL: Wizard tower $wizardId generated $actualManaGenerated mana (${state.currentMana.value}/${state.maxMana.value})")
        }
        
        return true
    }
    
    // Barricade Operations - delegated to BarricadeSystem
    fun performBuildBarricade(towerId: Int, barricadePosition: Position): Boolean =
        barricadeSystem.performBuildBarricade(towerId, barricadePosition)
    
    fun removeBarricade(position: Position): Int =
        barricadeSystem.removeBarricade(position)

    fun autoDefenderAttacks() {
        if (state.phase.value != GamePhase.PLAYER_TURN) return

        // Check if there are any attackers to target
        if (state.attackers.none { !it.isDefeated.value && !it.isBuildingBridge.value }) return

        for (defender in state.defenders) {
            if (!defender.isReady) continue
            if (defender.actionsRemaining.value <= 0) continue
            if (defender.isDisabled.value) continue
            if (defender.type.attackType == AttackType.NONE) continue

            while (defender.actionsRemaining.value > 0) {
                // Get fresh list of active attackers for each attack
                val activeAttackers = state.attackers.filter { attacker ->
                    !attacker.isDefeated.value && !attacker.isBuildingBridge.value
                }
                
                // If no attackers left, we're done
                if (activeAttackers.isEmpty()) return
                
                val attackSucceeded = when (defender.type.attackType) {
                    AttackType.MELEE, AttackType.RANGED -> {
                        val target = selectAutoTargetForDefender(defender, activeAttackers) ?: break
                        val success = combatSystem.defenderAttack(defender.id, target.id) {
                            combatSystem.processDefeatedAttackers()
                        }
                        // If attack failed, break to avoid infinite loop
                        if (!success) break
                        success
                    }
                    AttackType.AREA, AttackType.LASTING -> {
                        // For area/lasting attacks, find the best position that hits the most enemies
                        val targetPosition = selectBestAreaAttackPosition(defender, activeAttackers) ?: break
                        val success = combatSystem.defenderAttackPosition(defender.id, targetPosition) {
                            combatSystem.processDefeatedAttackers()
                        }
                        // If attack failed (invalid position), break to avoid infinite loop
                        if (!success) break
                        success
                    }
                    AttackType.NONE -> break
                }
            }
        }
    }
    
    fun checkAndActivateTraps() {
        mineOperations.checkAndActivateTraps { combatSystem.processDefeatedAttackers() }
    }

    private fun selectAutoTargetForDefender(defender: Defender, candidates: List<Attacker>): Attacker? {
        val attackable = candidates.filter { attacker ->
            !attacker.isDefeated.value && defender.canAttack(attacker)
        }
        if (attackable.isEmpty()) return null

        return attackable.minWithOrNull(
            compareByDescending<Attacker> { threatScore(it) }
                .thenBy { estimateRemainingDistanceToGoal(it) }
                .thenBy { it.currentHealth.value }
                .thenBy { it.id }
        )
    }

    /**
     * Select the best position for an area or lasting attack.
     * Tries to maximize the number of enemies hit, prioritizing high-threat enemies.
     */
    private fun selectBestAreaAttackPosition(defender: Defender, candidates: List<Attacker>): Position? {
        val radius = defender.areaEffectRadius
        
        // Collect all enemy positions that could potentially be in range
        val enemyPositions = candidates.filter { !it.isDefeated.value }.map { it.position.value }.toSet()
        
        // Find all valid attack positions (within defender's direct range)
        // These are positions we can target with our area attack
        val validAttackPositions = mutableSetOf<Position>()
        
        // Check all enemy positions and their neighbors as potential target positions
        for (enemyPos in enemyPositions) {
            // Add the enemy position itself
            val distance = defender.position.value.distanceTo(enemyPos)
            if (distance >= defender.type.minRange && distance <= defender.range) {
                if (state.level.isOnPath(enemyPos) || state.level.getRiverTile(enemyPos) != null || state.isBridgeAt(enemyPos)) {
                    validAttackPositions.add(enemyPos)
                }
            }
            
            // Also consider positions near the enemy (within area radius)
            // that are within our direct attack range
            if (radius > 0) {
                val nearbyPositions = enemyPos.getHexNeighborsWithinRadius(radius, state.level.gridWidth, state.level.gridHeight)
                for (nearbyPos in nearbyPositions) {
                    val nearbyDistance = defender.position.value.distanceTo(nearbyPos)
                    if (nearbyDistance >= defender.type.minRange && nearbyDistance <= defender.range) {
                        if (state.level.isOnPath(nearbyPos) || state.level.getRiverTile(nearbyPos) != null || state.isBridgeAt(nearbyPos)) {
                            validAttackPositions.add(nearbyPos)
                        }
                    }
                }
            }
        }
        
        if (validAttackPositions.isEmpty()) return null
        
        // For each position, count how many enemies would be hit (considering area effect)
        val positionScores = validAttackPositions.map { targetPos ->
            val affectedPositions = mutableSetOf(targetPos)
            
            if (radius == 1) {
                affectedPositions.addAll(
                    targetPos.getHexNeighbors().filter { neighbor ->
                        neighbor.x >= 0 && neighbor.x < state.level.gridWidth &&
                        neighbor.y >= 0 && neighbor.y < state.level.gridHeight &&
                        (state.level.isOnPath(neighbor) || state.isBridgeAt(neighbor))
                    }
                )
            } else {
                affectedPositions.addAll(
                    targetPos.getHexNeighborsWithinRadius(radius, state.level.gridWidth, state.level.gridHeight)
                        .filter { state.level.isOnPath(it) || state.isBridgeAt(it) }
                )
            }
            
            // Count enemies in affected area (considering immunities)
            val affectedEnemies = candidates.filter { attacker ->
                !attacker.isDefeated.value && 
                affectedPositions.contains(attacker.position.value) &&
                when (defender.type.attackType) {
                    AttackType.AREA -> attacker.canBeDamagedByFireball()
                    AttackType.LASTING -> attacker.canBeDamagedByAcid()
                    else -> true  // Should not happen for other attack types
                }
            }
            
            // Calculate score: number of enemies hit + sum of threat scores + proximity to goal
            val enemyCount = affectedEnemies.size
            val totalThreat = affectedEnemies.sumOf { threatScore(it) }
            val avgDistanceToGoal = if (affectedEnemies.isNotEmpty()) {
                affectedEnemies.map { estimateRemainingDistanceToGoal(it) }.average()
            } else {
                Double.MAX_VALUE
            }
            
            Triple(targetPos, enemyCount * 1000 + totalThreat, avgDistanceToGoal)
        }
        
        // Select position with highest score (most enemies + highest threat)
        // Tie-breaker: closest to goal
        return positionScores.maxWithOrNull(
            compareBy<Triple<Position, Int, Double>> { it.second }
                .thenBy { -it.third }  // Negative because lower distance is better
        )?.first
    }

    private fun threatScore(attacker: Attacker): Int {
        // Higher score = higher priority
        return when (attacker.type) {
            AttackerType.EWHAD -> 100
            AttackerType.DRAGON -> 90
            AttackerType.GREEN_WITCH -> 80
            AttackerType.RED_WITCH -> 75
            AttackerType.EVIL_MAGE -> 70
            AttackerType.EVIL_WIZARD -> 65
            AttackerType.RED_DEMON -> 60
            AttackerType.BLUE_DEMON -> 55
            else -> 0
        }
    }

    private fun estimateRemainingDistanceToGoal(attacker: Attacker): Int {
        val currentPos = attacker.position.value
        val nextGoal = attacker.currentTarget?.value
        val finalGoal = findClosestTargetPosition(currentPos)

        return if (nextGoal != null) {
            // Estimate remaining progress as: distance to nextGoal + heuristic distance from nextGoal to final goal
            currentPos.distanceTo(nextGoal) + nextGoal.distanceTo(finalGoal)
        } else {
            currentPos.distanceTo(finalGoal)
        }
    }
    
    /**
     * Process dragon greed: eat adjacent units based on greed level.
     * Called after dragon movement.
     */
    private fun processDragonGreed(dragon: Attacker) {
        if (!dragon.type.isDragon || dragon.greed <= 0) return
        
        val dragonPos = dragon.position.value
        val neighbors = dragonPos.getHexNeighbors()
        
        // Find all adjacent units (excluding Ewhad)
        val adjacentUnits = state.attackers.filter { unit ->
            unit.id != dragon.id &&
            !unit.isDefeated.value &&
            unit.type != AttackerType.EWHAD &&
            neighbors.contains(unit.position.value)
        }
        
        // Sort by health (eat weakest first) and limit to greed amount
        val unitsToEat = adjacentUnits.sortedBy { it.currentHealth.value }.take(dragon.greed)
        
        for (unit in unitsToEat) {
            if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
            println("Dragon ${dragon.id} (greed ${dragon.greed}) eating adjacent ${unit.type} at ${unit.position.value}, gaining ${unit.currentHealth.value} HP")
            }
            dragon.currentHealth.value += unit.currentHealth.value
            unit.isDefeated.value = true
        }
        
        // Update dragon level after eating
        if (unitsToEat.isNotEmpty()) {
            dragon.updateDragonLevel()
        }
    }
    
    /**
     * Find the nearest mine to a position.
     * Returns the mine defender and its distance, or null if no mines exist.
     */
    private fun findNearestMine(from: Position): Pair<Defender, Int>? {
        val mines = state.defenders.filter { 
            it.type == DefenderType.DWARVEN_MINE && 
            !state.destroyedMinePositions.contains(it.position.value)
        }
        
        if (mines.isEmpty()) return null
        
        return mines.map { mine -> 
            Pair(mine, from.distanceTo(mine.position.value))
        }.minByOrNull { it.second }
    }
    
    /**
     * Find the closest target position from a given position.
     * Used for dragons when not targeting mines.
     */
    private fun findClosestTargetPosition(from: Position): Position {
        return state.level.targetPositions.minByOrNull { 
            from.distanceTo(it) 
        } ?: state.level.targetPositions.first()
    }
    
    /**
     * Check if dragon should target mines (greed > 5 and mines exist).
     * Updates dragon's target if needed.
     */
    private fun updateDragonMineTargeting(dragon: Attacker) {
        if (!dragon.type.isDragon || dragon.greed <= 5) {
            // Not greedy enough, clear mine target if set
            if (dragon.targetMineId.value != null) {
                dragon.targetMineId.value = null
                // Target closest target position instead of first
                dragon.currentTarget?.value = if (state.level.waypoints.isNotEmpty()) {
                    // Use the first waypoint's next target, not the waypoint position itself
                    state.level.waypoints.first().nextTarget
                } else {
                    findClosestTargetPosition(dragon.position.value)
                }
            }
            return
        }
        
        // Very greedy - target nearest mine
        val nearestMine = findNearestMine(dragon.position.value)
        if (nearestMine != null) {
            val (mine, _) = nearestMine
            if (dragon.targetMineId.value != mine.id) {
                dragon.targetMineId.value = mine.id
                dragon.currentTarget?.value = mine.position.value
                dragon.mineWarningShown.value = false
                if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                println("Dragon ${dragon.id} (greed ${dragon.greed}) now targeting mine ${mine.id} at ${mine.position.value}")
                }
            }
        } else {
            // No mines left, go back to closest target
            if (dragon.targetMineId.value != null) {
                dragon.targetMineId.value = null
                // Target closest target position instead of first
                dragon.currentTarget?.value = if (state.level.waypoints.isNotEmpty()) {
                    // Use the first waypoint's next target, not the waypoint position itself
                    state.level.waypoints.first().nextTarget
                } else {
                    findClosestTargetPosition(dragon.position.value)
                }
                if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                println("Dragon ${dragon.id} no more mines, returning to closest target")
                }
            }
        }
    }
    
    /**
     * Check if dragon can reach its target mine in the next turn and show warning.
     * Warning is shown when dragon could reach mine on next move based on its movement pattern.
     */
    private fun checkMineWarning(dragon: Attacker) {
        if (dragon.targetMineId.value == null || dragon.mineWarningShown.value) return
        
        val targetMine = state.defenders.find { it.id == dragon.targetMineId.value }
        if (targetMine == null) return
        
        // Calculate if dragon can reach mine in next turn
        // Dragon alternates: odd turns walk (1 tile), even turns fly (5 tiles)
        val nextTurnNumber = dragon.dragonTurnsSinceSpawned.value + 1
        val isNextTurnOdd = nextTurnNumber % 2 == 1
        val nextTurnSpeed = if (isNextTurnOdd) 1 else 5  // Walking or flying
        
        val currentPos = dragon.position.value
        val minePos = targetMine.position.value
        val distance = currentPos.distanceTo(minePos)
        
        // Check if dragon can reach mine with next turn's movement
        val canReachNextTurn = distance <= nextTurnSpeed
        
        if (canReachNextTurn) {
            // Dragon can reach mine next turn, show warning
            if (!state.mineWarnings.contains(targetMine.id)) {
                state.mineWarnings.add(targetMine.id)
            }
            dragon.mineWarningShown.value = true
            if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
            println("Warning: Dragon ${dragon.id} can reach mine ${targetMine.id} next turn! (distance: $distance, next speed: $nextTurnSpeed)")
            }
        }
    }
    
    /**
     * Destroy mine if dragon reaches it.
     * Dragon gains health equal to a new dragon (500 HP).
     */
    private fun checkAndDestroyMine(dragon: Attacker) {
        if (dragon.targetMineId.value == null) return
        
        val targetMine = state.defenders.find { it.id == dragon.targetMineId.value }
        if (targetMine == null) return
        
        // Check if dragon reached the mine position (now dragons can move to mine tiles)
        val isAtMine = dragon.position.value == targetMine.position.value
        
        if (isAtMine && dragon.mineWarningShown.value) {
            // Destroy the mine - dragon has reached it and warning was already shown
            if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
            println("Dragon ${dragon.id} destroys mine ${targetMine.id} at ${targetMine.position.value}")
            }
            
            // Add health (same as new dragon base health: 500)
            val healthGain = AttackerType.DRAGON.health
            dragon.currentHealth.value += healthGain
            dragon.updateDragonLevel()
            
            // Mark position as destroyed
            state.destroyedMinePositions.add(targetMine.position.value)
            
            // Remove the mine
            state.defenders.remove(targetMine)
            
            // Remove warning
            state.mineWarnings.remove(targetMine.id)
            
            // Clear target to find next mine
            dragon.targetMineId.value = null
            dragon.mineWarningShown.value = false
        }
    }
    
    // Turn Management
    fun startFirstPlayerTurn() {
        if (state.phase.value != GamePhase.INITIAL_BUILDING) return
        
        // Play battle start sound
        GlobalSoundManager.playSound(SoundEvent.BATTLE_START)
        
        // Start tracking for achievement purposes
        startTurnTracking()
        
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
            // Use fixed spawn point if specified, otherwise use round-robin
            val preferredSpawnPoint = plannedSpawn.spawnPoint ?: spawnPoints[index % spawnPoints.size]
            
            // Find a free position near the preferred spawn point
            val spawnPos = enemyMovement.findFreePositionNear(preferredSpawnPoint)
            
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
            
            // Get initial target based on preferred spawn point (BEFORE congestion offsets)
            val initialTarget = enemyMovement.getInitialTarget(preferredSpawnPoint)
            val attacker = Attacker(
                id = state.nextAttackerId.value++,
                type = plannedSpawn.attackerType,
                position = mutableStateOf(spawnPos),
                level = mutableStateOf(plannedSpawn.level),
                currentTarget = mutableStateOf(initialTarget)
            )
            state.attackers.add(attacker)
            if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
            println("Spawned attacker ${attacker.id} at $spawnPos (preferred: $preferredSpawnPoint) with initial target: $initialTarget")
            }
        }

        // Move goblins immediately after initial spawning (this is not during enemy turn)
        enemyMovement.moveGoblinsAfterSpawn()
    }


    data class EnemyTurnMovements(
        val allMovementSteps: List<List<Pair<Int, Position>>>,
        val attackersStoppedByBarricade: Set<Pair<Attacker, Position>>
    )

    /**
     * Calculate all movement steps for attackers during enemy turn without applying them.
     * Returns a list of movement steps, where each step contains all movements that should happen together.
     * Uses a simulated approach to handle collisions between units moving simultaneously.
     */
    fun calculateEnemyTurnMovements(): EnemyTurnMovements {
        val allMovementSteps = mutableListOf<List<Pair<Int, Position>>>()
        
        // Get all non-defeated attackers, separating dragons from regular units
        val allAttackers = state.attackers.filter { !it.isDefeated.value }
        val dragons = allAttackers.filter { it.type.isDragon }
        val regularAttackers = allAttackers.filter { !it.type.isDragon }.toMutableList()
        
        if (allAttackers.isEmpty()) return EnemyTurnMovements(allMovementSteps, emptySet())
        
        // Handle dragon movement separately (they have special alternating walk/fly behavior)
        for (dragon in dragons) {
            val movementPath = enemyMovement.calculateDragonMovementPath(dragon)
            for (position in movementPath) {
                allMovementSteps.add(listOf(Pair(dragon.id, position)))
            }
        }
        
        // Handle regular attacker movement
        if (regularAttackers.isEmpty()) return EnemyTurnMovements(allMovementSteps, emptySet())
        
        // Track current positions for collision detection during simulation
        val currentPositions = mutableMapOf<Int, Position>()
        regularAttackers.forEach { currentPositions[it.id] = it.position.value }
        
        // Track which attackers encountered a barricade and should stop moving
        val attackersStoppedByBarricade = mutableSetOf<Pair<Attacker, Position>>()
        
        // Find the maximum speed to know how many steps to simulate
        val maxSpeed = regularAttackers.maxOfOrNull { it.type.speed } ?: 0
        
        // Pre-compute imminent bombs (turnsRemaining <= 1) for flee behavior
        val imminentBombs = state.activeSpellEffects.filter { it.spell == SpellType.BOMB && it.position != null && it.turnsRemaining <= 1 }
        
        // Simulate movement step by step
        for (stepIndex in 0 until maxSpeed) {
            val movementsInThisStep = mutableListOf<Pair<Int, Position>>()
            val positionsToOccupy = mutableSetOf<Position>()
            
            for (attacker in regularAttackers) {
                val currentPos = currentPositions[attacker.id] ?: continue
                
                // Check if attacker is frozen
                val isFrozen = state.activeSpellEffects.any {
                    it.spell == SpellType.FREEZE_SPELL && it.attackerId == attacker.id
                }
                if (isFrozen) {
                    println("Attacker ${attacker.id} (${attacker.type}) is frozen, skipping movement")
                    continue
                }
                
                // Skip if this attacker already encountered a barricade
                if (attackersStoppedByBarricade
                        .map { it.first }
                        .map { it.id }
                        .contains(attacker.id)) continue
                
                // Calculate effective speed by subtracting movement penalty from spike barbs
                var effectiveSpeed = maxOf(1, attacker.type.speed - attacker.movementPenalty.value)
                
                // Check if attacker is in a cooling area (reduces speed by 1)
                val isInCoolingArea = state.activeSpellEffects.any { effect ->
                    effect.spell == SpellType.COOLING_SPELL && 
                    effect.position != null &&
                    currentPos.hexDistanceTo(effect.position) <= 2
                }
                if (isInCoolingArea) {
                    effectiveSpeed = maxOf(1, effectiveSpeed - 1)
                    if (stepIndex == 0) {
                        println("Attacker ${attacker.id} (${attacker.type}) is in cooling area, speed reduced to $effectiveSpeed")
                    }
                }
                
                // Check if this attacker has more moves left
                if (stepIndex >= effectiveSpeed) continue
                
                // Check if unit should build a bridge BEFORE calculating path
                // This allows units adjacent to rivers to build bridges before moving sideways
                if (attacker.type.canBuildBridge && !attacker.isBuildingBridge.value) {
                    val bridgeablePositions = bridgeSystem.canBuildBridge(attacker)
                    if (bridgeablePositions.isNotEmpty() && bridgeSystem.shouldAutoBuildBridge(attacker)) {
                        val bridgeBuilt = bridgeSystem.autoBuildBridge(attacker)
                        if (bridgeBuilt) {
                            println("Unit ${attacker.id} (${attacker.type}) built bridge at $currentPos during movement at turn ${state.turnNumber.value}")
                            if (attacker.isDefeated.value) {
                                // Unit sacrificed itself for the bridge, cannot move
                                continue
                            }
                            // Bridge is built, now calculate path with the bridge
                        }
                    }
                }
                
                // Use the attacker's current target if set, otherwise use level target
                // Special case: Green Witch moves towards damaged enemies (especially Ewhad)
                // Special case: Red Witch moves towards closest not-disabled tower
                val target = if (attacker.type == AttackerType.GREEN_WITCH) {
                    val healingTarget = enemyAbilities.findHealingTarget(attacker)
                    if (healingTarget != null) {
                        // Move towards the healing target
                        if (stepIndex == 0) {
                            println("Green witch ${attacker.id} at $currentPos moving towards healing target ${healingTarget.type} at ${healingTarget.position.value}")
                        }
                        healingTarget.position.value
                    } else {
                        // No damaged enemies, move towards normal target
                        attacker.currentTarget?.value ?: state.level.targetPositions.first()
                    }
                } else if (attacker.type == AttackerType.RED_WITCH) {
                    val towerTarget = enemyAbilities.findTowerTarget(attacker)
                    if (towerTarget != null) {
                        // Move towards the tower to disable it
                        if (stepIndex == 0) {
                            println("Red witch ${attacker.id} at $currentPos moving towards tower at $towerTarget")
                        }
                        towerTarget
                    } else {
                        // No towers to disable, move towards normal target
                        attacker.currentTarget?.value ?: state.level.targetPositions.first()
                    }
                } else {
                    attacker.currentTarget?.value ?: state.level.targetPositions.first()
                }
                if (stepIndex == 0) {
                    println("Enemy turn: Attacker ${attacker.id} (${attacker.type}) at $currentPos pathing to target: $target")
                }
                
                // Enemy flee behavior: if a bomb with 1 turn remaining is nearby, move away from it
                val nearbyBomb = if (imminentBombs.isNotEmpty()) {
                    imminentBombs.find { effect ->
                        effect.position != null && currentPos.hexDistanceTo(effect.position) <= 3
                    }
                } else null
                var path = if (nearbyBomb?.position != null) {
                    val bombPos = nearbyBomb.position
                    // Find a valid path tile neighbor that is farther from the bomb
                    val neighbors = currentPos.getHexNeighbors().filter { neighbor ->
                        neighbor.x >= 0 && neighbor.x < state.level.gridWidth &&
                        neighbor.y >= 0 && neighbor.y < state.level.gridHeight &&
                        (state.level.isOnPath(neighbor) || state.level.isSpawnPoint(neighbor)) &&
                        state.barricades.none { b -> b.position == neighbor && !b.isDestroyed() } &&
                        state.attackers.none { a -> !a.isDefeated.value && a.id != attacker.id && a.position.value == neighbor }
                    }
                    val fleeTo = neighbors.maxByOrNull { it.hexDistanceTo(bombPos) }
                    if (fleeTo != null && fleeTo.hexDistanceTo(bombPos) > currentPos.hexDistanceTo(bombPos)) {
                        if (LogConfig.ENABLE_ENEMY_AI_LOGGING) {
                            println("Attacker ${attacker.id} (${attacker.type}) fleeing from bomb at $bombPos to $fleeTo")
                        }
                        listOf(currentPos, fleeTo)
                    } else {
                        pathfinding.findPath(currentPos, target, attacker)
                    }
                } else {
                    pathfinding.findPath(currentPos, target, attacker)
                }
                if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                println("path: $path")
                }
                // println("CETM ----------------------- Unit ${attacker.id} (${attacker.type}) at $currentPos: findPath returned path of size ${path.size}")

                // If still no path after bridge attempt, try one more time
                if (path.size < 2 && attacker.type.canBuildBridge && !attacker.isBuildingBridge.value) {
                    if (bridgeSystem.shouldAutoBuildBridge(attacker)) {
                        val bridgeBuilt = bridgeSystem.autoBuildBridge(attacker)
                        if (bridgeBuilt) {
                            println("Unit ${attacker.id} (${attacker.type}) built bridge (fallback) during movement at turn ${state.turnNumber.value}")
                            if (attacker.isDefeated.value) {
                                continue
                            }
                            path = pathfinding.findPath(currentPos, target, attacker)
                        }
                    }
                }
                
                if (path.size < 2) {
                    // No path found - check if we should attack a barricade
                    val nextPos = pathfinding.moveTowards(currentPos, target, attacker)
                    if (nextPos != currentPos) {
                        // moveTowards found a barricade to attack
                        val barricadeAtPos = barricadeSystem.getBarricadeAt(nextPos)
                        if (barricadeAtPos != null && !barricadeAtPos.isDestroyed()) {
                            attackersStoppedByBarricade.add(Pair(attacker, nextPos))
                            if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                            println("CETM -A---------------------- attackersStoppedByBarricade Unit ${attacker.id} (${attacker.type}) at $currentPos will attack barricade at $nextPos (HP: ${barricadeAtPos.healthPoints.value})")
                            }
                            continue
                        }
                    }
                    // No movement possible
                    continue
                }
                
                val newPos = path[1]  // Next position in path
                val barricadeAtPath1 = barricadeSystem.getBarricadeAt(newPos)
                // println("CETM ----------------------- Unit ${attacker.id} (${attacker.type}) at $currentPos wants to move to $newPos towards target $target (barricade at newPos: ${barricadeAtPath1 != null && !barricadeAtPath1.isDestroyed()})")
                
                // Check if there's a barricade at the new position (non-flying units only)
                // If so, this unit will attack the barricade and stop moving for the rest of this turn
                val isFlying = attacker.isFlying?.value == true
                if (!isFlying) {
                    val barricadeAtNewPos = barricadeSystem.getBarricadeAt(newPos)
                    if (barricadeAtNewPos != null && !barricadeAtNewPos.isDestroyed()) {
                        // Mark this attacker as stopped - it will attack the barricade in applyMovement
                        attackersStoppedByBarricade.add(Pair(attacker, newPos))

                        if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                        println("CETM -D---------------------- attackersStoppedByBarricade Unit ${attacker.id} (${attacker.type}) at $currentPos cannot move to $newPos due to barricade, will attack barricade instead")
                        }

                        continue
                    }
                }
                
                // Check if this position is already occupied or will be occupied by another unit in this step
                // Exception: Allow multiple units to move to the target position (they get defeated immediately)
                val isOccupied = if (state.level.isTargetPosition(newPos)) {
                    false  // Target position can accommodate multiple units
                } else {
                    currentPositions.any { (id, pos) ->
                        id != attacker.id && pos == newPos
                    }
                            || positionsToOccupy.contains(newPos)
                            || barricadeSystem.getBarricadeAt(newPos) != null
                }

                // println("CETM ----------------------- Unit ${attacker.id} (${attacker.type}) at $currentPos trying to move to $newPos. Occupied: $isOccupied")
                
                if (!isOccupied) {
                    movementsInThisStep.add(Pair(attacker.id, newPos))
                    if (!state.level.isTargetPosition(newPos)) {
                        // Only mark non-target positions as occupied
                        positionsToOccupy.add(newPos)
                    }
                    currentPositions[attacker.id] = newPos
                } else {
                    // If optimal path is blocked, try to find an alternative position
                    val alternativePos = findAlternativePosition(currentPos, target, attacker.id, currentPositions, positionsToOccupy)
                    if (alternativePos != null) {
                        if (barricadeSystem.getBarricadeAt(alternativePos) == null) {
                            movementsInThisStep.add(Pair(attacker.id, alternativePos))
                            if (!state.level.isTargetPosition(alternativePos)) {
                                positionsToOccupy.add(alternativePos)
                            }
                            currentPositions[attacker.id] = alternativePos
                        } else {
                            handleBarricades(currentPos, target, attacker, attackersStoppedByBarricade)
                        }
                    } else {
                        // No alternative position found - check if there are nearby barricades to attack
                        // Use moveTowards() to select the optimal barricade
                        val barricadePos = pathfinding.moveTowards(currentPos, target, attacker)
                        if (barricadePos != currentPos) {
                            // moveTowards found a barricade to attack
                            val barricadeAtPos = barricadeSystem.getBarricadeAt(barricadePos)
                            if (barricadeAtPos != null && !barricadeAtPos.isDestroyed()) {
                                attackersStoppedByBarricade.add(Pair(attacker, barricadePos))
                                if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                                println("CETM -B---------------------- attackersStoppedByBarricade Unit ${attacker.id} (${attacker.type}) at $currentPos blocked by other units, will attack optimal barricade at $barricadePos (HP: ${barricadeAtPos.healthPoints.value})")
                                }
                            }
                        }
                    }
                    // If still no movement, unit stays in place for this step

                    if (barricadeSystem.getBarricadeAt(newPos) != null) {
                        // If the original newPos has a barricade, mark attacker as stopped to handle barricade attack in applyMovement
                        attackersStoppedByBarricade.add(Pair(attacker, newPos))
                        if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                        println("CETM -F---------------------- attackersStoppedByBarricade Unit ${attacker.id} (${attacker.type}) at $currentPos cannot move to $newPos due to barricade, will attack barricade instead")
                        }
                    }
                }
            }
            // println("CETM ----------------------- End of step $stepIndex, movements planned: ${movementsInThisStep.size}, positions to occupy next: $positionsToOccupy")
            // println("CETM -- barricade-stopped attackers so far: $attackersStoppedByBarricade")
            
            if (movementsInThisStep.isNotEmpty()) {
                allMovementSteps.add(movementsInThisStep)
            }
        }
        
        return EnemyTurnMovements(allMovementSteps, attackersStoppedByBarricade)
    }

    private fun handleBarricades(
        currentPos: Position,
        target: Position,
        attacker: Attacker,
        attackersStoppedByBarricade: MutableSet<Pair<Attacker, Position>>
    ) {
        // Both primary and alternative positions have barricades
        val barricadePathSet = mutableSetOf<Pair<Barricade, List<Position>>>()
        /*
        val excludedBarricadePositions = mutableSetOf<Position>()

        val pathIgnoringBarricades = pathfinding.findPath(currentPos, target, attacker, ignoreBarricades = true)
        val barricadeAtPpathIgnoringBarricades = barricadeSystem.getBarricadeAt(pathIgnoringBarricades[1])
        if (barricadeAtPpathIgnoringBarricades != null) {
            excludedBarricadePositions.add(pathIgnoringBarricades[1])
            barricadePathSet.add(
                Pair(
                    barricadeAtPpathIgnoringBarricades,
                    pathIgnoringBarricades
                )
            )
        }

        val path1 = pathfinding.findPath(currentPos, target, attacker)
        val barricadeAtPath1 = barricadeSystem.getBarricadeAt(path1[1])
        if (barricadeAtPath1 != null) {
            excludedBarricadePositions.add(path1[1])
            barricadePathSet.add(Pair(barricadeAtPath1, path1))
        }

        // Loop to find all barricade options until we can reach the target
        var interator = 0
        while (true) {
            interator++
            val currentPath = pathfinding.findPath(currentPos, target, attacker, excludedBarricadePositions)
            val barricadeAtPath = barricadeSystem.getBarricadeAt(currentPath[1])
            if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
            println("currentPAth: $currentPath, barricadeAtPath: ${barricadeAtPath != null}")
            }
            if (barricadeAtPath != null) {
                excludedBarricadePositions.add(currentPath[1])
                barricadePathSet.add(Pair(barricadeAtPath, currentPath))
            }

            if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
            println("excludedBarricadePositions after iteration $interator: $excludedBarricadePositions")
            }
            val pathToTargetWithoutFoundBarricades = pathfinding.findPath(currentPos, target, attacker, excludedBarricadePositions, ignoreBarricades = true)

            if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
            println("kkkkkkkkkkk $interator ${pathToTargetWithoutFoundBarricades.last()} $target")
            }

            if (target != pathToTargetWithoutFoundBarricades.last()) {
                break
            }
            if (interator > 10) {
                println("Too many iterations finding barricade paths, breaking loop")
                break
            }
        }

         */

        // First, check the direct path (ignoring barricades) separately
        val pathIgnoringBarricades = pathfinding.findPath(currentPos, target, attacker, ignoreBarricades = true)
        if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
        println("[DEBUG] pathIgnoringBarricades: $pathIgnoringBarricades")
        }
        if (pathIgnoringBarricades.size > 1) {
            val barricadeAtPath = barricadeSystem.getBarricadeAt(pathIgnoringBarricades[1])
            if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
            println("[DEBUG] pathIgnoringBarricades[1]=${pathIgnoringBarricades[1]}, barricade=${barricadeAtPath?.let { "HP=${it.healthPoints.value} at ${it.position}" } ?: "null"}")
            }
            if (barricadeAtPath != null) {
                barricadePathSet.add(Pair(barricadeAtPath, pathIgnoringBarricades))
            }
        }
        
        // Now loop through alternative paths, progressively excluding positions found
        // This replaces the hardcoded path1, path2, path3 approach
        val excludedPositions = mutableSetOf<Position>()
        var maxIterations = 10  // Safety limit (original code had 3, but we support more)
        var iteration = 0
        
        while (iteration < maxIterations) {
            iteration++
            
            // Find path excluding previously found barricade positions
            val currentPath = pathfinding.findPath(currentPos, target, attacker, excludedPositions)
            if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
            println("[DEBUG] Iteration $iteration: excludedPositions=$excludedPositions, currentPath=$currentPath")
            }
            
            // Check if path has at least 2 positions (current + next)
            if (currentPath.size < 2) {
                println("[DEBUG] Iteration $iteration: Breaking - path too short (size=${currentPath.size})")
                break
            }
            
            // Check if next position has a barricade
            val nextPos = currentPath[1]
            val barricadeAtNextPos = barricadeSystem.getBarricadeAt(nextPos)
            if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
            println("[DEBUG] Iteration $iteration: nextPos=$nextPos, barricade=${barricadeAtNextPos?.let { "HP=${it.healthPoints.value} at ${it.position}" } ?: "null"}")
            }
            
            if (barricadeAtNextPos != null) {
                // Found a barricade - add to set
                barricadePathSet.add(Pair(barricadeAtNextPos, currentPath))
                // Exclude this position for next iteration
                excludedPositions.add(nextPos)
                // Continue loop to find more barricades
            } else {
                // No barricade at next position
                // Check if this path reaches the target - if so, we found a clear path
                if (currentPath.last() == target) {
                    println("[DEBUG] Iteration $iteration: Breaking - found clear path to target")
                    break
                } else {
                    // Path doesn't have a barricade at [1] AND doesn't reach target
                    // This means pathfinding is routing around something, exclude this position too
                    if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                    println("[DEBUG] Iteration $iteration: No barricade at nextPos, but path doesn't reach target. Excluding $nextPos and continuing.")
                    }
                    excludedPositions.add(nextPos)
                    // Continue to try finding another path
                }
            }
        }

        var min = Int.MAX_VALUE
        var pathOfLeastEffort: Pair<Barricade, List<Position>>? = null

        if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
        println("[REFACTORED] barricadePathSet size: ${barricadePathSet.size}")
        }
        
        barricadePathSet.forEach { (barricade, path) ->
            val hp = if (barricade.hasTower()) {
                barricade.healthPoints.value - 100
            } else {
                barricade.healthPoints.value
            }
            val pathSteps = path.size - 1
            val effort = (hp / getBarricadeDamageForEnemyUnit(attacker)) + pathSteps

            if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
            println("[REFACTORED] Barricade at ${barricade.position}, HP=${barricade.healthPoints.value}, hasTower=${barricade.hasTower()}, effectiveHP=$hp, pathSteps=$pathSteps, effort=$effort")
            }

            if (effort < min) {
                min = effort
                pathOfLeastEffort = Pair(barricade, path)
            }
        }

        if (pathOfLeastEffort != null) {
            println("[REFACTORED] SELECTED: Barricade at ${pathOfLeastEffort.first.position}, HP=${pathOfLeastEffort.first.healthPoints.value}")
            attackersStoppedByBarricade.add(Pair(attacker, pathOfLeastEffort.first.position))
        }
    }

    /**
     * Apply damage to health points when an enemy reaches the target.
     * Handles variable damage based on enemy type and marks the attacker as defeated.
     */
    private fun applyTargetDamage(attacker: Attacker) {
        val damage = attacker.calculateTargetDamage()
        if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
        println("!!! ENEMY ENTERED TARGET !!! Turn ${state.turnNumber.value}: ${attacker.type} (ID ${attacker.id}) at ${attacker.position.value} dealt $damage damage. HP: ${state.healthPoints.value} -> ${state.healthPoints.value - damage}")
        }
        state.healthPoints.value = maxOf(0, state.healthPoints.value - damage)
        attacker.isDefeated.value = true
    }
    
    /**
     * Apply a single movement step for the given attacker.
     */
    fun applyMovement(attackerId: Int, newPosition: Position) {
        val attacker = state.attackers.find { it.id == attackerId } ?: return
        if (attacker.isDefeated.value)  {
            println("attackBarricade A")
            if (attackBarricade(newPosition, attacker)) return
        }

        // Special handling for dragons - they can eat other units
        if (attacker.type.isDragon) {
            // Check if landing on another unit
            val unitAtPosition = state.attackers.find {
                it.id != attacker.id && !it.isDefeated.value && it.position.value == newPosition
            }
            
            if (unitAtPosition != null && unitAtPosition.type != AttackerType.EWHAD) {
                // Dragon eats the unit and gains its health
                if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                println("Dragon ${attacker.id} eating ${unitAtPosition.type} at $newPosition, gaining ${unitAtPosition.currentHealth.value} HP")
                }
                attacker.currentHealth.value += unitAtPosition.currentHealth.value
                attacker.updateDragonLevel()  // Update level based on new health
                unitAtPosition.isDefeated.value = true
                attacker.position.value = newPosition
                
                // Check for traps at the new position
                mineOperations.checkAndActivateTrapForAttacker(attacker)
                
                // Only continue if dragon was not defeated by trap
                if (!attacker.isDefeated.value) {
                    // Check if reached a waypoint and update target
                    // Only update if the waypoint position is the current target
                    if (state.level.isWaypoint(newPosition) && attacker.currentTarget?.value == newPosition) {
                        val waypoint = state.level.getWaypointAt(newPosition)
                        if (waypoint != null) {
                            attacker.currentTarget.value = waypoint.nextTarget
                            if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                            println("Dragon ${attacker.id} reached waypoint at $newPosition, next target: ${waypoint.nextTarget}")
                            }
                        }
                    }
                }
            } else if (unitAtPosition != null && unitAtPosition.type == AttackerType.EWHAD) {
                // Can't land on Ewhad, try to find alternate position
                val alternatePos = newPosition.getHexNeighbors()
                    .filter { pos ->
                        state.level.isOnPath(pos) &&
                        state.attackers.none { it.position.value == pos && !it.isDefeated.value }
                    }
                    .minByOrNull { it.distanceTo(state.level.targetPositions.first()) }
                
                if (alternatePos != null) {
                    println("Dragon ${attacker.id} can't land on Ewhad, moving to alternate position $alternatePos")
                    attacker.position.value = alternatePos
                    
                    // Check for traps at the alternate position
                    mineOperations.checkAndActivateTrapForAttacker(attacker)
                    
                    // Only continue if dragon was not defeated by trap
                    if (!attacker.isDefeated.value) {
                        // Check if reached a waypoint and update target
                        // Only update if the waypoint position is the current target
                        if (state.level.isWaypoint(alternatePos) && attacker.currentTarget?.value == alternatePos) {
                            val waypoint = state.level.getWaypointAt(alternatePos)
                            if (waypoint != null) {
                                attacker.currentTarget.value = waypoint.nextTarget
                                if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                                println("Dragon ${attacker.id} reached waypoint at $alternatePos, next target: ${waypoint.nextTarget}")
                                }
                            }
                        }
                    }
                } else {
                    if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                    println("Dragon ${attacker.id} blocked by Ewhad at $newPosition, staying in place")
                    }
                    // Stay in current position
                }
            } else {
                // No unit at target position, move normally
                attacker.position.value = newPosition
                
                // Check for traps at the new position
                mineOperations.checkAndActivateTrapForAttacker(attacker)
            }
            
            // Check if reached a waypoint and update target
            // Only update if the waypoint position is the current target
            if (state.level.isWaypoint(attacker.position.value) && attacker.currentTarget?.value == attacker.position.value) {
                val waypoint = state.level.getWaypointAt(attacker.position.value)
                if (waypoint != null) {
                    // Update target to the next waypoint or final target
                    attacker.currentTarget.value = waypoint.nextTarget
                    if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                    println("Dragon ${attacker.id} reached waypoint at ${attacker.position.value}, next target: ${waypoint.nextTarget}")
                    }
                }
            }
            
            // Check if reached any target
            if (state.level.isTargetPosition(attacker.position.value)) {
                applyTargetDamage(attacker)
            }
            
            // Process dragon greed mechanics if not defeated
            if (!attacker.isDefeated.value) {
                // Update mine targeting if greed > 5
                updateDragonMineTargeting(attacker)
                
                // Check for mine warning
                checkMineWarning(attacker)
                
                // Check and destroy mine if applicable
                checkAndDestroyMine(attacker)
                
                // Eat adjacent units based on greed
                processDragonGreed(attacker)
            }
            
            return
        }
        
        // Regular unit movement (non-dragons)
        // Check if position is occupied by another alive attacker
        // Exception: Allow movement to target position even if occupied (units get defeated immediately)
        val isOccupied = if (state.level.isTargetPosition(newPosition)) {
            false  // Target can accommodate multiple units
        } else {
            state.attackers.any {
                it.id != attacker.id && !it.isDefeated.value && it.position.value == newPosition
            }
        }
        
        // Only move if position is not occupied
        if (!isOccupied) {
            val oldPosition = attacker.position.value
            attacker.position.value = newPosition
            
            // Check for traps at the new position (only if enemy actually moved)
            if (oldPosition != newPosition) {
                mineOperations.checkAndActivateTrapForAttacker(attacker)
            }
            
            // Only continue if enemy was not defeated by trap
            if (!attacker.isDefeated.value) {
                // Check if reached a waypoint and update target
                // Only update if the waypoint position is the current target
                if (state.level.isWaypoint(newPosition) && attacker.currentTarget?.value == newPosition) {
                    val waypoint = state.level.getWaypointAt(newPosition)
                    if (waypoint != null) {
                        // Update target to the next waypoint or final target
                        attacker.currentTarget.value = waypoint.nextTarget
                        if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                        println("Attacker ${attacker.id} reached waypoint at $newPosition, next target: ${waypoint.nextTarget}")
                        }
                    }
                }
                
                // Check if reached any target
                if (state.level.isTargetPosition(newPosition)) {
                    applyTargetDamage(attacker)
                }
            }
        }
    }

    fun attackBarricade(
        newPosition: Position,
        attacker: Attacker
    ): Boolean {
        // Check if there's a barricade AT the new position
        // Flying dragons can fly over barricades (like they fly over non-playable tiles)

        // Flying dragons can move over barricades without attacking them
        // (They pass over barricades just like they pass over non-playable tiles)
        val barricadeAtPosition = barricadeSystem.getBarricadeAt(newPosition)
        val isFlying = attacker.isFlying.value

        if (barricadeAtPosition != null && !barricadeAtPosition.isDestroyed() && !isFlying) {
            println("Attack barricade: Attacker ${attacker.id} (${attacker.type}) at ${attacker.position.value} attacks barricade at $newPosition (HP: ${barricadeAtPosition.healthPoints.value})")
            // Non-flying enemy encounters barricade - attack it instead of moving
            val damage = getBarricadeDamageForEnemyUnit(attacker)

            val wasDestroyed = barricadeSystem.handleEnemyAttackBarricade(attacker, barricadeAtPosition, damage)
            if (wasDestroyed) {
                // Barricade was destroyed, enemy moves to barricade position
                attacker.position.value = newPosition

                // Check for traps at the new position
                mineOperations.checkAndActivateTrapForAttacker(attacker)

                // Only continue if enemy was not defeated by trap
                if (!attacker.isDefeated.value) {
                    // Check waypoint and target after moving to barricade position
                    if (state.level.isWaypoint(newPosition) && attacker.currentTarget?.value == newPosition) {
                        val waypoint = state.level.getWaypointAt(newPosition)
                        if (waypoint != null) {
                            attacker.currentTarget.value = waypoint.nextTarget
                        }
                    }

                    // Check if reached target
                    if (state.level.isTargetPosition(newPosition)) {
                        applyTargetDamage(attacker)
                    }
                }
            }
            // If barricade not destroyed, enemy doesn't move (stays at current position)
            return true
        }
        return false
    }

    fun getBarricadeDamageForEnemyUnit(attacker: Attacker): Int {
        val damage = if (attacker.type.isDragon) {
            attacker.level.value * 5
        } else {
            attacker.level.value
        }
        return damage
    }

    /**
     * Prepare for enemy turn: set phase but don't spawn yet.
     * Spawning happens after movements to ensure spawn points are clear.
     */
    fun startEnemyTurn() {
        if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
        println("GameEngine.startEnemyTurn: phase=${state.phase.value}")
        }
        if (state.phase.value != GamePhase.PLAYER_TURN) {
            println("GameEngine.startEnemyTurn: Not in PLAYER_TURN phase, returning")
            return
        }
        
        state.turnNumber.value++
        state.phase.value = GamePhase.ENEMY_TURN
        if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
        println("GameEngine.startEnemyTurn: Changed phase to ENEMY_TURN, turn=${state.turnNumber.value}")
        }
        
        // Process raft movements on rivers at the start of enemy turn
        // This happens immediately when player presses "Next Turn"
        if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
        println("GameEngine.startEnemyTurn: About to call raftSystem.processRaftMovements()")
        }
        raftSystem.processRaftMovements()
        if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
        println("GameEngine.startEnemyTurn: Completed raft movement processing")
        }
        
        // Play ticking sound if any bombs are active on the level
        val hasBombs = state.activeSpellEffects.any { it.spell == SpellType.BOMB }
        if (hasBombs) {
            GlobalSoundManager.playSound(SoundEvent.BOMB_TICKING)
        }
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
     * Updates waypoint targets during movement so fast units don't stop at waypoints.
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
        
        // Track which attackers encountered a barricade and should stop moving
        val attackersStoppedByBarricade = mutableSetOf<Int>()
        
        // Find the maximum speed to know how many steps to simulate
        val maxSpeed = newlySpawned.maxOfOrNull { it.type.speed } ?: 0
        
        // Simulate movement step by step
        for (stepIndex in 0 until maxSpeed) {
            val movementsInThisStep = mutableListOf<Pair<Int, Position>>()
            val positionsToOccupy = mutableSetOf<Position>()
            
            for (attacker in newlySpawned) {
                val currentPos = currentPositions[attacker.id] ?: continue
                
                // Skip if this attacker already encountered a barricade
                if (attackersStoppedByBarricade.contains(attacker.id)) continue
                
                // Calculate effective speed by subtracting movement penalty from spike barbs
                val effectiveSpeed = maxOf(1, attacker.type.speed - attacker.movementPenalty.value)
                
                // Check if this attacker has more moves left
                if (stepIndex >= effectiveSpeed) continue
                
                // Check if unit should build a bridge BEFORE calculating path
                // This allows units adjacent to rivers to build bridges before moving sideways
                if (attacker.type.canBuildBridge && !attacker.isBuildingBridge.value) {
                    val bridgeablePositions = bridgeSystem.canBuildBridge(attacker)
                    if (bridgeablePositions.isNotEmpty() && bridgeSystem.shouldAutoBuildBridge(attacker)) {
                        val bridgeBuilt = bridgeSystem.autoBuildBridge(attacker)
                        if (bridgeBuilt) {
                            println("Newly spawned unit ${attacker.id} (${attacker.type}) built bridge at $currentPos during movement at turn ${state.turnNumber.value}")
                            if (attacker.isDefeated.value) {
                                // Unit sacrificed itself for the bridge, cannot move
                                continue
                            }
                            // Bridge is built, now calculate path with the bridge
                        }
                    }
                }
                
                // Use the attacker's current target if set, otherwise use level target
                // Special case: Green Witch moves towards damaged enemies (especially Ewhad)
                // Special case: Red Witch moves towards closest not-disabled tower
                val target = if (attacker.type == AttackerType.GREEN_WITCH) {
                    val healingTarget = enemyAbilities.findHealingTarget(attacker)
                    if (healingTarget != null) {
                        // Move towards the healing target
                        healingTarget.position.value
                    } else {
                        // No damaged enemies, move towards normal target
                        attacker.currentTarget?.value ?: state.level.targetPositions.first()
                    }
                } else if (attacker.type == AttackerType.RED_WITCH) {
                    val towerTarget = enemyAbilities.findTowerTarget(attacker)
                    if (towerTarget != null) {
                        // Move towards the tower to disable it
                        towerTarget
                    } else {
                        // No towers to disable, move towards normal target
                        attacker.currentTarget?.value ?: state.level.targetPositions.first()
                    }
                } else {
                    attacker.currentTarget?.value ?: state.level.targetPositions.first()
                }
                if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                println("Newly spawned attacker ${attacker.id} at $currentPos pathing to target: $target (currentTarget: ${attacker.currentTarget?.value})")
                }
                var path = pathfinding.findPath(currentPos, target, attacker)
                
                // If still no path after bridge attempt, try one more time
                if (path.size < 2 && attacker.type.canBuildBridge && !attacker.isBuildingBridge.value) {
                    if (bridgeSystem.shouldAutoBuildBridge(attacker)) {
                        val bridgeBuilt = bridgeSystem.autoBuildBridge(attacker)
                        if (bridgeBuilt) {
                            println("Newly spawned unit ${attacker.id} (${attacker.type}) built bridge (fallback) during movement at turn ${state.turnNumber.value}")
                            if (attacker.isDefeated.value) {
                                continue
                            }
                            path = pathfinding.findPath(currentPos, target, attacker)
                        }
                    }
                }
                
                if (path.size < 2) {
                    // No path found - check if we should attack a barricade
                    val nextPos = pathfinding.moveTowards(currentPos, target, attacker)
                    if (nextPos != currentPos) {
                        // moveTowards found a barricade to attack
                        val barricadeAtPos = barricadeSystem.getBarricadeAt(nextPos)
                        if (barricadeAtPos != null && !barricadeAtPos.isDestroyed()) {
                            attackersStoppedByBarricade.add(attacker.id)
                            // println("CETM ----------------------- Newly spawned unit ${attacker.id} (${attacker.type}) at $currentPos will attack barricade at $nextPos (HP: ${barricadeAtPos.healthPoints.value})")
                            continue
                        }
                    }
                    // No movement possible
                    continue
                }
                
                val newPos = path[1]  // Next position in path
                
                // Check if there's a barricade at the new position (non-flying units only)
                // If so, this unit will attack the barricade and stop moving for the rest of this turn
                val isFlying = attacker.isFlying?.value == true
                if (!isFlying) {
                    val barricadeAtNewPos = barricadeSystem.getBarricadeAt(newPos)
                    if (barricadeAtNewPos != null && !barricadeAtNewPos.isDestroyed()) {
                        // Mark this attacker as stopped - it will attack the barricade in applyMovement
                        attackersStoppedByBarricade.add(attacker.id)
                    }
                }
                
                // Check if this position is already occupied or will be occupied by another unit in this step
                // Exception: Allow multiple units to move to the target position (they get defeated immediately)
                val isOccupied = if (state.level.isTargetPosition(newPos)) {
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
                    if (!state.level.isTargetPosition(newPos)) {
                        // Only mark non-target positions as occupied
                        positionsToOccupy.add(newPos)
                    }
                    currentPositions[attacker.id] = newPos
                    
                    // Update waypoint target immediately if reached a waypoint
                    // This ensures the next step uses the new target
                    // Only update if the waypoint position is the current target
                    if (state.level.isWaypoint(newPos) && attacker.currentTarget?.value == newPos) {
                        val waypoint = state.level.getWaypointAt(newPos)
                        if (waypoint != null) {
                            attacker.currentTarget.value = waypoint.nextTarget
                            if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                            println("Attacker ${attacker.id} reached waypoint at $newPos during spawn movement, next target: ${waypoint.nextTarget}")
                            }
                        }
                    }
                } else {
                    // If optimal path is blocked, try to find an alternative position
                    // This is crucial for clearing spawn points
                    val alternativePos = findAlternativePosition(currentPos, target, attacker.id, currentPositions, positionsToOccupy)
                    if (alternativePos != null) {
                        movementsInThisStep.add(Pair(attacker.id, alternativePos))
                        if (!state.level.isTargetPosition(alternativePos)) {
                            positionsToOccupy.add(alternativePos)
                        }
                        currentPositions[attacker.id] = alternativePos
                        
                        // Update waypoint target if the alternative position is a waypoint
                        // Only update if the waypoint position is the current target
                        if (state.level.isWaypoint(alternativePos) && attacker.currentTarget?.value == alternativePos) {
                            val waypoint = state.level.getWaypointAt(alternativePos)
                            if (waypoint != null) {
                                attacker.currentTarget.value = waypoint.nextTarget
                                if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                                println("Attacker ${attacker.id} reached waypoint at $alternativePos during spawn movement, next target: ${waypoint.nextTarget}")
                                }
                            }
                        }
                    } else {
                        // No alternative position found - check if there are nearby barricades to attack
                        // Use moveTowards() to select the optimal barricade
                        val barricadePos = pathfinding.moveTowards(currentPos, target, attacker)
                        if (barricadePos != currentPos) {
                            // moveTowards found a barricade to attack
                            val barricadeAtPos = barricadeSystem.getBarricadeAt(barricadePos)
                            if (barricadeAtPos != null && !barricadeAtPos.isDestroyed()) {
                                attackersStoppedByBarricade.add(attacker.id)
                                if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                                println("CETM -C---------------------- attackersStoppedByBarricade Newly spawned unit ${attacker.id} (${attacker.type}) at $currentPos blocked by other units, will attack optimal barricade at $barricadePos (HP: ${barricadeAtPos.healthPoints.value})")
                                }
                            }
                        }
                    }
                    // If still no movement, unit stays in place for this step
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
        
        // Filter valid positions (on the map, on path)
        val validNeighbors = neighbors.filter { neighbor ->
            neighbor.x >= 0 && neighbor.x < state.level.gridWidth &&
            neighbor.y >= 0 && neighbor.y < state.level.gridHeight &&
            state.level.isOnPath(neighbor)
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

        // Update spell buff effects (decrement turns remaining, remove expired)
        updateSpellBuffs()
        
        // Clear bomb explosion visual effects from previous turn
        state.bombExplosionEffects.clear()

        // Process special enemy abilities
        enemyAbilities.processEnemyAbilities()
        
        // Process bridge building and bridge turn updates
        bridgeSystem.processBridges()

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
            // Decrement wizard trap cooldown
            if (defender.type == DefenderType.WIZARD_TOWER && defender.trapCooldownRemaining.value > 0) {
                defender.trapCooldownRemaining.value--
            }
        }
    }
    
    /**
     * Update spell buffs: decrement turns remaining, handle special effects, and remove expired buffs
     */
    private fun updateSpellBuffs() {
        // Create a list of indices to remove (iterate backwards to avoid index issues)
        val toRemove = mutableListOf<Int>()
        
        state.activeSpellEffects.forEachIndexed { index, effect ->
            // Decrement turns remaining
            val newTurnsRemaining = effect.turnsRemaining - 1
            
            if (newTurnsRemaining <= 0) {
                // Handle special effects before expiration
                when (effect.spell) {
                    SpellType.BOMB -> {
                        // Bomb explodes! Deal damage to enemies, barricades, and bridges
                        if (effect.position != null) {
                            executeBombExplosion(effect.position)
                        }
                    }
                    else -> {
                        // Other spells just expire
                    }
                }
                
                // Buff expired, mark for removal
                toRemove.add(index)
                if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                println("Spell effect ${effect.spell.displayName} expired")
                }
            } else {
                // Update the effect with decremented turns
                state.activeSpellEffects[index] = effect.copy(turnsRemaining = newTurnsRemaining)
            }
        }
        
        // Remove expired buffs (iterate backwards)
        toRemove.reversed().forEach { index ->
            state.activeSpellEffects.removeAt(index)
        }
    }
    
    /**
     * Execute bomb explosion: damage enemies, barricades, and bridges in 3-hex range
     * with distance-based damage (heavy at 0-1, medium at 2, lower at 3)
     */
    private fun executeBombExplosion(position: Position) {
        val explosionRange = 3
        var enemiesDamaged = 0
        var barricadesDamaged = 0
        var bridgesDestroyed = 0

        // Calculate damage based on distance from blast center
        fun damageAt(distance: Int): Int = when (distance) {
            0 -> 200  // Direct hit: heaviest
            1 -> 150  // Adjacent: heavy
            2 -> 100  // 2 steps: medium
            else -> 50 // 3 steps: lower
        }

        // Damage enemies in range
        state.attackers.forEach { attacker ->
            if (!attacker.isDefeated.value) {
                val distance = attacker.position.value.hexDistanceTo(position)
                if (distance <= explosionRange) {
                    val dmg = damageAt(distance)
                    attacker.currentHealth.value = (attacker.currentHealth.value - dmg).coerceAtLeast(0)
                    enemiesDamaged++
                    // Update dragon level if it's a dragon
                    if (attacker.type.isDragon) {
                        attacker.updateDragonLevel()
                    }
                }
            }
        }
        
        // Damage barricades in range
        state.barricades.forEach { barricade ->
            val distance = barricade.position.hexDistanceTo(position)
            if (distance <= explosionRange) {
                val dmg = damageAt(distance)
                barricade.healthPoints.value = (barricade.healthPoints.value - dmg).coerceAtLeast(0)
                barricadesDamaged++
            }
        }
        
        // Destroy bridges in range
        val bridgesToRemove = state.bridges.filter { bridge ->
            bridge.positions.any { bridgePos ->
                bridgePos.hexDistanceTo(position) <= explosionRange
            }
        }
        bridgesDestroyed = bridgesToRemove.size
        bridgesToRemove.forEach { bridge ->
            state.bridges.remove(bridge)
        }

        // Collect affected positions for the explosion visual effect
        val affectedPositions = position.getHexNeighborsWithinRadius(
            explosionRange,
            state.level.gridWidth,
            state.level.gridHeight
        ).toMutableList()
        affectedPositions.add(position)

        // Record explosion for UI animation (cleared next turn)
        state.bombExplosionEffects.add(
            BombExplosionEffect(
                center = position,
                affectedPositions = affectedPositions,
                turnNumber = state.turnNumber.value
            )
        )

        if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
        println("Bomb exploded at $position! Damaged $enemiesDamaged enemies, $barricadesDamaged barricades, destroyed $bridgesDestroyed bridges")
        }
        GlobalSoundManager.playSound(SoundEvent.BOMB_EXPLOSION)
    }
    
    /**
     * Get effective level for a defender, accounting for active spell buffs
     */
    fun getEffectiveLevel(defender: Defender): Int {
        val hasDoubleLevelBuff = state.activeSpellEffects.any {
            it.spell == SpellType.DOUBLE_TOWER_LEVEL && it.defenderId == defender.id
        }
        return if (hasDoubleLevelBuff) defender.level.value * 2 else defender.level.value
    }
    
    /**
     * Get effective range for a defender, accounting for active spell buffs
     */
    fun getEffectiveRange(defender: Defender): Int {
        val hasDoubleRangeBuff = state.activeSpellEffects.any {
            it.spell == SpellType.DOUBLE_TOWER_REACH && it.defenderId == defender.id
        }
        return if (hasDoubleRangeBuff) defender.range * 2 else defender.range
    }
    
    // Cheat code support for testing
    fun addCoins(amount: Int) {
        state.coins.value += amount
    }
    
    fun setCoins(amount: Int) {
        state.coins.value = amount
    }
    
    fun addMana(amount: Int) {
        state.currentMana.value = minOf(state.maxMana.value, state.currentMana.value + amount)
    }
    
    fun removeMana(amount: Int) {
        state.currentMana.value = maxOf(0, state.currentMana.value - amount)
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
            currentHealth = mutableStateOf(scaledHealth),
            dragonName = if (type.isDragon) DragonNames.getRandomName() else null,
            currentTarget = mutableStateOf(enemyMovement.getInitialTarget(spawnPos))
        )
        
        // Set dragon level change callback if this is a dragon
        if (type.isDragon) {
            attacker.onDragonLevelChanged = dragonLevelChangeCallback
        }
        
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
    
    /**
     * Set callback for combat results (for achievements)
     */
    fun setCombatResultCallback(callback: (CombatResult) -> Unit) {
        combatSystem.onCombatResult = callback
    }
    
    /**
     * Get kills this turn for achievement tracking
     */
    fun getKillsThisTurn(): Int = combatSystem.getKillsThisTurn()
    
    /**
     * Set callback for raft loss events (for achievements)
     */
    fun setRaftLossCallback(callback: (RaftLossReason) -> Unit) {
        raftSystem.onRaftLost = callback
    }
    
    /**
     * Set callback for dragon level changes (for achievements and XP)
     */
    fun setDragonLevelChangeCallback(callback: (oldLevel: Int, newLevel: Int) -> Unit) {
        val wrappedCallback: (oldLevel: Int, newLevel: Int) -> Unit = { oldLevel, newLevel ->
            // Track XP for dragon level loss (not multiplied by level)
            if (oldLevel > newLevel) {
                // Dragon lost levels - award XP for each level lost
                val levelsLost = oldLevel - newLevel
                val xpPerLevel = AttackerType.DRAGON.xp  // 50 XP per level lost
                val xpEarned = xpPerLevel * levelsLost
                state.xpEarnedThisLevel.value += xpEarned
            }
            // Call the original callback for achievements
            callback(oldLevel, newLevel)
        }
        dragonLevelChangeCallback = wrappedCallback
        // Set callback for all existing dragons
        state.attackers.filter { it.type.isDragon }.forEach { dragon ->
            dragon.onDragonLevelChanged = wrappedCallback
        }
    }
    
    /**
     * Reset turn counters at start of turn (for achievements)
     */
    fun startTurnTracking() {
        combatSystem.startTurn()
    }
}
