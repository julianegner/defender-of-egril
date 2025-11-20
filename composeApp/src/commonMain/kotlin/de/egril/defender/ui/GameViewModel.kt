package de.egril.defender.ui

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import de.egril.defender.game.GameEngine
import de.egril.defender.game.LevelData
import de.egril.defender.model.*
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
    object LevelEditor : Screen()
    object LoadGame : Screen()
    data class GamePlay(val levelId: Int) : Screen()
    data class LevelComplete(val levelId: Int, val won: Boolean, val isLastLevel: Boolean) : Screen()
}

class GameViewModel {
    
    private val _currentScreen = MutableStateFlow<Screen>(Screen.MainMenu)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()
    
    private val _worldLevels = MutableStateFlow<List<WorldLevel>>(emptyList())
    val worldLevels: StateFlow<List<WorldLevel>> = _worldLevels.asStateFlow()
    
    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState: StateFlow<GameState?> = _gameState.asStateFlow()
    
    private val _cheatDigOutcome = MutableStateFlow<DigOutcome?>(null)
    val cheatDigOutcome: StateFlow<DigOutcome?> = _cheatDigOutcome.asStateFlow()

    private var gameEngine: GameEngine? = null
    private val viewModelScope = CoroutineScope(Dispatchers.Default)
    
    init {
        initializeWorldMap()
    }
    
    private fun initializeWorldMap() {
        val levels = LevelData.createLevels()
        println("DEBUG: Total levels loaded: ${levels.size}")
        
        // Load saved world map status
        val savedStatuses = de.egril.defender.save.SaveFileStorage.loadWorldMapStatus()
        
        _worldLevels.value = levels.mapIndexed { index, level ->
            println("DEBUG: Loaded Level ${level.id} - Name: ${level.name} - Path Cells: ${level.pathCells.size} - Build Islands: ${level.buildIslands.size}")

            val status = savedStatuses?.get(level.id) ?: 
                if (index == 0) LevelStatus.UNLOCKED else LevelStatus.LOCKED
            
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
        _currentScreen.value = Screen.MainMenu
    }
    
    fun navigateToWorldMap() {
        // Reload levels from disk to ensure latest changes are visible
        reloadWorldMap()
        _currentScreen.value = Screen.WorldMap
    }
    
    fun navigateToRules() {
        _currentScreen.value = Screen.Rules
    }
    
    fun navigateToLevelEditor() {
        _currentScreen.value = Screen.LevelEditor
    }
    
    fun startLevel(levelId: Int) {
        val worldLevel = _worldLevels.value.find { it.level.id == levelId }
        if (worldLevel != null && worldLevel.status != LevelStatus.LOCKED) {
            val newGameState = GameState(level = worldLevel.level)
            _gameState.value = newGameState
            gameEngine = GameEngine(newGameState)
            _currentScreen.value = Screen.GamePlay(levelId)
        }
    }
    
    fun placeDefender(type: DefenderType, position: Position): Boolean {
        return gameEngine?.placeDefender(type, position) ?: false
    }
    
    fun upgradeDefender(defenderId: Int): Boolean {
        return gameEngine?.upgradeDefender(defenderId) ?: false
    }
    
    fun undoTower(defenderId: Int): Boolean {
        return gameEngine?.undoTower(defenderId) ?: false
    }
    
    fun sellTower(defenderId: Int): Boolean {
        return gameEngine?.sellTower(defenderId) ?: false
    }
    
    fun startFirstPlayerTurn() {
        println("DEBUG: startFirstPlayerTurn called")
        val stateBefore = _gameState.value
        println("DEBUG: Phase before: ${stateBefore?.phase?.value}")
        println("DEBUG: Attackers before: ${stateBefore?.attackers?.size}")
        
        gameEngine?.startFirstPlayerTurn()
        
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
        return gameEngine?.performMineDig(mineId)
    }
    
    fun performMineBuildTrap(mineId: Int, trapPosition: Position): Boolean {
        return gameEngine?.performMineBuildTrap(mineId, trapPosition) ?: false
    }
    
    fun performMineDigWithOutcome(outcome: DigOutcome): DigOutcome? {
        return gameEngine?.performMineDigWithOutcome(outcome)
    }

    fun endPlayerTurn() {
        val state = _gameState.value ?: return
        val engine = gameEngine ?: return
        
        // Process enemy turn with animations
        viewModelScope.launch(Dispatchers.Default) {
            // Start enemy turn: change phase to ENEMY_TURN
            engine.startEnemyTurn()
            
            // Show "ENEMY TURN" indicator
            delay(800)
            
            // Calculate all movement steps for existing units
            val movementSteps = engine.calculateEnemyTurnMovements()
            
            // Apply each movement step with a delay between steps
            for (stepMovements in movementSteps) {
                // Apply all movements in this step simultaneously
                for ((attackerId, newPosition) in stepMovements) {
                    engine.applyMovement(attackerId, newPosition)
                }
                // Delay between movement steps so user can see the animation
                delay(400)
            }
            
            // Add a small delay to see final positions before spawning new units
            if (movementSteps.isNotEmpty()) {
                delay(300)
            }
            
            // Now spawn new units (spawn points should be clear after movements)
            engine.spawnEnemyTurnAttackers()
            
            // Show spawned units briefly
            delay(400)
            
            // Move newly spawned units away from spawn points
            val newSpawnMovements = engine.calculateNewlySpawnedMovements()
            for (stepMovements in newSpawnMovements) {
                for ((attackerId, newPosition) in stepMovements) {
                    engine.applyMovement(attackerId, newPosition)
                }
                // Delay between movement steps
                delay(400)
            }
            
            // Add a small delay after newly spawned units have moved
            if (newSpawnMovements.isNotEmpty()) {
                delay(300)
            }
            
            // Complete enemy turn: apply effects and return to player turn
            engine.completeEnemyTurn()
            
            // Check win/loss conditions
            val updatedState = _gameState.value ?: return@launch
            if (updatedState.isLevelWon()) {
                completeLevel(updatedState.level.id, won = true)
            } else if (updatedState.isLevelLost()) {
                completeLevel(updatedState.level.id, won = false)
            }
        }
    }

    private fun completeLevel(levelId: Int, won: Boolean) {
        val isLastLevel = _worldLevels.value.lastOrNull()?.level?.id == levelId
        if (won) {
            val updatedLevels = _worldLevels.value.toMutableList()
            val currentIndex = updatedLevels.indexOfFirst { it.level.id == levelId }
            if (currentIndex >= 0) {
                updatedLevels[currentIndex] = updatedLevels[currentIndex].copy(status = LevelStatus.WON)
                // Unlock next level
                if (currentIndex + 1 < updatedLevels.size && updatedLevels[currentIndex + 1].status == LevelStatus.LOCKED) {
                    updatedLevels[currentIndex + 1] = updatedLevels[currentIndex + 1].copy(status = LevelStatus.UNLOCKED)
                }
                _worldLevels.value = updatedLevels
                // Save world map status
                saveWorldMapStatus()
            }
        }
        _currentScreen.value = Screen.LevelComplete(levelId, won, isLastLevel)
    }
    
    fun restartLevel() {
        val levelId = (_currentScreen.value as? Screen.LevelComplete)?.levelId
            ?: (_currentScreen.value as? Screen.GamePlay)?.levelId
            ?: return
        startLevel(levelId)
    }
    
    fun applyCheatCode(code: String): Boolean {
        val (success, digOutcome) = CheatCodeHandler.applyCheatCode(
            code = code,
            addCoins = { amount -> gameEngine?.addCoins(amount) },
            performMineDigWithOutcome = { outcome -> performMineDigWithOutcome(outcome) },
            spawnEnemy = { attackerType, level -> gameEngine?.spawnEnemy(attackerType, level) }
        )
        
        if (digOutcome != null) {
            _cheatDigOutcome.value = digOutcome
        }
        
        return success
    }
    
    fun clearCheatDigOutcome() {
        _cheatDigOutcome.value = null
    }
    
    fun applyWorldMapCheatCode(code: String): Boolean {
        return CheatCodeHandler.applyWorldMapCheatCode(
            code = code,
            unlockAllLevels = { unlockAllLevels() }
        )
    }
    
    private fun unlockAllLevels() {
        _worldLevels.value = CheatCodeHandler.unlockAllLevels(_worldLevels.value)
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
        return saveId
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
        }
    }
    
    fun deleteSavedGame(saveId: String) {
        de.egril.defender.save.SaveFileStorage.deleteSavedGame(saveId)
        refreshSavedGames()
    }
    
    private fun refreshSavedGames() {
        _savedGames.value = de.egril.defender.save.SaveFileStorage.getAllSavedGames()
    }
    
    private fun saveWorldMapStatus() {
        de.egril.defender.save.SaveFileStorage.saveWorldMapStatus(_worldLevels.value)
    }
}
