package de.egril.defender.ui

import de.egril.defender.config.GameLogBuffer
import de.egril.defender.config.LogConfig
import de.egril.defender.editor.OfficialContent
import de.egril.defender.editor.getLevel
import de.egril.defender.editor.getWorldMapData
import de.egril.defender.editor.isLevelUnlocked
import de.egril.defender.model.*
import de.egril.defender.ui.gameplay.GamePlayConstants
import de.egril.defender.ui.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun GameViewModel.endPlayerTurn() {
    val state = _gameState.value ?: return
    val engine = gameEngine ?: return
    if (state.scriptedVictory.value) {
        surfaceNextPendingMessageIfIdle()
        return
    }

    // NOTE: Auto-attacks are NOT triggered here when clicking "End Turn".
    // They only happen when clicking "Auto-Attack and End Turn" button (see autoAttackAndEndTurn()).

    // Cancel instant tower spell if active (mana was never consumed, so nothing to refund)
    if (state.instantTowerSpellActive.value) {
        state.instantTowerSpellActive.value = false
        if (LogConfig.ENABLE_SPELL_LOGGING) {
            println("=== SPELL: Instant Tower spell cancelled on end turn (no mana consumed)")
        }
    }

    // Process enemy turn with animations
    viewModelScope.launch(Dispatchers.Default) {
        // Start enemy turn: change phase to ENEMY_TURN
        // The UI immediately shows "ENEMY TURN" indicator when phase changes
        engine.startEnemyTurn()
        surfaceNextPendingMessageIfIdle()
        if (state.scriptedVictory.value) return@launch
        waitForBlockingNarrativeMessageDismissal()

        // Calculate all movement steps for existing units
        val enemyTurnMovements = engine.calculateEnemyTurnMovements()
        val movementSteps = enemyTurnMovements.allMovementSteps
        val attackersStoppedByBarricade = enemyTurnMovements.attackersStoppedByBarricade

        // Apply each movement step with a delay between steps
        for (stepMovements in movementSteps) {
            val trapCountBefore = _gameState.value?.trapTriggerEffects?.size ?: 0
            // Apply all movements in this step simultaneously
            for ((attackerId, newPosition) in stepMovements) {
                engine.applyMovement(attackerId, newPosition)
                if (state.scriptedVictory.value) {
                    surfaceNextPendingMessageIfIdle()
                    return@launch
                }
            }
            // Delay between movement steps so user can see the animation (reduced from 400ms to 200ms)
            delay(200)
            // If a trap was triggered in this step, pause long enough for the trap animation to
            // complete (~830ms) before the enemy continues moving.
            val trapCountAfter = _gameState.value?.trapTriggerEffects?.size ?: 0
            if (trapCountAfter > trapCountBefore) {
                delay(TRAP_ANIMATION_EXTRA_DELAY_MS)
                // Process any enemies killed by this trap now so their death animation plays
                // directly after the trap animation, before the next movement step begins.
                processAndDelayForTrapDeaths()
            }
        }

        attackersStoppedByBarricade.forEach { it ->
            if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                println("attackBarricade B")
            }
            // fixit HERE
            engine.attackBarricade(it.second, it.first)
        }

        // Add a small delay to see final positions before spawning new units (reduced from 300ms to 150ms)
        if (movementSteps.isNotEmpty()) {
            delay(150)
        }

        // Now spawn new units (spawn points should be clear after movements)
        engine.spawnEnemyTurnAttackers()
        if (state.scriptedVictory.value) {
            surfaceNextPendingMessageIfIdle()
            return@launch
        }

        // Show spawned units briefly (reduced from 400ms to 200ms)
        delay(200)

        // Surface any pending spawn messages (e.g. Ewhad enters) while units are still at
        // their spawn points, so the message is displayed before they move away.
        surfaceNextPendingMessageIfIdle()
        waitForBlockingNarrativeMessageDismissal()

        // Move newly spawned units away from spawn points
        val newSpawnMovements = engine.calculateNewlySpawnedMovements()
        for (stepMovements in newSpawnMovements) {
            val trapCountBefore = _gameState.value?.trapTriggerEffects?.size ?: 0
            for ((attackerId, newPosition) in stepMovements) {
                engine.applyMovement(attackerId, newPosition)
                if (state.scriptedVictory.value) {
                    surfaceNextPendingMessageIfIdle()
                    return@launch
                }
            }
            // Delay between movement steps (reduced from 400ms to 200ms)
            delay(200)
            // Pause extra if a trap fired during this step
            val trapCountAfter = _gameState.value?.trapTriggerEffects?.size ?: 0
            if (trapCountAfter > trapCountBefore) {
                delay(TRAP_ANIMATION_EXTRA_DELAY_MS)
                // Process any enemies killed by this trap now so their death animation plays
                // directly after the trap animation, before the next movement step begins.
                processAndDelayForTrapDeaths()
            }
        }

        // Add a small delay after newly spawned units have moved (reduced from 300ms to 150ms)
        if (newSpawnMovements.isNotEmpty()) {
            delay(150)
        }

        // Complete enemy turn: apply effects and return to player turn
        engine.completeEnemyTurn()

        // Wait for Morvath's shadow orb animation to reach the distant fog tile before
        // applying the fog, so the fog appears only when the orb arrives.
        if (_gameState.value?.morvathShadowOrbEffects?.isNotEmpty() == true) {
            delay(GamePlayConstants.AnimationTimings.MORVATH_ORB_FLIGHT_DELAY_MS)
            engine.applyPendingMorvathFog()
        }

        // Snotling cannon survivors arrive only after the projectile animation hits home.
        if (_gameState.value?.pendingSnotlingCannonArrivals?.isNotEmpty() == true) {
            delay(GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS)
            engine.processPendingSnotlingCannonArrivals()
        }

        // Process pending barge deletions from Roderich's Broadside after the cannonball animation completes.
        // The animation duration is BALLISTA_FLIGHT_DELAY_MS (1000ms), so we delay before processing.
        if (_gameState.value?.pendingBargeDeletions?.isNotEmpty() == true) {
            delay(GamePlayConstants.AnimationTimings.BALLISTA_FLIGHT_DELAY_MS)
            engine.processPendingBargeDeletions()
        }

        // Trigger camera pan to bomb explosion position if any bomb exploded this turn
        val currentStateForBombs = _gameState.value
        if (currentStateForBombs != null && currentStateForBombs.bombExplosionEffects.isNotEmpty()) {
            _pendingScrollToPosition.value = currentStateForBombs.bombExplosionEffects.first().center
        }

        // Surface any remaining pending game messages (target taken, gate destroyed, etc.)
        // Only surface if no message is currently being shown (e.g. from the spawn phase above).
        // Each dismiss triggers the next message via dismissGameMessage().
        surfaceNextPendingMessageIfIdle()

        // Autosave at the beginning of the new player turn (after enemy turn completes)
        // This ensures the phase is PLAYER_TURN when the save is created
        autoSaveGame()

        // Check win/loss conditions
        val updatedState = _gameState.value ?: return@launch
        // Track turn count achievement (100 turns in a single level)
        achievementManager?.onTurnReached(updatedState.turnNumber.value)
        // Check loss first: a villain breaching a target loses immediately, even if all enemies are defeated
        if (updatedState.isLevelLost()) {
            completeLevel(updatedState.level.id, won = false)
        } else if (updatedState.isLevelWon()) {
            completeLevel(updatedState.level.id, won = true)
        }
    }
}

fun GameViewModel.autoAttackAndEndTurn() {
    // This method is called from the "Auto-Attack and End Turn" button in the confirmation dialog
    // It explicitly calls autoDefenderAttacks() before ending the turn
    val engine = gameEngine ?: return
    val currentState = gameState.value ?: return

    viewModelScope.launch {
        // Track whether any attacks fired / enemies killed so we can wait for animations
        val triggerCountBefore = currentState.attackTriggerCount.value
        val deathCountBefore = currentState.defeatedEnemyEffects.size

        // Explicitly trigger auto-attacks for all ready defenders
        engine.autoDefenderAttacks()

        val anyAttacksFired = currentState.attackTriggerCount.value > triggerCountBefore
        val enemiesKilled = currentState.defeatedEnemyEffects.size > deathCountBefore

        if (anyAttacksFired) {
            // Determine the projectile flight duration (ballista/wizard/alchemy = 1000 ms,
            // arrows/spears/pikes = 900 ms).
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

            // Wait for the projectile to arrive visually.
            delay(flightDelayMs)

            if (enemiesKilled) {
                // Death animation, then the full coin-gain sequence (bubble-up + fly-to-counter)
                delay(GamePlayConstants.AnimationTimings.ENEMY_DEATH_ANIMATION_DURATION_MS)
                delay(GamePlayConstants.AnimationTimings.COIN_GAIN_DELAY_AFTER_DEATH_MS)
                delay(GamePlayConstants.AnimationTimings.COIN_GAIN_ANIMATION_DURATION_MS)
            } else {
                // No kills — just let the impact flash finish.
                delay(GamePlayConstants.AnimationTimings.ATTACK_IMPACT_DURATION_MS)
            }

            if (currentState.pendingBridgeDamage.isNotEmpty()) {
                engine.processPendingBridgeDamage()
            }
        }

        // Surface any messages queued by scripted events fired by the auto-attacks (kills), so they
        // are shown even when the turn does not end immediately (special actions remaining below).
        surfaceNextPendingMessageIfIdle()
        if (currentState.scriptedVictory.value) return@launch

        // Check if there are special actions remaining (mines, alchemy, wizard traps)
        val specialActionTypes = currentState.getDefenderTypesWithSpecialActions()

        if (specialActionTypes.isNotEmpty()) {
            // There are special actions remaining - show warning dialog instead of ending turn
            // Store the types so the UI can display them
            _specialActionsRemaining.value = specialActionTypes
        } else {
            // No special actions remaining - proceed with ending turn
            endPlayerTurn()
        }
    }
}

fun GameViewModel.clearSpecialActionsWarning() {
    _specialActionsRemaining.value = emptyList()
}

/**
 * Instantly wins the current level when it is guaranteed to be won (see [GameState.canWinLevelNow]).
 * Triggered by the "Win Level now" action, available when the remaining enemies cannot deplete the
 * player's health points.
 */
fun GameViewModel.winLevelNow() {
    val state = _gameState.value ?: return
    if (!state.canWinLevelNow()) return
    // Mark all remaining enemies as defeated so the game state is consistent with a completed level.
    state.attackers.forEach { it.isDefeated.value = true }
    completeLevel(state.level.id, won = true)
}

internal fun GameViewModel.completeLevel(
    levelId: Int,
    won: Boolean,
) {
    if (won && _gameState.value?.scriptedVictory?.value == true) {
        if (_currentScreen.value !is Screen.GamePlay) return
        if (_pendingGameMessage.value != null) return
        if (_gameState.value?.pendingMessages?.isNotEmpty() == true) {
            surfaceNextPendingMessageIfIdle()
            return
        }
    }
    val activeEditorPlaytest = editorPlaytestSession
    val currentHP = _gameState.value?.healthPoints?.value ?: 0
    val rawXpEarned = _gameState.value?.xpEarnedThisLevel?.value ?: 0
    val isSandbox = _gameState.value?.level?.isSandbox == true
    // Sandbox levels never award XP.
    val xpEarned = if (isSandbox) 0 else GameViewModel.calculateAwardedXpForLevelCompletion(rawXpEarned, won)
    val levelName = _gameState.value?.level?.name ?: "unknown"
    val turnNumber = _gameState.value?.turnNumber?.value
    var newPlayerLevel = 0
    var playerLevelGained = 0
    var abilityPointsGained = 0
    var hasUpdatedPlayerProfile = false

    GameLogBuffer.log("GAME", "Level '$levelName' ${if (won) "WON" else "LOST"} — HP: $currentHP, Turn: $turnNumber, XP: $xpEarned")

    de.egril.defender.analytics.reportEvent(
        if (won) de.egril.defender.analytics.GameEventType.LEVEL_WON else de.egril.defender.analytics.GameEventType.LEVEL_LOST,
        levelName,
        turnNumber,
        _gameState.value?.difficulty?.name ?: AppSettings.difficulty.value.name,
    )

    if (activeEditorPlaytest != null) {
        deleteBackgroundSave()
        _currentScreen.value =
            Screen.LevelComplete(
                levelId = levelId,
                won = won,
                isLastLevel = false,
                xpEarned = 0,
                newPlayerLevel = 0,
                playerLevelGained = 0,
                abilityPointsGained = 0,
            )
        return
    }

    // Track achievement for level completion
    if (won) {
        achievementManager?.onWinLevel(currentHP)
    } else {
        achievementManager?.onLoseLevel()
    }

    // Award XP to player profile (full XP on win, 20% on loss)
    val currentPlayer = _currentPlayer.value
    if (currentPlayer != null && xpEarned > 0) {
        val previousAbilities = currentPlayer.abilities
        val updatedStats = currentPlayer.abilities.addXP(xpEarned)
        newPlayerLevel = updatedStats.level
        playerLevelGained = (updatedStats.level - previousAbilities.level).coerceAtLeast(0)
        abilityPointsGained = (updatedStats.availableAbilityPoints - previousAbilities.availableAbilityPoints).coerceAtLeast(0)
        val updatedPlayer = currentPlayer.copy(abilities = updatedStats)
        _currentPlayer.value = updatedPlayer
        de.egril.defender.save.PlayerProfileStorage
            .updateProfile(updatedPlayer)
        hasUpdatedPlayerProfile = true
    }

    val isLastLevel =
        _worldLevels.value
            .firstOrNull { it.level.id == levelId }
            ?.level
            ?.editorLevelId == OfficialContent.FINAL_LEVEL_ID

    if (won && isLastLevel) {
        de.egril.defender.analytics.reportEvent(
            de.egril.defender.analytics.GameEventType.GAME_WON,
            levelName,
            turnNumber,
            _gameState.value?.difficulty?.name ?: AppSettings.difficulty.value.name,
        )
    }

    // Find the current level's index once; reused for both next-level computation and status update below
    val currentLevelIndex = _worldLevels.value.indexOfFirst { it.level.id == levelId }

    // Compute next level: the level immediately following the current one in the world levels list (only when won and not the final level)
    val nextLevel: WorldLevel? =
        if (won && !isLastLevel && currentLevelIndex >= 0 && currentLevelIndex + 1 < _worldLevels.value.size) {
            _worldLevels.value[currentLevelIndex + 1]
        } else {
            null
        }

    if (won) {
        val updatedLevels = _worldLevels.value.toMutableList()
        val wonWorldLevel = updatedLevels.find { it.level.id == levelId }
        if (wonWorldLevel != null) {
            val wonIndex = updatedLevels.indexOf(wonWorldLevel)
            updatedLevels[wonIndex] = wonWorldLevel.copy(status = LevelStatus.WON)

            // Get the set of all won level IDs (including the just-won level)
            val wonLevelIds =
                updatedLevels
                    .filter { it.status == LevelStatus.WON }
                    .mapNotNull { it.level.editorLevelId }
                    .toSet()

            // Unlock levels based on prerequisites
            for (i in updatedLevels.indices) {
                val worldLevel = updatedLevels[i]
                if (worldLevel.status == LevelStatus.LOCKED && worldLevel.level.editorLevelId != null) {
                    if (de.egril.defender.editor.EditorStorage
                            .isLevelUnlocked(worldLevel.level.editorLevelId, wonLevelIds)
                    ) {
                        updatedLevels[i] = worldLevel.copy(status = LevelStatus.UNLOCKED)
                    }
                }
            }

            _worldLevels.value = updatedLevels
            // Save world map status
            saveWorldMapStatus()

            // Save level handoff for any connected level on the same map
            val wonEditorLevelId = wonWorldLevel.level.editorLevelId
            val wonMapId = wonWorldLevel.level.mapId
            if (wonEditorLevelId != null && wonMapId != null) {
                saveHandoffForConnectedLevels(wonEditorLevelId, wonMapId, updatedLevels)
            }
        }
        // Sync updated abilities (XP awarded) and level progress to backend
        uploadUserDataToBackend()
    } else if (hasUpdatedPlayerProfile) {
        // Sync profile when XP is awarded on a lost level.
        uploadUserDataToBackend()
    }

    if (_isDemoMode.value) {
        // In demo mode: show the Level Won/Lost screen for 4 seconds, then load the next level
        _currentScreen.value =
            Screen.LevelComplete(levelId, won, isLastLevel, xpEarned, newPlayerLevel, playerLevelGained, abilityPointsGained)
        viewModelScope.launch {
            delay(4000L)
            if (_isDemoMode.value) {
                demoLevelIndex =
                    (demoLevelIndex + 1) %
                    de.egril.defender.game.DemoMode
                        .getLevelCount(demoScenario)
                loadDemoLevel(demoLevelIndex)
            }
        }
        return
    }
    // Level ended – remove any background save so it is not restored on the next cold start.
    deleteBackgroundSave()
    _currentScreen.value =
        Screen.LevelComplete(
            levelId = levelId,
            won = won,
            isLastLevel = isLastLevel,
            xpEarned = xpEarned,
            newPlayerLevel = newPlayerLevel,
            playerLevelGained = playerLevelGained,
            abilityPointsGained = abilityPointsGained,
            nextLevelId = nextLevel?.level?.id,
            nextLevelName = nextLevel?.level?.name,
        )
}

fun GameViewModel.restartLevel() {
    editorPlaytestSession?.let { playtest ->
        startEditorPlaytest(playtest.editorLevel)
        return
    }
    val levelId =
        (_currentScreen.value as? Screen.LevelComplete)?.levelId
            ?: (_currentScreen.value as? Screen.GamePlay)?.levelId
            ?: return
    startLevel(levelId)
}

/**
 * Saves the current game state as a level handoff for any connected level that is "next"
 * on the world map path from the won level.
 *
 * "Next on the path" means:
 *  1. Another level at the SAME world map location that directly lists [wonEditorLevelId]
 *     as a prerequisite (within-location chain, e.g., fortress_1 → fortress_2).
 *  2. A level at a DESTINATION location reachable via an outgoing path from the won level's
 *     location (cross-location step).
 *
 * Handoffs are only saved for official levels (not community / user-created) that
 * also have [connectedToPreviousLevel] = true and share the same map as the won level.
 */
private fun GameViewModel.saveHandoffForConnectedLevels(
    wonEditorLevelId: String,
    wonMapId: String,
    allLevels: List<WorldLevel>,
) {
    val gameState = _gameState.value ?: return

    // Don't generate handoffs for community or non-official (user-created) levels.
    val wonWorldLevel = allLevels.find { it.level.editorLevelId == wonEditorLevelId }
    if (wonWorldLevel?.level?.isCommunity == true) return
    // isOfficial == false means user-created; skip those too
    val wonEditorLevel =
        de.egril.defender.editor.EditorStorage
            .getLevel(wonEditorLevelId)
    if (wonEditorLevel != null && !wonEditorLevel.isOfficial) return

    // Use world map data to determine which levels are "next on the path".
    val worldMapData =
        de.egril.defender.editor.EditorStorage
            .getWorldMapData()
    val wonLocation = worldMapData.findLocationByLevelId(wonEditorLevelId)

    // Collect the set of candidate editor-level IDs that may receive the handoff.
    val candidateLevelIds = mutableSetOf<String>()
    if (wonLocation != null) {
        // Within-location: levels at the same world map node that list wonEditorLevelId as a prerequisite
        candidateLevelIds += wonLocation.levelIds.filter { it != wonEditorLevelId }

        // Cross-location: levels at destination locations of outgoing paths from wonLocation
        worldMapData.paths
            .filter { it.fromLocationId == wonLocation.id }
            .forEach { path ->
                worldMapData.findLocation(path.toLocationId)?.levelIds?.let {
                    candidateLevelIds += it
                }
            }
    }

    if (candidateLevelIds.isEmpty()) return // hermit tower and other dead-ends have no candidates

    // Among candidates, only keep levels that:
    //  - have connectedToPreviousLevel = true
    //  - are on the same map
    //  - are official (not community, not user-created)
    //  - directly list wonEditorLevelId as a prerequisite (ensures strict ordering)
    val connectedLevels =
        allLevels
            .filter { wl ->
                wl.level.connectedToPreviousLevel &&
                    !wl.level.isCommunity &&
                    wl.level.mapId == wonMapId &&
                    wl.level.editorLevelId != null &&
                    wl.level.editorLevelId in candidateLevelIds
            }.filter { wl ->
                // Verify the won level is a direct prerequisite (strict path ordering)
                val editorLevel =
                    de.egril.defender.editor.EditorStorage
                        .getLevel(wl.level.editorLevelId!!)
                editorLevel != null && wonEditorLevelId in editorLevel.prerequisites
            }

    if (connectedLevels.isEmpty()) return

    // Build handoff from current game state
    val handoffDefenders =
        gameState.defenders.map { d ->
            de.egril.defender.save.SavedDefender(
                id = d.id,
                type = d.type,
                position = d.position.value,
                level = d.level.value,
                buildTimeRemaining = 0, // Always fully built on carry-over
                placedOnTurn = d.placedOnTurn,
                actionsRemaining = d.actionsRemaining.value,
                dragonName = d.dragonName,
                raftId = d.raftId.value,
                towerBaseBarricadeId = d.towerBaseBarricadeId.value,
                trapCooldownRemaining = d.trapCooldownRemaining.value,
            )
        }
    val handoffBarricades =
        gameState.barricades.map { b ->
            de.egril.defender.save.SavedBarricade(
                position = b.position,
                healthPoints = b.healthPoints.value,
                defenderId = b.defenderId,
                id = b.id,
                supportedTowerId = b.supportedTowerId.value,
            )
        }
    val handoffTraps =
        gameState.traps.map { t ->
            de.egril.defender.save.SavedTrap(
                position = t.position,
                damage = t.damage,
                defenderId = t.defenderId,
                type = t.type.name,
            )
        }
    val handoffRafts =
        gameState.rafts.map { r ->
            de.egril.defender.save.SavedRaft(
                id = r.id,
                defenderId = r.defenderId,
                position = r.currentPosition.value,
                healthPoints = r.healthPoints.value,
            )
        }

    for (connectedLevel in connectedLevels) {
        val toEditorId = connectedLevel.level.editorLevelId ?: continue
        val handoff =
            de.egril.defender.save.LevelHandoffSave(
                fromLevelEditorId = wonEditorLevelId,
                toLevelEditorId = toEditorId,
                coins = gameState.coins.value,
                currentMana = gameState.currentMana.value,
                maxMana = gameState.maxMana.value,
                defenders = handoffDefenders,
                barricades = handoffBarricades,
                traps = handoffTraps,
                rafts = handoffRafts,
                nextDefenderId = gameState.nextDefenderId.value,
                nextRaftId = gameState.nextRaftId.value,
                mapId = wonMapId,
            )
        de.egril.defender.save.SaveFileStorage
            .saveLevelHandoff(handoff)
    }
}
