package com.defenderofegril.ui

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.defenderofegril.game.GameEngine
import com.defenderofegril.game.LevelData
import com.defenderofegril.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    
    // MutableState for coins to ensure UI reactivity
    private val _coins = mutableStateOf(0)
    val coins: State<Int> = _coins
    
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
            _coins.value = newGameState.coins  // Initialize coins MutableState
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
        
        // Automatically process enemy turn with animations
        viewModelScope.launch(Dispatchers.Default) {
            // Step 1: Set phase to ENEMY_TURN and show indicator
            engine.setEnemyPhaseAndIncrementTurn()
            triggerStateUpdate()
            delay(800) // Show "ENEMY TURN" text
            
            // Step 2: Spawn new attackers (with spawn animation flag set)
            val beforeSpawnCount = state.attackers.size
            engine.spawnAttackersStep()
            // Re-check the updated state after spawning
            val currentState = _gameState.value ?: return@launch
            val afterSpawnCount = currentState.attackers.size
            
            if (afterSpawnCount > beforeSpawnCount) {
                triggerStateUpdate()
                delay(600) // Show spawning animation
            }
            
            // Step 3: Move attackers with animation
            engine.moveAttackersStep()
            triggerStateUpdate()
            delay(1000) // Show movement animation
            
            // Clear animation flags after movement is visible
            engine.clearAnimationFlags()
            triggerStateUpdate()
            delay(200)
            
            // Step 4: Apply DOT effects
            engine.applyDotEffectsStep()
            triggerStateUpdate()
            delay(300)
            
            // Step 5: Process defeated attackers
            engine.processDefeatedAttackersStep()
            triggerStateUpdate()
            delay(200)
            
            // Step 6: Check if we should load next wave
            engine.loadNextWaveStep()
            triggerStateUpdate() // Update UI after potentially loading new wave
            delay(200)
            
            // Step 7: Advance building timers and start next player turn
            engine.advanceBuildTimersAndStartPlayerTurn()
            triggerStateUpdate()
            
            delay(300)
            
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
        // Use StateFlow.update to ensure proper emission
        _gameState.update { currentState ->
            if (currentState == null) return@update null
            
            // Filter out defeated enemies from the lists
            currentState.attackers.removeAll { it.isDefeated }
            
            // Update the coins MutableState for UI reactivity
            _coins.value = currentState.coins
            
            println("DEBUG: triggerStateUpdate - State updated, updateCounter=${++updateCounter}")
            println("DEBUG: State after update - Phase: ${currentState.phase}, Turn: ${currentState.turnNumber}, Attackers: ${currentState.attackers.size}, Coins: ${currentState.coins}")
            
            // Check affordability for all tower types
            println("DEBUG: Tower affordability check:")
            DefenderType.entries.forEach { type ->
                val canAfford = currentState.coins >= type.baseCost
                println("DEBUG:   ${type.displayName} (cost ${type.baseCost}): canAfford=$canAfford (coins=${currentState.coins})")
            }
            
            currentState.attackers.forEach { attacker ->
                println("DEBUG: After update - Enemy ${attacker.id} - Type: ${attacker.type}, Position: (${attacker.position.x}, ${attacker.position.y})")
            }
            
            // Return a copy to force emission
            currentState.copy()
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
        return when (code.lowercase()) {
            "moneybags", "1000coins", "cash" -> {
                gameEngine?.addCoins(1000)
                triggerStateUpdate()
                true
            }
            else -> false
        }
    }
}
