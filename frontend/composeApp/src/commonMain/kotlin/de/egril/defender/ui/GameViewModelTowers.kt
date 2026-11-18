package de.egril.defender.ui

import de.egril.defender.config.LogConfig
import de.egril.defender.game.GameEngine
import de.egril.defender.model.*
import de.egril.defender.ui.gameplay.GamePlayConstants
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun GameViewModel.placeDefender(
    type: DefenderType,
    position: Position,
): Boolean {
    val gameState = _gameState.value
    val isInstantDeploy = gameState?.instantTowerSpellActive?.value == true

    val result = gameEngine?.placeDefender(type, position, isInstantDeploy) ?: false
    if (result) {
        if (isInstantDeploy) {
            // isInstantDeploy=true implies gameState!=null (see initialization above)
            gameState.currentMana.value = (gameState.currentMana.value - SpellType.INSTANT_TOWER.manaCost).coerceAtLeast(0)
            gameState.instantTowerSpellActive.value = false
            if (LogConfig.ENABLE_SPELL_LOGGING) {
                println("=== SPELL: Instant Tower spell consumed - tower placed instantly, mana deducted")
            }
        }
        // Track achievement
        val isRiverTile = gameState?.level?.isRiverTile(position) ?: false
        if (isRiverTile) {
            achievementManager?.onBuildRaft()
        } else {
            achievementManager?.onBuildTower()
        }
        // Track number of towers standing at the same time (non-raft defenders)
        val standingTowers = _gameState.value?.defenders?.count { it.raftId.value == null } ?: 0
        achievementManager?.onTowersStanding(standingTowers)
        // Surface any messages queued by scripted events triggered by the coin spend.
        surfaceNextPendingMessageIfIdle()
    }
    return result
}

fun GameViewModel.upgradeDefender(defenderId: Int): Boolean {
    // Check if this is a raft before upgrading
    val defender = _gameState.value?.defenders?.find { it.id == defenderId }
    val isRaft = defender?.raftId?.value != null

    val result = gameEngine?.upgradeDefender(defenderId) ?: false
    if (result) {
        // Track achievement
        if (isRaft) {
            achievementManager?.onUpgradeRaft()
        } else {
            achievementManager?.onUpgradeTower()
        }
        // Surface any messages queued by scripted events triggered by the coin spend.
        surfaceNextPendingMessageIfIdle()
    }
    return result
}

fun GameViewModel.undoTower(defenderId: Int): Boolean {
    // Check if this is a raft before undoing
    val defender = _gameState.value?.defenders?.find { it.id == defenderId }
    val isRaft = defender?.raftId?.value != null

    val result = gameEngine?.undoTower(defenderId) ?: false
    if (result) {
        // Track achievement
        if (isRaft) {
            achievementManager?.onUndoRaft()
        } else {
            achievementManager?.onUndoTower()
        }
    }
    return result
}

fun GameViewModel.sanctifyDefender(defenderId: Int): Boolean = performAltarAction { it.sanctifyDefender(defenderId) }

fun GameViewModel.activateAltar(defenderId: Int): Boolean = performAltarAction { it.activateAltar(defenderId) }

private fun GameViewModel.performAltarAction(action: (GameEngine) -> Boolean): Boolean {
    val engine = gameEngine ?: return false
    if (!action(engine)) return false
    surfaceNextPendingMessageIfIdle()
    val state = _gameState.value ?: return true
    if (state.isLevelLost()) {
        completeLevel(state.level.id, won = false)
    } else if (state.isLevelWon()) {
        completeLevel(state.level.id, won = true)
    }
    return true
}

fun GameViewModel.sellTower(defenderId: Int): Boolean {
    // Check if this is a raft before selling
    val defender = _gameState.value?.defenders?.find { it.id == defenderId }
    val isRaft = defender?.raftId?.value != null

    val result = gameEngine?.sellTower(defenderId) ?: false
    if (result) {
        // Track achievement
        if (isRaft) {
            achievementManager?.onSellRaft()
        } else {
            achievementManager?.onSellTower()
        }
    }
    return result
}

/**
 * Generate mana using a wizard tower
 * - Costs 1 action
 * - Generates base 5 mana + (wizard level / 5) bonus mana
 * - Cannot exceed max mana
 */
fun GameViewModel.generateMana(wizardId: Int): Boolean {
    val state = _gameState.value ?: return false
    val wizard = state.defenders.find { it.id == wizardId } ?: return false

    // Validate: must be wizard tower, must be ready, must have actions, not at max mana
    if (wizard.type != DefenderType.WIZARD_TOWER) return false
    if (!wizard.isReady) return false
    if (wizard.actionsRemaining.value <= 0) return false
    if (state.currentMana.value >= state.maxMana.value) return false

    // Calculate mana amount: base 5 + (level / 5)
    val manaAmount = 5 + (wizard.level.value / 5)

    // Add mana (capped at max)
    val newMana = minOf(state.currentMana.value + manaAmount, state.maxMana.value)
    state.currentMana.value = newMana

    // Consume action
    wizard.actionsRemaining.value -= 1

    return true
}

fun GameViewModel.startFirstPlayerTurn() {
    if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
        println("DEBUG: startFirstPlayerTurn called")
    }
    val stateBefore = _gameState.value
    if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
        println("DEBUG: Phase before: ${stateBefore?.phase?.value}")
    }
    if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
        println("DEBUG: Attackers before: ${stateBefore?.attackers?.size}")
    }

    gameEngine?.startFirstPlayerTurn()

    // Surface any messages queued during initial spawn (e.g. EWHAD_ENTERS) immediately,
    // so they appear as soon as the player's first turn begins rather than after they end it.
    surfaceNextPendingMessageIfIdle()

    // Track turn start for achievements
    achievementManager?.startTurn()
    gameEngine?.startTurnTracking()

    val stateAfter = _gameState.value
    if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
        println("DEBUG: Phase after: ${stateAfter?.phase?.value}")
    }
    if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
        println("DEBUG: Attackers after: ${stateAfter?.attackers?.size}")
    }
    stateAfter?.attackers?.forEach { attacker ->
        if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
            println(
                "DEBUG: Enemy ${attacker.id} - Type: ${attacker.type}, Position: (${attacker.position.value.x}, ${attacker.position.value.y})",
            )
        }
    }

    if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
        println("DEBUG: startFirstPlayerTurn completed")
    }
}

private fun GameViewModel.schedulePendingBridgeDamageAfterAttack() {
    val currentState = _gameState.value ?: return
    if (currentState.pendingBridgeDamage.isEmpty()) return

    viewModelScope.launch {
        val flightDelayMs =
            if (
                currentState.ballistaAttackEffects.any { it.turnNumber == currentState.turnNumber.value } ||
                currentState.wizardAttackEffects.any { it.turnNumber == currentState.turnNumber.value } ||
                currentState.alchemyAttackEffects.any { it.turnNumber == currentState.turnNumber.value }
            ) {
                GamePlayConstants.AnimationTimings.BALLISTA_FLIGHT_DELAY_MS
            } else {
                GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS
            }

        delay(flightDelayMs + GamePlayConstants.AnimationTimings.ATTACK_IMPACT_DURATION_MS)
        gameEngine?.processPendingBridgeDamage()
    }
}

fun GameViewModel.defenderAttack(
    defenderId: Int,
    targetId: Int,
): Boolean {
    val result = gameEngine?.defenderAttack(defenderId, targetId) ?: false
    if (result) {
        schedulePendingBridgeDamageAfterAttack()
        // Surface any messages queued by the attack (e.g. EWHAD_RETREATS/EWHAD_DEFEATED) immediately.
        surfaceNextPendingMessageIfIdle()
        // Check for immediate level end after attack
        // Check loss first: a villain breaching a target loses immediately, even if all enemies are defeated
        val state = _gameState.value
        if (state != null && state.isLevelLost()) {
            completeLevel(state.level.id, won = false)
        } else if (state != null && state.isLevelWon()) {
            completeLevel(state.level.id, won = true)
        }
    }
    return result
}

fun GameViewModel.defenderAttackPosition(
    defenderId: Int,
    targetPosition: Position,
): Boolean {
    val result = gameEngine?.defenderAttackPosition(defenderId, targetPosition) ?: false
    if (result) {
        schedulePendingBridgeDamageAfterAttack()
        // triggerStateUpdate()

        // Surface any messages queued by the attack (e.g. EWHAD_RETREATS/EWHAD_DEFEATED) immediately.
        surfaceNextPendingMessageIfIdle()
        // Check for immediate level end after attack
        // Check loss first: a villain breaching a target loses immediately, even if all enemies are defeated
        val state = _gameState.value
        if (state != null && state.isLevelLost()) {
            completeLevel(state.level.id, won = false)
        } else if (state != null && state.isLevelWon()) {
            completeLevel(state.level.id, won = true)
        }
    }
    return result
}

fun GameViewModel.performMineDig(mineId: Int): DigOutcome? {
    val outcome = gameEngine?.performMineDig(mineId)
    if (outcome != null) {
        // Track first dig achievement
        achievementManager?.onDigFirstTime()

        // Track specific outcome achievements
        when (outcome) {
            DigOutcome.GOLD -> achievementManager?.onFindGold()
            DigOutcome.DIAMOND -> achievementManager?.onFindDiamond()
            DigOutcome.DRAGON -> achievementManager?.onSummonDragon()
            else -> {} // No specific achievement for other outcomes
        }
    }
    return outcome
}

fun GameViewModel.performMineBuildTrap(
    mineId: Int,
    trapPosition: Position,
): Boolean = gameEngine?.performMineBuildTrap(mineId, trapPosition) ?: false

fun GameViewModel.performWizardPlaceMagicalTrap(
    wizardId: Int,
    trapPosition: Position,
): Boolean = gameEngine?.performWizardPlaceMagicalTrap(wizardId, trapPosition) ?: false

/**
 * Perform wizard mana generation
 * Called when player clicks the "Generate Mana" button on a wizard tower
 * Returns true if mana was generated successfully
 */
fun GameViewModel.performWizardGenerateMana(wizardId: Int): Boolean = gameEngine?.performWizardGenerateMana(wizardId) ?: false

fun GameViewModel.performBuildBarricade(
    towerId: Int,
    barricadePosition: Position,
): Boolean {
    val result = gameEngine?.performBuildBarricade(towerId, barricadePosition) ?: false
    if (result) {
        // Check if this is a new barricade or adding health to existing one
        val barricade = _gameState.value?.barricades?.find { it.position == barricadePosition }
        if (barricade != null) {
            // Barricade exists, so this added health to it
            achievementManager?.onAddHealthBarricade()
        } else {
            // New barricade was built
            achievementManager?.onBuildBarricade()
        }
    }
    return result
}

fun GameViewModel.performRemoveBarricade(barricadePosition: Position): Int = gameEngine?.removeBarricade(barricadePosition) ?: 0

fun GameViewModel.performMineDigWithOutcome(outcome: DigOutcome): DigOutcome? = gameEngine?.performMineDigWithOutcome(outcome)

/**
 * Returns the best auto-attack target position for the given defender,
 * using the same logic as the automatic attack system. Returns null if there
 * is no suitable target (e.g. no enemies in range, or the tower is a mine/lair).
 */
fun GameViewModel.getAutoAttackTargetPosition(defenderId: Int): Position? {
    val engine = gameEngine ?: return null
    val defender = _gameState.value?.defenders?.find { it.id == defenderId } ?: return null
    return engine.getNextAutoAttackTargetPosition(defender)
}
