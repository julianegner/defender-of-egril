package com.defenderofegril.ui

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
    private var updateCounter = 0L
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
        val result = gameEngine?.placeDefender(type, position) ?: false
        if (result) {
            // Trigger state update by reassigning
            triggerStateUpdate()
        }
        return result
    }
    
    fun upgradeDefender(defenderId: Int): Boolean {
        val result = gameEngine?.upgradeDefender(defenderId) ?: false
        if (result) {
            triggerStateUpdate()
        }
        return result
    }
    
    fun startFirstPlayerTurn() {
        println("DEBUG: startFirstPlayerTurn called")
        val stateBefore = _gameState.value
        println("DEBUG: Phase before: ${stateBefore?.phase}")
        println("DEBUG: Attackers before: ${stateBefore?.attackers?.size}")
        
        gameEngine?.startFirstPlayerTurn()
        
        val stateAfter = _gameState.value
        println("DEBUG: Phase after: ${stateAfter?.phase}")
        println("DEBUG: Attackers after: ${stateAfter?.attackers?.size}")
        stateAfter?.attackers?.forEach { attacker ->
            println("DEBUG: Enemy ${attacker.id} - Type: ${attacker.type}, Position: (${attacker.position.x}, ${attacker.position.y})")
        }
        
        triggerStateUpdate()
        println("DEBUG: triggerStateUpdate completed")
    }
    
    fun defenderAttack(defenderId: Int, targetId: Int): Boolean {
        val result = gameEngine?.defenderAttack(defenderId, targetId) ?: false
        if (result) {
            triggerStateUpdate()
        }
        return result
    }
    
    fun endPlayerTurn() {
        val state = _gameState.value ?: return
        
        // Set phase to ENEMY_TURN for UI feedback
        state.phase = GamePhase.ENEMY_TURN
        triggerStateUpdate()
        
        // Automatically process enemy turn after a delay
        viewModelScope.launch(Dispatchers.Default) {
            delay(500) // Shorter delay to see "ENEMY TURN" indicator
            
            // Now process the actual enemy turn logic
            gameEngine?.endPlayerTurn()
            
            // Force UI update after enemy movements to show the changes
            triggerStateUpdate()
            
            // Add another small delay so user can see the enemy movements
            delay(1000)
            
            // Check win/loss conditions
            val updatedState = _gameState.value ?: return@launch
            if (updatedState.isLevelWon()) {
                completeLevel(updatedState.level.id, won = true)
            } else if (updatedState.isLevelLost()) {
                completeLevel(updatedState.level.id, won = false)
            }
        }
    }
    
    private fun triggerStateUpdate() {
        // Force StateFlow to emit by creating a new copy of the state
        // This ensures Compose's collectAsState() detects the change
        val currentState = _gameState.value ?: return
        
        // Create a deep copy with new list instances to trigger state change detection
        // This is necessary because Compose's collectAsState() compares object references
        _gameState.value = currentState.copy(
            defenders = currentState.defenders.toMutableList(),
            attackers = currentState.attackers.toMutableList(),
            attackersToSpawn = currentState.attackersToSpawn.toMutableList()
        )
        
        println("DEBUG: triggerStateUpdate - New state instance created, updateCounter=${++updateCounter}")
        println("DEBUG: State after update - Phase: ${_gameState.value?.phase}, Attackers: ${_gameState.value?.attackers?.size}")
        _gameState.value?.attackers?.forEach { attacker ->
            println("DEBUG: After update - Enemy ${attacker.id} - Type: ${attacker.type}, Position: (${attacker.position.x}, ${attacker.position.y})")
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
}
