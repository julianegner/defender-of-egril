package de.egril.defender.ui.gameplay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.egril.defender.model.*
import de.egril.defender.ui.CheatCodeDialog
import de.egril.defender.ui.getGameplayUIScale

@Composable
fun GamePlayScreen(
    gameState: GameState,
    onPlaceDefender: (DefenderType, Position) -> Boolean,
    onUpgradeDefender: (Int) -> Boolean,
    onUndoTower: (Int) -> Boolean,
    onSellTower: (Int) -> Boolean,
    onStartFirstPlayerTurn: () -> Unit,
    onDefenderAttack: (Int, Int) -> Boolean,
    onDefenderAttackPosition: (Int, Position) -> Boolean,
    onEndPlayerTurn: () -> Unit,
    onAutoAttackAndEndTurn: () -> Unit,  // Add auto-attack callback
    onBackToMap: () -> Unit,
    onSaveGame: ((String?) -> String?)? = null,  // Add save game callback with optional comment
    onCheatCode: ((String) -> Boolean)? = null,  // Add cheat code callback
    onMineDig: ((Int) -> DigOutcome?)? = null,  // Add mine dig callback
    onMineBuildTrap: ((Int, Position) -> Boolean)? = null,  // Add mine build trap callback
    onWizardPlaceMagicalTrap: ((Int, Position) -> Boolean)? = null,  // Add wizard magical trap callback
    cheatDigOutcome: DigOutcome? = null,  // Dig outcome from cheat code
    onClearCheatDigOutcome: (() -> Unit)? = null,  // Callback to clear cheat dig outcome
    showPlatformInfo: Boolean = false,  // Show platform info from cheat code
    onClearPlatformInfo: (() -> Unit)? = null,  // Callback to clear platform info
    hasUnsavedChanges: (() -> Boolean)? = null  // Callback to check for unsaved changes
) {
    GamePlayScreenContent(
        gameState = gameState,
        onPlaceDefender = onPlaceDefender,
        onUpgradeDefender = onUpgradeDefender,
        onUndoTower = onUndoTower,
        onSellTower = onSellTower,
        onStartFirstPlayerTurn = onStartFirstPlayerTurn,
        onDefenderAttack = onDefenderAttack,
        onDefenderAttackPosition = onDefenderAttackPosition,
        onEndPlayerTurn = onEndPlayerTurn,
        onAutoAttackAndEndTurn = onAutoAttackAndEndTurn,
        onBackToMap = onBackToMap,
        onSaveGame = onSaveGame,
        onCheatCode = onCheatCode,
        onMineDig = onMineDig,
        onMineBuildTrap = onMineBuildTrap,
        onWizardPlaceMagicalTrap = onWizardPlaceMagicalTrap,
        cheatDigOutcome = cheatDigOutcome,
        onClearCheatDigOutcome = onClearCheatDigOutcome,
        showPlatformInfo = showPlatformInfo,
        onClearPlatformInfo = onClearPlatformInfo,
        hasUnsavedChanges = hasUnsavedChanges
    )
}

@Composable
private fun GamePlayScreenContent(
    gameState: GameState,
    onPlaceDefender: (DefenderType, Position) -> Boolean,
    onUpgradeDefender: (Int) -> Boolean,
    onUndoTower: (Int) -> Boolean,
    onSellTower: (Int) -> Boolean,
    onStartFirstPlayerTurn: () -> Unit,
    onDefenderAttack: (Int, Int) -> Boolean,
    onDefenderAttackPosition: (Int, Position) -> Boolean,
    onEndPlayerTurn: () -> Unit,
    onAutoAttackAndEndTurn: () -> Unit,  // Add auto-attack callback
    onBackToMap: () -> Unit,
    onSaveGame: ((String?) -> String?)? = null,  // Add save game callback with optional comment
    onCheatCode: ((String) -> Boolean)? = null,
    onMineDig: ((Int) -> DigOutcome?)? = null,
    onMineBuildTrap: ((Int, Position) -> Boolean)? = null,
    onWizardPlaceMagicalTrap: ((Int, Position) -> Boolean)? = null,  // Add wizard magical trap callback
    cheatDigOutcome: DigOutcome? = null,  // Dig outcome from cheat code
    onClearCheatDigOutcome: (() -> Unit)? = null,  // Callback to clear cheat dig outcome
    showPlatformInfo: Boolean = false,  // Show platform info from cheat code
    onClearPlatformInfo: (() -> Unit)? = null,  // Callback to clear platform info
    hasUnsavedChanges: (() -> Boolean)? = null  // Callback to check for unsaved changes

) {
    var selectedDefenderType by remember { mutableStateOf<DefenderType?>(null) }
    var selectedDefenderId by remember { mutableStateOf<Int?>(null) }
    var selectedAttackerId by remember { mutableStateOf<Int?>(null) }  // Add enemy selection
    var selectedTargetId by remember { mutableStateOf<Int?>(null) }
    var selectedTargetPosition by remember { mutableStateOf<Position?>(null) }
    var showCheatDialog by remember { mutableStateOf(false) }
    var cheatCodeInput by remember { mutableStateOf("") }
    var showMineActionDialog by remember { mutableStateOf(false) }
    var selectedMineAction by remember { mutableStateOf<MineAction?>(null) }
    var selectedWizardAction by remember { mutableStateOf<WizardAction?>(null) }  // For wizard magical trap placement
    var currentDigOutcome by remember { mutableStateOf<DigOutcome?>(null) }
    var currentDragonName by remember { mutableStateOf<String?>(null) }  // Track dragon name for dig outcome
    var showDigOutcomeDialog by remember { mutableStateOf(false) }
    var showOverlay by remember { mutableStateOf(false) }  // MutableState for overlay visibility
    var showSaveDialog by remember { mutableStateOf(false) }  // Save dialog with comment
    var saveCommentInput by remember { mutableStateOf("") }  // Comment input for save
    var showSaveConfirmation by remember { mutableStateOf(false) }  // Save confirmation
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }  // Unsaved changes dialog
    var showEndTurnConfirmation by remember { mutableStateOf(false) }  // End turn confirmation dialog
    
    // Check if unsaved changes feature is enabled (both hasUnsavedChanges and onSaveGame must be available)
    val unsavedChangesEnabled = hasUnsavedChanges != null && onSaveGame != null

    // Get platform-specific UI scale for mobile (affects layout, not just rendering)
    val uiScale = getGameplayUIScale()

    // Create a scaled density with separate scaling for layout (dp) and text (sp)
    // Layout elements (padding, spacing) scaled to 0.5x to save space
    // Text/icons scaled to 1.5x (doubled from 0.75x) for better readability on mobile
    val density = LocalDensity.current
    val scaledDensity = remember(density, uiScale) {
        // Scale layout (dp) by uiScale, but scale text (sp) to be larger
        val textScale = if (uiScale < 1f) {
            // For mobile, use 1.5x for text (doubled from previous 0.75x)
            // This means text will be at ~original size despite layout being 0.5x
            1.5f
        } else {
            1f // Desktop unchanged
        }
        Density(density.density * uiScale, density.fontScale * textScale)
    }
    
    // Watch for cheat dig outcomes
    LaunchedEffect(cheatDigOutcome) {
        if (cheatDigOutcome != null) {
            currentDigOutcome = cheatDigOutcome
            showDigOutcomeDialog = true
            onClearCheatDigOutcome?.invoke()
        }
    }
    
    // Background music management based on health points
    LaunchedEffect(gameState.healthPoints.value) {
        val currentMusic = de.egril.defender.audio.GlobalBackgroundMusicManager.getInstance()?.getCurrentMusic()
        
        if (gameState.healthPoints.value < 5) {
            // Switch to low health music if not already playing
            if (currentMusic != de.egril.defender.audio.BackgroundMusic.GAMEPLAY_LOW_HEALTH) {
                de.egril.defender.audio.GlobalBackgroundMusicManager.playMusic(
                    de.egril.defender.audio.BackgroundMusic.GAMEPLAY_LOW_HEALTH,
                    loop = true
                )
            }
        } else {
            // Play normal gameplay music if not already playing
            if (currentMusic != de.egril.defender.audio.BackgroundMusic.GAMEPLAY_NORMAL) {
                de.egril.defender.audio.GlobalBackgroundMusicManager.playMusic(
                    de.egril.defender.audio.BackgroundMusic.GAMEPLAY_NORMAL,
                    loop = true
                )
            }
        }
    }
    
    // Stop background music when leaving gameplay
    DisposableEffect(Unit) {
        onDispose {
            de.egril.defender.audio.GlobalBackgroundMusicManager.stopMusic()
        }
    }
    
    // Check for dragon-related infos and show appropriate tutorial
    LaunchedEffect(gameState.attackers.size, gameState.infoState.value) {
        val infoState = gameState.infoState.value
        
        // Skip if already showing an info
        if (infoState.currentInfo != InfoType.NONE) {
            return@LaunchedEffect
        }
        
        // Check for dragons and show appropriate info
        val dragons = gameState.attackers.filter { it.type.isDragon && !it.isDefeated.value }
        
        if (dragons.isNotEmpty()) {
            // Priority: Very greedy > Greed > Dragon info
            val veryGreedyDragon = dragons.any { it.greed > 5 }
            val greedyDragon = dragons.any { it.greed > 0 }
            
            when {
                veryGreedyDragon && !infoState.hasSeen(InfoType.VERY_GREEDY_INFO) -> {
                    gameState.infoState.value = infoState.showInfo(InfoType.VERY_GREEDY_INFO)
                }
                greedyDragon && !infoState.hasSeen(InfoType.GREED_INFO) -> {
                    gameState.infoState.value = infoState.showInfo(InfoType.GREED_INFO)
                }
                !infoState.hasSeen(InfoType.DRAGON_INFO) -> {
                    gameState.infoState.value = infoState.showInfo(InfoType.DRAGON_INFO)
                }
            }
        }
    }
    
    // Witch info popups
    LaunchedEffect(gameState.attackers.size, gameState.infoState.value) {
        val infoState = gameState.infoState.value
        
        // Skip if already showing an info
        if (infoState.currentInfo != InfoType.NONE) {
            return@LaunchedEffect
        }
        
        // Check for green witches on the field
        val greenWitches = gameState.attackers.filter { 
            it.type == AttackerType.GREEN_WITCH && !it.isDefeated.value 
        }
        if (greenWitches.isNotEmpty() && !infoState.hasSeen(InfoType.GREEN_WITCH_INFO)) {
            gameState.infoState.value = infoState.showInfo(InfoType.GREEN_WITCH_INFO)
            return@LaunchedEffect
        }
        
        // Check for red witches on the field
        val redWitches = gameState.attackers.filter { 
            it.type == AttackerType.RED_WITCH && !it.isDefeated.value 
        }
        if (redWitches.isNotEmpty() && !infoState.hasSeen(InfoType.RED_WITCH_INFO)) {
            gameState.infoState.value = infoState.showInfo(InfoType.RED_WITCH_INFO)
        }
    }
    
    // Check for mine warnings
    LaunchedEffect(gameState.mineWarnings.size, gameState.infoState.value) {
        val infoState = gameState.infoState.value
        
        // Skip if already showing an info
        if (infoState.currentInfo != InfoType.NONE) {
            return@LaunchedEffect
        }
        
        // Show mine warning if there are warnings and not currently showing one
        if (gameState.mineWarnings.isNotEmpty()) {
            val mineId = gameState.mineWarnings.first()
            gameState.infoState.value = infoState.showInfo(InfoType.MINE_WARNING, mineId)
        }
    }
    
    // Check for 1 HP warning at the start of the level
    LaunchedEffect(gameState.level.healthPoints) {
        val infoState = gameState.infoState.value
        
        // Skip if already showing an info or already seen
        if (infoState.currentInfo != InfoType.NONE || infoState.hasSeen(InfoType.ONE_HP_WARNING)) {
            return@LaunchedEffect
        }
        
        // Show warning if level starts with only 1 HP
        if (gameState.level.healthPoints == 1) {
            gameState.infoState.value = infoState.showInfo(InfoType.ONE_HP_WARNING)
        }
    }

    // Check for first-time tower availability and show info dialogs
    LaunchedEffect(gameState.level.availableTowers, gameState.infoState.value) {
        val infoState = gameState.infoState.value

        // Skip if already showing an info
        if (infoState.currentInfo != InfoType.NONE) {
            return@LaunchedEffect
        }

        // Check each advanced tower type and show info if available but not seen
        val availableTowers = gameState.level.availableTowers

        when {
            availableTowers.contains(DefenderType.WIZARD_TOWER) && !infoState.hasSeen(InfoType.WIZARD_FIRST_USE) -> {
                gameState.infoState.value = infoState.showInfo(InfoType.WIZARD_FIRST_USE)
            }
            availableTowers.contains(DefenderType.ALCHEMY_TOWER) && !infoState.hasSeen(InfoType.ALCHEMY_FIRST_USE) -> {
                gameState.infoState.value = infoState.showInfo(InfoType.ALCHEMY_FIRST_USE)
            }
            availableTowers.contains(DefenderType.BALLISTA_TOWER) && !infoState.hasSeen(InfoType.BALLISTA_FIRST_USE) -> {
                gameState.infoState.value = infoState.showInfo(InfoType.BALLISTA_FIRST_USE)
            }
            availableTowers.contains(DefenderType.DWARVEN_MINE) && !infoState.hasSeen(InfoType.MINE_FIRST_USE) -> {
                gameState.infoState.value = infoState.showInfo(InfoType.MINE_FIRST_USE)
            }
        }
    }

    // Check for river tiles and show river mechanics info (first time only)
    LaunchedEffect(gameState.level.id) {
        val infoState = gameState.infoState.value

        // Skip if already showing an info or already seen river info
        if (infoState.currentInfo != InfoType.NONE || infoState.hasSeen(InfoType.RIVER_INFO)) {
            return@LaunchedEffect
        }

        // Show info if level has river tiles
        if (gameState.level.riverTiles.isNotEmpty()) {
            gameState.infoState.value = infoState.showInfo(InfoType.RIVER_INFO)
        }
    }

    // Mine action handler
    val handleMineAction: (Int, MineAction) -> Unit = { mineId, action ->
        when (action) {
            MineAction.DIG -> {
                val outcome = onMineDig?.invoke(mineId)
                if (outcome != null) {
                    currentDigOutcome = outcome
                    // If a dragon was spawned, find the newly created lair and get the dragon's name
                    if (outcome == DigOutcome.DRAGON) {
                        val newLair = gameState.defenders.lastOrNull { it.type == DefenderType.DRAGONS_LAIR }
                        currentDragonName = newLair?.dragonName
                    } else {
                        currentDragonName = null
                    }
                    showDigOutcomeDialog = true
                }
            }

            MineAction.BUILD_TRAP -> {
                selectedMineAction = action
                showMineActionDialog = true
            }
        }
    }
    
    // Wizard action handler - similar to mine action, click button first then select on map
    val handleWizardAction: (Int, WizardAction) -> Unit = { wizardId, action ->
        when (action) {
            WizardAction.PLACE_MAGICAL_TRAP -> {
                selectedWizardAction = action
                // The user will now click on the map to place the trap
            }
        }
    }
    
    // Keyboard event handler for Ctrl+S save shortcut
    // Using onPreviewKeyEvent to intercept before HexagonalMapView handles it
    // This works in the "capture" phase and doesn't require focus on this element
    val keyboardHandler: (KeyEvent) -> Boolean = remember(onSaveGame) {
        { event ->
            if (event.type == KeyEventType.KeyDown && 
                event.key == Key.S && 
                event.isCtrlPressed &&
                onSaveGame != null) {
                // Trigger save dialog
                showSaveDialog = true
                true
            } else {
                false
            }
        }
    }

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent(keyboardHandler),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
        // Header with prominent phase indicator (collapsible)
        GameHeader(
            gameState = gameState,
            showOverlay = showOverlay,
            onShowOverlayChange = { showOverlay = it },
            onBackToMap = {
                // Check for unsaved changes before navigating back
                if (unsavedChangesEnabled && hasUnsavedChanges!!.invoke()) {
                    showUnsavedChangesDialog = true
                } else {
                    onBackToMap()
                }
            },
            onSaveGame = if (onSaveGame != null) {{ showSaveDialog = true }} else null,
            onCheatCode = if (onCheatCode != null) {{ showCheatDialog = true }} else null
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Game Grid with toggle button and overlay
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Scrollable Game Grid
            GameGrid(
                gameState = gameState,
                selectedDefenderType = selectedDefenderType,
                selectedDefenderId = selectedDefenderId,
                selectedTargetId = selectedTargetId,
                selectedTargetPosition = selectedTargetPosition,
                selectedMineAction = selectedMineAction,
                selectedWizardAction = selectedWizardAction,
                onCellClick = { position ->
                    // Try to place defender if one is selected
                    selectedDefenderType?.let { type ->
                        if (onPlaceDefender(type, position)) {
                            selectedDefenderType = null
                            // Track tutorial progress
                            if (gameState.tutorialState.value.isActive && 
                                !gameState.tutorialState.value.hasPlacedFirstTower) {
                                gameState.tutorialState.value = gameState.tutorialState.value.markTowerPlaced()
                            }
                        }
                        return@GameGrid
                    }

                    val previousSelectedDefenderId = selectedDefenderId
                    val previousSelectedAttackerId = selectedAttackerId
                    
                    // Check if there's a defender at this position
                    val defender = gameState.defenders.find { it.position.value == position }
                    if (defender != null) {
                        if (previousSelectedDefenderId == defender.id) {
                            // Deselect if clicking the same defender
                            selectedDefenderId = null
                        } else {
                            // Select this defender, deselect any selected attacker
                            selectedDefenderId = defender.id
                            selectedAttackerId = null
                            selectedTargetId = null
                            selectedTargetPosition = null
                            return@GameGrid
                        }
                    }
                    
                    // Check if there's an attacker at this position (only if no defender is being placed)
                    val attacker = gameState.attackers.find { it.position.value == position && !it.isDefeated.value }
                    if (attacker != null && selectedDefenderId == null) {
                        if (previousSelectedAttackerId == attacker.id) {
                            // Deselect if clicking the same attacker
                            selectedAttackerId = null
                        } else {
                            // Select this attacker, deselect any selected defender
                            selectedAttackerId = attacker.id
                            selectedDefenderId = null
                            selectedTargetId = null
                            selectedTargetPosition = null
                            return@GameGrid
                        }
                    }

                    // Handle targeting for selected defender
                    if (selectedDefenderId != null) {
                        val selectedDefender = gameState.defenders.find { it.id == selectedDefenderId }
                        if (selectedDefender != null) {
                            // Handle trap building for mines
                            if (selectedDefender.type == DefenderType.DWARVEN_MINE && selectedMineAction == MineAction.BUILD_TRAP) {
                                // Check if position is on the path and in range
                                val distance = selectedDefender.position.value.distanceTo(position)
                                if (gameState.level.isOnPath(position) && distance <= selectedDefender.range) {
                                    if (onMineBuildTrap?.invoke(selectedDefender.id, position) == true) {
                                        selectedMineAction = null
                                        showMineActionDialog = false
                                    }
                                }
                                return@GameGrid
                            }
                            
                            // Handle magical trap placement for wizard towers (level 10+)
                            if (selectedDefender.type == DefenderType.WIZARD_TOWER && 
                                selectedDefender.level.value >= 10 && 
                                selectedWizardAction == WizardAction.PLACE_MAGICAL_TRAP) {
                                // Check if position is on the path and in range
                                val distance = selectedDefender.position.value.distanceTo(position)
                                val hasEnemy = gameState.attackers.any { it.position.value == position && !it.isDefeated.value }
                                val hasTrap = gameState.traps.any { it.position == position }
                                if (gameState.level.isOnPath(position) && 
                                    distance <= selectedDefender.range && 
                                    !hasEnemy && 
                                    !hasTrap) {
                                    if (onWizardPlaceMagicalTrap?.invoke(selectedDefender.id, position) == true) {
                                        selectedWizardAction = null
                                    }
                                }
                                return@GameGrid
                            }

                            // For AREA/LASTING (fireball and acid) attacks, allow targeting path tiles OR river tiles
                            if (selectedDefender.type.attackType == AttackType.AREA ||
                                selectedDefender.type.attackType == AttackType.LASTING
                            ) {
                                // Check if position is on the path or river and in range
                                val distance = selectedDefender.position.value.distanceTo(position)
                                val isOnPath = gameState.level.isOnPath(position)
                                val isOnRiver = gameState.level.getRiverTile(position) != null

                                if ((isOnPath || isOnRiver) &&
                                    distance >= selectedDefender.type.minRange &&
                                    distance <= selectedDefender.range
                                ) {
                                    selectedTargetPosition = position
                                    // Also set targetId if there's an enemy at this position
                                    val enemyAtPosition =
                                        gameState.attackers.find { it.position.value == position && !it.isDefeated.value }
                                    selectedTargetId = enemyAtPosition?.id
                                }
                            } else {
                                // For single-target attacks, allow targeting enemies or bridges
                                val distance = selectedDefender.position.value.distanceTo(position)
                                val attackerForTargeting =
                                    gameState.attackers.find { it.position.value == position && !it.isDefeated.value }
                                val bridgeAtPosition = gameState.getBridgeAt(position)

                                if (distance >= selectedDefender.type.minRange && distance <= selectedDefender.range) {
                                    if (attackerForTargeting != null) {
                                        selectedTargetId = attackerForTargeting.id
                                        selectedTargetPosition = position // to be able to show the 3 circles to highlight the target
                                    } else if (bridgeAtPosition != null && bridgeAtPosition.isActive) {
                                        // Allow targeting bridge tiles
                                        selectedTargetId = null  // Bridges don't have attacker IDs
                                        selectedTargetPosition = position
                                    }
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Overlay panel with Legend and Enemy List (conditionally shown)
            if (showOverlay) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .width(250.dp)
                        .fillMaxHeight()
                        .padding(8.dp)
                ) {
                    // Legend
                    GameLegend(modifier = Modifier.fillMaxWidth())

                    Spacer(modifier = Modifier.height(16.dp))

                    // Enemy List
                    EnemyListPanel(gameState = gameState, modifier = Modifier.fillMaxWidth().weight(1f))
                }
            }
            
            // Tutorial card (positioned in upper right corner)
            if (gameState.tutorialState.value.shouldShowOverlay() || gameState.infoState.value.shouldShowOverlay()) {
                // Check if we should allow skipping attack step
                // (tower has no actions left or can't reach any enemies)
                if (gameState.tutorialState.value.currentStep == TutorialStep.ATTACKING &&
                    !gameState.tutorialState.value.canSkipAttacking) {
                    val hasTowerWithActions = gameState.defenders.any { defender ->
                        defender.actionsRemaining.value > 0 && defender.buildTimeRemaining.value == 0
                    }
                    val selectedDefender = selectedDefenderId?.let { id ->
                        gameState.defenders.find { it.id == id }
                    }
                    val canReachEnemies = selectedDefender?.let { defender ->
                        gameState.attackers.any { attacker ->
                            !attacker.isDefeated.value &&
                            defender.position.value.distanceTo(attacker.position.value) <= defender.range
                        }
                    } ?: false
                    
                    // Allow skipping if no tower has actions or selected tower can't reach enemies
                    if (!hasTowerWithActions || (selectedDefender != null && !canReachEnemies)) {
                        gameState.tutorialState.value = gameState.tutorialState.value.allowSkipAttacking()
                    }
                }
                
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    // Show info or tutorial in the tutorial overlay
                    TutorialOverlay(
                        currentStep = gameState.tutorialState.value.currentStep,
                        isNextEnabled = gameState.tutorialState.value.isNextEnabled(),
                        onNext = {
                            val currentTutorialState = gameState.tutorialState.value
                            gameState.tutorialState.value = currentTutorialState.advanceStep()
                        },
                        onSkip = {
                            gameState.tutorialState.value = gameState.tutorialState.value.skip()
                        },
                        currentInfo = gameState.infoState.value.currentInfo,
                        onDismissInfo = {
                            val currentInfoState = gameState.infoState.value
                            val dismissedInfo = currentInfoState.dismissInfo()
                            gameState.infoState.value = dismissedInfo
                            
                            // Remove mine warning from the list if it was a mine warning
                            if (currentInfoState.currentInfo == InfoType.MINE_WARNING) {
                                currentInfoState.mineWarningId?.let { gameState.mineWarnings.remove(it) }
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Control Panel based on phase
        when (gameState.phase.value) {
            GamePhase.INITIAL_BUILDING -> {
                GameControlsPanel(
                    phase = GamePhase.INITIAL_BUILDING,
                    gameState = gameState,
                    coinsState = gameState.coins,
                    selectedDefenderType = selectedDefenderType,
                    selectedDefenderId = selectedDefenderId,
                    selectedAttackerId = selectedAttackerId,
                    selectedTargetId = null,
                    selectedTargetPosition = null,
                    onSelectDefenderType = { selectedDefenderType = it },
                    onUpgradeDefender = { onUpgradeDefender(it) },
                    onUndoTower = { defenderId ->
                        if (onUndoTower(defenderId)) {
                            selectedDefenderType = null
                            selectedDefenderId = null
                        }
                    },
                    onSellTower = { defenderId ->
                        if (onSellTower(defenderId)) {
                            selectedDefenderType = null
                            selectedDefenderId = null
                        }
                    },
                    onDefenderAttack = { _, _ -> false },
                    onDefenderAttackPosition = { _, _ -> false },
                    onPrimaryAction = {
                        selectedDefenderType = null  // Clear defender type selection when starting battle
                        selectedDefenderId = null  // Clear defender selection when starting battle
                        selectedAttackerId = null  // Clear attacker selection when starting battle
                        onStartFirstPlayerTurn()
                        // Track tutorial progress and auto-advance START_COMBAT step
                        if (gameState.tutorialState.value.isActive) {
                            if (!gameState.tutorialState.value.hasStartedFirstTurn) {
                                gameState.tutorialState.value = gameState.tutorialState.value.markTurnStarted()
                            }
                            // Auto-advance if currently showing START_COMBAT step
                            if (gameState.tutorialState.value.currentStep == TutorialStep.START_COMBAT) {
                                gameState.tutorialState.value = gameState.tutorialState.value.advanceStep()
                            }
                        }
                    },
                    onMineAction = handleMineAction,
                    onWizardAction = handleWizardAction,
                    uiScale = uiScale,
                    onShowDragonInfo = { 
                        gameState.infoState.value = gameState.infoState.value.showInfo(InfoType.DRAGON_INFO)
                    }
                )
            }

            GamePhase.PLAYER_TURN -> {
                GameControlsPanel(
                    phase = GamePhase.PLAYER_TURN,
                    gameState = gameState,
                    coinsState = gameState.coins,
                    selectedDefenderType = selectedDefenderType,
                    selectedDefenderId = selectedDefenderId,
                    selectedAttackerId = selectedAttackerId,
                    selectedTargetId = selectedTargetId,
                    selectedTargetPosition = selectedTargetPosition,
                    onSelectDefenderType = { selectedDefenderType = it },
                    onUpgradeDefender = { onUpgradeDefender(it) },
                    onUndoTower = { defenderId ->
                        if (onUndoTower(defenderId)) {
                            selectedDefenderType = null
                            selectedDefenderId = null
                        }
                    },
                    onSellTower = { defenderId ->
                        if (onSellTower(defenderId)) {
                            selectedDefenderType = null
                            selectedDefenderId = null
                        }
                    },
                    onDefenderAttack = { defenderId, targetId ->
                        if (onDefenderAttack(defenderId, targetId)) {
                            selectedTargetId = null
                            selectedTargetPosition = null
                            // Track tutorial progress
                            if (gameState.tutorialState.value.isActive && 
                                !gameState.tutorialState.value.hasAttackedEnemy) {
                                gameState.tutorialState.value = gameState.tutorialState.value.markAttackedEnemy()
                            }
                            true
                        } else {
                            false
                        }
                    },
                    onDefenderAttackPosition = { defenderId, targetPos ->
                        if (onDefenderAttackPosition(defenderId, targetPos)) {
                            selectedTargetId = null
                            selectedTargetPosition = null
                            // Track tutorial progress
                            if (gameState.tutorialState.value.isActive && 
                                !gameState.tutorialState.value.hasAttackedEnemy) {
                                gameState.tutorialState.value = gameState.tutorialState.value.markAttackedEnemy()
                            }
                            true
                        } else {
                            false
                        }
                    },
                    onPrimaryAction = {
                        // Check if there are unused action points before ending turn
                        if (gameState.hasDefendersWithUnusedActions()) {
                            // Show confirmation dialog
                            showEndTurnConfirmation = true
                        } else {
                            // End turn directly
                            onEndPlayerTurn()
                            // Track tutorial progress
                            if (gameState.tutorialState.value.isActive && 
                                !gameState.tutorialState.value.hasStartedFirstTurn) {
                                gameState.tutorialState.value = gameState.tutorialState.value.markTurnStarted()
                            }
                        }
                    },
                    onMineAction = handleMineAction,
                    onWizardAction = handleWizardAction,
                    uiScale = uiScale,
                    onShowDragonInfo = { 
                        gameState.infoState.value = gameState.infoState.value.showInfo(InfoType.DRAGON_INFO)
                    }
                )
            }

            GamePhase.ENEMY_TURN -> {
                EnemyTurnInfo()
            }
        }

        // Dig outcome dialog
        if (showDigOutcomeDialog && currentDigOutcome != null) {
            DigOutcomeDialog(
                outcome = currentDigOutcome!!,
                onDismiss = { showDigOutcomeDialog = false },
                onResetSelections = {
                    selectedDefenderType = null
                    selectedDefenderId = null
                },
                dragonName = currentDragonName
            )
        }
        
        // Save game dialog (with optional comment input)
        if (showSaveDialog && onSaveGame != null) {
            SaveGameDialog(
                saveCommentInput = saveCommentInput,
                onSaveCommentChange = { saveCommentInput = it },
                onSave = { comment ->
                    onSaveGame(comment)
                    showSaveDialog = false
                    saveCommentInput = ""
                    showSaveConfirmation = true
                },
                onDismiss = {
                    showSaveDialog = false
                    saveCommentInput = ""
                }
            )
        }

        // Save confirmation dialog
        if (showSaveConfirmation) {
            SaveConfirmationDialog(
                onDismiss = { showSaveConfirmation = false }
            )
        }

        // Cheat code dialog
        if (showCheatDialog && onCheatCode != null) {
            CheatCodeDialog(
                onDismiss = {
                    showCheatDialog = false
                    cheatCodeInput = ""
                },
                onApplyCheatCode = onCheatCode,
                showHints = true,
                initialInput = cheatCodeInput,
                onInputChange = { cheatCodeInput = it }
            )
        }
        
        // Platform info dialog (from platform cheat code)
        if (showPlatformInfo && onClearPlatformInfo != null) {
            de.egril.defender.ui.PlatformInfoDialog(
                platformInfo = de.egril.defender.utils.getPlatform().name,
                onDismiss = onClearPlatformInfo
            )
        }
        
        // Unsaved changes dialog
        if (showUnsavedChangesDialog && unsavedChangesEnabled) {
            UnsavedChangesDialog(
                onSaveAndExit = {
                    // Save the game first
                    onSaveGame!!(null)
                    showUnsavedChangesDialog = false
                    // Then navigate back to map
                    onBackToMap()
                },
                onDiscardChanges = {
                    showUnsavedChangesDialog = false
                    // Navigate back without saving
                    onBackToMap()
                },
                onCancel = {
                    // Just close the dialog and stay in the game
                    showUnsavedChangesDialog = false
                }
            )
        }
        
        // End turn confirmation dialog
        if (showEndTurnConfirmation) {
            EndTurnConfirmationDialog(
                onConfirm = {
                    showEndTurnConfirmation = false
                    onEndPlayerTurn()
                    // Track tutorial progress
                    if (gameState.tutorialState.value.isActive && 
                        !gameState.tutorialState.value.hasStartedFirstTurn) {
                        gameState.tutorialState.value = gameState.tutorialState.value.markTurnStarted()
                    }
                },
                onAutoAttackAndConfirm = {
                    showEndTurnConfirmation = false
                    onAutoAttackAndEndTurn()
                    // Track tutorial progress
                    if (gameState.tutorialState.value.isActive && 
                        !gameState.tutorialState.value.hasStartedFirstTurn) {
                        gameState.tutorialState.value = gameState.tutorialState.value.markTurnStarted()
                    }
                },
                onCancel = {
                    showEndTurnConfirmation = false
                },
                showAutoAttackButton = gameState.level.allowAutoAttack
            )
        }
        }
    }
    }
}
