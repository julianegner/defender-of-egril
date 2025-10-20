package com.defenderofegril.ui

import com.defenderofegril.game.GameEngine
import com.defenderofegril.game.LevelData
import com.defenderofegril.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class Screen {
    object MainMenu : Screen()
    object WorldMap : Screen()
    data class GamePlay(val levelId: Int) : Screen()
    data class LevelComplete(val levelId: Int, val won: Boolean) : Screen()
}

// Wrapper to force state updates
data class GameStateHolder(
    val state: GameState,
    val updateId: Long = 0
)

class GameViewModel {
    
    private val _currentScreen = MutableStateFlow<Screen>(Screen.MainMenu)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()
    
    private val _worldLevels = MutableStateFlow<List<WorldLevel>>(emptyList())
    val worldLevels: StateFlow<List<WorldLevel>> = _worldLevels.asStateFlow()
    
    private val _gameStateHolder = MutableStateFlow<GameStateHolder?>(null)
    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState: StateFlow<GameState?> = _gameState.asStateFlow()
    
    private var gameEngine: GameEngine? = null
    private var updateCounter = 0L
    
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
            _gameStateHolder.value = GameStateHolder(newGameState, ++updateCounter)
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
        val stateBefore = _gameStateHolder.value?.state
        println("DEBUG: Phase before: ${stateBefore?.phase}")
        println("DEBUG: Attackers before: ${stateBefore?.attackers?.size}")
        
        gameEngine?.startFirstPlayerTurn()
        
        val stateAfter = _gameStateHolder.value?.state
        println("DEBUG: Phase after: ${stateAfter?.phase}")
        println("DEBUG: Attackers after: ${stateAfter?.attackers?.size}")
        
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
        gameEngine?.endPlayerTurn()
        triggerStateUpdate()
        
        val state = _gameStateHolder.value?.state ?: return
        if (state.isLevelWon()) {
            completeLevel(state.level.id, won = true)
        } else if (state.isLevelLost()) {
            completeLevel(state.level.id, won = false)
        }
    }
    
    private fun triggerStateUpdate() {
        // Force StateFlow to emit by creating new holder with updated counter
        _gameStateHolder.value?.state?.let { currentState ->
            _gameStateHolder.value = GameStateHolder(currentState, ++updateCounter)
            _gameState.value = currentState
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
