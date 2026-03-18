package de.egril.defender

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import de.egril.defender.ui.*
import de.egril.defender.ui.editor.level.LevelEditorScreen
import de.egril.defender.ui.gameplay.GamePlayScreen
import de.egril.defender.ui.gameplay.LevelLoadingScreen
import de.egril.defender.ui.infopage.InfoPageScreen
import de.egril.defender.ui.loadgame.LoadGameScreen
import de.egril.defender.ui.settings.AppSettings
import de.egril.defender.ui.worldmap.WorldMapScreen
import de.egril.defender.utils.WindowCloseHandler
import kotlinx.coroutines.delay

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
        // Track repository data error
        var repositoryDataError by remember { mutableStateOf<de.egril.defender.editor.MissingRepositoryDataException?>(null) }
        
        // Try to create ViewModel, catch repository data errors
        val viewModel = remember {
            try {
                GameViewModel()
            } catch (e: de.egril.defender.editor.MissingRepositoryDataException) {
                repositoryDataError = e
                null
            }
        }
        
        // Show error dialog if repository data is missing
        if (repositoryDataError != null) {
            MissingRepositoryDataDialog(
                missingCategories = repositoryDataError!!.missingCategories,
                onDismiss = {
                    // Cannot dismiss - this is a fatal error
                    // User needs to reinstall or restore data
                }
            )
            return@MaterialTheme
        }
        
        // Null check for viewModel (should not happen if no exception was thrown)
        if (viewModel == null) {
            return@MaterialTheme
        }
        
        val currentScreen by viewModel.currentScreen.collectAsState()
        val worldLevels by viewModel.worldLevels.collectAsState()
        val gameState by viewModel.gameState.collectAsState()
        val savedGames by viewModel.savedGames.collectAsState()
        val cheatDigOutcome by viewModel.cheatDigOutcome.collectAsState()
        val showPlatformInfo by viewModel.showPlatformInfo.collectAsState()
        val showCheatHelp by viewModel.showCheatHelp.collectAsState()
        val needsPlayerSelection by viewModel.needsPlayerSelection.collectAsState()
        val currentPlayer by viewModel.currentPlayer.collectAsState()
        val allPlayers by viewModel.allPlayers.collectAsState()
        val worldMapConflict by viewModel.worldMapConflict.collectAsState()
        val specialActionsRemaining by viewModel.specialActionsRemaining.collectAsState()
        val reminderMessage by viewModel.reminderMessage.collectAsState()
        val newAchievement by viewModel.newAchievement.collectAsState()
        val showMagicPanel by viewModel.showMagicPanel.collectAsState()
        val selectedSpell by viewModel.selectedSpell.collectAsState()
        val pendingSpellCast by viewModel.pendingSpellCast.collectAsState()
        val showSpellTargetConfirmation by viewModel.showSpellTargetConfirmation.collectAsState()
        val showFreezeImmuneWarning by viewModel.showFreezeImmuneWarning.collectAsState()
        val pendingScrollToPosition by viewModel.pendingScrollToPosition.collectAsState()
        val pendingGameMessage by viewModel.pendingGameMessage.collectAsState()

        // Show player selection dialog if needed
        var showPlayerSelection by remember { mutableStateOf(false) }
        var showCreatePlayer by remember { mutableStateOf(false) }
        var showEditPlayer by remember { mutableStateOf(false) }
        
        // Show initial language chooser dialog on first start
        var showInitialLanguageChooser by remember { mutableStateOf(false) }
        
        // On first launch, check if we need to show language chooser first
        LaunchedEffect(needsPlayerSelection) {
            if (needsPlayerSelection) {
                // Check if language has been chosen before
                if (!AppSettings.hasChosenLanguage()) {
                    // First time - show language chooser first
                    showInitialLanguageChooser = true
                } else {
                    // Language already chosen - proceed to player creation
                    showCreatePlayer = true
                }
            }
        }
        
        // Register official data change checker for window close handling
        // This runs once at app start and checks if OfficialEditMode is enabled
        LaunchedEffect(Unit) {
            if (de.egril.defender.OfficialEditMode.enabled) {
                WindowCloseHandler.setOfficialDataChangedChecker { 
                    de.egril.defender.editor.OfficialDataChangeTracker.hasModifiedOfficialData()
                }
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
        
        // Initial language chooser dialog (first start only)
        if (showInitialLanguageChooser) {
            de.egril.defender.ui.settings.InitialLanguageChooserDialog(
                onLanguageSelected = {
                    showInitialLanguageChooser = false
                    showCreatePlayer = true
                }
            )
        }
        
        // Player selection dialogs
        if (showCreatePlayer) {
            CreatePlayerDialog(
                showCancelButton = currentPlayer != null,
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
        
        // Achievement notification dialog
        AchievementNotificationDialog(
            achievement = newAchievement,
            onDismiss = { viewModel.clearAchievementNotification() }
        )
        
        when (val screen = currentScreen) {
            is Screen.MainMenu -> {
                MainMenuScreen(
                    onStartGame = { viewModel.navigateToWorldMap() },
                    onContinueGame = { viewModel.continueFromAutosave() },
                    hasAutosave = viewModel.hasAutosave(),
                    onShowRules = { viewModel.navigateToRules() },
                    onShowInstallationInfo = { viewModel.navigateToInstallationInfo() },
                    onSelectPlayer = { showPlayerSelection = true },
                    onEditPlayerName = { viewModel.navigateToPlayerProfile() },
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
                    onEditPlayerName = { viewModel.navigateToPlayerProfile() },
                    currentPlayerName = currentPlayer?.name,
                    showPlatformInfo = showPlatformInfo,
                    onClearPlatformInfo = { viewModel.clearPlatformInfo() },
                    showCheatHelp = showCheatHelp,
                    onClearCheatHelp = { viewModel.clearCheatHelp() }
                )
            }
            
            is Screen.Rules -> {
                RulesScreen(
                    onBack = { viewModel.navigateToMainMenu() }
                )
            }
            
            is Screen.InstallationInfo -> {
                InfoPageScreen(
                    onBack = { viewModel.navigateToMainMenu() }
                )
            }
            
            is Screen.PlayerProfile -> {
                currentPlayer?.let { profile ->
                    de.egril.defender.ui.infopage.PlayerProfileScreen(
                        playerProfile = profile,
                        onBack = { viewModel.navigateToMainMenu() },
                        onEditName = { showEditPlayer = true },
                        onNavigateToStats = { viewModel.navigateToStatsUpgrade() },
                        onUpgradeAbility = { abilityType -> viewModel.upgradeAbility(abilityType) },
                        onUnlockSpell = { spell -> viewModel.unlockSpell(spell) }
                    )
                }
            }

            is Screen.StatsUpgrade -> {
                currentPlayer?.let { profile ->
                    AbilitiesUpgradeScreen(
                        playerProfile = profile,
                        onUpgradeAbility = { abilityType -> viewModel.upgradeAbility(abilityType) },
                        onUnlockSpell = { spell -> viewModel.unlockSpell(spell) },
                        onBack = { viewModel.navigateToPlayerProfile() },
                        onCheatCode = { code -> viewModel.applyWorldMapCheatCode(code) }
                    )
                }
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
                        onAutoAttackAndEndTurn = { viewModel.autoAttackAndEndTurn() },
                        onBackToMap = { viewModel.navigateToWorldMap() },
                        onSaveGame = { comment -> viewModel.saveCurrentGame(comment) },
                        onCheatCode = { code -> viewModel.applyCheatCode(code) },
                        onMineDig = { mineId -> viewModel.performMineDig(mineId) },
                        onMineBuildTrap = { mineId, trapPos -> viewModel.performMineBuildTrap(mineId, trapPos) },
                        onWizardPlaceMagicalTrap = { wizardId, trapPos -> viewModel.performWizardPlaceMagicalTrap(wizardId, trapPos) },
                        onWizardGenerateMana = { wizardId -> viewModel.performWizardGenerateMana(wizardId) },
                        onBuildBarricade = { towerId, barricadePos -> viewModel.performBuildBarricade(towerId, barricadePos) },
                        onRemoveBarricade = { barricadePos -> viewModel.performRemoveBarricade(barricadePos) },
                        cheatDigOutcome = cheatDigOutcome,
                        onClearCheatDigOutcome = { viewModel.clearCheatDigOutcome() },
                        showPlatformInfo = showPlatformInfo,
                        onClearPlatformInfo = { viewModel.clearPlatformInfo() },
                        showCheatHelp = showCheatHelp,
                        onClearCheatHelp = { viewModel.clearCheatHelp() },
                        hasUnsavedChanges = { viewModel.hasUnsavedChanges() },
                        specialActionsRemaining = specialActionsRemaining,
                        onClearSpecialActionsWarning = { viewModel.clearSpecialActionsWarning() },
                        reminderMessage = reminderMessage,
                        onClearReminderMessage = { viewModel.clearReminderMessage() },
                        // Magic panel callbacks
                        showMagicPanel = showMagicPanel,
                        playerStats = currentPlayer?.abilities ?: de.egril.defender.model.PlayerAbilities(),
                        selectedSpell = selectedSpell,
                        onOpenMagicPanel = { viewModel.openMagicPanel() },
                        onCloseMagicPanel = { viewModel.closeMagicPanel() },
                        onCastSpell = { spell -> viewModel.setPendingSpell(spell) },
                        onCancelInstantTowerSpell = { viewModel.cancelInstantTowerSpell() },
                        pendingSpellCast = pendingSpellCast,
                        onConfirmSpellCast = {
                            viewModel.pendingSpellCast.value?.let { spell ->
                                viewModel.castSpell(spell)
                            }
                        },
                        onCancelSpellCast = { viewModel.cancelPendingSpell() },
                        onSelectSpellTarget = { target -> viewModel.selectSpellTarget(target) },
                        onExitSpellTargeting = { viewModel.exitSpellTargetingMode() },
                        // Post-target confirmation callbacks
                        showSpellTargetConfirmation = showSpellTargetConfirmation,
                        onConfirmTargetSpell = { viewModel.confirmSpellCast() },
                        onDismissTargetConfirmation = { viewModel.dismissSpellConfirmation() },
                        showFreezeImmuneWarning = showFreezeImmuneWarning,
                        onDismissFreezeWarning = { viewModel.dismissFreezeImmuneWarning() },
                        scrollToPosition = pendingScrollToPosition,
                        onScrollToPositionConsumed = { viewModel.clearPendingScrollPosition() },
                        pendingGameMessage = pendingGameMessage,
                        onDismissGameMessage = { viewModel.dismissGameMessage() }
                    )
                }
            }
            
            is Screen.LevelComplete -> {
                LevelCompleteScreen(
                    levelId = screen.levelId,
                    won = screen.won,
                    isLastLevel = screen.isLastLevel,
                    xpEarned = screen.xpEarned,
                    onRestart = { viewModel.restartLevel() },
                    onBackToMap = { viewModel.navigateToWorldMap() },
                    onShowFinalCredits = if (screen.isLastLevel && screen.won) {
                        { viewModel.navigateToFinalCredits() }
                    } else {
                        null
                    }
                )
            }
            
            is Screen.FinalCredits -> {
                FinalCreditsScreen(
                    onDismiss = { viewModel.navigateToWorldMap() }
                )
            }
            
            is Screen.Sticker -> {
                StickerScreen(
                    onBack = { viewModel.navigateToWorldMap() }
                )
            }
            
            is Screen.LoadingSpinnerDemo -> {
                LaunchedEffect(Unit) {
                    delay(30_000L)
                    viewModel.navigateToWorldMap()
                }
                LevelLoadingScreen(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
