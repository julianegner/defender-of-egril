package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.audio.GlobalSoundManager
import de.egril.defender.audio.SoundEvent
import de.egril.defender.config.GameLogBuffer
import de.egril.defender.config.LogConfig
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
    private val barricadeSystem = BarricadeSystem(state)
    private val eventScriptSystem = EventScriptSystem(state) // Scripted level events
    private val supportLogic = SupportLogic(state)
    private val dragonLogic =
        DragonLogic(
            state = state,
            mineOperations = mineOperations,
        )
    private val spawnLogic =
        SpawnLogic(
            state = state,
            enemyMovement = enemyMovement,
            dragonLogic = dragonLogic,
        )
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
            performWizardGenerateMana = supportLogic::performWizardGenerateMana,
            evaluateImmediateEvents = ::evaluateImmediateEvents,
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
            applyTargetDamage = supportLogic::applyTargetDamage,
            destroyFiefAt = supportLogic::destroyFiefAt,
            consumeMushroomAt = supportLogic::consumeMushroomAt,
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
            applyTargetDamage = supportLogic::applyTargetDamage,
            updateDragonMineTargeting = ::updateDragonMineTargeting,
            checkMineWarning = ::checkMineWarning,
            checkAndDestroyMine = ::checkAndDestroyMine,
            processDragonGreed = ::processDragonGreed,
            applyOrkFrenzyTowerAttack = waaghLogic::applyOrkFrenzyTowerAttack,
            destroyFiefAt = supportLogic::destroyFiefAt,
            consumeMushroomAt = supportLogic::consumeMushroomAt,
            recordDragonLevelChange = dragonLogic::recordDragonLevelChange,
        )
    private val turnLifecycleLogic =
        TurnLifecycleLogic(
            state = state,
            enemyMovement = enemyMovement,
            enemyAbilities = enemyAbilities,
            bridgeSystem = bridgeSystem,
            raftSystem = raftSystem,
            combatSystem = combatSystem,
            eventScriptSystem = eventScriptSystem,
            waaghLogic = waaghLogic,
            processSoulCallResurrections = spawnLogic::processSoulCallResurrections,
            spawnInitialEnemies = spawnLogic::spawnInitialEnemies,
            checkAndActivateTraps = ::checkAndActivateTraps,
            startTurnTracking = ::startTurnTracking,
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
        type: FiefType,
    ): Boolean = supportLogic.placeSupportFief(position, type)

    /**
     * Perform wizard mana generation action
     * Generates base 5 mana + (wizard level / 5) bonus mana
     * Consumes one wizard action
     * Returns true if mana was generated successfully
     */
    fun performWizardGenerateMana(wizardId: Int): Boolean = supportLogic.performWizardGenerateMana(wizardId)

    // Barricade Operations - delegated to BarricadeSystem
    fun performBuildBarricade(
        towerId: Int,
        barricadePosition: Position,
    ): Boolean = barricadeSystem.performBuildBarricade(towerId, barricadePosition)

    fun removeBarricade(position: Position): Int = barricadeSystem.removeBarricade(position)

    fun autoDefenderAttacks() = autoAttackLogic.autoDefenderAttacks()

    /**
     * Auto-dig all dwarven mines that still have actions remaining.
     * Called during "Auto-Attack and End Turn" so the player never has to manually confirm
     * mine digging when using auto-attack.
     */
    fun autoMineDig() {
        if (state.phase.value != GamePhase.PLAYER_TURN) return
        for (defender in state.defenders) {
            if (defender.type != DefenderType.DWARVEN_MINE) continue
            if (!defender.isReady) continue
            while (defender.actionsRemaining.value > 0) {
                val outcome = mineOperations.performMineDig(defender.id) ?: break
                if (outcome == DigOutcome.DRAGON) break
            }
        }
    }

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

    private fun findClosestTargetPosition(from: Position): Position = spawnLogic.findClosestTargetPosition(from)

    private fun updateDragonMineTargeting(dragon: Attacker) = mineLogic.updateDragonMineTargeting(dragon)

    private fun checkMineWarning(dragon: Attacker) = mineLogic.checkMineWarning(dragon)

    private fun checkAndDestroyMine(dragon: Attacker) = mineLogic.checkAndDestroyMine(dragon)

    // Turn Management
    fun startFirstPlayerTurn() = turnLifecycleLogic.startFirstPlayerTurn()

    /**
     * Calculate all movement steps for attackers during enemy turn without applying them.
     */
    fun calculateEnemyTurnMovements(): EnemyTurnMovements = gameEngineMovement.calculateEnemyTurnMovements()

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
    fun startEnemyTurn() = turnLifecycleLogic.startEnemyTurn()

    /**
     * Spawn new attackers during enemy turn.
     * Called after movements to ensure spawn points are clear.
     */
    fun spawnEnemyTurnAttackers() = turnLifecycleLogic.spawnEnemyTurnAttackers()

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
    fun completeEnemyTurn() = turnLifecycleLogic.completeEnemyTurn()


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
    fun addCoins(amount: Int) = supportLogic.addCoins(amount)

    fun setCoins(amount: Int) = supportLogic.setCoins(amount)

    fun addMana(amount: Int) = supportLogic.addMana(amount)

    fun removeMana(amount: Int) = supportLogic.removeMana(amount)

    fun spawnEnemy(
        type: AttackerType,
        level: Int = 1,
        preferredSpawnPoint: Position? = null,
    ) = spawnLogic.spawnEnemy(type, level, preferredSpawnPoint)

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
