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
        de.egril.defender.audio.GlobalBackgroundMusicManager.initialize()
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
        val needsPlayerSelection by viewModel.needsPlayerSelection.collectAsState()
        val currentPlayer by viewModel.currentPlayer.collectAsState()
        val allPlayers by viewModel.allPlayers.collectAsState()
        val worldMapConflict by viewModel.worldMapConflict.collectAsState()
        
        // Show player selection dialog if needed
        var showPlayerSelection by remember { mutableStateOf(false) }
        var showCreatePlayer by remember { mutableStateOf(false) }
        var showEditPlayer by remember { mutableStateOf(false) }
        
        // On first launch, show create player dialog if no players exist
        LaunchedEffect(needsPlayerSelection) {
            if (needsPlayerSelection) {
                showCreatePlayer = true
            }
        }
        
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
        
        // Player selection dialogs
        if (showCreatePlayer) {
            CreatePlayerDialog(
                onCreatePlayer = { name ->
                    val success = viewModel.createPlayer(name)
                    if (success) {
                        showCreatePlayer = false
                    }
                },
                onDismiss = {
                    // Only allow dismiss if we already have a player
                    if (currentPlayer != null) {
                        showCreatePlayer = false
                    }
                }
            )
        }
        
        if (showPlayerSelection) {
            SelectPlayerDialog(
                players = allPlayers,
                currentPlayerId = currentPlayer?.id,
                onSelectPlayer = { playerId ->
                    viewModel.switchPlayer(playerId)
                    showPlayerSelection = false
                },
                onCreateNewPlayer = {
                    showPlayerSelection = false
                    showCreatePlayer = true
                },
                onDeletePlayer = { playerId ->
                    viewModel.deletePlayer(playerId)
                },
                onDismiss = { showPlayerSelection = false }
            )
        }
        
        if (showEditPlayer) {
            PlayerNameDialog(
                initialName = currentPlayer?.name ?: "",
                isEdit = true,
                onSave = { newName ->
                    val success = viewModel.renameCurrentPlayer(newName)
                    if (success) {
                        showEditPlayer = false
                    }
                },
                onDismiss = { showEditPlayer = false }
            )
        }
        
        // World map conflict dialog
        worldMapConflict?.let { conflict ->
            de.egril.defender.ui.loadgame.WorldMapConflictDialog(
                conflict = conflict,
                onUseSavedVersion = { viewModel.resolveWorldMapConflict(useSavedVersion = true) },
                onUseCurrentVersion = { viewModel.resolveWorldMapConflict(useSavedVersion = false) },
                onCancel = { viewModel.cancelWorldMapConflict() }
            )
        }
        
        when (val screen = currentScreen) {
            is Screen.MainMenu -> {
                MainMenuScreen(
                    onStartGame = { viewModel.navigateToWorldMap() },
                    onShowRules = { viewModel.navigateToRules() },
                    onSelectPlayer = { showPlayerSelection = true },
                    onEditPlayerName = { showEditPlayer = true },
                    currentPlayerName = currentPlayer?.name
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
                    onReloadWorldMap = { viewModel.reloadWorldMap() },
                    onSwitchPlayer = { showPlayerSelection = true },
                    onEditPlayerName = { showEditPlayer = true },
                    currentPlayerName = currentPlayer?.name
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
                    onDownloadGame = { saveId, includeGameState -> viewModel.downloadSaveGame(saveId, includeGameState) },
                    onDownloadAll = { includeGameState -> viewModel.downloadAllSaveGames(includeGameState) },
                    onExportGameProgress = { viewModel.downloadGameState() },
                    onImportGameProgress = { json -> viewModel.importWorldMapProgress(json) },
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
            
            is Screen.Sticker -> {
                StickerScreen(
                    onBack = { viewModel.navigateToWorldMap() }
                )
            }
        }
    }
}
