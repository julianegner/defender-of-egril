package com.defenderofegril.ui

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.defenderofegril.game.GameEngine
import com.defenderofegril.game.LevelData
import com.defenderofegril.model.*
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
    data class GamePlay(val levelId: Int) : Screen()
    data class LevelComplete(val levelId: Int, val won: Boolean) : Screen()
}

class GameViewModel {
    
    private val _currentScreen = MutableStateFlow<Screen>(Screen.MainMenu)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()
    
    private val _worldLevels = MutableStateFlow<List<WorldLevel>>(emptyList())
    val worldLevels: StateFlow<List<WorldLevel>> = _worldLevels.asStateFlow()
    
    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState: StateFlow<GameState?> = _gameState.asStateFlow()

    private var gameEngine: GameEngine? = null
    private val viewModelScope = CoroutineScope(Dispatchers.Default)
    
    init {
        initializeWorldMap()
    }
    
    private fun initializeWorldMap() {
        val levels = LevelData.createLevels()
        _worldLevels.value = levels.mapIndexed { index, level ->
            WorldLevel(
                level = level,
                status = if (index == 0) LevelStatus.UNLOCKED else LevelStatus.LOCKED
            )
        }
    }
    
    fun navigateToMainMenu() {
        _currentScreen.value = Screen.MainMenu
    }
    
    fun navigateToWorldMap() {
        _currentScreen.value = Screen.WorldMap
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
            }
        }
        _currentScreen.value = Screen.LevelComplete(levelId, won)
    }
    
    fun restartLevel() {
        val levelId = (_currentScreen.value as? Screen.LevelComplete)?.levelId
            ?: (_currentScreen.value as? Screen.GamePlay)?.levelId
            ?: return
        startLevel(levelId)
    }
    
    fun applyCheatCode(code: String): Boolean {
        val lowercaseCode = code.lowercase().trim()
        
        // Handle simple one-word cheatcodes
        when (lowercaseCode) {
            "moneybags", "1000coins", "cash" -> {
                gameEngine?.addCoins(1000)
                return true
            }
        }
        
        // Handle "spawn <type> <level>" cheatcode
        if (lowercaseCode.startsWith("spawn ")) {
            val parts = lowercaseCode.split(" ").filter { it.isNotBlank() }
            if (parts.size >= 2) {
                val typeName = parts[1]
                val level = if (parts.size >= 3) parts[2].toIntOrNull() ?: 1 else 1
                
                // Map type name to AttackerType
                val attackerType = when (typeName) {
                    "goblin" -> AttackerType.GOBLIN
                    "ork", "orc" -> AttackerType.ORK
                    "ogre" -> AttackerType.OGRE
                    "skeleton" -> AttackerType.SKELETON
                    "wizard", "evil_wizard", "evilwizard" -> AttackerType.EVIL_WIZARD
                    "witch" -> AttackerType.WITCH
                    else -> return false
                }
                
                gameEngine?.spawnEnemy(attackerType, level)
                return true
            }
        }
        
        return false
    }
    
    fun applyWorldMapCheatCode(code: String): Boolean {
        val lowercaseCode = code.lowercase().trim()
        
        // Handle "unlock" or "unlockall" cheatcode to unlock all levels
        when (lowercaseCode) {
            "unlock", "unlockall", "unlock all" -> {
                unlockAllLevels()
                return true
            }
        }
        
        return false
    }
    
    private fun unlockAllLevels() {
        _worldLevels.value = _worldLevels.value.map { worldLevel ->
            when (worldLevel.status) {
                LevelStatus.LOCKED -> worldLevel.copy(status = LevelStatus.UNLOCKED)
                else -> worldLevel
            }
        }
    }
}
