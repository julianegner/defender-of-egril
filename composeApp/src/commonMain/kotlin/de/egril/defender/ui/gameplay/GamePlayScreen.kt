package de.egril.defender.ui.gameplay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onBackToMap: () -> Unit,
    onSaveGame: ((String?) -> String?)? = null,  // Add save game callback with optional comment
    onCheatCode: ((String) -> Boolean)? = null,  // Add cheat code callback
    onMineDig: ((Int) -> DigOutcome?)? = null,  // Add mine dig callback
    onMineBuildTrap: ((Int, Position) -> Boolean)? = null,  // Add mine build trap callback
    cheatDigOutcome: DigOutcome? = null,  // Dig outcome from cheat code
    onClearCheatDigOutcome: (() -> Unit)? = null  // Callback to clear cheat dig outcome
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
        onBackToMap = onBackToMap,
        onSaveGame = onSaveGame,
        onCheatCode = onCheatCode,
        onMineDig = onMineDig,
        onMineBuildTrap = onMineBuildTrap,
        cheatDigOutcome = cheatDigOutcome,
        onClearCheatDigOutcome = onClearCheatDigOutcome
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
    onBackToMap: () -> Unit,
    onSaveGame: ((String?) -> String?)? = null,  // Add save game callback with optional comment
    onCheatCode: ((String) -> Boolean)? = null,
    onMineDig: ((Int) -> DigOutcome?)? = null,
    onMineBuildTrap: ((Int, Position) -> Boolean)? = null,
    cheatDigOutcome: DigOutcome? = null,  // Dig outcome from cheat code
    onClearCheatDigOutcome: (() -> Unit)? = null  // Callback to clear cheat dig outcome

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
    var currentDigOutcome by remember { mutableStateOf<DigOutcome?>(null) }
    var currentDragonName by remember { mutableStateOf<String?>(null) }  // Track dragon name for dig outcome
    var showDigOutcomeDialog by remember { mutableStateOf(false) }
    var showDragonInfoDialog by remember { mutableStateOf(false) }  // Dragon info tutorial
    var showGreedInfoDialog by remember { mutableStateOf(false) }  // Dragon greed tutorial (greed > 0)
    var showVeryGreedyInfoDialog by remember { mutableStateOf(false) }  // Dragon very greedy tutorial (greed > 5)
    var showMineWarningDialog by remember { mutableStateOf(false) }  // Mine warning dialog
    var warningMineId by remember { mutableStateOf<Int?>(null) }  // Track which mine has warning
    var showOverlay by remember { mutableStateOf(false) }  // MutableState for overlay visibility
    var showSaveDialog by remember { mutableStateOf(false) }  // Save dialog with comment
    var saveCommentInput by remember { mutableStateOf("") }  // Comment input for save
    var showSaveConfirmation by remember { mutableStateOf(false) }  // Save confirmation

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
    
    // Check if a dragon exists and show info if it's the first time
    LaunchedEffect(gameState.attackers.size, gameState.hasSeenDragonInfo.value) {
        val hasDragon = gameState.attackers.any { it.type.isDragon && !it.isDefeated.value }
        if (hasDragon && !gameState.hasSeenDragonInfo.value) {
            showDragonInfoDialog = true
        }
    }
    
    // Check for dragon greed (greed > 0) and show tutorial
    LaunchedEffect(gameState.attackers.size, gameState.hasSeenGreedInfo.value) {
        val hasGreedyDragon = gameState.attackers.any { 
            it.type.isDragon && !it.isDefeated.value && it.greed > 0 
        }
        if (hasGreedyDragon && !gameState.hasSeenGreedInfo.value) {
            showGreedInfoDialog = true
        }
    }
    
    // Check for very greedy dragon (greed > 5) and show tutorial
    LaunchedEffect(gameState.attackers.size, gameState.hasSeenVeryGreedyInfo.value) {
        val hasVeryGreedyDragon = gameState.attackers.any { 
            it.type.isDragon && !it.isDefeated.value && it.greed > 5 
        }
        if (hasVeryGreedyDragon && !gameState.hasSeenVeryGreedyInfo.value) {
            showVeryGreedyInfoDialog = true
        }
    }
    
    // Check for mine warnings
    LaunchedEffect(gameState.mineWarnings.size) {
        if (gameState.mineWarnings.isNotEmpty()) {
            warningMineId = gameState.mineWarnings.first()
            showMineWarningDialog = true
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

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        Surface(
            modifier = Modifier.fillMaxSize(),
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
            onBackToMap = onBackToMap,
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
                    val defender = gameState.defenders.find { it.position == position }
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
                                val distance = selectedDefender.position.distanceTo(position)
                                if (gameState.level.isOnPath(position) && distance <= selectedDefender.range) {
                                    if (onMineBuildTrap?.invoke(selectedDefender.id, position) == true) {
                                        selectedMineAction = null
                                        showMineActionDialog = false
                                    }
                                }
                                return@GameGrid
                            }

                            // For AREA/LASTING (fireball and acid) attacks, allow targeting path tiles
                            if (selectedDefender.type.attackType == AttackType.AREA ||
                                selectedDefender.type.attackType == AttackType.LASTING
                            ) {
                                // Check if position is on the path and in range
                                val distance = selectedDefender.position.distanceTo(position)
                                if (gameState.level.isOnPath(position) &&
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
                                // For single-target attacks, only allow targeting enemies
                                val attackerForTargeting =
                                    gameState.attackers.find { it.position.value == position && !it.isDefeated.value }
                                if (attackerForTargeting != null) {
                                    selectedTargetId = attackerForTargeting.id
                                    selectedTargetPosition = position // to be able to show the 3 circles to highlight the target
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
            if (gameState.tutorialState.value.shouldShowOverlay() || showDragonInfoDialog || 
                showGreedInfoDialog || showVeryGreedyInfoDialog || showMineWarningDialog) {
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
                            defender.position.distanceTo(attacker.position.value) <= defender.range
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
                    // Show dragon info or tutorial in the tutorial overlay
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
                        showDragonInfo = showDragonInfoDialog,
                        onDismissDragonInfo = {
                            showDragonInfoDialog = false
                            gameState.hasSeenDragonInfo.value = true
                        },
                        showGreedInfo = showGreedInfoDialog,
                        onDismissGreedInfo = {
                            showGreedInfoDialog = false
                            gameState.hasSeenGreedInfo.value = true
                        },
                        showVeryGreedyInfo = showVeryGreedyInfoDialog,
                        onDismissVeryGreedyInfo = {
                            showVeryGreedyInfoDialog = false
                            gameState.hasSeenVeryGreedyInfo.value = true
                        },
                        showMineWarning = showMineWarningDialog,
                        onDismissMineWarning = {
                            showMineWarningDialog = false
                            warningMineId?.let { gameState.mineWarnings.remove(it) }
                            warningMineId = null
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
                    uiScale = uiScale
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
                        onEndPlayerTurn()
                        // Track tutorial progress
                        if (gameState.tutorialState.value.isActive && 
                            !gameState.tutorialState.value.hasStartedFirstTurn) {
                            gameState.tutorialState.value = gameState.tutorialState.value.markTurnStarted()
                        }
                    },
                    onMineAction = handleMineAction,
                    uiScale = uiScale
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
        }
    }
    }
}
