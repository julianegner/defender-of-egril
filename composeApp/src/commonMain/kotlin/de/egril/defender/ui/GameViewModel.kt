package de.egril.defender.ui

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import de.egril.defender.game.GameEngine
import de.egril.defender.game.GameEngine.EnemyTurnMovements
import de.egril.defender.game.LevelData
import de.egril.defender.model.*
import de.egril.defender.model.DifficultyModifiers
import de.egril.defender.ui.settings.AppSettings
import com.hyperether.resources.LocalizedStrings
import com.hyperether.resources.currentLanguage
import de.egril.defender.utils.CheatCodeHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import de.egril.defender.config.LogConfig

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
    
    private val _showCheatHelp = MutableStateFlow(false)
    val showCheatHelp: StateFlow<Boolean> = _showCheatHelp.asStateFlow()

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
    
    // Magic panel state
    private val _showMagicPanel = MutableStateFlow(false)
    val showMagicPanel: StateFlow<Boolean> = _showMagicPanel.asStateFlow()

    private val _selectedSpell = MutableStateFlow<SpellType?>(null)
    val selectedSpell: StateFlow<SpellType?> = _selectedSpell.asStateFlow()

    private val _pendingSpellCast = MutableStateFlow<SpellType?>(null)
    val pendingSpellCast: StateFlow<SpellType?> = _pendingSpellCast.asStateFlow()

    // Post-target confirmation dialog state
    private val _showSpellTargetConfirmation = MutableStateFlow<Pair<SpellType, Any>?>(null)
    val showSpellTargetConfirmation: StateFlow<Pair<SpellType, Any>?> = _showSpellTargetConfirmation.asStateFlow()

    private val _showFreezeImmuneWarning = MutableStateFlow<Attacker?>(null)
    val showFreezeImmuneWarning: StateFlow<Attacker?> = _showFreezeImmuneWarning.asStateFlow()

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
                if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                println("Migrated existing saves to player profile: ${migratedProfile.name}")
                }
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

                // Reload the player profile after updateLastPlayed to ensure we have the latest data
                val reloadedProfile = de.egril.defender.save.PlayerProfileStorage.getProfile(playerToUse.id)
                if (reloadedProfile != null) {
                    _currentPlayer.value = reloadedProfile
                }
            } else {
                // Shouldn't happen, but handle gracefully
                _needsPlayerSelection.value = true
            }
        }
    }
    
    private fun initializeWorldMap() {
        // Load official levels
        val officialLevels = LevelData.createLevels()
        if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
        println("DEBUG: Total official levels loaded: ${officialLevels.size}")
        }

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
        if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
        println("DEBUG: Total user levels loaded: ${userLevels.size}")
        }

        // Combine official and user levels
        val allLevels = officialLevels + userLevels
        
        // Load saved world map status
        val savedStatuses = de.egril.defender.save.SaveFileStorage.loadWorldMapStatus()
        
        // Get the set of won level IDs
        val wonLevelIds = savedStatuses?.filter { it.value == LevelStatus.WON }?.keys?.toSet() ?: emptySet()
        
        _worldLevels.value = allLevels.mapIndexed { index, level ->
            if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                println("DEBUG: Loaded Level ${level.id} - Name: ${level.name} - Path Cells: ${level.pathCells.size}")
            }
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
                if (LogConfig.ENABLE_UI_LOGGING) {
                println("WARNING: Level ${level.id} (${level.name}) has no editorLevelId - using fallback status")
                }
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
        if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
        println("Reloading world map from disk...")
        }
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

    fun upgradeAbility(statType: de.egril.defender.model.AbilityType) {
        val currentPlayer = _currentPlayer.value ?: return
        val oldStats = currentPlayer.abilities
        val updatedStats = oldStats.spendAbilityPoint(statType) ?: return
        val updatedPlayer = currentPlayer.copy(abilities = updatedStats)
        _currentPlayer.value = updatedPlayer
        de.egril.defender.save.PlayerProfileStorage.updateProfile(updatedPlayer)

        // Check for achievements
        val playerId = currentPlayer.id
        val tempAchievementManager = de.egril.defender.game.AchievementManager(playerId)
        tempAchievementManager.onAchievementEarned = { achievement ->
            _newAchievement.value = achievement
        }

        // First stat upgrade achievement
        val totalSpentBefore = oldStats.healthAbility + oldStats.treasuryAbility + oldStats.incomeAbility +
                               oldStats.constructionAbility + oldStats.manaAbility
        if (totalSpentBefore == 0) {
            tempAchievementManager.onFirstStatUpgrade()
        }

        // Construction level 3 achievement
        if (statType == de.egril.defender.model.AbilityType.CONSTRUCTION && updatedStats.constructionAbility >= 3) {
            tempAchievementManager.onConstructionLevel3()
        }

        // Player level achievements
        val playerLevel = de.egril.defender.model.PlayerAbilities.calculateLevel(updatedStats.totalXP)
        if (playerLevel >= 10) {
            tempAchievementManager.onPlayerLevel10()
        }
        if (playerLevel >= 100) {
            tempAchievementManager.onPlayerLevel100()
        }
    }

    fun unlockSpell(spell: de.egril.defender.model.SpellType) {
        val currentPlayer = _currentPlayer.value ?: return
        val oldStats = currentPlayer.abilities
        val updatedStats = oldStats.unlockSpell(spell) ?: return
        val updatedPlayer = currentPlayer.copy(abilities = updatedStats)
        _currentPlayer.value = updatedPlayer
        de.egril.defender.save.PlayerProfileStorage.updateProfile(updatedPlayer)

        // Check for first spell unlock achievement
        val playerId = currentPlayer.id
        val tempAchievementManager = de.egril.defender.game.AchievementManager(playerId)
        tempAchievementManager.onAchievementEarned = { achievement ->
            _newAchievement.value = achievement
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
            val playerStats = _currentPlayer.value?.abilities ?: PlayerAbilities()

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
            val constructionLevel = playerStats.constructionAbility

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
    
    /**
     * Generate mana using a wizard tower
     * - Costs 1 action
     * - Generates base 5 mana + (wizard level / 5) bonus mana
     * - Cannot exceed max mana
     */
    fun generateMana(wizardId: Int): Boolean {
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

    fun startFirstPlayerTurn() {
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
            println("DEBUG: Enemy ${attacker.id} - Type: ${attacker.type}, Position: (${attacker.position.value.x}, ${attacker.position.value.y})")
            }
        }
        
        if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
        println("DEBUG: startFirstPlayerTurn completed")
        }
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
    
    /**
     * Perform wizard mana generation
     * Called when player clicks the "Generate Mana" button on a wizard tower
     * Returns true if mana was generated successfully
     */
    fun performWizardGenerateMana(wizardId: Int): Boolean {
        return gameEngine?.performWizardGenerateMana(wizardId) ?: false
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
                val updatedStats = currentPlayer.abilities.addXP(xpEarned)
                val updatedPlayer = currentPlayer.copy(abilities = updatedStats)
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
                        if (de.egril.defender.editor.EditorStorage.isLevelUnlocked(worldLevel.level.editorLevelId, wonLevelIds)) {
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
            showPlatformInfo = { _showPlatformInfo.value = true },
            setBigHeadMode = { enabled -> de.egril.defender.utils.BigHeadMode.isEnabled.value = enabled },
            addMana = { amount -> gameEngine?.addMana(amount) },
            removeMana = { amount -> gameEngine?.removeMana(amount) },
            showCheatHelp = { _showCheatHelp.value = true }
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
    
    fun clearCheatHelp() {
        _showCheatHelp.value = false
    }

    // Player stat/XP/spell cheat methods
    private fun addPlayerXP(amount: Int) {
        val currentPlayer = _currentPlayer.value ?: return
        val updatedStats = currentPlayer.abilities.addXP(amount)
        val updatedPlayer = currentPlayer.copy(abilities = updatedStats)
        _currentPlayer.value = updatedPlayer
        de.egril.defender.save.PlayerProfileStorage.updateProfile(updatedPlayer)
    }

    private fun removePlayerXP(amount: Int) {
        val currentPlayer = _currentPlayer.value ?: return
        val currentXP = currentPlayer.abilities.totalXP
        val newXP = maxOf(0, currentXP - amount)
        val updatedStats = currentPlayer.abilities.copy(totalXP = newXP)
        val updatedPlayer = currentPlayer.copy(abilities = updatedStats)
        _currentPlayer.value = updatedPlayer
        de.egril.defender.save.PlayerProfileStorage.updateProfile(updatedPlayer)
    }

    private fun addPlayerStat(statName: String, amount: Int) {
        val currentPlayer = _currentPlayer.value ?: return
        val oldStats = currentPlayer.abilities

        val updatedStats = when (statName.lowercase()) {
            "health" -> oldStats.copy(healthAbility = oldStats.healthAbility + amount)
            "treasury" -> oldStats.copy(treasuryAbility = oldStats.treasuryAbility + amount)
            "income" -> oldStats.copy(incomeAbility = oldStats.incomeAbility + amount)
            "construction" -> oldStats.copy(constructionAbility = oldStats.constructionAbility + amount)
            "mana" -> oldStats.copy(manaAbility = oldStats.manaAbility + amount)
            else -> return  // Invalid stat name
        }

        val updatedPlayer = currentPlayer.copy(abilities = updatedStats)
        _currentPlayer.value = updatedPlayer
        de.egril.defender.save.PlayerProfileStorage.updateProfile(updatedPlayer)
    }

    private fun removePlayerStat(statName: String, amount: Int) {
        val currentPlayer = _currentPlayer.value ?: return
        val oldStats = currentPlayer.abilities

        val updatedStats = when (statName.lowercase()) {
            "health" -> oldStats.copy(healthAbility = maxOf(0, oldStats.healthAbility - amount))
            "treasury" -> oldStats.copy(treasuryAbility = maxOf(0, oldStats.treasuryAbility - amount))
            "income" -> oldStats.copy(incomeAbility = maxOf(0, oldStats.incomeAbility - amount))
            "construction" -> oldStats.copy(constructionAbility = maxOf(0, oldStats.constructionAbility - amount))
            "mana" -> oldStats.copy(manaAbility = maxOf(0, oldStats.manaAbility - amount))
            else -> return  // Invalid stat name
        }

        val updatedPlayer = currentPlayer.copy(abilities = updatedStats)
        _currentPlayer.value = updatedPlayer
        de.egril.defender.save.PlayerProfileStorage.updateProfile(updatedPlayer)
    }

    private fun unlockPlayerSpell(spellName: String) {
        val currentPlayer = _currentPlayer.value ?: return
        val oldStats = currentPlayer.abilities

        // Parse spell name to SpellType
        val spell = when (spellName.lowercase().replace(" ", "_")) {
            "attack_aimed", "attackaimed" -> de.egril.defender.model.SpellType.ATTACK_AIMED
            "attack_area", "attackarea", "fireball" -> de.egril.defender.model.SpellType.ATTACK_AREA
            "heal" -> de.egril.defender.model.SpellType.HEAL
            "instant_tower", "instanttower" -> de.egril.defender.model.SpellType.INSTANT_TOWER
            "bomb" -> de.egril.defender.model.SpellType.BOMB
            "double_level", "doublelevel", "double_tower_level", "doubletowerlevel" -> de.egril.defender.model.SpellType.DOUBLE_TOWER_LEVEL
            "cooling", "cooling_spell", "coolingspell" -> de.egril.defender.model.SpellType.COOLING_SPELL
            "freeze", "freeze_spell", "freezespell" -> de.egril.defender.model.SpellType.FREEZE_SPELL
            "double_reach", "doublereach", "double_tower_reach", "doubletowerreach" -> de.egril.defender.model.SpellType.DOUBLE_TOWER_REACH
            else -> return  // Invalid spell name
        }

        val updatedStats = oldStats.copy(unlockedSpells = oldStats.unlockedSpells + spell)
        val updatedPlayer = currentPlayer.copy(abilities = updatedStats)
        _currentPlayer.value = updatedPlayer
        de.egril.defender.save.PlayerProfileStorage.updateProfile(updatedPlayer)
    }

    private fun lockPlayerSpell(spellName: String) {
        val currentPlayer = _currentPlayer.value ?: return
        val oldStats = currentPlayer.abilities

        // Parse spell name to SpellType
        val spell = when (spellName.lowercase().replace(" ", "_")) {
            "attack_aimed", "attackaimed" -> de.egril.defender.model.SpellType.ATTACK_AIMED
            "attack_area", "attackarea", "fireball" -> de.egril.defender.model.SpellType.ATTACK_AREA
            "heal" -> de.egril.defender.model.SpellType.HEAL
            "instant_tower", "instanttower" -> de.egril.defender.model.SpellType.INSTANT_TOWER
            "bomb" -> de.egril.defender.model.SpellType.BOMB
            "double_level", "doublelevel", "double_tower_level", "doubletowerlevel" -> de.egril.defender.model.SpellType.DOUBLE_TOWER_LEVEL
            "cooling", "cooling_spell", "coolingspell" -> de.egril.defender.model.SpellType.COOLING_SPELL
            "freeze", "freeze_spell", "freezespell" -> de.egril.defender.model.SpellType.FREEZE_SPELL
            "double_reach", "doublereach", "double_tower_reach", "doubletowerreach" -> de.egril.defender.model.SpellType.DOUBLE_TOWER_REACH
            else -> return  // Invalid spell name
        }

        val updatedStats = oldStats.copy(unlockedSpells = oldStats.unlockedSpells - spell)
        val updatedPlayer = currentPlayer.copy(abilities = updatedStats)
        _currentPlayer.value = updatedPlayer
        de.egril.defender.save.PlayerProfileStorage.updateProfile(updatedPlayer)
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
            showPlatformInfo = { _showPlatformInfo.value = true },
            addXP = { amount -> addPlayerXP(amount) },
            removeXP = { amount -> removePlayerXP(amount) },
            addStatLevel = { statName, amount -> addPlayerStat(statName, amount) },
            removeStatLevel = { statName, amount -> removePlayerStat(statName, amount) },
            unlockSpell = { spellName -> unlockPlayerSpell(spellName) },
            lockSpell = { spellName -> lockPlayerSpell(spellName) },
            showCheatHelp = { _showCheatHelp.value = true }
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
                if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                println("WARNING: Save file has different map ID (saved: ${savedGame.mapId}, current: ${level.mapId})")
                }
                if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                println("Level sequence may have changed. Attempting to find level with matching map ID...")
                }

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
                    if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                    println("ERROR: Could not find any level with map ID ${savedGame.mapId}. Save file may be incompatible.")
                    }
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
        val locale = currentLanguage.value

        return buildString {
            if (hours > 0) {
                val key = if (hours == 1L) "hour" else "hours"
                append(LocalizedStrings.get(key, locale).replace("%d", hours.toString()))
            }
            if (minutes > 0) {
                if (hours > 0) append(" ")
                val key = if (minutes == 1L) "minute" else "minutes"
                append(LocalizedStrings.get(key, locale).replace("%d", minutes.toString()))
            }
        }
    }
    
    /**
     * Get the local hour (0-23) from timestamp
     */
    private fun getLocalHour(timestamp: Long): Int {
        return de.egril.defender.utils.getLocalHour(timestamp)
    }

    /**
     * Toggle magic panel display
     */
    fun toggleMagicPanel() {
        _showMagicPanel.value = !_showMagicPanel.value
        if (LogConfig.ENABLE_SPELL_LOGGING) {
            println("=== SPELL: Magic panel toggled - now ${if (_showMagicPanel.value) "OPEN" else "CLOSED"}")
        }
    }

    /**
     * Open magic panel
     */
    fun openMagicPanel() {
        _showMagicPanel.value = true
        if (LogConfig.ENABLE_SPELL_LOGGING) {
            val gameState = _gameState.value
            println("=== SPELL: Magic panel OPENED - Current mana: ${gameState?.currentMana?.value}/${gameState?.maxMana?.value}")
            val playerStats = _currentPlayer.value?.abilities
            val unlockedSpells = playerStats?.unlockedSpells?.size ?: 0
            println("=== SPELL: Player has $unlockedSpells unlocked spell(s)")
        }
    }

    /**
     * Close magic panel
     */
    fun closeMagicPanel() {
        _showMagicPanel.value = false
        _pendingSpellCast.value = null
        if (LogConfig.ENABLE_SPELL_LOGGING) {
            println("=== SPELL: Magic panel CLOSED")
        }
    }

    /**
     * Toggle spell selection (select or deselect)
     * No confirmation dialog - enters targeting mode directly
     */
    fun setPendingSpell(spell: SpellType) {
        val gameState = _gameState.value
        if (gameState != null && gameState.currentMana.value >= spell.manaCost) {
            // Toggle selection: if same spell clicked again, deselect it
            if (_selectedSpell.value == spell) {
                _selectedSpell.value = null
                _pendingSpellCast.value = null
                exitSpellTargetingMode()
                if (LogConfig.ENABLE_SPELL_LOGGING) {
                    println("=== SPELL: Spell deselected - ${spell.displayName}")
                }
            } else {
                _selectedSpell.value = spell
                _pendingSpellCast.value = spell
                if (LogConfig.ENABLE_SPELL_LOGGING) {
                    println("=== SPELL: Spell selected - ${spell.displayName} (Cost: ${spell.manaCost} mana)")
                }
                // For targeting spells, enter targeting mode immediately
                if (spell.requiresTarget) {
                    enterSpellTargetingMode(spell)
                    if (LogConfig.ENABLE_SPELL_LOGGING) {
                        println("=== SPELL: Entered targeting mode for ${spell.displayName}")
                    }
                } else {
                    // For non-targeting spells, show confirmation with spell details
                    _showSpellTargetConfirmation.value = Pair(spell, Unit)
                }
            }
        } else {
            if (LogConfig.ENABLE_SPELL_LOGGING) {
                println("=== SPELL: Cannot cast ${spell.displayName} - Insufficient mana (Need: ${spell.manaCost}, Have: ${gameState?.currentMana?.value ?: 0})")
            }
        }
    }

    /**
     * Cancel pending spell cast
     */
    fun cancelPendingSpell() {
        if (LogConfig.ENABLE_SPELL_LOGGING) {
            val spell = _pendingSpellCast.value
            if (spell != null) {
                println("=== SPELL: Spell cast CANCELLED - ${spell.displayName}")
            }
        }
        _pendingSpellCast.value = null
        _selectedSpell.value = null
    }

    /**
     * Handle target selection for spell (called when player clicks a target)
     * Shows confirmation or warning dialog based on target validity
     */
    fun onSpellTargetSelected(target: Any) {
        val spell = _pendingSpellCast.value ?: return

        // Check for freeze immunity
        if (spell == SpellType.FREEZE_SPELL && target is Attacker) {
            if (isImmuneToFreeze(target.type)) {
                _showFreezeImmuneWarning.value = target
                if (LogConfig.ENABLE_SPELL_LOGGING) {
                    println("=== SPELL: ${target.type.displayName} is immune to Freeze!")
                }
                return
            }
        }

        // Show confirmation dialog with target details
        _showSpellTargetConfirmation.value = Pair(spell, target)
        if (LogConfig.ENABLE_SPELL_LOGGING) {
            println("=== SPELL: Target selected for ${spell.displayName}")
        }
    }

    /**
     * Confirm spell cast after target selection
     */
    fun confirmSpellCast() {
        val confirmation = _showSpellTargetConfirmation.value
        if (confirmation != null) {
            val (spell, target) = confirmation
            castSpell(spell, target)
            _showSpellTargetConfirmation.value = null
            _selectedSpell.value = null
        }
    }

    /**
     * Dismiss spell target confirmation dialog
     */
    fun dismissSpellConfirmation() {
        _showSpellTargetConfirmation.value = null
        // Keep spell selected and targeting mode active
    }

    /**
     * Dismiss freeze immune warning
     */
    fun dismissFreezeImmuneWarning() {
        _showFreezeImmuneWarning.value = null
        // Keep spell selected and targeting mode active
    }

    /**
     * Check if an enemy type is immune to freeze spell
     */
    private fun isImmuneToFreeze(type: AttackerType): Boolean {
        return type == AttackerType.BLUE_DEMON ||
               type == AttackerType.RED_DEMON ||
               type == AttackerType.DRAGON ||
               type == AttackerType.EWHAD
    }

    /**
     * Cast a spell (called after targeting is complete)
     * For non-targeting spells, this is called immediately after confirmation
     */
    fun castSpell(spell: SpellType, target: Any? = null) {
        val gameState = _gameState.value ?: return
        val currentPlayer = _currentPlayer.value ?: return

        if (LogConfig.ENABLE_SPELL_LOGGING) {
            println("=== SPELL: Cast spell called - ${spell.displayName}")
            println("=== SPELL: Current mana: ${gameState.currentMana.value}/${gameState.maxMana.value}")
        }

        // Validate mana cost
        if (gameState.currentMana.value < spell.manaCost) {
            if (LogConfig.ENABLE_SPELL_LOGGING) {
                println("=== SPELL: FAILED - Not enough mana to cast ${spell.displayName}")
            }
            return
        }

        // Validate spell is unlocked
        if (!currentPlayer.abilities.unlockedSpells.contains(spell)) {
            if (LogConfig.ENABLE_SPELL_LOGGING) {
                println("=== SPELL: FAILED - Spell ${spell.displayName} is not unlocked")
            }
            return
        }

        // If spell requires targeting and no target provided, enter targeting mode
        if (spell.requiresTarget && target == null) {
            if (LogConfig.ENABLE_SPELL_LOGGING) {
                println("=== SPELL: Spell requires targeting - Entering targeting mode")
            }
            enterSpellTargetingMode(spell)
            return
        }

        // Deduct mana cost
        val previousMana = gameState.currentMana.value
        gameState.currentMana.value -= spell.manaCost
        if (LogConfig.ENABLE_SPELL_LOGGING) {
            println("=== SPELL: Mana deducted - ${previousMana} -> ${gameState.currentMana.value}")
        }

        // Execute spell effect
        executeSpellEffect(spell, target, gameState)

        // Clear pending spell and targeting state
        _pendingSpellCast.value = null
        exitSpellTargetingMode()

        // Close magic panel after casting
        closeMagicPanel()

        if (LogConfig.ENABLE_SPELL_LOGGING) {
            println("=== SPELL: Spell cast COMPLETE - ${spell.displayName}")
        }
    }

    /**
     * Execute the effect of a cast spell
     */
    private fun executeSpellEffect(spell: SpellType, target: Any?, gameState: GameState) {
        when (spell) {
            SpellType.ATTACK_AIMED -> {
                // Attack Aimed: Deal 80 damage to single enemy
                val attacker = target as? Attacker
                if (attacker != null) {
                    attacker.currentHealth.value = (attacker.currentHealth.value - 80).coerceAtLeast(0)
                    if (LogConfig.ENABLE_SPELL_LOGGING) {
                    println("Attack Aimed: Dealt 80 damage to ${attacker.type.displayName} (HP: ${attacker.currentHealth.value})")
                    }
                }
            }
            SpellType.HEAL -> {
                // Heal: Restore 3 health points (cap at max)
                val currentHP = gameState.healthPoints.value
                val maxHP = gameState.level.healthPoints + (_currentPlayer.value?.abilities?.healthAbility ?: 0)
                val newHP = (currentHP + 3).coerceAtMost(maxHP)
                gameState.healthPoints.value = newHP
                if (LogConfig.ENABLE_SPELL_LOGGING) {
                println("Heal: Restored health to $newHP/$maxHP")
                }
            }
            SpellType.ATTACK_AREA -> {
                // Attack Area: Deal 50 damage to all enemies within 2 hex range of position
                val position = target as? Position
                if (position != null) {
                    var damagedCount = 0
                    gameState.attackers.forEach { attacker ->
                        val distance = attacker.position.value.hexDistanceTo(position)
                        if (distance <= 2) {
                            attacker.currentHealth.value = (attacker.currentHealth.value - 50).coerceAtLeast(0)
                            damagedCount++
                        }
                    }
                    if (LogConfig.ENABLE_SPELL_LOGGING) {
                    println("Attack Area: Dealt 50 damage to $damagedCount enemies within 2 hex range of $position")
                    }
                }
            }
            SpellType.INSTANT_TOWER -> {
                // Instant Tower: Skip build time for one tower under construction
                val defender = target as? Defender
                if (defender != null && defender.buildTimeRemaining.value > 0) {
                    defender.buildTimeRemaining.value = 0
                    if (LogConfig.ENABLE_SPELL_LOGGING) {
                    println("Instant Tower: ${defender.type.displayName} at ${defender.position.value} is now ready!")
                    }
                }
            }
            SpellType.DOUBLE_TOWER_LEVEL -> {
                // Double Tower Level: Double tower level for 1 turn
                val defender = target as? Defender
                if (defender != null) {
                    val effect = ActiveSpellEffect(
                        spell = SpellType.DOUBLE_TOWER_LEVEL,
                        defenderId = defender.id,
                        turnsRemaining = 1,
                        castTurn = gameState.turnNumber.value
                    )
                    gameState.activeSpellEffects.add(effect)
                    if (LogConfig.ENABLE_SPELL_LOGGING) {
                    println("Double Tower Level: ${defender.type.displayName} level doubled for 1 turn!")
                    }
                }
            }
            SpellType.DOUBLE_TOWER_REACH -> {
                // Double Tower Reach: Double tower range for 1 turn
                val defender = target as? Defender
                if (defender != null) {
                    val effect = ActiveSpellEffect(
                        spell = SpellType.DOUBLE_TOWER_REACH,
                        defenderId = defender.id,
                        turnsRemaining = 1,
                        castTurn = gameState.turnNumber.value
                    )
                    gameState.activeSpellEffects.add(effect)
                    if (LogConfig.ENABLE_SPELL_LOGGING) {
                    println("Double Tower Reach: ${defender.type.displayName} range doubled for 1 turn!")
                    }
                }
            }
            SpellType.BOMB -> {
                // Bomb: Place bomb at position, explodes after 2 turns
                val position = target as? Position
                if (position != null) {
                    val effect = ActiveSpellEffect(
                        spell = SpellType.BOMB,
                        position = position,
                        turnsRemaining = 3,  // Explodes at the start of turn 3 (2 full turns + explosion)
                        castTurn = gameState.turnNumber.value
                    )
                    gameState.activeSpellEffects.add(effect)
                    if (LogConfig.ENABLE_SPELL_LOGGING) {
                    println("Bomb: Placed at $position, will explode in 2 turns!")
                    }
                }
            }
            SpellType.FREEZE_SPELL -> {
                // Freeze: Freeze enemy for 1+ turns (base 1 turn for 10 mana)
                val attacker = target as? Attacker
                if (attacker != null) {
                    // Check immunity: Does not work on Demons, Dragons, Ewhad
                    val isImmune = attacker.type.isDragon ||
                                   attacker.type == AttackerType.BLUE_DEMON ||
                                   attacker.type == AttackerType.RED_DEMON ||
                                   attacker.type == AttackerType.EWHAD

                    if (isImmune) {
                        println("Freeze Spell: ${attacker.type.displayName} is immune to freeze!")
                    } else {
                        // For now, freeze for 1 turn (base cost)
                        // TODO: Add duration selection dialog for spending more mana
                        val effect = ActiveSpellEffect(
                            spell = SpellType.FREEZE_SPELL,
                            attackerId = attacker.id,
                            turnsRemaining = 1,
                            castTurn = gameState.turnNumber.value
                        )
                        gameState.activeSpellEffects.add(effect)
                        if (LogConfig.ENABLE_SPELL_LOGGING) {
                        println("Freeze Spell: Froze ${attacker.type.displayName} for 1 turn!")
                        }
                    }
                }
            }
            SpellType.COOLING_SPELL -> {
                // Cooling Spell: Create area that slows enemies for 3 turns
                val position = target as? Position
                if (position != null) {
                    val effect = ActiveSpellEffect(
                        spell = SpellType.COOLING_SPELL,
                        position = position,
                        turnsRemaining = 3,
                        castTurn = gameState.turnNumber.value
                    )
                    gameState.activeSpellEffects.add(effect)
                    if (LogConfig.ENABLE_SPELL_LOGGING) {
                    println("Cooling Spell: Created cooling area at $position for 3 turns!")
                    }
                }
            }
            else -> {
                // Other spells not yet implemented
                if (LogConfig.ENABLE_SPELL_LOGGING) {
                println("Cast ${spell.displayName} - Effect not yet implemented")
                }
            }
        }
    }

    /**
     * Enter spell targeting mode
     */
    fun enterSpellTargetingMode(spell: SpellType) {
        val gameState = _gameState.value ?: return

        // Calculate valid targets based on spell type
        val validTargets: Set<Any> = when (spell.targetType) {
            SpellTargetType.POSITION -> {
                // All tiles on the map are valid positions
                val positions = mutableSetOf<Position>()
                for (x in 0 until gameState.level.gridWidth) {
                    for (y in 0 until gameState.level.gridHeight) {
                        positions.add(Position(x, y))
                    }
                }
                positions
            }
            SpellTargetType.ENEMY -> {
                // All active (non-defeated) enemies are valid targets
                gameState.attackers.filter { !it.isDefeated.value }.toSet()
            }
            SpellTargetType.TOWER -> {
                // All placed defenders are valid targets
                gameState.defenders.toSet()
            }
            SpellTargetType.NONE -> emptySet()
        }

        // Set targeting state
        gameState.spellTargeting.value = SpellTargetingState(
            activeSpell = spell,
            validTargets = validTargets
        )

        // Clear pending spell cast (we're now in targeting mode)
        _pendingSpellCast.value = null
    }

    /**
     * Exit spell targeting mode (cancel)
     */
    fun exitSpellTargetingMode() {
        val gameState = _gameState.value ?: return
        gameState.spellTargeting.value = null
    }

    /**
     * Select a target and cast the spell
     */
    fun selectSpellTarget(target: Any) {
        val gameState = _gameState.value ?: return
        val targeting = gameState.spellTargeting.value ?: return

        // Validate target is in valid targets set
        if (!targeting.validTargets.contains(target)) {
            println("Invalid target for spell")
            return
        }

        // Show confirmation dialog with target details (or warning for immune enemies)
        onSpellTargetSelected(target)
    }
}
