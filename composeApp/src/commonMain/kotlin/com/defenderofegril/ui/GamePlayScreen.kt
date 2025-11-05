package com.defenderofegril.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.defenderofegril.model.*
import com.defenderofegril.ui.gameplay.*

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
    var selectedTargetId by remember { mutableStateOf<Int?>(null) }
    var selectedTargetPosition by remember { mutableStateOf<Position?>(null) }
    var showCheatDialog by remember { mutableStateOf(false) }
    var cheatCodeInput by remember { mutableStateOf("") }
    var showMineActionDialog by remember { mutableStateOf(false) }
    var selectedMineAction by remember { mutableStateOf<MineAction?>(null) }
    var currentDigOutcome by remember { mutableStateOf<DigOutcome?>(null) }
    var showDigOutcomeDialog by remember { mutableStateOf(false) }
    var showOverlay by remember { mutableStateOf(false) }  // MutableState for overlay visibility
    var headerExpanded by remember { mutableStateOf(true) }  // State for header fold/expand
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

    // Auto-fold header when turn 1 starts (turn 0 for mobile)
    val currentTurn = gameState.turnNumber.value
    LaunchedEffect(currentTurn) {
        val collapseAtTurn = if (uiScale < 1f) 0 else 1  // Mobile: turn 0, Desktop: turn 1
        if (currentTurn >= collapseAtTurn) {
            headerExpanded = false
        }
    }
    
    // Watch for cheat dig outcomes
    LaunchedEffect(cheatDigOutcome) {
        if (cheatDigOutcome != null) {
            currentDigOutcome = cheatDigOutcome
            showDigOutcomeDialog = true
            onClearCheatDigOutcome?.invoke()
        }
    }

    // Mine action handler
    val handleMineAction: (Int, MineAction) -> Unit = { mineId, action ->
        when (action) {
            MineAction.DIG -> {
                val outcome = onMineDig?.invoke(mineId)
                if (outcome != null) {
                    currentDigOutcome = outcome
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
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        // Header with prominent phase indicator (collapsible)
        // Wrap header in Box to ensure z-axis ordering (header in front of map)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(8.dp)
                .zIndex(2f)
        ) {
            if (headerExpanded) {
                // Expanded header
                Column(modifier = Modifier.fillMaxWidth()) {
                    // First row: Level name centered with fold button on right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Empty spacer for symmetry
                        Spacer(modifier = Modifier.width(100.dp))

                        // Level name centered (without "Level:" prefix)
                        Text(
                            text = gameState.level.name,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )

                        // Fold button at right end (same size as other buttons)
                        Button(
                            onClick = { headerExpanded = false }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TriangleUpIcon(size = 14.dp, tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Fold Header")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Main header content
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            // Clickable coins display for cheat codes with icon
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable(
                                    onClick = {
                                        if (onCheatCode != null) {
                                            showCheatDialog = true
                                        }
                                    }
                                )
                            ) {
                                MoneyIcon(size = 20.dp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "${gameState.coins.value}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                HeartIcon(size = 20.dp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${gameState.healthPoints.value}", style = MaterialTheme.typography.bodyLarge)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ReloadIcon(size = 18.dp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Turn ${gameState.turnNumber.value}", style = MaterialTheme.typography.bodyMedium)
                            }

                            val activeEnemies = gameState.attackers.count { !it.isDefeated.value }
                            val totalSpawned = gameState.nextAttackerId.value - 1
                            val plannedSpawns = gameState.spawnPlan.drop(totalSpawned)
                            val remainingEnemies = plannedSpawns.size

                            Text(
                                "Enemies: $activeEnemies active, $remainingEnemies to come",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFF44336)
                            )
                        }

                        // Prominent phase indicator
                        val phaseText = when (gameState.phase.value) {
                            GamePhase.INITIAL_BUILDING -> "Initial Building Phase"
                            GamePhase.PLAYER_TURN -> "YOUR TURN"
                            GamePhase.ENEMY_TURN -> "ENEMY TURN"
                        }
                        val phaseColor = when (gameState.phase.value) {
                            GamePhase.INITIAL_BUILDING -> Color(0xFF2196F3)
                            GamePhase.PLAYER_TURN -> Color(0xFF4CAF50)
                            GamePhase.ENEMY_TURN -> Color(0xFFF44336)
                        }
                        Text(
                            text = phaseText,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = phaseColor,
                            modifier = Modifier.background(phaseColor.copy(alpha = 0.1f)).padding(12.dp)
                        )

                        Column {
                            Button(onClick = onBackToMap) {
                                Text("Back to Map")
                            }
                            
                            if (onSaveGame != null) {
                                Button(
                                    onClick = { 
                                        showSaveDialog = true
                                    }
                                ) {
                                    Text("Save Game")
                                }
                            }

                            // Toggle button positioned above the map and far to the right
                            Button(
                                onClick = { showOverlay = !showOverlay },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (showOverlay) Color(0xFF4CAF50) else Color(0xFF2196F3)
                                )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(if (showOverlay) "Hide Info" else "Show Info")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    if (showOverlay) {
                                        TriangleRightIcon(size = 18.dp, tint = Color.White)
                                    } else {
                                        TriangleLeftIcon(size = 18.dp, tint = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Collapsed header - single row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Statistics at far left
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Clickable coins display for cheat codes
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable(
                                onClick = {
                                    if (onCheatCode != null) {
                                        showCheatDialog = true
                                    }
                                }
                            )
                        ) {
                            MoneyIcon(size = 16.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "${gameState.coins.value}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            HeartIcon(size = 16.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${gameState.healthPoints.value}", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ReloadIcon(size = 14.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${gameState.turnNumber.value}", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    // Level name in center (without prefix, bold when collapsed)
                    Text(
                        text = gameState.level.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )

                    // Three buttons at far right (four on mobile if save is available)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                showSaveDialog = true
                            },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            SaveIcon(size = 16.dp, modifier = Modifier.align(Alignment.CenterVertically))
                        }

                        Button(
                            onClick = onBackToMap,
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("Map", fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterVertically))
                        }

                        Button(
                            onClick = { showOverlay = !showOverlay },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (showOverlay) Color(0xFF4CAF50) else Color(0xFF2196F3)
                            )
                        ) {
                            if (showOverlay) {
                                TriangleRightIcon(size = 12.dp)
                            } else {
                                TriangleLeftIcon(size = 12.dp)
                            }
                        }

                        // Fold button
                        Button(
                            onClick = { headerExpanded = true },
                            modifier = Modifier.size(32.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            TriangleDownIcon(size = 12.dp, tint = Color.White)
                        }
                    }
                }
            }
        }

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
                        }
                        return@GameGrid
                    }

                    val previousSelectedDefenderId = selectedDefenderId
                    // Check if there's a defender at this position
                    val defender = gameState.defenders.find { it.position == position }
                    if (defender != null) {
                        if (previousSelectedDefenderId == defender.id) {
                            // Deselect if clicking the same defender
                            selectedDefenderId = null
                        } else {
                            // Select this defender
                            selectedDefenderId = defender.id
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
                                val attacker =
                                    gameState.attackers.find { it.position.value == position && !it.isDefeated.value }
                                if (attacker != null) {
                                    selectedTargetId = attacker.id
                                    selectedTargetPosition = null
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
                        .background(Color.White.copy(alpha = 0.95f))
                        .padding(8.dp)
                ) {
                    // Legend
                    GameLegend(modifier = Modifier.fillMaxWidth())

                    Spacer(modifier = Modifier.height(16.dp))

                    // Enemy List
                    EnemyListPanel(gameState = gameState, modifier = Modifier.fillMaxWidth().weight(1f))
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
                    selectedTargetId = null,
                    selectedTargetPosition = null,
                    onSelectDefenderType = { selectedDefenderType = it },
                    onUpgradeDefender = { onUpgradeDefender(it) },
                    onUndoTower = { onUndoTower(it) },
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
                        onStartFirstPlayerTurn()
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
                    selectedTargetId = selectedTargetId,
                    selectedTargetPosition = selectedTargetPosition,
                    onSelectDefenderType = { selectedDefenderType = it },
                    onUpgradeDefender = { onUpgradeDefender(it) },
                    onUndoTower = { onUndoTower(it) },
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
                            true
                        } else {
                            false
                        }
                    },
                    onDefenderAttackPosition = { defenderId, targetPos ->
                        if (onDefenderAttackPosition(defenderId, targetPos)) {
                            selectedTargetId = null
                            selectedTargetPosition = null
                            true
                        } else {
                            false
                        }
                    },
                    onPrimaryAction = onEndPlayerTurn,
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
            val outcome = currentDigOutcome!! // Extract to local variable for safety
            // Reset selections when dragon awakes
            if (outcome == DigOutcome.DRAGON) {
                selectedDefenderType = null
                selectedDefenderId = null
            }
            AlertDialog(
                onDismissRequest = {
                    showDigOutcomeDialog = false
                },
                title = { Text("Mining Result") },
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Image on the left
                        DigOutcomeIcon(
                            outcome = outcome,
                            size = 80.dp
                        )
                        
                        // Text on the right
                        val message = when (outcome) {
                            DigOutcome.NOTHING -> "You found nothing..."
                            DigOutcome.BRASS -> "You found brass!\n+${outcome.coins} coins"
                            DigOutcome.SILVER -> "You found silver!\n+${outcome.coins} coins"
                            DigOutcome.GOLD -> "You found gold!\n+${outcome.coins} coins"
                            DigOutcome.GEMS -> "You found gems!\n+${outcome.coins} coins"
                            DigOutcome.DIAMOND -> "You found a diamond!\n+${outcome.coins} coins"
                            DigOutcome.DRAGON -> "A DRAGON AWAKENS!\nThe mine is destroyed!"
                        }
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        showDigOutcomeDialog = false
                    }) {
                        Text("OK")
                    }
                }
            )
        }
        
        // Save game dialog (with optional comment input)
        if (showSaveDialog && onSaveGame != null) {
            AlertDialog(
                onDismissRequest = {
                    showSaveDialog = false
                    saveCommentInput = ""
                },
                title = { Text("Save Game") },
                text = {
                    Column {
                        Text(
                            "Add an optional comment to help identify this save:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = saveCommentInput,
                            onValueChange = {
                                // Limit comment to 200 characters
                                if (it.length <= 200) {
                                    saveCommentInput = it
                                }
                            },
                            placeholder = { Text("e.g., 'Before final wave', 'Good position'...") },
                            singleLine = false,
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                            supportingText = {
                                Text(
                                    "${saveCommentInput.length}/200",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val comment = if (saveCommentInput.isBlank()) null else saveCommentInput.trim()
                            onSaveGame(comment)
                            showSaveDialog = false
                            saveCommentInput = ""
                            showSaveConfirmation = true
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    Button(onClick = {
                        showSaveDialog = false
                        saveCommentInput = ""
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Save confirmation dialog
        if (showSaveConfirmation) {
            AlertDialog(
                onDismissRequest = { showSaveConfirmation = false },
                title = { Text("Game Saved") },
                text = {
                    Text("Your game has been saved successfully!", style = MaterialTheme.typography.bodyLarge)
                },
                confirmButton = {
                    Button(onClick = { showSaveConfirmation = false }) {
                        Text("OK")
                    }
                }
            )
        }

        // Cheat code dialog
        if (showCheatDialog && onCheatCode != null) {
            AlertDialog(
                onDismissRequest = {
                    showCheatDialog = false
                    cheatCodeInput = ""
                },
                title = { Text("Cheat Code") },
                text = {
                    Column {
                        Text("Enter cheat code:")
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = cheatCodeInput,
                            onValueChange = { cheatCodeInput = it },
                            singleLine = true,
                            placeholder = { Text("e.g. moneybags") }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Available codes:", style = MaterialTheme.typography.labelSmall)
                        Text("• cash - Get 1000 coins", style = MaterialTheme.typography.bodySmall)
                        Text("• mmmoney - Get a million coins", style = MaterialTheme.typography.bodySmall)
                        Text("• dragon- Spawn dragon from mine", style = MaterialTheme.typography.bodySmall)
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val success = onCheatCode(cheatCodeInput)
                        if (success) {
                            showCheatDialog = false
                            cheatCodeInput = ""
                        }
                    }) {
                        Text("Apply")
                    }
                },
                dismissButton = {
                    Button(onClick = {
                        showCheatDialog = false
                        cheatCodeInput = ""
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
    }
}









/**
 * Platform-specific modifier for mouse wheel zoom support.
 * On desktop, this enables mouse wheel scrolling to zoom in/out.
 * On mobile platforms, this is a no-op since touch gestures handle zooming.
 */
expect fun Modifier.mouseWheelZoom(
    containerSize: IntSize,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    onScaleChange: (Float) -> Unit,
    onOffsetChange: (Float, Float) -> Unit
): Modifier

