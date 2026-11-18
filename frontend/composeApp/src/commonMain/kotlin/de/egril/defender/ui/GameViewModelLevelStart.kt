package de.egril.defender.ui

import androidx.compose.runtime.mutableStateOf
import com.hyperether.resources.currentLanguage
import de.egril.defender.game.GameEngine
import de.egril.defender.model.*
import de.egril.defender.ui.settings.AppSettings
import de.egril.defender.utils.DeepLink
import de.egril.defender.utils.checkCurrentDeepLink

fun GameViewModel.startLevel(levelId: Int) {
    editorPlaytestSession = null
    // Clear any pending message from a previous level
    _pendingGameMessage.value = null
    val worldLevel = _worldLevels.value.find { it.level.id == levelId }
    if (worldLevel != null && worldLevel.status != LevelStatus.LOCKED) {
        val level = worldLevel.level
        val editorLevelId = level.editorLevelId

        // Check if this level is connected to the previous level and a handoff exists.
        // Only show the dialog for official levels (not community / user-created).
        val isOfficialLevel =
            !level.isCommunity &&
                editorLevelId != null &&
                de.egril.defender.editor.EditorStorage
                    .getLevel(editorLevelId)
                    ?.isOfficial == true
        if (level.connectedToPreviousLevel && isOfficialLevel) {
            if (
                de.egril.defender.save.SaveFileStorage
                    .hasLevelHandoff(editorLevelId)
            ) {
                val handoff =
                    de.egril.defender.save.SaveFileStorage
                        .loadLevelHandoff(editorLevelId)
                if (handoff != null && handoff.mapId == level.mapId) {
                    // Show the handoff choice dialog
                    _pendingLevelHandoff.value = handoff
                    pendingLevelIdForHandoff = levelId
                    return
                }
            }
        }

        // No handoff – start the level normally
        startLevelInternal(levelId)
    }
}

/** Dismiss the level handoff dialog without starting the level */
fun GameViewModel.dismissLevelHandoff() {
    _pendingLevelHandoff.value = null
    pendingLevelIdForHandoff = null
}

/** Start the pending level fresh (ignore handoff state) */
fun GameViewModel.startLevelFresh() {
    val levelId = pendingLevelIdForHandoff ?: return
    val editorLevelId =
        _worldLevels.value
            .find { it.level.id == levelId }
            ?.level
            ?.editorLevelId
    _pendingLevelHandoff.value = null
    pendingLevelIdForHandoff = null
    // Delete the handoff so it doesn't show up again
    if (editorLevelId != null) {
        de.egril.defender.save.SaveFileStorage
            .deleteLevelHandoff(editorLevelId)
    }
    startLevelInternal(levelId)
}

/** Start the pending level using the handoff state (carry-over from previous level) */
fun GameViewModel.startLevelWithHandoff() {
    val levelId = pendingLevelIdForHandoff ?: return
    val handoff = _pendingLevelHandoff.value ?: return
    _pendingLevelHandoff.value = null
    pendingLevelIdForHandoff = null
    startLevelInternalWithHandoff(levelId, handoff)
}

private fun GameViewModel.startLevelInternal(levelId: Int) {
    editorPlaytestSession = null
    // Clear any pending message from a previous level
    _pendingGameMessage.value = null
    val worldLevel = _worldLevels.value.find { it.level.id == levelId }
    if (worldLevel != null && worldLevel.status != LevelStatus.LOCKED) {
        val difficulty = AppSettings.difficulty.value
        val level = worldLevel.level
        val playerStats = _currentPlayer.value?.abilities ?: PlayerAbilities()

        // Apply difficulty modifiers to spawn plan
        val modifiedSpawnPlan =
            if (level.directSpawnPlan != null) {
                DifficultyModifiers.applySpawnPlanModifier(level.directSpawnPlan, difficulty)
            } else {
                val basePlan = generateSpawnPlan(level.attackerWaves)
                DifficultyModifiers.applySpawnPlanModifier(basePlan, difficulty)
            }

        // Apply player stats bonuses
        val baseCoins = DifficultyModifiers.applyCoinsModifier(level.initialCoins, difficulty)
        val bonusCoins = playerStats.getBonusStartCoins()
        val totalCoins = baseCoins + bonusCoins

        val baseHealth = DifficultyModifiers.applyHealthPointsModifier(level.healthPoints, difficulty)
        val bonusHealth = playerStats.getBonusHealth()
        val totalHealth = baseHealth + bonusHealth

        val maxMana = playerStats.getMaxMana()
        val incomeMultiplier = playerStats.getIncomeMultiplier()
        val constructionLevel = playerStats.constructionAbility

        // Create GameState with difficulty-modified and stats-bonus values
        val newGameState =
            GameState(
                level = level,
                difficulty = difficulty,
                coins = mutableStateOf(totalCoins),
                healthPoints = mutableStateOf(totalHealth),
                spawnPlan = modifiedSpawnPlan,
                spawnGroups = level.spawnGroups?.let { DifficultyModifiers.applySpawnGroupsModifier(it, difficulty) },
                maxMana = mutableStateOf(maxMana),
                currentMana = mutableStateOf(maxMana), // Start with full mana
                incomeMultiplier = incomeMultiplier,
                constructionLevel = constructionLevel,
            )

        // Initialize pre-placed elements if any
        newGameState.initializePrePlacedElements()

        _gameState.value = newGameState
        gameEngine = GameEngine(newGameState)
        _currentScreen.value = Screen.GamePlay(levelId)

        // Show story intro message if this level has one (all levels except the tutorial)
        val editorLevelId = level.editorLevelId
        if (editorLevelId != null && editorLevelId != "welcome_to_defender_of_egril") {
            presentGameMessage(
                de.egril.defender.model.GameMessage(
                    type = de.egril.defender.model.GameMessageType.STORY_INTRO,
                    name = editorLevelId,
                ),
            )
        }

        // Capture initial state snapshot
        initialGameStateSnapshot = createGameStateSnapshot(newGameState)
        lastSaveSnapshot = initialGameStateSnapshot

        de.egril.defender.analytics.reportEvent(
            de.egril.defender.analytics.GameEventType.LEVEL_STARTED,
            level.name,
            difficulty = difficulty.name,
        )

        // Initialize achievement manager for this level
        val playerId = _currentPlayer.value?.id
        if (playerId != null) {
            achievementManager =
                de.egril.defender.game.AchievementManager(playerId).apply {
                    onAchievementEarned = { achievement ->
                        _newAchievement.value = achievement
                        // Refresh player profile to show updated achievements
                        refreshCurrentPlayer()
                    }
                    startLevel(newGameState.healthPoints.value)
                }

            // Set up combat result callback for kill tracking
            gameEngine?.setCombatResultCallback { result ->
                // Track kills from this attack
                result.killInfos.forEach { killInfo ->
                    achievementManager?.onEnemyKilled(
                        enemyType = killInfo.enemyType,
                        killsInThisAttack = result.killsThisAttack,
                        wasUninjured = killInfo.wasUninjured,
                        usedSupportElement = killInfo.usedSupportElement,
                    )
                }
            }

            // Set up raft loss callback
            gameEngine?.setRaftLossCallback { reason ->
                when (reason) {
                    de.egril.defender.game.RaftLossReason.MAP_EDGE ->
                        achievementManager?.onRaftLostToMapEdge()
                    de.egril.defender.game.RaftLossReason.MAELSTROM ->
                        achievementManager?.onRaftLostToMaelstrom()
                    de.egril.defender.game.RaftLossReason.OTHER -> {} // No achievement
                }
            }

            // Set up dragon level change callback
            gameEngine?.setDragonLevelChangeCallback { oldLevel, newLevel ->
                if (newLevel > oldLevel) {
                    achievementManager?.onIncreaseDragonLevel()
                } else if (newLevel < oldLevel) {
                    achievementManager?.onReduceDragonLevel()
                }
            }
        }

        // Start time tracking for reminders
        startTimeTracking()
    }
}

/**
 * Start a level using the carry-over state from the previous connected level.
 * Restores defenders, barricades, traps, rafts, coins, and mana from [handoff].
 */
private fun GameViewModel.startLevelInternalWithHandoff(
    levelId: Int,
    handoff: de.egril.defender.save.LevelHandoffSave,
) {
    editorPlaytestSession = null
    _pendingGameMessage.value = null
    val worldLevel = _worldLevels.value.find { it.level.id == levelId }
    if (worldLevel != null && worldLevel.status != LevelStatus.LOCKED) {
        val difficulty = AppSettings.difficulty.value
        val level = worldLevel.level
        val playerStats = _currentPlayer.value?.abilities ?: PlayerAbilities()

        // Apply difficulty modifiers to spawn plan
        val modifiedSpawnPlan =
            if (level.directSpawnPlan != null) {
                DifficultyModifiers.applySpawnPlanModifier(level.directSpawnPlan, difficulty)
            } else {
                val basePlan = generateSpawnPlan(level.attackerWaves)
                DifficultyModifiers.applySpawnPlanModifier(basePlan, difficulty)
            }

        val baseHealth = DifficultyModifiers.applyHealthPointsModifier(level.healthPoints, difficulty)
        val bonusHealth = playerStats.getBonusHealth()
        val totalHealth = baseHealth + bonusHealth

        val incomeMultiplier = playerStats.getIncomeMultiplier()
        val constructionLevel = playerStats.constructionAbility

        // Use coins and mana from the handoff.
        // If the previous level had no mana (maxMana == 0), fall back to the player's current
        // mana capacity so the connected level still functions correctly (e.g., a new campaign
        // stage that introduces mana for the first time).
        val startCoins = handoff.coins
        val startMana = handoff.currentMana.coerceAtMost(handoff.maxMana)
        val maxMana = handoff.maxMana.takeIf { it > 0 } ?: playerStats.getMaxMana()

        val newGameState =
            GameState(
                level = level,
                difficulty = difficulty,
                coins = mutableStateOf(startCoins),
                healthPoints = mutableStateOf(totalHealth),
                spawnPlan = modifiedSpawnPlan,
                spawnGroups = level.spawnGroups?.let { DifficultyModifiers.applySpawnGroupsModifier(it, difficulty) },
                maxMana = mutableStateOf(maxMana),
                currentMana = mutableStateOf(startMana),
                incomeMultiplier = incomeMultiplier,
                constructionLevel = constructionLevel,
            )

        // Restore defenders from handoff (reset actionsRemaining; build time is already 0)
        newGameState.nextDefenderId.value = handoff.nextDefenderId
        newGameState.nextRaftId.value = handoff.nextRaftId
        for (savedDefender in handoff.defenders) {
            val defender =
                de.egril.defender.model.Defender(
                    id = savedDefender.id,
                    type = savedDefender.type,
                    position = mutableStateOf(savedDefender.position),
                    placedOnTurn = savedDefender.placedOnTurn,
                    dragonName = savedDefender.dragonName,
                )
            defender.level.value = savedDefender.level
            defender.buildTimeRemaining.value = 0 // Always fully built
            defender.actionsRemaining.value = savedDefender.type.actionsPerTurn
            defender.raftId.value = savedDefender.raftId
            defender.towerBaseBarricadeId.value = savedDefender.towerBaseBarricadeId
            defender.trapCooldownRemaining.value = savedDefender.trapCooldownRemaining
            newGameState.defenders.add(defender)
        }

        // Restore rafts
        for (savedRaft in handoff.rafts) {
            val raft =
                de.egril.defender.model.Raft(
                    id = savedRaft.id,
                    defenderId = savedRaft.defenderId,
                    currentPosition = mutableStateOf(savedRaft.position),
                    healthPoints = mutableStateOf(savedRaft.healthPoints),
                )
            newGameState.rafts.add(raft)
        }

        // Restore barricades
        for (savedBarricade in handoff.barricades) {
            val barricade =
                de.egril.defender.model.Barricade(
                    id = savedBarricade.id,
                    position = savedBarricade.position,
                    healthPoints = mutableStateOf(savedBarricade.healthPoints),
                    defenderId = savedBarricade.defenderId,
                    supportedTowerId = mutableStateOf(savedBarricade.supportedTowerId),
                )
            newGameState.barricades.add(barricade)
        }

        // Restore traps
        for (savedTrap in handoff.traps) {
            val trapType =
                try {
                    de.egril.defender.model.TrapType
                        .valueOf(savedTrap.type)
                } catch (e: Exception) {
                    de.egril.defender.model.TrapType.DWARVEN
                }
            val trap =
                de.egril.defender.model.Trap(
                    position = savedTrap.position,
                    damage = savedTrap.damage,
                    defenderId = savedTrap.defenderId,
                    type = trapType,
                )
            newGameState.traps.add(trap)
        }

        // Skip initializePrePlacedElements: the handoff already includes all defenders,
        // barricades, traps, and rafts carried over from the previous level.
        // Pre-placed elements from the level definition would create duplicates at the
        // same positions, so we intentionally omit them here.

        // Delete the handoff file now that it has been used
        level.editorLevelId?.let {
            de.egril.defender.save.SaveFileStorage
                .deleteLevelHandoff(it)
        }

        _gameState.value = newGameState
        gameEngine = GameEngine(newGameState)
        _currentScreen.value = Screen.GamePlay(levelId)

        val editorLevelId = level.editorLevelId
        if (editorLevelId != null && editorLevelId != "welcome_to_defender_of_egril") {
            presentGameMessage(
                de.egril.defender.model.GameMessage(
                    type = de.egril.defender.model.GameMessageType.STORY_INTRO,
                    name = editorLevelId,
                ),
            )
        }

        initialGameStateSnapshot = createGameStateSnapshot(newGameState)
        lastSaveSnapshot = initialGameStateSnapshot

        de.egril.defender.analytics.reportEvent(
            de.egril.defender.analytics.GameEventType.LEVEL_STARTED,
            level.name,
            difficulty = difficulty.name,
        )

        val playerId = _currentPlayer.value?.id
        if (playerId != null) {
            achievementManager =
                de.egril.defender.game.AchievementManager(playerId).apply {
                    onAchievementEarned = { achievement ->
                        _newAchievement.value = achievement
                        refreshCurrentPlayer()
                    }
                    startLevel(newGameState.healthPoints.value)
                }
            gameEngine?.setCombatResultCallback { result ->
                result.killInfos.forEach { killInfo ->
                    achievementManager?.onEnemyKilled(
                        enemyType = killInfo.enemyType,
                        killsInThisAttack = result.killsThisAttack,
                        wasUninjured = killInfo.wasUninjured,
                        usedSupportElement = killInfo.usedSupportElement,
                    )
                }
            }
            gameEngine?.setRaftLossCallback { reason ->
                when (reason) {
                    de.egril.defender.game.RaftLossReason.MAP_EDGE ->
                        achievementManager?.onRaftLostToMapEdge()
                    de.egril.defender.game.RaftLossReason.MAELSTROM ->
                        achievementManager?.onRaftLostToMaelstrom()
                    de.egril.defender.game.RaftLossReason.OTHER -> {}
                }
            }
            gameEngine?.setDragonLevelChangeCallback { oldLevel, newLevel ->
                if (newLevel > oldLevel) {
                    achievementManager?.onIncreaseDragonLevel()
                } else if (newLevel < oldLevel) {
                    achievementManager?.onReduceDragonLevel()
                }
            }
        }

        startTimeTracking()
    }
}

/**
 * Handles deep link navigation for web/WASM variant.
 * Called once during app startup to check for deep links like /data-privacy/en, /info/installation, or /tutorial
 */
fun GameViewModel.handleDeepLink() {
    val deepLink = checkCurrentDeepLink()
    when (deepLink) {
        is DeepLink.DataPrivacy -> {
            // Set the language from the deep link
            currentLanguage.value = deepLink.language
            // Navigate to the backend info page (which is the data-privacy page)
            navigateToBackendInfo()
        }
        is DeepLink.InfoPage -> {
            // Navigate directly to the requested info tab
            _currentScreen.value = Screen.InstallationInfoAtTab(deepLink.tab)
        }
        DeepLink.Tutorial -> {
            // Show a loading screen; App.kt will start the tutorial once data + player are ready
            _currentScreen.value = Screen.TutorialDeepLink
            _pendingTutorialDeepLink.value = true
            de.egril.defender.analytics.reportEvent(
                de.egril.defender.analytics.GameEventType.TUTORIAL_DEEP_LINK,
                null,
            )
        }
        DeepLink.Demo -> {
            // Show a loading screen; App.kt will start demo mode once data + player are ready
            _currentScreen.value = Screen.DemoDeepLink
            _pendingDemoDeepLink.value = true
        }
        DeepLink.Settings -> {
            _currentScreen.value = Screen.MainMenu
            _pendingSettingsDeepLink.value = true
        }
        DeepLink.VillainsAnnouncement -> {
            _currentScreen.value = Screen.VillainsAnnouncement
        }
        DeepLink.None -> {
            // No deep link, proceed normally
        }
    }
}

fun GameViewModel.clearPendingSettingsDeepLink() {
    _pendingSettingsDeepLink.value = false
}

/**
 * Starts the tutorial (first) level. Called when the user visits /tutorial on web.
 * Resets the pending tutorial flag and starts level 1.
 */
fun GameViewModel.startTutorialLevel() {
    _pendingTutorialDeepLink.value = false
    startLevel(GameViewModel.TUTORIAL_LEVEL_ID)
}
