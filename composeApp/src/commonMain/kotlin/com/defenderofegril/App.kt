package com.defenderofegril

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import com.defenderofegril.ui.*

@Composable
fun App() {
    MaterialTheme {
        val viewModel = remember { GameViewModel() }
        val currentScreen by viewModel.currentScreen.collectAsState()
        val worldLevels by viewModel.worldLevels.collectAsState()
        val gameState by viewModel.gameState.collectAsState()
        
        when (val screen = currentScreen) {
            is Screen.MainMenu -> {
                MainMenuScreen(
                    onStartGame = { viewModel.navigateToWorldMap() }
                )
            }
            
            is Screen.WorldMap -> {
                WorldMapScreen(
                    worldLevels = worldLevels,
                    onLevelSelected = { levelId -> viewModel.startLevel(levelId) },
                    onBackToMenu = { viewModel.navigateToMainMenu() },
                    onCheatCode = { code -> viewModel.applyWorldMapCheatCode(code) }
                )
            }
            
            is Screen.GamePlay -> {
                gameState?.let { state ->
                    GamePlayScreen(
                        gameState = state,
                        onPlaceDefender = { type, pos -> viewModel.placeDefender(type, pos) },
                        onUpgradeDefender = { id -> viewModel.upgradeDefender(id) },
                        onUndoTower = { id -> viewModel.undoTower(id) },
                        onSellTower = { id -> viewModel.sellTower(id) },
                        onStartFirstPlayerTurn = { viewModel.startFirstPlayerTurn() },
                        onDefenderAttack = { defenderId, targetId -> viewModel.defenderAttack(defenderId, targetId) },
                        onDefenderAttackPosition = { defenderId, targetPos -> viewModel.defenderAttackPosition(defenderId, targetPos) },
                        onEndPlayerTurn = { viewModel.endPlayerTurn() },
                        onBackToMap = { viewModel.navigateToWorldMap() },
                        onCheatCode = { code -> viewModel.applyCheatCode(code) },
                        onMineDig = { mineId -> viewModel.performMineDig(mineId) },
                        onMineBuildTrap = { mineId, trapPos -> viewModel.performMineBuildTrap(mineId, trapPos) }
                    )
                }
            }
            
            is Screen.LevelComplete -> {
                LevelCompleteScreen(
                    levelId = screen.levelId,
                    won = screen.won,
                    onRestart = { viewModel.restartLevel() },
                    onBackToMap = { viewModel.navigateToWorldMap() }
                )
            }
        }
    }
}
