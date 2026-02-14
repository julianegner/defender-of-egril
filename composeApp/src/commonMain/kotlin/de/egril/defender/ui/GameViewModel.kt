package de.egril.defender.ui

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import de.egril.defender.game.GameEngine
import de.egril.defender.game.GameEngine.EnemyTurnMovements
import de.egril.defender.game.LevelData
import de.egril.defender.model.*
import de.egril.defender.model.DifficultyModifiers
import de.egril.defender.ui.settings.AppSettings
import de.egril.defender.utils.CheatCodeHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class Screen {
    object MainMenu : Screen()
    object WorldMap : Screen()
    object Rules : Screen()
    object InstallationInfo : Screen()
    object LevelEditor : Screen()
    object LoadGame : Screen()
    object Sticker : Screen()
    object PlayerProfile : Screen()
    object StatsUpgrade : Screen()  // New screen for stats/spells upgrade
    data class GamePlay(val levelId: Int) : Screen()
    data class LevelComplete(
        val levelId: Int, 
        val won: Boolean, 
        val isLastLevel: Boolean,
        val xpEarned: Int = 0
    ) : Screen()
}

/**
 * Represents a conflict between saved world map progress and current world map progress
 */
data class WorldMapConflict(
    val savedGame: de.egril.defender.save.SavedGame?,  // Null when importing just game state
    val savedWorldMap: de.egril.defender.save.WorldMapSave,
    val currentWorldMap: Map<String, LevelStatus>,
    val level: Level?  // Null when importing just game state (no level to load)
)

/**
 * Data class for reminder messages
 */
data class ReminderMessage(
    val type: de.egril.defender.ui.gameplay.ReminderType,
    val elapsedTime: String? = null,
    val timeDescription: String? = null
)

class GameViewModel {
    
    private val _currentScreen = MutableStateFlow<Screen>(Screen.MainMenu)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()
    
    private val _worldLevels = MutableStateFlow<List<WorldLevel>>(emptyList())
    val worldLevels: StateFlow<List<WorldLevel>> = _worldLevels.asStateFlow()
    
    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState: StateFlow<GameState?> = _gameState.asStateFlow()
    
    private val _cheatDigOutcome = MutableStateFlow<DigOutcome?>(null)
    val cheatDigOutcome: StateFlow<DigOutcome?> = _cheatDigOutcome.asStateFlow()
    
    private val _showPlatformInfo = MutableStateFlow(false)
    val showPlatformInfo: StateFlow<Boolean> = _showPlatformInfo.asStateFlow()
    
    // Player profile state
    private val _currentPlayer = MutableStateFlow<de.egril.defender.save.PlayerProfile?>(null)
    val currentPlayer: StateFlow<de.egril.defender.save.PlayerProfile?> = _currentPlayer.asStateFlow()
    
    private val _allPlayers = MutableStateFlow<List<de.egril.defender.save.PlayerProfile>>(emptyList())
    val allPlayers: StateFlow<List<de.egril.defender.save.PlayerProfile>> = _allPlayers.asStateFlow()
    
    private val _needsPlayerSelection = MutableStateFlow(false)
    val needsPlayerSelection: StateFlow<Boolean> = _needsPlayerSelection.asStateFlow()
    
    // World map progress conflict state
    private val _worldMapConflict = MutableStateFlow<WorldMapConflict?>(null)
    val worldMapConflict: StateFlow<WorldMapConflict?> = _worldMapConflict.asStateFlow()
    
    // Special actions remaining after auto-attack
    private val _specialActionsRemaining = MutableStateFlow<List<DefenderType>>(emptyList())
    val specialActionsRemaining: StateFlow<List<DefenderType>> = _specialActionsRemaining.asStateFlow()

    // Time reminders for breaks and sleep
    private val _reminderMessage = MutableStateFlow<ReminderMessage?>(null)
    val reminderMessage: StateFlow<ReminderMessage?> = _reminderMessage.asStateFlow()
    
    // Achievement system
    private var achievementManager: de.egril.defender.game.AchievementManager? = null
    private val _newAchievement = MutableStateFlow<Achievement?>(null)
    val newAchievement: StateFlow<Achievement?> = _newAchievement.asStateFlow()
    
    // Track game session time
    private var gameSessionStartTime: Long? = null
    private var lastBreakReminderTime: Long? = null
    private var lastSleepReminderTime: Long? = null
    
    // Constants for reminder intervals
    private val BREAK_REMINDER_INTERVAL_MS = 2 * 60 * 60 * 1000L  // 2 hours
    private val SLEEP_REMINDER_INTERVAL_MS = 60 * 60 * 1000L       // 1 hour
    private val SLEEP_START_HOUR = 23  // 23:00 (11 PM)

    // Track initial game state to detect unsaved changes
    private var initialGameStateSnapshot: String? = null
    private var lastSaveSnapshot: String? = null

    private var gameEngine: GameEngine? = null
    private val viewModelScope = CoroutineScope(Dispatchers.Default)
    
    
    init {
        // Ensure EditorStorage is initialized with repository data
        de.egril.defender.editor.EditorStorage.ensureInitialized()
        
        initializePlayerProfile()
        initializeWorldMap()
    }
    
    /**
     * Initialize player profile system
     * - Checks for existing players
     * - Migrates old saves if needed
     * - Sets up the current player
     */
    private fun initializePlayerProfile() {
        // Check for existing player profiles
        val profiles = de.egril.defender.save.PlayerProfileStorage.getAllProfiles()
        _allPlayers.value = profiles.profiles
        
        if (profiles.profiles.isEmpty()) {
            // No profiles exist - check if we need to migrate old saves
            val migratedProfile = de.egril.defender.save.PlayerProfileStorage.migrateExistingSaves()
            if (migratedProfile != null) {
                // Migration successful, use the migrated profile
                println("Migrated existing saves to player profile: ${migratedProfile.name}")
                _currentPlayer.value = migratedProfile
                de.egril.defender.save.SaveFileStorage.setCurrentPlayer(migratedProfile.id)
                _allPlayers.value = listOf(migratedProfile)
            } else {
                // No existing saves, need to create first player
                _needsPlayerSelection.value = true
            }
        } else {
            // Load the last used player or the most recently played one
            val lastPlayerId = profiles.lastUsedPlayerId
            val playerToUse = if (lastPlayerId != null) {
                profiles.profiles.find { it.id == lastPlayerId }
            } else {
                profiles.profiles.maxByOrNull { it.lastPlayedAt }
            }
            
            if (playerToUse != null) {
                _currentPlayer.value = playerToUse
                de.egril.defender.save.SaveFileStorage.setCurrentPlayer(playerToUse.id)
                de.egril.defender.save.PlayerProfileStorage.updateLastPlayed(playerToUse.id)
            } else {
                // Shouldn't happen, but handle gracefully
                _needsPlayerSelection.value = true
            }
        }
    }
    
    private fun initializeWorldMap() {
        // Load official levels
        val officialLevels = LevelData.createLevels()
        println("DEBUG: Total official levels loaded: ${officialLevels.size}")
        
        // Load user levels from user sequence
        val userSequence = de.egril.defender.editor.EditorStorage.getUserLevelSequence()
        val userLevels = userSequence.sequence.mapNotNull { levelId ->
            val editorLevel = de.egril.defender.editor.EditorStorage.reloadLevel(levelId)
            if (editorLevel != null && !editorLevel.isOfficial) {
                // Check if level is ready to play
                if (de.egril.defender.editor.EditorStorage.isLevelReadyToPlay(editorLevel)) {
                    // Convert editor level to game level
                    de.egril.defender.editor.EditorStorage.convertToGameLevel(
                        editorLevel, 
                        officialLevels.size + userSequence.sequence.indexOf(levelId) + 1
                    )
                } else {
                    null
                }
            } else {
                null
            }
        }
        println("DEBUG: Total user levels loaded: ${userLevels.size}")
        
        // Combine official and user levels
        val allLevels = officialLevels + userLevels
        
        // Load saved world map status
        val savedStatuses = de.egril.defender.save.SaveFileStorage.loadWorldMapStatus()
        
        // Get the set of won level IDs
        val wonLevelIds = savedStatuses?.filter { it.value == LevelStatus.WON }?.keys?.toSet() ?: emptySet()
        
        _worldLevels.value = allLevels.mapIndexed { index, level ->
            println("DEBUG: Loaded Level ${level.id} - Name: ${level.name} - Path Cells: ${level.pathCells.size} - Build Islands: ${level.buildIslands.size}")

            // Look up status by editorLevelId if available
            val status = if (level.editorLevelId != null) {
                // Check saved status first
                val savedStatus = savedStatuses?.get(level.editorLevelId)
                if (savedStatus != null) {
                    savedStatus
                } else {
                    // User levels are always unlocked
                    val editorLevel = de.egril.defender.editor.EditorStorage.getLevel(level.editorLevelId)
                    if (editorLevel?.isOfficial == false) {
                        LevelStatus.UNLOCKED
                    } else {
                        // Check if official level should be unlocked based on prerequisites
                        if (de.egril.defender.editor.EditorStorage.isLevelUnlocked(level.editorLevelId, wonLevelIds)) {
                            LevelStatus.UNLOCKED
                        } else {
                            LevelStatus.LOCKED
                        }
                    }
                }
            } else {
                // Fallback for legacy levels or levels created without editor (shouldn't happen in normal gameplay)
                println("WARNING: Level ${level.id} (${level.name}) has no editorLevelId - using fallback status")
                if (index == 0) LevelStatus.UNLOCKED else LevelStatus.LOCKED
            }
            
            WorldLevel(
                level = level,
                status = status
            )
        }
    }
    
    fun reloadWorldMap() {
        // Reload levels from disk to get latest changes
        println("Reloading world map from disk...")
        initializeWorldMap()
    }
    
    fun navigateToMainMenu() {
        stopTimeTracking()
        _currentScreen.value = Screen.MainMenu
    }
    
    fun navigateToWorldMap() {
        stopTimeTracking()
        // Reload levels from disk to ensure latest changes are visible
        reloadWorldMap()
        _currentScreen.value = Screen.WorldMap
    }
    
    fun navigateToRules() {
        _currentScreen.value = Screen.Rules
    }
    
    fun navigateToInstallationInfo() {
        _currentScreen.value = Screen.InstallationInfo
    }
    
    fun navigateToLevelEditor() {
        _currentScreen.value = Screen.LevelEditor
    }
    
    fun navigateToSticker() {
        _currentScreen.value = Screen.Sticker
    }
    
    fun navigateToPlayerProfile() {
        _currentScreen.value = Screen.PlayerProfile
    }
    
    fun navigateToStatsUpgrade() {
        _currentScreen.value = Screen.StatsUpgrade
    }
    
    fun upgradeStat(statType: de.egril.defender.model.StatType) {
        val currentPlayer = _currentPlayer.value ?: return
        val oldStats = currentPlayer.stats
        val updatedStats = oldStats.spendStatPoint(statType) ?: return
        val updatedPlayer = currentPlayer.copy(stats = updatedStats)
        _currentPlayer.value = updatedPlayer
        de.egril.defender.save.PlayerProfileStorage.updateProfile(updatedPlayer)
        
        // Check for achievements
        val playerId = currentPlayer.id
        val tempAchievementManager = de.egril.defender.game.AchievementManager(playerId)
        tempAchievementManager.onAchievementEarned = { achievement ->
            _showAchievementNotification.value = true
            _currentAchievement.value = achievement
        }
        
        // First stat upgrade achievement
        val totalSpentBefore = oldStats.healthStat + oldStats.treasuryStat + oldStats.incomeStat + 
                               oldStats.constructionStat + oldStats.manaStat
        if (totalSpentBefore == 0) {
            tempAchievementManager.onFirstStatUpgrade()
        }
        
        // Construction level 3 achievement
        if (statType == de.egril.defender.model.StatType.CONSTRUCTION && updatedStats.constructionStat >= 3) {
            tempAchievementManager.onConstructionLevel3()
        }
        
        // Player level achievements
        val playerLevel = de.egril.defender.model.PlayerStats.calculateLevel(updatedStats.totalXP)
        if (playerLevel >= 10) {
            tempAchievementManager.onPlayerLevel10()
        }
        if (playerLevel >= 100) {
            tempAchievementManager.onPlayerLevel100()
        }
    }
    
    fun unlockSpell(spell: de.egril.defender.model.SpellType) {
        val currentPlayer = _currentPlayer.value ?: return
        val oldStats = currentPlayer.stats
        val updatedStats = oldStats.unlockSpell(spell) ?: return
        val updatedPlayer = currentPlayer.copy(stats = updatedStats)
        _currentPlayer.value = updatedPlayer
        de.egril.defender.save.PlayerProfileStorage.updateProfile(updatedPlayer)
        
        // Check for first spell unlock achievement
        val playerId = currentPlayer.id
        val tempAchievementManager = de.egril.defender.game.AchievementManager(playerId)
        tempAchievementManager.onAchievementEarned = { achievement ->
            _showAchievementNotification.value = true
            _currentAchievement.value = achievement
        }
        
        if (oldStats.unlockedSpells.isEmpty()) {
            tempAchievementManager.onFirstSpellUnlock()
        }
    }
    
    fun startLevel(levelId: Int) {
        val worldLevel = _worldLevels.value.find { it.level.id == levelId }
        if (worldLevel != null && worldLevel.status != LevelStatus.LOCKED) {
            val difficulty = AppSettings.difficulty.value
            val level = worldLevel.level
            val playerStats = _currentPlayer.value?.stats ?: PlayerStats()
            
            // Apply difficulty modifiers to spawn plan
            val modifiedSpawnPlan = if (level.directSpawnPlan != null) {
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
            val constructionLevel = playerStats.constructionStat
            
            // Create GameState with difficulty-modified and stats-bonus values
            val newGameState = GameState(
                level = level,
                difficulty = difficulty,
                coins = mutableStateOf(totalCoins),
                healthPoints = mutableStateOf(totalHealth),
                spawnPlan = modifiedSpawnPlan,
                maxMana = mutableStateOf(maxMana),
                currentMana = mutableStateOf(maxMana),  // Start with full mana
                incomeMultiplier = incomeMultiplier,
                constructionLevel = constructionLevel
            )
            
            // Initialize pre-placed elements if any
            newGameState.initializePrePlacedElements()
            
            _gameState.value = newGameState
            gameEngine = GameEngine(newGameState)
            _currentScreen.value = Screen.GamePlay(levelId)
            // Capture initial state snapshot
            initialGameStateSnapshot = createGameStateSnapshot(newGameState)
            lastSaveSnapshot = initialGameStateSnapshot
            
            // Initialize achievement manager for this level
            val playerId = _currentPlayer.value?.id
            if (playerId != null) {
                achievementManager = de.egril.defender.game.AchievementManager(playerId).apply {
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
                    result.killedEnemyTypes.forEach { enemyType ->
                        achievementManager?.onEnemyKilled(enemyType, result.killsThisAttack)
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
    
    fun placeDefender(type: DefenderType, position: Position): Boolean {
        val result = gameEngine?.placeDefender(type, position) ?: false
        if (result) {
            // Track achievement
            val isRiverTile = _gameState.value?.level?.isRiverTile(position) ?: false
            if (isRiverTile) {
                achievementManager?.onBuildRaft()
            } else {
                achievementManager?.onBuildTower()
            }
        }
        return result
    }
    
    fun upgradeDefender(defenderId: Int): Boolean {
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
        }
        return result
    }
    
    fun undoTower(defenderId: Int): Boolean {
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
    
    fun sellTower(defenderId: Int): Boolean {
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
    
    fun startFirstPlayerTurn() {
        println("DEBUG: startFirstPlayerTurn called")
        val stateBefore = _gameState.value
        println("DEBUG: Phase before: ${stateBefore?.phase?.value}")
        println("DEBUG: Attackers before: ${stateBefore?.attackers?.size}")
        
        gameEngine?.startFirstPlayerTurn()
        
        // Track turn start for achievements
        achievementManager?.startTurn()
        gameEngine?.startTurnTracking()
        
        val stateAfter = _gameState.value
        println("DEBUG: Phase after: ${stateAfter?.phase?.value}")
        println("DEBUG: Attackers after: ${stateAfter?.attackers?.size}")
        stateAfter?.attackers?.forEach { attacker ->
            println("DEBUG: Enemy ${attacker.id} - Type: ${attacker.type}, Position: (${attacker.position.value.x}, ${attacker.position.value.y})")
        }
        
        println("DEBUG: startFirstPlayerTurn completed")
    }
    
    fun defenderAttack(defenderId: Int, targetId: Int): Boolean {
        val result = gameEngine?.defenderAttack(defenderId, targetId) ?: false
        if (result) {
            // Check for immediate victory after attack
            val state = _gameState.value
            if (state != null && state.isLevelWon()) {
                completeLevel(state.level.id, won = true)
            }
        }
        return result
    }
    
    fun defenderAttackPosition(defenderId: Int, targetPosition: Position): Boolean {
        val result = gameEngine?.defenderAttackPosition(defenderId, targetPosition) ?: false
        if (result) {
            // triggerStateUpdate()

            // Check for immediate victory after attack
            val state = _gameState.value
            if (state != null && state.isLevelWon()) {
                completeLevel(state.level.id, won = true)
            }
        }
        return result
    }
    
    fun performMineDig(mineId: Int): DigOutcome? {
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
    
    fun performMineBuildTrap(mineId: Int, trapPosition: Position): Boolean {
        return gameEngine?.performMineBuildTrap(mineId, trapPosition) ?: false
    }
    
    fun performWizardPlaceMagicalTrap(wizardId: Int, trapPosition: Position): Boolean {
        return gameEngine?.performWizardPlaceMagicalTrap(wizardId, trapPosition) ?: false
    }
    
    fun performBuildBarricade(towerId: Int, barricadePosition: Position): Boolean {
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
    
    fun performRemoveBarricade(barricadePosition: Position): Int {
        return gameEngine?.removeBarricade(barricadePosition) ?: 0
    }
    
    fun performMineDigWithOutcome(outcome: DigOutcome): DigOutcome? {
        return gameEngine?.performMineDigWithOutcome(outcome)
    }

    fun endPlayerTurn() {
        val state = _gameState.value ?: return
        val engine = gameEngine ?: return

        // NOTE: Auto-attacks are NOT triggered here when clicking "End Turn".
        // They only happen when clicking "Auto-Attack and End Turn" button (see autoAttackAndEndTurn()).
        
        // Process enemy turn with animations
        viewModelScope.launch(Dispatchers.Default) {
            // Start enemy turn: change phase to ENEMY_TURN
            // The UI immediately shows "ENEMY TURN" indicator when phase changes
            engine.startEnemyTurn()
            
            // Calculate all movement steps for existing units
            val enemyTurnMovements = engine.calculateEnemyTurnMovements()
            val movementSteps = enemyTurnMovements.allMovementSteps
            val attackersStoppedByBarricade = enemyTurnMovements.attackersStoppedByBarricade
            
            // Apply each movement step with a delay between steps
            for (stepMovements in movementSteps) {
                // Apply all movements in this step simultaneously
                for ((attackerId, newPosition) in stepMovements) {
                    engine.applyMovement(attackerId, newPosition)
                }
                // Delay between movement steps so user can see the animation (reduced from 400ms to 200ms)
                delay(200)
            }

            attackersStoppedByBarricade.forEach { it ->
                println("attackBarricade B")
                // fixit HERE
                engine.attackBarricade(it.second, it.first)
            }
            
            // Add a small delay to see final positions before spawning new units (reduced from 300ms to 150ms)
            if (movementSteps.isNotEmpty()) {
                delay(150)
            }

            // Now spawn new units (spawn points should be clear after movements)
            engine.spawnEnemyTurnAttackers()
            
            // Show spawned units briefly (reduced from 400ms to 200ms)
            delay(200)
            
            // Move newly spawned units away from spawn points
            val newSpawnMovements = engine.calculateNewlySpawnedMovements()
            for (stepMovements in newSpawnMovements) {
                for ((attackerId, newPosition) in stepMovements) {
                    engine.applyMovement(attackerId, newPosition)
                }
                // Delay between movement steps (reduced from 400ms to 200ms)
                delay(200)
            }
            
            // Add a small delay after newly spawned units have moved (reduced from 300ms to 150ms)
            if (newSpawnMovements.isNotEmpty()) {
                delay(150)
            }
            
            // Complete enemy turn: apply effects and return to player turn
            engine.completeEnemyTurn()
            
            // Autosave at the beginning of the new player turn (after enemy turn completes)
            // This ensures the phase is PLAYER_TURN when the save is created
            autoSaveGame()
            
            // Check win/loss conditions
            val updatedState = _gameState.value ?: return@launch
            if (updatedState.isLevelWon()) {
                completeLevel(updatedState.level.id, won = true)
            } else if (updatedState.isLevelLost()) {
                completeLevel(updatedState.level.id, won = false)
            }
        }
    }

    fun autoAttackAndEndTurn() {
        // This method is called from the "Auto-Attack and End Turn" button in the confirmation dialog
        // It explicitly calls autoDefenderAttacks() before ending the turn
        val engine = gameEngine ?: return
        val currentState = gameState.value ?: return
        
        // Explicitly trigger auto-attacks for all ready defenders
        engine.autoDefenderAttacks()
        
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
    
    fun clearSpecialActionsWarning() {
        _specialActionsRemaining.value = emptyList()
    }

    private fun completeLevel(levelId: Int, won: Boolean) {
        val currentHP = _gameState.value?.healthPoints?.value ?: 0
        val xpEarned = _gameState.value?.xpEarnedThisLevel?.value ?: 0
        
        // Track achievement for level completion
        if (won) {
            achievementManager?.onWinLevel(currentHP)
            
            // Award XP to player profile
            val currentPlayer = _currentPlayer.value
            if (currentPlayer != null) {
                val updatedStats = currentPlayer.stats.addXP(xpEarned)
                val updatedPlayer = currentPlayer.copy(stats = updatedStats)
                _currentPlayer.value = updatedPlayer
                de.egril.defender.save.PlayerProfileStorage.updateProfile(updatedPlayer)
            }
        } else {
            achievementManager?.onLoseLevel()
        }
        
        val isLastLevel = _worldLevels.value.lastOrNull()?.level?.id == levelId
        if (won) {
            val updatedLevels = _worldLevels.value.toMutableList()
            val currentIndex = updatedLevels.indexOfFirst { it.level.id == levelId }
            if (currentIndex >= 0) {
                updatedLevels[currentIndex] = updatedLevels[currentIndex].copy(status = LevelStatus.WON)
                
                // Get the set of all won level IDs (including the just-won level)
                val wonLevelIds = updatedLevels
                    .filter { it.status == LevelStatus.WON }
                    .mapNotNull { it.level.editorLevelId }
                    .toSet()
                
                // Unlock levels based on prerequisites
                for (i in updatedLevels.indices) {
                    val worldLevel = updatedLevels[i]
                    if (worldLevel.status == LevelStatus.LOCKED && worldLevel.level.editorLevelId != null) {
                        if (de.egril.defender.editor.EditorStorage.isLevelUnlocked(worldLevel.level.editorLevelId!!, wonLevelIds)) {
                            updatedLevels[i] = worldLevel.copy(status = LevelStatus.UNLOCKED)
                        }
                    }
                }
                
                _worldLevels.value = updatedLevels
                // Save world map status
                saveWorldMapStatus()
            }
        }
        _currentScreen.value = Screen.LevelComplete(levelId, won, isLastLevel, xpEarned)
    }
    
    fun restartLevel() {
        val levelId = (_currentScreen.value as? Screen.LevelComplete)?.levelId
            ?: (_currentScreen.value as? Screen.GamePlay)?.levelId
            ?: return
        startLevel(levelId)
    }
    
    fun applyCheatCode(code: String): Boolean {
        // Check for reminder testing cheat codes first
        val lowerCode = code.lowercase().trim()
        if (lowerCode == "breakreminder" || lowerCode == "break") {
            // Trigger break reminder with current session time
            val currentTime = de.egril.defender.utils.currentTimeMillis()
            gameSessionStartTime?.let { sessionStart ->
                val elapsedMs = currentTime - sessionStart
                val elapsedTime = formatElapsedTime(elapsedMs)
                _reminderMessage.value = ReminderMessage(
                    type = de.egril.defender.ui.gameplay.ReminderType.BREAK,
                    elapsedTime = elapsedTime
                )
            }
            return true
        }
        
        if (lowerCode == "sleepreminder" || lowerCode == "sleep") {
            // Trigger sleep reminder
            val currentTime = de.egril.defender.utils.currentTimeMillis()
            val hour = de.egril.defender.utils.getLocalHour(currentTime)
            val timeDescription = when {
                hour == 23 -> "close_to_midnight"
                hour == 0 -> "midnight"
                else -> "after_midnight"
            }
            _reminderMessage.value = ReminderMessage(
                type = de.egril.defender.ui.gameplay.ReminderType.SLEEP,
                timeDescription = timeDescription
            )
            return true
        }
        
        val (success, digOutcome) = CheatCodeHandler.applyCheatCode(
            code = code,
            addCoins = { amount -> gameEngine?.addCoins(amount) },
            setCoins = { amount -> gameEngine?.setCoins(amount) },
            performMineDigWithOutcome = { outcome -> performMineDigWithOutcome(outcome) },
            spawnEnemy = { attackerType, level -> gameEngine?.spawnEnemy(attackerType, level) },
            showPlatformInfo = { _showPlatformInfo.value = true }
        )
        
        if (digOutcome != null) {
            _cheatDigOutcome.value = digOutcome
        }
        
        return success
    }
    
    fun clearCheatDigOutcome() {
        _cheatDigOutcome.value = null
    }
    
    fun clearPlatformInfo() {
        _showPlatformInfo.value = false
    }
    
    fun applyWorldMapCheatCode(code: String): Boolean {
        // Check for "sticker" cheat code first (navigation cheat)
        if (code.lowercase().trim() == "sticker") {
            navigateToSticker()
            return true
        }
        
        return CheatCodeHandler.applyWorldMapCheatCode(
            code = code,
            unlockAllLevels = { unlockAllLevels() },
            unlockLevel = { editorLevelId -> unlockLevel(editorLevelId) },
            lockAllLevels = { lockAllLevels() },
            lockLevel = { editorLevelId -> lockLevel(editorLevelId) },
            worldLevels = _worldLevels.value,
            showPlatformInfo = { _showPlatformInfo.value = true }
        )
    }
    
    private fun unlockAllLevels() {
        _worldLevels.value = CheatCodeHandler.unlockAllLevels(_worldLevels.value)
        // Save updated world map status
        saveWorldMapStatus()
    }
    
    private fun unlockLevel(editorLevelId: String) {
        _worldLevels.value = CheatCodeHandler.unlockLevel(_worldLevels.value, editorLevelId)
        // Save updated world map status
        saveWorldMapStatus()
    }
    
    private fun lockAllLevels() {
        _worldLevels.value = CheatCodeHandler.lockAllLevels(_worldLevels.value)
        // Save updated world map status
        saveWorldMapStatus()
    }
    
    private fun lockLevel(editorLevelId: String) {
        _worldLevels.value = CheatCodeHandler.lockLevel(_worldLevels.value, editorLevelId)
        // Save updated world map status
        saveWorldMapStatus()
    }
    
    // Save/Load functionality
    
    private val _savedGames = MutableStateFlow<List<de.egril.defender.save.SaveGameMetadata>>(emptyList())
    val savedGames: StateFlow<List<de.egril.defender.save.SaveGameMetadata>> = _savedGames.asStateFlow()
    
    fun navigateToLoadGame() {
        refreshSavedGames()
        _currentScreen.value = Screen.LoadGame
    }
    
    fun saveCurrentGame(comment: String? = null): String? {
        val state = _gameState.value ?: return null
        val saveId = de.egril.defender.save.SaveFileStorage.saveGameState(state, comment)
        refreshSavedGames()
        // Update the last save snapshot
        lastSaveSnapshot = createGameStateSnapshot(state)
        return saveId
    }
    
    /**
     * Create an autosave at the beginning of a new turn.
     * Autosaves always use the fixed ID "autosave_game" so they overwrite previous autosaves.
     */
    private fun autoSaveGame() {
        val state = _gameState.value ?: return
        // Use fixed ID "autosave_game" and add "Autosave" as comment
        de.egril.defender.save.SaveFileStorage.saveGameState(state, comment = "Autosave", saveId = "autosave_game")
        refreshSavedGames()
        // Don't update lastSaveSnapshot for autosaves - we still want to track manual saves separately
    }
    
    /**
     * Check if an autosave exists
     */
    fun hasAutosave(): Boolean {
        return de.egril.defender.save.SaveFileStorage.loadGameState("autosave_game") != null
    }
    
    /**
     * Load the autosave and start playing
     */
    fun continueFromAutosave() {
        loadGame("autosave_game")
    }
    
    /**
     * Create a snapshot of the current game state for change detection
     */
    private fun createGameStateSnapshot(state: GameState): String {
        // Create a simple hash/snapshot of key game state properties
        return buildString {
            append("turn:${state.turnNumber.value}")
            append("|coins:${state.coins.value}")
            append("|health:${state.healthPoints.value}")
            append("|phase:${state.phase.value}")
            append("|defenders:${state.defenders.size}")
            state.defenders.sortedBy { it.id }.forEach { defender ->
                append("|d${defender.id}:${defender.type},${defender.position.value.x},${defender.position.value.y},${defender.level.value},${defender.buildTimeRemaining.value}")
            }
            append("|attackers:${state.attackers.size}")
            state.attackers.sortedBy { it.id }.forEach { attacker ->
                append("|a${attacker.id}:${attacker.type},${attacker.position.value.x},${attacker.position.value.y},${attacker.currentHealth.value},${attacker.isDefeated.value}")
            }
            append("|effects:${state.fieldEffects.size}")
            append("|traps:${state.traps.size}")
        }
    }
    
    /**
     * Check if there are unsaved changes
     */
    fun hasUnsavedChanges(): Boolean {
        val currentState = _gameState.value ?: return false
        val currentSnapshot = createGameStateSnapshot(currentState)
        val referenceSnapshot = lastSaveSnapshot ?: initialGameStateSnapshot ?: return false
        return currentSnapshot != referenceSnapshot
    }
    
    fun loadGame(saveId: String) {
        val savedGame = de.egril.defender.save.SaveFileStorage.loadGameState(saveId) ?: return
        
        // Find the level by ID
        val level = _worldLevels.value.find { it.level.id == savedGame.levelId }?.level
        
        // Verify map ID matches if both are available
        if (level != null && savedGame.mapId != null && level.mapId != null) {
            if (savedGame.mapId != level.mapId) {
                // Map mismatch - the level at this numeric ID now uses a different map
                println("WARNING: Save file has different map ID (saved: ${savedGame.mapId}, current: ${level.mapId})")
                println("Level sequence may have changed. Attempting to find level with matching map ID...")
                
                // Try to find any level that uses the same map as the saved game
                val levelWithCorrectMap = _worldLevels.value
                    .map { it.level }
                    .find { it.mapId == savedGame.mapId }
                
                if (levelWithCorrectMap != null) {
                    println("Found level with matching map ID: ${levelWithCorrectMap.name} (ID: ${levelWithCorrectMap.id})")
                    val gameState = de.egril.defender.save.SaveFileStorage.convertSavedGameToGameState(savedGame, levelWithCorrectMap)
                    _gameState.value = gameState
                    gameEngine = GameEngine(gameState)
                    _currentScreen.value = Screen.GamePlay(levelWithCorrectMap.id)
                    // Set snapshots when loading a saved game
                    initialGameStateSnapshot = createGameStateSnapshot(gameState)
                    lastSaveSnapshot = initialGameStateSnapshot
                    
                    // Start time tracking for reminders
                    startTimeTracking()
                    return
                } else {
                    println("ERROR: Could not find any level with map ID ${savedGame.mapId}. Save file may be incompatible.")
                    // TODO: Show error dialog to user
                    return
                }
            }
        }
        
        // No map ID mismatch or one/both map IDs are null (backward compatibility)
        if (level != null) {
            val gameState = de.egril.defender.save.SaveFileStorage.convertSavedGameToGameState(savedGame, level)
            _gameState.value = gameState
            gameEngine = GameEngine(gameState)
            _currentScreen.value = Screen.GamePlay(savedGame.levelId)
            // Set snapshots when loading a saved game
            initialGameStateSnapshot = createGameStateSnapshot(gameState)
            lastSaveSnapshot = initialGameStateSnapshot
            
            // Start time tracking for reminders
            startTimeTracking()
        }
    }
    
    /**
     * Import world map progress and check for conflicts
     * Returns true if conflict detected and shown to user
     */
    fun importWorldMapProgress(json: String): Boolean {
        val importedWorldMap = de.egril.defender.save.SaveFileStorage.importWorldMapProgress(json)
        
        if (importedWorldMap != null) {
            // Conflict detected - show dialog
            val currentWorldMap = de.egril.defender.save.SaveFileStorage.loadWorldMapStatus() ?: emptyMap()
            _worldMapConflict.value = WorldMapConflict(
                savedGame = null,  // No associated save game
                savedWorldMap = importedWorldMap,
                currentWorldMap = currentWorldMap,
                level = null  // No level to load
            )
            return true
        }
        
        return false  // No conflict, nothing to do
    }
    
    /**
     * Resolve world map conflict by choosing which version to keep
     */
    fun resolveWorldMapConflict(useSavedVersion: Boolean) {
        val conflict = _worldMapConflict.value ?: return
        
        if (useSavedVersion) {
            // Use the world map from the import/save
            val updatedWorldLevels = de.egril.defender.save.SaveFileStorage.applyWorldMapProgress(
                conflict.savedWorldMap,
                _worldLevels.value
            )
            _worldLevels.value = updatedWorldLevels
        }
        // If not using saved version, keep current world map (do nothing)
        
        // Clear the conflict
        _worldMapConflict.value = null
        
        // If there's an associated saved game and level, load it
        if (conflict.savedGame != null && conflict.level != null) {
            val gameState = de.egril.defender.save.SaveFileStorage.convertSavedGameToGameState(conflict.savedGame, conflict.level)
            _gameState.value = gameState
            gameEngine = GameEngine(gameState)
            _currentScreen.value = Screen.GamePlay(conflict.level.id)
            // Set snapshots when loading a saved game
            initialGameStateSnapshot = createGameStateSnapshot(gameState)
            lastSaveSnapshot = initialGameStateSnapshot
        }
    }
    
    /**
     * Cancel world map conflict resolution
     */
    fun cancelWorldMapConflict() {
        _worldMapConflict.value = null
    }
    
    fun deleteSavedGame(saveId: String) {
        de.egril.defender.save.SaveFileStorage.deleteSavedGame(saveId)
        refreshSavedGames()
    }
    
    // Download/Upload functionality
    
    fun downloadSaveGame(saveId: String, includeGameState: Boolean = false) {
        viewModelScope.launch {
            val jsonContent = if (includeGameState) {
                de.egril.defender.save.SaveFileStorage.getSaveGameWithWorldMapJson(saveId)
            } else {
                de.egril.defender.save.SaveFileStorage.getSaveGameJson(saveId)
            }
            
            if (jsonContent != null) {
                val fileExportImport = de.egril.defender.save.getFileExportImport()
                val filename = if (includeGameState) {
                    "${saveId}_with_progress.json"
                } else {
                    "$saveId.json"
                }
                fileExportImport.exportFile(filename, jsonContent)
            }
        }
    }
    
    fun downloadAllSaveGames(includeGameState: Boolean = false) {
        viewModelScope.launch {
            val allSaves = if (includeGameState) {
                // Get all saves with world map included
                de.egril.defender.save.SaveFileStorage.getAllSaveGamesJson().mapValues { (filename, _) ->
                    val saveId = filename.removeSuffix(".json")
                    de.egril.defender.save.SaveFileStorage.getSaveGameWithWorldMapJson(saveId) ?: ""
                }.filter { it.value.isNotEmpty() }
            } else {
                de.egril.defender.save.SaveFileStorage.getAllSaveGamesJson()
            }
            
            if (allSaves.isNotEmpty()) {
                val timestamp = de.egril.defender.utils.formatTimestampISO(de.egril.defender.utils.currentTimeMillis())
                val zipFilename = if (includeGameState) {
                    "defender-of-egril-saves-with-progress-$timestamp.zip"
                } else {
                    "defender-of-egril-saves-$timestamp.zip"
                }
                val fileExportImport = de.egril.defender.save.getFileExportImport()
                fileExportImport.exportZip(zipFilename, allSaves)
            }
        }
    }
    
    fun downloadGameState() {
        viewModelScope.launch {
            val jsonContent = de.egril.defender.save.SaveFileStorage.exportWorldMapProgress()
            val fileExportImport = de.egril.defender.save.getFileExportImport()
            val timestamp = de.egril.defender.utils.formatTimestampISO(de.egril.defender.utils.currentTimeMillis())
            fileExportImport.exportFile("game-progress-$timestamp.json", jsonContent)
        }
    }
    
    /**
     * Upload save files and handle override conflicts
     * Returns a state flow with import results: (success count, conflicts)
     */
    suspend fun uploadSaveGames(): Pair<Int, List<String>> {
        val fileExportImport = de.egril.defender.save.getFileExportImport()
        val importedFiles = fileExportImport.importFiles() ?: return Pair(0, emptyList())
        
        var successCount = 0
        val conflicts = mutableListOf<String>()
        
        importedFiles.forEach { file ->
            if (de.egril.defender.save.SaveFileStorage.saveGameExists(file.filename)) {
                conflicts.add(file.filename)
            } else {
                if (de.egril.defender.save.SaveFileStorage.importSaveGame(file.filename, file.content, overwrite = false)) {
                    successCount++
                }
            }
        }
        
        refreshSavedGames()
        return Pair(successCount, conflicts)
    }
    
    /**
     * Import a specific file with override option
     */
    suspend fun importSaveGameWithOverride(filename: String, content: String, overwrite: Boolean): Boolean {
        val success = de.egril.defender.save.SaveFileStorage.importSaveGame(filename, content, overwrite)
        if (success) {
            refreshSavedGames()
        }
        return success
    }
    
    private fun refreshSavedGames() {
        _savedGames.value = de.egril.defender.save.SaveFileStorage.getAllSavedGames()
    }
    
    private fun saveWorldMapStatus() {
        de.egril.defender.save.SaveFileStorage.saveWorldMapStatus(_worldLevels.value)
    }
    
    // Player Profile Management
    
    /**
     * Create a new player profile
     * @return true if successful, false if name is invalid or already exists
     */
    fun createPlayer(name: String): Boolean {
        val profile = de.egril.defender.save.PlayerProfileStorage.createProfile(name)
        if (profile != null) {
            _currentPlayer.value = profile
            _allPlayers.value = de.egril.defender.save.PlayerProfileStorage.getAllProfiles().profiles
            de.egril.defender.save.SaveFileStorage.setCurrentPlayer(profile.id)
            _needsPlayerSelection.value = false
            
            // Reload world map for new player
            initializeWorldMap()
            
            return true
        }
        return false
    }
    
    /**
     * Switch to a different player profile
     */
    fun switchPlayer(playerId: String) {
        val profile = de.egril.defender.save.PlayerProfileStorage.getProfile(playerId)
        if (profile != null) {
            _currentPlayer.value = profile
            de.egril.defender.save.SaveFileStorage.setCurrentPlayer(playerId)
            de.egril.defender.save.PlayerProfileStorage.updateLastPlayed(playerId)
            
            // Reload world map for new player
            initializeWorldMap()
            
            // Reload saved games list
            refreshSavedGames()
        }
    }
    
    /**
     * Delete a player profile
     */
    fun deletePlayer(playerId: String): Boolean {
        // Don't allow deleting the current player
        if (_currentPlayer.value?.id == playerId) {
            return false
        }
        
        val success = de.egril.defender.save.PlayerProfileStorage.deleteProfile(playerId)
        if (success) {
            _allPlayers.value = de.egril.defender.save.PlayerProfileStorage.getAllProfiles().profiles
        }
        return success
    }
    
    /**
     * Rename the current player profile
     * @return true if successful, false if name is invalid or already exists
     */
    fun renameCurrentPlayer(newName: String): Boolean {
        val currentPlayerId = _currentPlayer.value?.id ?: return false
        val updatedProfile = de.egril.defender.save.PlayerProfileStorage.renameProfile(currentPlayerId, newName)
        if (updatedProfile != null) {
            _currentPlayer.value = updatedProfile
            _allPlayers.value = de.egril.defender.save.PlayerProfileStorage.getAllProfiles().profiles
            
            // Update SaveFileStorage if the ID changed
            if (updatedProfile.id != currentPlayerId) {
                de.egril.defender.save.SaveFileStorage.setCurrentPlayer(updatedProfile.id)
            }
            
            return true
        }
        return false
    }
    
    /**
     * Refresh the list of all player profiles
     */
    fun refreshPlayerProfiles() {
        _allPlayers.value = de.egril.defender.save.PlayerProfileStorage.getAllProfiles().profiles
    }
    
    /**
     * Refresh the current player profile (e.g., after earning an achievement)
     */
    private fun refreshCurrentPlayer() {
        val currentPlayerId = _currentPlayer.value?.id ?: return
        val updatedProfile = de.egril.defender.save.PlayerProfileStorage.getProfile(currentPlayerId)
        if (updatedProfile != null) {
            _currentPlayer.value = updatedProfile
        }
    }
    
    /**
     * Clear the new achievement notification
     */
    fun clearAchievementNotification() {
        _newAchievement.value = null
    }
    
    /**
     * Start tracking time for reminders when a game is started
     */
    fun startTimeTracking() {
        val currentTime = de.egril.defender.utils.currentTimeMillis()
        gameSessionStartTime = currentTime
        lastBreakReminderTime = currentTime
        lastSleepReminderTime = currentTime
        
        // Start coroutine to check for reminders
        viewModelScope.launch {
            checkTimeReminders()
        }
    }
    
    /**
     * Stop tracking time when leaving the game screen
     */
    fun stopTimeTracking() {
        gameSessionStartTime = null
        lastBreakReminderTime = null
        lastSleepReminderTime = null
    }
    
    /**
     * Check and show time reminders periodically
     */
    private suspend fun checkTimeReminders() {
        while (gameSessionStartTime != null) {
            delay(60000L)  // Check every minute
            
            val currentTime = de.egril.defender.utils.currentTimeMillis()
            
            // Check break reminder (every 2 hours)
            lastBreakReminderTime?.let { lastBreak ->
                if (currentTime - lastBreak >= BREAK_REMINDER_INTERVAL_MS) {
                    gameSessionStartTime?.let { sessionStart ->
                        val elapsedMs = currentTime - sessionStart
                        val elapsedTime = formatElapsedTime(elapsedMs)
                        _reminderMessage.value = ReminderMessage(
                            type = de.egril.defender.ui.gameplay.ReminderType.BREAK,
                            elapsedTime = elapsedTime
                        )
                        lastBreakReminderTime = currentTime
                    }
                }
            }
            
            // Check sleep reminder (after 23:00, every hour)
            lastSleepReminderTime?.let { lastSleep ->
                if (currentTime - lastSleep >= SLEEP_REMINDER_INTERVAL_MS) {
                    val hour = getLocalHour(currentTime)
                    if (hour >= SLEEP_START_HOUR || hour < 6) {  // Between 23:00 and 06:00
                        val timeDescription = when {
                            hour == 23 -> "close_to_midnight"
                            hour == 0 -> "midnight"
                            else -> "after_midnight"
                        }
                        _reminderMessage.value = ReminderMessage(
                            type = de.egril.defender.ui.gameplay.ReminderType.SLEEP,
                            timeDescription = timeDescription
                        )
                        lastSleepReminderTime = currentTime
                    }
                }
            }
        }
    }
    
    /**
     * Clear the current reminder message
     */
    fun clearReminderMessage() {
        _reminderMessage.value = null
    }
    
    /**
     * Format elapsed time as "X hours Y minutes"
     */
    private fun formatElapsedTime(elapsedMs: Long): String {
        val hours = elapsedMs / (60 * 60 * 1000)
        val minutes = (elapsedMs % (60 * 60 * 1000)) / (60 * 1000)
        
        return buildString {
            if (hours > 0) {
                append("$hours ")
                append(if (hours == 1L) "hour" else "hours")
            }
            if (minutes > 0) {
                if (hours > 0) append(" ")
                append("$minutes ")
                append(if (minutes == 1L) "minute" else "minutes")
            }
        }
    }
    
    /**
     * Get the local hour (0-23) from timestamp
     */
    private fun getLocalHour(timestamp: Long): Int {
        return de.egril.defender.utils.getLocalHour(timestamp)
    }
}
