package de.egril.defender.game.gameengine

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.audio.GlobalSoundManager
import de.egril.defender.audio.SoundEvent
import de.egril.defender.config.GameLogBuffer
import de.egril.defender.config.LogConfig
import de.egril.defender.game.BarricadeSystem
import de.egril.defender.game.BridgeSystem
import de.egril.defender.game.CombatSystem
import de.egril.defender.game.CombatResult
import de.egril.defender.game.EnemyAbilitySystem
import de.egril.defender.game.EnemyMovementSystem
import de.egril.defender.game.EventScriptSystem
import de.egril.defender.game.EventTrigger
import de.egril.defender.game.MineOperations
import de.egril.defender.game.PathfindingSystem
import de.egril.defender.game.RaftSystem
import de.egril.defender.game.RaftLossReason
import de.egril.defender.game.TowerManager
import de.egril.defender.model.*

/**
 * Main game engine that coordinates all game systems.
 * Delegates to specialized subsystems for better organization and maintainability.
 */
class GameEngine(
    private val state: GameState,
) {
    // Specialized subsystems
    private val towerManager = TowerManager(state)
    private val pathfinding = PathfindingSystem(state)
    private val bridgeSystem = BridgeSystem(state)
    private val combatSystem =
        CombatSystem(
            state,
            bridgeSystem,
            getEffectiveLevel = { defender -> getEffectiveLevel(defender) },
            getEffectiveRange = { defender -> getEffectiveRange(defender) },
        )
    private val enemyMovement = EnemyMovementSystem(state, pathfinding)
    private val enemyAbilities = EnemyAbilitySystem(state, pathfinding)
    private val mineOperations =
        MineOperations(state) { attackerType, wasUninjured ->
            combatSystem.recordSupportTrapKill(attackerType, wasUninjured)
        }
    private val raftSystem = RaftSystem(state)
    private val barricadeSystem = BarricadeSystem(state) // Add barricade system
    private val eventScriptSystem = EventScriptSystem(state) // Scripted level events
    private val autoAttackSelector =
        AutoAttackSelector(
            state = state,
            getEffectiveRange = ::getEffectiveRange,
            findClosestTargetPosition = ::findClosestTargetPosition,
        )
    private val autoAttackLogic =
        AutoAttackLogic(
            state = state,
            selector = autoAttackSelector,
            combatSystem = combatSystem,
            evaluateImmediateEvents = ::evaluateImmediateEvents,
        )
    private val dragonLogic =
        DragonLogic(
            state = state,
            mineOperations = mineOperations,
        )
    private val mineLogic =
        MineLogic(
            state = state,
            findClosestTargetPosition = ::findClosestTargetPosition,
            recordDragonLevelChange = dragonLogic::recordDragonLevelChange,
        )
    private val waaghLogic = WaaghLogic(state)
    private val barricadeLogic =
        BarricadeLogic(
            state = state,
            barricadeSystem = barricadeSystem,
            mineOperations = mineOperations,
            getFrenzyMultiplier = waaghLogic::getBarricadeFrenzyMultiplier,
            applyTargetDamage = ::applyTargetDamage,
            destroyFiefAt = ::destroyFiefAt,
            consumeMushroomAt = ::consumeMushroomAt,
        )
    private val gameEngineMovement =
        Movement(
            state = state,
            pathfinding = pathfinding,
            barricadeSystem = barricadeSystem,
            bridgeSystem = bridgeSystem,
            enemyMovement = enemyMovement,
            enemyAbilities = enemyAbilities,
            mineOperations = mineOperations,
            attackBarricade = ::attackBarricade,
            getBarricadeDamageForEnemyUnit = ::getBarricadeDamageForEnemyUnit,
            applyTargetDamage = ::applyTargetDamage,
            updateDragonMineTargeting = ::updateDragonMineTargeting,
            checkMineWarning = ::checkMineWarning,
            checkAndDestroyMine = ::checkAndDestroyMine,
            processDragonGreed = ::processDragonGreed,
            applyOrkFrenzyTowerAttack = waaghLogic::applyOrkFrenzyTowerAttack,
            destroyFiefAt = ::destroyFiefAt,
            consumeMushroomAt = ::consumeMushroomAt,
            recordDragonLevelChange = dragonLogic::recordDragonLevelChange,
        )

    // Tower Management - delegated to TowerManager
    fun placeDefender(
        type: DefenderType,
        position: Position,
        instantDeploy: Boolean = false,
    ): Boolean {
        val result = towerManager.placeDefender(type, position, instantDeploy)
        if (result) evaluateImmediateEvents()
        return result
    }

    fun upgradeDefender(defenderId: Int): Boolean {
        val result = towerManager.upgradeDefender(defenderId)
        if (result) evaluateImmediateEvents()
        return result
    }

    fun undoTower(defenderId: Int): Boolean = towerManager.undoTower(defenderId)

    fun sellTower(defenderId: Int): Boolean = towerManager.sellTower(defenderId)

    /**
     * Re-evaluate scripted events that react to state changes (enemies killed, coins/mana/health
     * thresholds, units reaching tiles) so they fire immediately during the player's turn rather
     * than waiting for the next turn boundary. Turn-start events are unaffected.
     */
    fun evaluateImmediateEvents() {
        eventScriptSystem.evaluate(EventTrigger.IMMEDIATE)
    }

    // Combat System - delegated to CombatSystem
    fun defenderAttack(
        defenderId: Int,
        targetId: Int,
    ): Boolean {
        val result = combatSystem.defenderAttack(defenderId, targetId) { combatSystem.processDefeatedAttackers() }
        if (result) evaluateImmediateEvents()
        return result
    }

    fun defenderAttackPosition(
        defenderId: Int,
        targetPosition: Position,
    ): Boolean {
        val result =
            combatSystem.defenderAttackPosition(defenderId, targetPosition) { combatSystem.processDefeatedAttackers() }
        if (result) evaluateImmediateEvents()
        return result
    }

    // Mine Operations - delegated to MineOperations
    fun performMineDig(mineId: Int): DigOutcome? = mineOperations.performMineDig(mineId)

    fun performMineDigWithOutcome(outcomeType: DigOutcome): DigOutcome? = mineOperations.performMineDigWithOutcome(outcomeType)

    fun performMineBuildTrap(
        mineId: Int,
        trapPosition: Position,
    ): Boolean = mineOperations.performMineBuildTrap(mineId, trapPosition)

    fun performWizardPlaceMagicalTrap(
        wizardId: Int,
        trapPosition: Position,
    ): Boolean = mineOperations.performWizardPlaceMagicalTrap(wizardId, trapPosition)

    /**
     * Place a support trap (dwarven or magical) directly from a player-granted level support.
     * Does not require a tower or consume any tower actions.
     */
    fun placeSupportTrap(
        trapPosition: Position,
        damage: Int,
        type: TrapType,
    ): Boolean = mineOperations.placeSupportTrap(trapPosition, damage, type)

    /**
     * Place a support barricade directly from a player-granted level support.
     * Does not require a tower or consume any tower actions.
     */
    fun placeSupportBarricade(
        barricadePosition: Position,
        hp: Int,
    ): Boolean = barricadeSystem.placeSupportBarricade(barricadePosition, hp)

    /**
     * Place a fief from a player-granted support token.
     * The position must be a path tile with no attacker, trap, barricade, or existing fief.
     * Fisher fiefs additionally require at least one adjacent water tile.
     * Returns true if the fief was placed successfully.
     */
    fun placeSupportFief(
        position: Position,
        type: de.egril.defender.model.FiefType,
    ): Boolean {
        if (!state.level.isOnPath(position)) return false
        if (type == de.egril.defender.model.FiefType.FISHER && !state.level.hasAdjacentWaterTile(position)) return false
        val hasEnemy = state.attackers.any { !it.isDefeated.value && it.position.value == position }
        val hasTrap = state.traps.any { it.position == position }
        val hasBarricade = state.barricades.any { it.position == position }
        val hasFief = state.fiefs.any { it.position == position }
        val hasMushroom = state.mushrooms.any { it.position == position }
        if (hasEnemy || hasTrap || hasBarricade || hasFief || hasMushroom) return false
        state.fiefs.add(
            de.egril.defender.model
                .Fief(position = position, type = type),
        )
        return true
    }

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
            println(
                "=== SPELL: Wizard tower $wizardId generated $actualManaGenerated mana (${state.currentMana.value}/${state.maxMana.value})",
            )
        }

        return true
    }

    // Barricade Operations - delegated to BarricadeSystem
    fun performBuildBarricade(
        towerId: Int,
        barricadePosition: Position,
    ): Boolean = barricadeSystem.performBuildBarricade(towerId, barricadePosition)

    fun removeBarricade(position: Position): Int = barricadeSystem.removeBarricade(position)

    fun autoDefenderAttacks() = autoAttackLogic.autoDefenderAttacks()

    fun checkAndActivateTraps() {
        mineOperations.checkAndActivateTraps { combatSystem.processDefeatedAttackers() }
        evaluateImmediateEvents()
    }

    /**
     * Returns the next auto-attack target position for a single defender action, without performing
     * the attack. Used by demo mode to show aiming circles before committing the attack.
     * Returns null if this defender has no valid target right now.
     */
    fun getNextAutoAttackTargetPosition(defender: Defender): Position? = autoAttackLogic.getNextAutoAttackTargetPosition(defender)

    /**
     * Performs exactly one auto-attack action for the defender identified by [defenderId].
     * Returns true if an attack was successfully executed, false if nothing could be done.
     * Used by demo mode so that aiming circles can be displayed between individual attacks.
     */
    fun performOneAutoAttack(defenderId: Int): Boolean = autoAttackLogic.performOneAutoAttack(defenderId)

    private fun processDragonGreed(dragon: Attacker) = dragonLogic.processDragonGreed(dragon)

    private fun recordDragonLevelChange(
        dragon: Attacker,
        oldLevel: Int,
    ) = dragonLogic.recordDragonLevelChange(dragon, oldLevel)

    /**
     * Find the closest active (non-taken) target position from a given position.
     * Falls back to any target position if all are taken (level is then lost).
     * Used for dragons when not targeting mines.
     */
    private fun findClosestTargetPosition(from: Position): Position {
        val active = state.getActiveTargetPositions()
        return (if (active.isNotEmpty()) active else state.level.targetPositions)
            .minByOrNull { from.distanceTo(it) } ?: state.level.targetPositions.firstOrNull() ?: from
    }

    private fun updateDragonMineTargeting(dragon: Attacker) = mineLogic.updateDragonMineTargeting(dragon)

    private fun checkMineWarning(dragon: Attacker) = mineLogic.checkMineWarning(dragon)

    private fun checkAndDestroyMine(dragon: Attacker) = mineLogic.checkAndDestroyMine(dragon)

    private fun processSoulCallResurrections() {
        val dueResurrections = state.pendingSoulCalls.filter { it.reviveTurn <= state.turnNumber.value }
        if (dueResurrections.isEmpty()) return

        val reservedPositions = mutableSetOf<Position>()
        for (pending in dueResurrections) {
            val spawnPos =
                if (canSpawnSoulCallAt(pending.position, reservedPositions)) {
                    pending.position
                } else {
                    enemyMovement.findFreePositionNear(
                        pending.position,
                        waterOnly = pending.attackerType.canOnlyMoveOnWater,
                        canUseRiver = pending.attackerType.canTraverseRiver,
                    )
                } ?: continue

            reservedPositions.add(spawnPos)
            val currentTarget = pending.currentTarget ?: enemyMovement.getInitialTarget(spawnPos)
            val attacker =
                Attacker(
                    id = state.nextAttackerId.value++,
                    type = pending.attackerType,
                    position = mutableStateOf(spawnPos),
                    level = mutableStateOf(pending.level),
                    dragonName = pending.dragonName,
                    currentTarget = mutableStateOf(currentTarget),
                )
            dragonLogic.applyDragonLevelChangeCallback(attacker)
            state.attackers.add(attacker)
            state.enemySpawnEffects.add(
                EnemySpawnEffect(
                    position = spawnPos,
                    turnNumber = state.turnNumber.value,
                    attackerType = pending.attackerType,
                    suppressPortalAnimation = pending.attackerType == AttackerType.SKELETON,
                ),
            )
        }

        state.pendingSoulCalls.removeAll(dueResurrections.toSet())
    }

    private fun canSpawnSoulCallAt(
        position: Position,
        reservedPositions: Set<Position>,
    ): Boolean =
        position.x >= 0 &&
            position.x < state.level.gridWidth &&
            position.y >= 0 &&
            position.y < state.level.gridHeight &&
            position !in reservedPositions &&
            state.level.isEnemyTraversable(position) &&
            !isOccupiedByStaticObject(position) &&
            state.attackers.none { !it.isDefeated.value && it.position.value == position }

    private fun isOccupiedByStaticObject(position: Position): Boolean =
        state.defenders.any { it.position.value == position } ||
            state.barricades.any { it.position == position && !it.isDestroyed() } ||
            state.traps.any { it.position == position } ||
            state.fiefs.any { it.position == position } ||
            state.mushrooms.any { it.position == position }

    // Turn Management
    fun startFirstPlayerTurn() {
        if (state.phase.value != GamePhase.INITIAL_BUILDING) return

        GameLogBuffer.log("GAME", "Game started — Turn 1, ${state.defenders.size} towers placed")

        // Play battle start sound
        GlobalSoundManager.playSound(SoundEvent.BATTLE_START)

        // Start tracking for achievement purposes
        startTurnTracking()

        state.phase.value = GamePhase.PLAYER_TURN
        state.turnNumber.value = 1 // Start at turn 1 when game begins

        // Load first wave
        if (state.currentWaveIndex.value == 0 && state.attackersToSpawn.isEmpty()) {
            enemyMovement.loadNextWave()
        }

        // Spawn initial enemies immediately
        spawnInitialEnemies()

        // Reset all defender actions
        resetDefenderActions()

        // Evaluate scripted events for the first player turn
        eventScriptSystem.evaluate(EventTrigger.PLAYER_TURN_START)
    }

    private fun spawnInitialEnemies() {
        // Spawn all enemies scheduled for turn 1 from the spawn plan
        val turn1Spawns = state.spawnPlan.filter { it.spawnTurn == 1 }

        turn1Spawns.forEachIndexed { index, plannedSpawn ->
            // Use fixed spawn point if specified; otherwise pick a compatible spawn point via
            // round-robin, honouring the enemy's canSpawnOnLand/canSpawnOnWater flags.
            val preferredSpawnPoint =
                plannedSpawn.spawnPoint
                    ?: run {
                        val compatiblePoints = state.level.getCompatibleSpawnPoints(plannedSpawn.attackerType)
                        compatiblePoints[index % compatiblePoints.size]
                    }

            // Find a free position near the preferred spawn point
            val spawnPos =
                enemyMovement.findFreePositionNear(
                    preferredSpawnPoint,
                    waterOnly = plannedSpawn.attackerType.canOnlyMoveOnWater,
                    canUseRiver = plannedSpawn.attackerType.canTraverseRiver,
                )

            if (spawnPos == null) {
                // No free position found - skip this enemy for now
                // This should rarely happen unless the map is completely congested
                return@forEachIndexed
            }

            // Ensure unique enemies (Ewhad boss and villains) only exist once at a time
            if (isUniqueEnemyAlreadyPresent(plannedSpawn.attackerType, state.attackers)) {
                // Skip spawning another copy of this unique enemy if one already exists
                return@forEachIndexed
            }

            // Get initial target based on preferred spawn point (BEFORE congestion offsets)
            val initialTarget = enemyMovement.getInitialTarget(preferredSpawnPoint)
            val attacker =
                Attacker(
                    id = state.nextAttackerId.value++,
                    type = plannedSpawn.attackerType,
                    position = mutableStateOf(spawnPos),
                    level = mutableStateOf(plannedSpawn.level),
                    currentTarget = mutableStateOf(initialTarget),
                )
            state.attackers.add(attacker)
            GameLogBuffer.log("SPAWN", "${attacker.type} Lv${attacker.level.value} spawned at $spawnPos")
            if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                println(
                    "Spawned attacker ${attacker.id} at $spawnPos (preferred: $preferredSpawnPoint) with initial target: $initialTarget",
                )
            }

            // Queue Ewhad enters message when Ewhad spawns
            if (plannedSpawn.attackerType == AttackerType.EWHAD) {
                state.pendingMessages.add(
                    GameMessage(type = GameMessageType.EWHAD_ENTERS),
                )
            }

            // Queue villain backstory message when a villain enters the battlefield.
            // Ewhad is a villain but has its own dedicated narrative (EWHAD_ENTERS above), so it is
            // excluded here to avoid showing two dialogs.
            if (plannedSpawn.attackerType.isRealVillain && plannedSpawn.attackerType != AttackerType.EWHAD) {
                state.pendingMessages.add(
                    GameMessage(type = GameMessageType.VILLAIN_ENTERS, name = plannedSpawn.attackerType.name),
                )
            }
        }

        // Move goblins immediately after initial spawning (this is not during enemy turn)
        enemyMovement.moveGoblinsAfterSpawn()
    }

    /**
     * Calculate all movement steps for attackers during enemy turn without applying them.
     */
    fun calculateEnemyTurnMovements(): EnemyTurnMovements = gameEngineMovement.calculateEnemyTurnMovements()

    private fun applyTargetDamage(attacker: Attacker) {
        val position = attacker.position.value
        val targetInfo = state.level.targetInfoMap[position]

        if (targetInfo?.type == de.egril.defender.model.TargetType.SINGLE_HIT) {
            // Single-hit target: mark as taken, no HP damage
            if (!state.takenTargets.contains(position)) {
                state.takenTargets.add(position)
                val name = targetInfo.name.takeIf { it.isNotBlank() }
                GameLogBuffer.log("DAMAGE", "Target '${name ?: position}' taken by ${attacker.type} Lv${attacker.displayLevel}")
                state.pendingMessages.add(
                    de.egril.defender.model.GameMessage(
                        type = de.egril.defender.model.GameMessageType.TARGET_TAKEN,
                        name = name,
                    ),
                )
                if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                    println(
                        "!!! SINGLE_HIT TARGET TAKEN !!! Turn ${state.turnNumber.value}: ${attacker.type} (ID ${attacker.id}) took target '${name ?: position}'",
                    )
                }
                // Redirect all enemies that were heading to this taken target
                state.retargetEnemiesFromTakenTarget(position)
            }
        } else {
            // Standard target: deal HP damage
            val damage = attacker.calculateTargetDamage()
            if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                println(
                    "!!! ENEMY ENTERED TARGET !!! Turn ${state.turnNumber.value}: ${attacker.type} (ID ${attacker.id}) at $position dealt $damage damage. HP: ${state.healthPoints.value} -> ${state.healthPoints.value - damage}",
                )
            }
            GameLogBuffer.log(
                "DAMAGE",
                "${attacker.type} Lv${attacker.displayLevel} reached target — dealt $damage damage (HP: ${state.healthPoints.value} -> ${state.healthPoints.value - damage})",
            )
            state.healthPoints.value = maxOf(0, state.healthPoints.value - damage)
        }
        // A villain breaching a target loses the level immediately (see issue #538), regardless of
        // how much health remains. Record it so GameState.isLevelLost() reports the loss.
        if (attacker.type.isRealVillain) {
            state.villainReachedTarget.value = true
        }
        attacker.isDefeated.value = true
    }

    /**
     * Apply a single movement step for the given attacker.
     */
    fun applyMovement(
        attackerId: Int,
        newPosition: Position,
    ) {
        gameEngineMovement.applyMovement(attackerId, newPosition)
    }

    fun attackBarricade(
        newPosition: Position,
        attacker: Attacker,
    ): Boolean = barricadeLogic.attackBarricade(newPosition, attacker)

    fun getBarricadeDamageForEnemyUnit(attacker: Attacker): Int = barricadeLogic.getBarricadeDamageForEnemyUnit(attacker)

    private fun tickBloodlustAfterMovement() {
        state.attackers.forEach { attacker ->
            if (attacker.bloodlustRoundsLeft.value > 0) {
                attacker.bloodlustRoundsLeft.value--
            }
            if (attacker.mushroomTurnsRemaining.value > 0) {
                attacker.mushroomTurnsRemaining.value--
                if (attacker.mushroomTurnsRemaining.value == 0) {
                    // Expire the temporary mushroom health together with the doubled level.
                    attacker.currentHealth.value =
                        if (attacker.isDefeated.value) {
                            maxOf(0, attacker.currentHealth.value - attacker.mushroomBonusHealth)
                        } else {
                            maxOf(1, attacker.currentHealth.value - attacker.mushroomBonusHealth)
                        }
                    attacker.mushroomLevelBonus.value = 0
                }
            }
        }
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
        processSoulCallResurrections()
        state.phase.value = GamePhase.ENEMY_TURN
        waaghLogic.updateWaaghFrenzyAtEnemyTurnStart()
        enemyAbilities.processSnotlingGrowth()
        state.enemyTurnStartPositions.clear()
        state.attackers
            .filter { !it.isDefeated.value }
            .forEach { attacker ->
                state.enemyTurnStartPositions[attacker.id] = attacker.position.value
            }

        // Evaluate scripted events at the start of the enemy turn
        eventScriptSystem.evaluate(EventTrigger.ENEMY_TURN_START)

        GameLogBuffer.log(
            "TURN",
            "Enemy turn ${state.turnNumber.value} — HP: ${state.healthPoints.value}, Coins: ${state.coins.value}, Enemies alive: ${state.attackers.count {
                !it.isDefeated.value
            }}",
        )
        if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
            println("GameEngine.startEnemyTurn: Changed phase to ENEMY_TURN, turn=${state.turnNumber.value}")
        }

        // Ensure trap trigger effects are clean at the start of enemy turn (belt-and-suspenders;
        // they are also cleared at the end of completeEnemyTurn before transitioning to PLAYER_TURN).
        state.trapTriggerEffects.clear()

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
    fun calculateNewlySpawnedMovements(): List<List<Pair<Int, Position>>> = gameEngineMovement.calculateNewlySpawnedMovements()

    /**
     * Complete enemy turn: apply effects and start player turn.
     */
    fun completeEnemyTurn() {
        if (state.phase.value != GamePhase.ENEMY_TURN) return

        // Safety flush: if any coin-gain animations hadn't fired by the time the turn ends
        // (e.g., no enemy movements → very short enemy turn), credit the remaining pending
        // coins now before the effects list is cleared.
        if (state.pendingCoinGains.value > 0) {
            state.coins.value += state.pendingCoinGains.value
            state.pendingCoinGains.value = 0
        }

        // Clear all visual effects from the previous turn before adding new ones for this turn.
        // This ensures effects added during this call (traps, construction, spawns, deaths, coins)
        // persist through the following player turn and are cleaned up on the next enemy turn.
        state.bombExplosionEffects.clear()
        state.defeatedEnemyEffects.clear()
        state.coinGainEffects.clear()
        state.towerAttackEffects.clear()
        state.constructionCompleteEffects.clear()
        state.enemySpawnEffects.clear()
        state.enemyMoveEffects.clear()
        state.dragonLevelChangeEffects.clear()
        state.mineDigEffects.clear()
        state.arrowAttackEffects.clear()
        state.ballistaAttackEffects.clear()
        state.bowAttackEffects.clear()
        state.spearAttackEffects.clear()
        state.wizardAttackEffects.clear()
        state.alchemyAttackEffects.clear()
        state.rocketAttackEffects.clear()
        state.snotlingCannonThrowEffects.clear()
        state.garokkWarCryEffects.clear()
        state.shadowSpewEffects.clear()
        state.morvathShadowOrbEffects.clear()

        tickBloodlustAfterMovement()
        enemyAbilities.processHordeEating()

        // Check and activate traps after all movements
        checkAndActivateTraps()

        // Apply damage over time effects
        combatSystem.applyLastingEffects()

        // Update field effects
        enemyMovement.updateFieldEffects()

        // Update spell buff effects (decrement turns remaining, remove expired)
        updateSpellBuffs()

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

        // Clear trap trigger effects so they don't persist into the player's turn.
        // They were visible during the enemy turn when the traps fired; clearing here
        // prevents the lingering overlay from showing during the player's turn.
        state.trapTriggerEffects.clear()

        // Tick down cooldown-based support powers (one turn per full round) and clear the
        // Coin Surge effect so its doubling only applies to the round it was activated in.
        for (type in state.cooldownPowerReadyIn.keys.toList()) {
            val remaining = state.cooldownPowerReadyIn[type] ?: 0
            if (remaining > 0) {
                state.cooldownPowerReadyIn[type] = remaining - 1
            }
        }
        state.coinSurgeActive.value = false

        // Grant fief income for each active fief at the start of each player turn
        val fiefIncome = state.fiefs.sumOf { it.type.incomePerTurn }
        if (fiefIncome > 0) {
            state.coins.value += fiefIncome
        }

        state.phase.value = GamePhase.PLAYER_TURN
        resetDefenderActions()

        // Evaluate scripted events at the start of the player turn
        eventScriptSystem.evaluate(EventTrigger.PLAYER_TURN_START)
        waaghLogic.updateWaaghFrenzyAtEnemyTurnEnd()
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
                    // Record construction complete visual effect
                    state.constructionCompleteEffects.add(
                        TowerConstructionEffect(
                            position = defender.position.value,
                            turnNumber = state.turnNumber.value,
                        ),
                    )
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
        fun damageAt(distance: Int): Int =
            when (distance) {
                0 -> 200 // Direct hit: heaviest
                1 -> 150 // Adjacent: heavy
                2 -> 100 // 2 steps: medium
                else -> 50 // 3 steps: lower
            }

        // Damage enemies in range
        state.attackers.forEach { attacker ->
            if (!attacker.isDefeated.value) {
                val distance = attacker.position.value.hexDistanceTo(position)
                if (distance <= explosionRange && !attacker.type.isMirrorImage) {
                    val dmg = damageAt(distance)
                    attacker.currentHealth.value = (attacker.currentHealth.value - dmg).coerceAtLeast(0)
                    enemiesDamaged++
                    // Mark as defeated if health reaches 0
                    if (attacker.currentHealth.value <= 0) {
                        attacker.isDefeated.value = true
                    }
                    // Update dragon level if it's a dragon
                    if (attacker.type.isDragon) {
                        val oldLevelBomb = attacker.level.value
                        attacker.updateDragonLevel()
                        recordDragonLevelChange(attacker, oldLevelBomb)
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
        val bridgesToRemove =
            state.bridges.filter { bridge ->
                bridge.positions.any { bridgePos ->
                    bridgePos.hexDistanceTo(position) <= explosionRange
                }
            }
        bridgesDestroyed = bridgesToRemove.size
        bridgesToRemove.forEach { bridge ->
            state.bridges.remove(bridge)
        }

        // Process defeated enemies (grant coins, remove from map, etc.)
        combatSystem.processDefeatedAttackers()

        // Collect affected positions for the explosion visual effect
        val affectedPositions =
            position
                .getHexNeighborsWithinRadius(
                    explosionRange,
                    state.level.gridWidth,
                    state.level.gridHeight,
                ).toMutableList()
        affectedPositions.add(position)

        // Record explosion for UI animation (cleared next turn)
        state.bombExplosionEffects.add(
            BombExplosionEffect(
                center = position,
                affectedPositions = affectedPositions,
                turnNumber = state.turnNumber.value,
            ),
        )

        if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
            println(
                "Bomb exploded at $position! Damaged $enemiesDamaged enemies, $barricadesDamaged barricades, destroyed $bridgesDestroyed bridges",
            )
        }
        GlobalSoundManager.playSound(SoundEvent.BOMB_EXPLOSION)
    }

    /**
     * Get effective level for a defender, accounting for active spell buffs
     */
    fun getEffectiveLevel(defender: Defender): Int {
        val hasDoubleLevelBuff =
            state.activeSpellEffects.any {
                it.spell == SpellType.DOUBLE_TOWER_LEVEL && it.defenderId == defender.id
            }
        return if (hasDoubleLevelBuff) defender.level.value * 2 else defender.level.value
    }

    /**
     * Get effective range for a defender, accounting for active spell buffs
     */
    fun getEffectiveRange(defender: Defender): Int {
        val hasDoubleRangeBuff =
            state.activeSpellEffects.any {
                it.spell == SpellType.DOUBLE_TOWER_REACH && it.defenderId == defender.id
            }
        return if (hasDoubleRangeBuff) defender.range * 2 else defender.range
    }

    // Cheat code support for testing
    fun addCoins(amount: Int) {
        state.coins.value += amount
    }

    /**
     * Destroy a fief at [position] if one exists.
     * Called whenever an enemy moves through a tile — fiefs are destroyed by enemy passage.
     * If [attacker] is Cap'n Roderich, 10× the fief's per-turn income is added to his treasure.
     */
    private fun destroyFiefAt(
        position: Position,
        attacker: Attacker? = null,
    ) {
        val fief = state.fiefs.find { it.position == position }
        if (fief != null) {
            state.fiefs.remove(fief)
            // Cap'n Roderich loots the fief: 10× its per-turn income goes to his treasure
            if (attacker?.type == AttackerType.CAPTAIN_RODERICH) {
                attacker.treasureCoins.value += fief.type.incomePerTurn * 10
            }
        }
    }

    /**
     * Consume the mushroom at [position] if any, applying the mushroom buff to [attacker].
     * Only horde units and witches eat mushrooms; other units leave them in place.
     * The buff doubles the attacker's movement speed and effective level for 2 turns.
     */
    private fun consumeMushroomAt(
        position: Position,
        attacker: Attacker,
    ) {
        if (!attacker.type.canEatMushroom) return
        // Don't consume a second mushroom while already buffed
        if (attacker.mushroomTurnsRemaining.value > 0) return
        val mushroom = state.mushrooms.find { it.position == position }
        if (mushroom != null) {
            state.mushrooms.remove(mushroom)
            // Store original level as bonus (net effect: level.value + mushroomLevelBonus = 2× level)
            attacker.mushroomLevelBonus.value = attacker.level.value
            attacker.mushroomTurnsRemaining.value = 2
            attacker.currentHealth.value += attacker.mushroomBonusHealth
        }
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

    fun spawnEnemy(
        type: AttackerType,
        level: Int = 1,
        preferredSpawnPoint: Position? = null,
    ) {
        // Find a free spawn position, honoring a requested spawn point when given.
        val spawnPos =
            if (preferredSpawnPoint != null) {
                enemyMovement.findFreePositionNear(
                    preferredSpawnPoint,
                    waterOnly = type.canOnlyMoveOnWater,
                    canUseRiver = type.canTraverseRiver,
                )
            } else {
                enemyMovement.findFreeSpawnPosition()
            } ?: return

        // Create the enemy with scaled health based on level
        val scaledHealth = type.health * level
        val attacker =
            Attacker(
                id = state.nextAttackerId.value++,
                type = type,
                position = mutableStateOf(spawnPos),
                currentHealth = mutableStateOf(scaledHealth),
                dragonName = if (type.isDragon) DragonNames.getRandomName() else null,
                currentTarget = mutableStateOf(enemyMovement.getInitialTarget(spawnPos)),
            )

        dragonLogic.applyDragonLevelChangeCallback(attacker)

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
        return dragonLogic.spawnDragonCheat()
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
     * Process defeated attackers (award coins, remove from list). Used after spell effects.
     */
    fun processDefeatedAttackers() = combatSystem.processDefeatedAttackers()

    /**
     * Process pending barge deletions from Roderich's Broadside attacks.
     * Called after the cannonball animation duration has passed.
     */
    fun processPendingBargeDeletions() = enemyAbilities.processPendingBargeDeletions()

    fun processPendingSnotlingCannonArrivals() = enemyAbilities.processPendingSnotlingCannonArrivals()

    /**
     * Apply the distant shadow fog tiles that Morvath targeted this turn.
     * Called after the orb animation completes so the fog appears at the end of the animation.
     */
    fun applyPendingMorvathFog() = enemyAbilities.applyPendingMorvathFog()

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
        dragonLogic.setDragonLevelChangeCallback(callback)
    }

    /**
     * Reset turn counters at start of turn (for achievements)
     */
    fun startTurnTracking() {
        combatSystem.startTurn()
    }
}
