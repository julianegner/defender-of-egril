package de.egril.defender

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import de.egril.defender.ui.*
import de.egril.defender.ui.editor.level.LevelEditorScreen
import de.egril.defender.ui.gameplay.GamePlayScreen
import de.egril.defender.ui.loadgame.LoadGameScreen
import de.egril.defender.ui.settings.AppSettings
import de.egril.defender.ui.worldmap.WorldMapScreen
import de.egril.defender.utils.WindowCloseHandler

@Composable
fun App() {
    // Initialize settings and sound on app start
    LaunchedEffect(Unit) {
        AppSettings.initialize()
        de.egril.defender.audio.GlobalSoundManager.initialize()
    }
    
    // Observe dark mode state
    val isDarkMode by AppSettings.isDarkMode
    
    // Use custom color schemes with softer dark mode colors
    val colorScheme = if (isDarkMode) {
        AppTheme.darkColorScheme
    } else {
        AppTheme.lightColorScheme
    }
    
    MaterialTheme(colorScheme = colorScheme) {
        val viewModel = remember { GameViewModel() }
        val currentScreen by viewModel.currentScreen.collectAsState()
        val worldLevels by viewModel.worldLevels.collectAsState()
        val gameState by viewModel.gameState.collectAsState()
        val savedGames by viewModel.savedGames.collectAsState()
        val cheatDigOutcome by viewModel.cheatDigOutcome.collectAsState()
        
        // Register unsaved changes checker for window close handling
        LaunchedEffect(currentScreen) {
            when (currentScreen) {
                is Screen.GamePlay -> {
                    WindowCloseHandler.setUnsavedChangesChecker { viewModel.hasUnsavedChanges() }
                    WindowCloseHandler.setSaveGameCallback { viewModel.saveCurrentGame() }
                }
                else -> {
                    WindowCloseHandler.setUnsavedChangesChecker(null)
                    WindowCloseHandler.setSaveGameCallback(null)
                }
            }
        }
        
        when (val screen = currentScreen) {
            is Screen.MainMenu -> {
                MainMenuScreen(
                    onStartGame = { viewModel.navigateToWorldMap() },
                    onShowRules = { viewModel.navigateToRules() }
                )
            }
            
            is Screen.WorldMap -> {
                WorldMapScreen(
                    worldLevels = worldLevels,
                    onLevelSelected = { levelId -> viewModel.startLevel(levelId) },
                    onBackToMenu = { viewModel.navigateToMainMenu() },
                    onShowRules = { viewModel.navigateToRules() },
                    onOpenEditor = { viewModel.navigateToLevelEditor() },
                    onLoadGame = { viewModel.navigateToLoadGame() },
                    onCheatCode = { code -> viewModel.applyWorldMapCheatCode(code) },
                    onReloadWorldMap = { viewModel.reloadWorldMap() }
                )
            }
            
            is Screen.Rules -> {
                RulesScreen(
                    onBack = { viewModel.navigateToMainMenu() }
                )
            }
            
            is Screen.LevelEditor -> {
                LevelEditorScreen(
                    onBack = { viewModel.navigateToWorldMap() }
                )
            }
            
            is Screen.LoadGame -> {
                LoadGameScreen(
                    savedGames = savedGames,
                    onLoadGame = { saveId -> viewModel.loadGame(saveId) },
                    onDeleteGame = { saveId -> viewModel.deleteSavedGame(saveId) },
                    onDownloadGame = { saveId -> viewModel.downloadSaveGame(saveId) },
                    onDownloadAll = { viewModel.downloadAllSaveGames() },
                    onUpload = {
                        // Trigger refresh of saved games list after upload
                        viewModel.navigateToLoadGame()
                    },
                    onBack = { viewModel.navigateToWorldMap() }
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
                        onSaveGame = { comment -> viewModel.saveCurrentGame(comment) },
                        onCheatCode = { code -> viewModel.applyCheatCode(code) },
                        onMineDig = { mineId -> viewModel.performMineDig(mineId) },
                        onMineBuildTrap = { mineId, trapPos -> viewModel.performMineBuildTrap(mineId, trapPos) },
                        onWizardPlaceMagicalTrap = { wizardId, trapPos -> viewModel.performWizardPlaceMagicalTrap(wizardId, trapPos) },
                        cheatDigOutcome = cheatDigOutcome,
                        onClearCheatDigOutcome = { viewModel.clearCheatDigOutcome() },
                        hasUnsavedChanges = { viewModel.hasUnsavedChanges() }
                    )
                }
            }
            
            is Screen.LevelComplete -> {
                LevelCompleteScreen(
                    levelId = screen.levelId,
                    won = screen.won,
                    isLastLevel = screen.isLastLevel,
                    onRestart = { viewModel.restartLevel() },
                    onBackToMap = { viewModel.navigateToWorldMap() }
                )
            }
        }
    }
}
