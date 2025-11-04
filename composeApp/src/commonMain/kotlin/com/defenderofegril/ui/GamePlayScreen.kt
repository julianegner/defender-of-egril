package com.defenderofegril.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.zIndex
import com.defenderofegril.model.*
import kotlin.math.sqrt

// UI Constants
private val ATTACK_BUTTON_COLOR = Color(0xFFD32F2F)

/**
 * A pointy-top hexagon shape for Compose
 */
class HexagonShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val centerY = height / 2f

            // For pointy-top hexagon:
            // The hexagon has flat sides on left and right
            // Points at top and bottom
            val radius = minOf(width, height) / 2f

            // Calculate the 6 vertices of a pointy-top hexagon
            // Starting from the top and going clockwise
            val sqrt3 = sqrt(3.0).toFloat()

            // Top point
            moveTo(centerX, centerY - radius)
            // Top-right
            lineTo(centerX + radius * sqrt3 / 2f, centerY - radius / 2f)
            // Bottom-right
            lineTo(centerX + radius * sqrt3 / 2f, centerY + radius / 2f)
            // Bottom point
            lineTo(centerX, centerY + radius)
            // Bottom-left
            lineTo(centerX - radius * sqrt3 / 2f, centerY + radius / 2f)
            // Top-left
            lineTo(centerX - radius * sqrt3 / 2f, centerY - radius / 2f)
            // Close the path
            close()
        }
        return Outline.Generic(path)
    }
}

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
    onMineBuildTrap: ((Int, Position) -> Boolean)? = null  // Add mine build trap callback
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
        onMineBuildTrap = onMineBuildTrap
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

) {
    var selectedDefenderType by remember { mutableStateOf<DefenderType?>(null) }
    var selectedDefenderId by remember { mutableStateOf<Int?>(null) }
    var selectedTargetId by remember { mutableStateOf<Int?>(null) }
    var selectedTargetPosition by remember { mutableStateOf<Position?>(null) }
    var showCheatDialog by remember { mutableStateOf(false) }
    var cheatCodeInput by remember { mutableStateOf("") }
    var showMineActionDialog by remember { mutableStateOf(false) }
    var selectedMineAction by remember { mutableStateOf<MineAction?>(null) }
    var digOutcomeMessage by remember { mutableStateOf<String?>(null) }
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

    // Mine action handler
    val handleMineAction: (Int, MineAction) -> Unit = { mineId, action ->
        when (action) {
            MineAction.DIG -> {
                val outcome = onMineDig?.invoke(mineId)
                if (outcome != null) {
                    val message = when (outcome) {
                        DigOutcome.NOTHING -> "You found nothing..."
                        DigOutcome.BRASS -> "You found brass! +${outcome.coins} coins"
                        DigOutcome.SILVER -> "You found silver! +${outcome.coins} coins"
                        DigOutcome.GOLD -> "You found gold! +${outcome.coins} coins"
                        DigOutcome.GEMS -> "You found gems! +${outcome.coins} coins"
                        DigOutcome.DIAMOND -> "You found a diamond! +${outcome.coins} coins"
                        DigOutcome.DRAGON -> "A DRAGON AWAKENS! The mine is destroyed!"
                    }
                    digOutcomeMessage = message
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
                    onSellTower = { onSellTower(it) },
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
                    onSellTower = { onSellTower(it) },
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
        if (showDigOutcomeDialog) {
            AlertDialog(
                onDismissRequest = { showDigOutcomeDialog = false },
                title = { Text("Mining Result") },
                text = {
                    Text(digOutcomeMessage ?: "", style = MaterialTheme.typography.bodyLarge)
                },
                confirmButton = {
                    Button(onClick = { showDigOutcomeDialog = false }) {
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GameGrid(
    gameState: GameState,
    selectedDefenderType: DefenderType?,
    selectedDefenderId: Int?,
    selectedTargetId: Int?,
    selectedTargetPosition: Position?,
    selectedMineAction: MineAction?,
    onCellClick: (Position) -> Unit,
    modifier: Modifier = Modifier
) {
    // State for pan and zoom
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var actualContentSize by remember { mutableStateOf(IntSize.Zero) }

    val hexSize = 40.dp  // Radius of hexagon (center to corner)

    // Calculate hex dimensions for pointy-top hexagons
    val sqrt3 = sqrt(3.0).toFloat()
    val hexWidth = hexSize.value * sqrt3  // Width of hexagon (flat-to-flat)
    val hexHeight = hexSize.value * 2f    // Height of hexagon (point-to-point)

    // For pointy-top hexagons, vertical spacing between centers is 3/4 of height
    val verticalSpacing = hexHeight * 0.75f

    // Odd rows are offset to the right to create hexagonal grid pattern
    val oddRowOffset = hexWidth * 0.42f

    // Calculate total grid dimensions
    // Need enough width for all hexagons without compression
    // Each hexagon is hexWidth wide, we have gridWidth hexagons per row
    // Odd rows add oddRowOffset padding at the start
    // Add generous buffer (3 extra hexWidths) to ensure no compression
    val totalGridWidth = ((gameState.level.gridWidth + 3) * hexWidth + oddRowOffset).dp
    val totalGridHeight = ((gameState.level.gridHeight) * verticalSpacing + hexHeight).dp

    Box(
        modifier = modifier
            .onSizeChanged { containerSize = it }
            .mouseWheelZoom(
                containerSize = containerSize,
                scale = scale,
                offsetX = offsetX,
                offsetY = offsetY,
                onScaleChange = { newScale -> scale = newScale },
                onOffsetChange = { newOffsetX, newOffsetY -> 
                    offsetX = newOffsetX
                    offsetY = newOffsetY
                }
            )
            // Combined gesture handling for pan and pinch-zoom
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    // Apply zoom (for pinch gestures on mobile)
                    if (zoom != 1f) {
                        scale = (scale * zoom).coerceIn(0.5f, 3f)
                    }

                    // Apply pan
                    offsetX += pan.x
                    offsetY += pan.y

                    // Constrain pan to keep content visible
                    // Center the content initially and allow symmetric panning to see all edges
                    // Use actualContentSize which is the measured size of the Column
                    val contentWidth = actualContentSize.width * scale
                    val contentHeight = actualContentSize.height * scale

                    val maxOffsetX = if (contentWidth > containerSize.width) {
                        (contentWidth - containerSize.width) / 2  // Half the overflow for symmetric panning
                    } else {
                        (containerSize.width * (scale - 1) / 2).coerceAtLeast(0f)
                    }

                    val maxOffsetY = if (contentHeight > containerSize.height) {
                        (contentHeight - containerSize.height) / 2  // Half the overflow for symmetric panning
                    } else {
                        (containerSize.height * (scale - 1) / 2).coerceAtLeast(0f)
                    }

                    // Allow symmetric panning: +maxOffset (left/top edge) to -maxOffset (right/bottom edge)
                    offsetX = offsetX.coerceIn(-maxOffsetX, maxOffsetX)
                    offsetY = offsetY.coerceIn(-maxOffsetY, maxOffsetY)
                }
            }
    ) {
        // Map content with pan and zoom applied
        // Use layout modifier to allow Column to exceed parent bounds
        Column(
            modifier = Modifier
                .layout { measurable, constraints ->
                    // Measure with infinite constraints to prevent compression
                    val placeable = measurable.measure(
                        constraints.copy(
                            maxWidth = Constraints.Infinity,
                            maxHeight = Constraints.Infinity
                        )
                    )
                    // Capture the actual content size for pan calculations
                    actualContentSize = IntSize(placeable.width, placeable.height)
                    // Report the actual size to parent (for proper container sizing)
                    layout(placeable.width, placeable.height) {
                        placeable.place(0, 0)
                    }
                }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                ),
            verticalArrangement = Arrangement.spacedBy((-hexHeight + verticalSpacing - 7f).dp)
        ) {
            for (y in 0 until gameState.level.gridHeight) {
                Row(
                    modifier = Modifier
                        .padding(
                            start = if (y % 2 == 1) (hexWidth * 0.42f).dp else 0.dp
                        )
                        .offset(y = (-(y-1)).dp),
                    horizontalArrangement = Arrangement.spacedBy((-10).dp)
                ) {
                    for (x in 0 until gameState.level.gridWidth) {
                        val position = Position(x, y)
                        GridCell(
                            position = position,
                            gameState = gameState,
                            isSelected = selectedDefenderType != null,
                            isDefenderSelected = selectedDefenderId?.let { selId ->
                                gameState.defenders.find { it.position == position }?.id == selId
                            } ?: false,
                            isTargetSelected = gameState.attackers.find { it.position.value == position }?.id == selectedTargetId,
                            selectedDefenderId = selectedDefenderId,
                            selectedMineAction = selectedMineAction,
                            onClick = { onCellClick(position) },
                            hexSize = hexSize
                        )
                    }
                }
            }
        }

        // Minimap - shown when zoomed in
        if (scale > 1.1f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(120.dp)
            ) {
                HexagonMinimap(
                    level = gameState.level,
                    config = MinimapConfig(
                        showSpawnPoints = true,
                        showTarget = true,
                        showTowers = true,
                        showEnemies = true,
                        showViewport = true,
                        minimapSizeDp = 120f
                    ),
                    gameState = gameState,
                    scale = scale,
                    offsetX = offsetX,
                    offsetY = offsetY,
                    containerSize = containerSize,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun GridCell(
    position: Position,
    gameState: GameState,
    isSelected: Boolean,
    isDefenderSelected: Boolean,
    isTargetSelected: Boolean,
    selectedDefenderId: Int?,
    selectedMineAction: MineAction?,
    onClick: () -> Unit,
    hexSize: androidx.compose.ui.unit.Dp = 48.dp
) {
    val isSpawnPoint = gameState.level.isSpawnPoint(position)
    val isTarget = position == gameState.level.targetPosition
    val isOnPath = gameState.level.isOnPath(position)
    val isBuildIsland = gameState.level.isBuildIsland(position)
    val isBuildArea = gameState.level.isBuildArea(position)
    val defender = gameState.defenders.find { it.position == position }
    val attacker = gameState.attackers.find { it.position.value == position && !it.isDefeated.value }

    // Check for field effects at this position
    val fieldEffect = gameState.fieldEffects.find { it.position == position }

    // Check for traps at this position
    val trap = gameState.traps.find { it.position == position }

    // Check if this cell is in range of the selected defender
    val cellIsInRange = selectedDefenderId?.let { defenderId ->
        val selectedDefender = gameState.defenders.find { it.id == defenderId }
        selectedDefender?.let { sel ->
            if (sel.position == position) {
                false  // Don't highlight the defender's own cell
            } else {
                val distance = sel.position.distanceTo(position)
                distance >= sel.type.minRange && distance <= sel.range
            }
        } ?: false
    } ?: false

    // Base background color based on area type - ALWAYS visible
    // Build islands + strips adjacent to path allow tower placement
    val baseBackgroundColor = when {
        isBuildIsland -> Color(0xFF8BC34A)  // Light green for build islands
        isBuildArea -> Color(0xFFA5D6A7)  // Medium green for strips adjacent to path
        isOnPath -> Color(0xFFFFF8DC)  // Cream/beige for enemy path
        else -> Color(0xFFE0E0E0)  // Light gray for off-path areas (non-playable)
    }

    // Apply slight tint for selection states, but keep base color visible
    // Override with red background for enemy units and colored background for defenders
    // During INITIAL_BUILDING phase, don't apply any selection tints
    // Field effects also modify the background color
    val backgroundColor = when {
        attacker != null -> Color(0xFFF44336)  // Red background for enemies
        defender != null -> {
            when {
                !defender.isReady -> Color(0xFF9E9E9E)  // Gray for building
                defender.actionsRemaining.value <= 0 -> Color(0xFF7986CB)  // Blue-gray mix for used up actions
                else -> Color(0xFF2196F3)  // Blue for ready with actions
            }
        }

        fieldEffect != null -> {
            when (fieldEffect.type) {
                FieldEffectType.FIREBALL -> Color(0xFFFF9800).copy(alpha = 0.5f)  // Orange tint for fireball
                FieldEffectType.ACID -> Color(0xFF4CAF50).copy(alpha = 0.6f)  // Green tint for acid
            }
        }

        trap != null -> Color(0xFF8B4513).copy(alpha = 0.6f)  // Brown tint for trap
        isDefenderSelected && gameState.phase.value != GamePhase.INITIAL_BUILDING -> baseBackgroundColor.copy(alpha = 0.7f)
        isTargetSelected && gameState.phase.value != GamePhase.INITIAL_BUILDING -> baseBackgroundColor.copy(alpha = 0.8f)
        else -> baseBackgroundColor  // No selection highlighting during placement or in initial phase
    }

    // Border color - use borders to indicate entities instead of background
    // For range visualization, show green border on path tiles in range (only if tower has actions)
    val showRange = selectedDefenderId?.let { defenderId ->
        val selectedDefender = gameState.defenders.find { it.id == defenderId }
        selectedDefender?.isReady == true && selectedDefender.actionsRemaining.value > 0
    } ?: false

    // When placing trap, don't show green border on tiles with enemies
    val isTrapPlacement = selectedMineAction == MineAction.BUILD_TRAP
    val hasEnemy = attacker != null
    val canPlaceTrapHere = !hasEnemy || !isTrapPlacement

    val borderColor = when {
        cellIsInRange && isOnPath && showRange && canPlaceTrapHere -> Color(0xFF4CAF50)  // Green border for tiles in range (exclude enemy tiles during trap placement)
        isDefenderSelected && gameState.phase.value != GamePhase.INITIAL_BUILDING -> Color(0xFFFFEB3B)  // Yellow border for selected defender (not during initial building)
        isSpawnPoint -> Color(0xFFFF9800)  // Orange border for spawn
        isTarget -> Color(0xFF4CAF50)  // Green border for target
        attacker != null -> Color(0xFFF44336)  // Red border for enemies
        defender != null -> if (defender.isReady) Color(0xFF2196F3) else Color(0xFF9E9E9E)  // Blue/gray border for towers
        fieldEffect != null -> {
            when (fieldEffect.type) {
                FieldEffectType.FIREBALL -> Color(0xFFFF5722)  // Deep orange border for fireball
                FieldEffectType.ACID -> Color(0xFF4CAF50)  // Green border for acid
            }
        }

        trap != null -> Color(0xFF8B4513)  // Brown border for trap
        else -> Color.Transparent  // No borders for empty cells
    }

    // Thicker borders for important elements
    val borderWidth = when {
        isDefenderSelected && gameState.phase.value != GamePhase.INITIAL_BUILDING -> 5.dp  // Extra thick border for selected defender (not during initial building)
        cellIsInRange && isOnPath && showRange && canPlaceTrapHere -> 4.dp  // Thick border for cells in range (exclude enemy tiles during trap placement)
        isSpawnPoint || isTarget -> 3.dp
        attacker != null || defender != null -> 3.dp
        fieldEffect != null -> 3.dp  // Thick border for field effects
        else -> 0.dp  // No border for empty cells
    }

    // Calculate hex dimensions for proper sizing
    val sqrt3 = sqrt(3.0).toFloat()
    val hexWidth = hexSize.value * sqrt3
    val hexHeight = hexSize.value * 2f

    Box(
        modifier = Modifier
            .width((hexWidth).dp)
            .height((hexHeight).dp)
            .clip(HexagonShape())
            .background(backgroundColor)
            .border(borderWidth, borderColor, HexagonShape())
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when {
            attacker != null -> {
                // Use graphical icon for enemy units
                // Key by id, position, and currentHealth to force recomposition when any changes
                key(attacker.id, attacker.position.value.x, attacker.position.value.y, attacker.currentHealth.value) {
                    EnemyIcon(attacker = attacker)
                }
            }

            defender != null -> {
                // Use graphical icon for towers
                // Key by id, level and actionsRemaining to force recomposition when these change
                key(defender.id, defender.level, defender.actionsRemaining.value, defender.buildTimeRemaining.value) {
                    TowerIcon(defender = defender, gameState = gameState)
                }
            }

            fieldEffect != null -> {
                // Show field effect info
                when (fieldEffect.type) {
                    FieldEffectType.FIREBALL -> {
                        // Show fireball symbol
                        ExplosionIcon(size = 28.dp)
                    }

                    FieldEffectType.ACID -> {
                        // Show acid splash with damage and duration
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            TestTubeIcon(size = 20.dp)
                            Text(
                                "-${fieldEffect.damage}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${fieldEffect.turnsRemaining}T",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFFEB3B)
                            )
                        }
                    }
                }
            }

            trap != null -> {
                // Show trap icon with damage (no duration since traps are permanent until triggered)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    HoleIcon(size = 20.dp)
                    Text(
                        "-${trap.damage}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            isSpawnPoint -> {
                // Show spawn indicator when cell is empty
                Text("Spawn", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF9800))
            }

            isTarget -> {
                // Show target indicator when cell is empty
                Text("Target", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
            }
        }
    }
}

@Composable
fun GameControlsPanel(
    phase: GamePhase,
    gameState: GameState,
    coinsState: State<Int>,
    selectedDefenderType: DefenderType?,
    selectedDefenderId: Int?,
    selectedTargetId: Int?,
    selectedTargetPosition: Position?,
    onSelectDefenderType: (DefenderType?) -> Unit,
    onUpgradeDefender: (Int) -> Unit,
    onUndoTower: (Int) -> Unit,
    onSellTower: (Int) -> Unit,
    onDefenderAttack: (Int, Int) -> Boolean,
    onDefenderAttackPosition: (Int, Position) -> Boolean,
    onPrimaryAction: () -> Unit,
    onMineAction: ((Int, MineAction) -> Unit)? = null,
    uiScale: Float = 1f  // Add platform scale parameter
) {
    // Automatically fold buy panel when a defender is selected
    val compactBuyPanel = selectedDefenderId != null

    // Determine phase-specific properties
    val isPlayerTurn = phase == GamePhase.PLAYER_TURN
    val title = if (isPlayerTurn) {
        "Your Turn - Place towers and attack enemies"
    } else {
        "Initial Building Phase - Place towers (no build time)"
    }
    val primaryButtonText = if (isPlayerTurn) "End Turn" else "Start Battle"
    val primaryButtonColor = if (isPlayerTurn) {
        Color(0xFFFF5722)
    } else {
        ButtonDefaults.buttonColors().containerColor
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Title
        Text(title, style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(8.dp))

        if (compactBuyPanel) {
            // Folded view: Compact layout with defender info on left, buy buttons on right
            Row(modifier = Modifier.fillMaxWidth()) {
                // Selected defender info on left (smaller)
                selectedDefenderId?.let { defenderId ->
                    val defender = gameState.defenders.find { it.id == defenderId }
                    if (defender != null) {
                        Box(modifier = Modifier.weight(1f)) {
                            Column {
                                DefenderInfo(
                                    defender,
                                    gameState,
                                    onUpgradeDefender,
                                    onUndoTower,
                                    onSellTower,
                                    onMineAction = onMineAction,
                                    compactBuyPanel,
                                    isMobile = uiScale < 1f
                                )

                                if (isPlayerTurn &&
                                    defender.type != DefenderType.DWARVEN_MINE &&
                                    defender.type != DefenderType.DRAGONS_LAIR) {
                                    AttackButton(
                                        defender = defender,
                                        gameState = gameState,
                                        selectedTargetId = selectedTargetId,
                                        selectedTargetPosition = selectedTargetPosition,
                                        onDefenderAttack = { defenderId, targetId ->
                                            onDefenderAttack(defenderId, targetId)
                                        },
                                        onDefenderAttackPosition = { defenderId, targetPos ->
                                            onDefenderAttackPosition(defenderId, targetPos)
                                        },
                                        modifier = Modifier
                                            .layout { measurable, constraints ->
                                                val placeable = measurable.measure(constraints)
                                                layout(0, 0) {
                                                    placeable.place(0, 0)
                                                }
                                            }
                                            .width(200.dp)
                                            .height(100.dp)
                                            .absoluteOffset(x = 1000.dp, y = (-130).dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }

                // Compact buy buttons on right
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.width(350.dp).height(170.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        DefenderType.entries
                            .filter { it != DefenderType.DRAGONS_LAIR }
                            .toTypedArray(),
                        key = { type -> "${type.name}_folded_${coinsState.value}" }) { type ->
                        val canAfford = coinsState.value >= type.baseCost
                        CompactDefenderButton(
                            type = type,
                            isSelected = selectedDefenderType == type,
                            canAfford = canAfford,
                            onClick = {
                                onSelectDefenderType(if (selectedDefenderType == type) null else type)
                            }
                        )
                    }
                }
            }
        } else {
            // Expanded view: Original layout
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.fillMaxWidth().height(75.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(
                    DefenderType.entries
                        .filter { it != DefenderType.DRAGONS_LAIR }
                        .toTypedArray(),
                    key = { type -> "${type.name}_${coinsState.value}_${gameState.defenders.count { it.type == type }}" }) { type ->
                    val canAfford = coinsState.value >= type.baseCost
                    println("DEBUG: ${phase.name} Button for ${type.displayName} - coins: ${coinsState.value}, cost: ${type.baseCost}, canAfford: $canAfford")
                    DefenderButton(
                        type = type,
                        isSelected = selectedDefenderType == type,
                        canAfford = canAfford,
                        coinsState = coinsState,
                        onClick = {
                            onSelectDefenderType(if (selectedDefenderType == type) null else type)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Selected defender info and attack button
            selectedDefenderId?.let { defenderId ->
                val defender = gameState.defenders.find { it.id == defenderId }
                if (defender != null) {
                    DefenderInfo(
                        defender,
                        gameState,
                        onUpgradeDefender,
                        onUndoTower,
                        onSellTower,
                        onMineAction = onMineAction,
                        compactBuyPanel,
                        isMobile = uiScale < 1f
                    )

                    // Don't show attack button for dwarven mines or dragon's lair
                    if (isPlayerTurn &&
                        defender.type != DefenderType.DWARVEN_MINE &&
                        defender.type != DefenderType.DRAGONS_LAIR
                    ) {
                        AttackButton(
                            defender = defender,
                            gameState = gameState,
                            selectedTargetId = selectedTargetId,
                            selectedTargetPosition = selectedTargetPosition,
                            onDefenderAttack = { defenderId, targetId ->
                                onDefenderAttack(defenderId, targetId)
                            },
                            onDefenderAttackPosition = { defenderId, targetPos ->
                                onDefenderAttackPosition(defenderId, targetPos)
                            },
                            modifier = Modifier
                                .layout { measurable, constraints ->
                                    // Measure the tooltip but don't add it to the layout
                                    val placeable = measurable.measure(constraints)
                                    layout(0, 0) { // Set the size to 0 to avoid taking up space and move other elements
                                        placeable.place(0, 0)
                                    }
                                }
                                .width(200.dp)
                                .height(100.dp)
                                .absoluteOffset(
                                    x = 1000.dp,
                                    y = (-130).dp
                                ) // .absoluteOffset(x = 700.dp, y = (-130).dp)

                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onPrimaryAction,
            modifier = Modifier.fillMaxWidth(),
            colors = if (isPlayerTurn) {
                ButtonDefaults.buttonColors(containerColor = primaryButtonColor)
            } else {
                ButtonDefaults.buttonColors()
            }
        ) {
            Text(primaryButtonText)
        }
    }
}

@Composable
fun CompactDefenderButton(
    type: DefenderType,
    isSelected: Boolean,
    canAfford: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = canAfford,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF1976D2) else MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier.fillMaxWidth().height(40.dp),
        contentPadding = PaddingValues(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // Tower icon
            Box(
                modifier = Modifier.size(28.dp),
                contentAlignment = Alignment.Center
            ) {
                TowerTypeIcon(defenderType = type, modifier = Modifier.size(30.dp))
            }

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                type.displayName
                    .replace(" Tower", ""),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Cost
            Row(verticalAlignment = Alignment.CenterVertically) {
                MoneyIcon(size = 14.dp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "${type.baseCost}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun AttackButton(
    defender: Defender,
    gameState: GameState,
    selectedTargetId: Int?,
    selectedTargetPosition: Position?,
    onDefenderAttack: (Int, Int) -> Unit,
    onDefenderAttackPosition: (Int, Position) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth().height(56.dp)
) {
    if (defender.isReady && defender.actionsRemaining.value > 0) {
        // For AOE/DOT towers with position selected
        if ((defender.type.attackType == AttackType.AREA || defender.type.attackType == AttackType.LASTING) && selectedTargetPosition != null) {
            // If there's an enemy at the position, show enemy info
            if (selectedTargetId != null) {
                val target = gameState.attackers.find { it.id == selectedTargetId }
                if (target != null && defender.canAttack(target)) {
                    Button(
                        onClick = { onDefenderAttackPosition(defender.id, selectedTargetPosition) },
                        modifier = modifier,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ATTACK_BUTTON_COLOR
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SwordIcon(size = 24.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    "ATTACK",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${target.type.displayName} (${target.currentHealth.value}/${target.maxHealth} HP) + Area",
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            } else {
                // No enemy at position, show position coordinates
                Button(
                    onClick = { onDefenderAttackPosition(defender.id, selectedTargetPosition) },
                    modifier = modifier,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ATTACK_BUTTON_COLOR
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SwordIcon(size = 24.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "ATTACK AREA",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "at (${selectedTargetPosition.x}, ${selectedTargetPosition.y})",
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        } else if (selectedTargetId != null) {
            // For all towers, allow attacking enemies
            val target = gameState.attackers.find { it.id == selectedTargetId }
            if (target != null && defender.canAttack(target)) {
                Button(
                    onClick = { onDefenderAttack(defender.id, selectedTargetId) },
                    modifier = modifier,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ATTACK_BUTTON_COLOR
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SwordIcon(size = 24.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "ATTACK",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${target.type.displayName} (${target.currentHealth.value}/${target.maxHealth} HP)",
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TowerStats(minRange: Int, damage: Int, range: Int, actionsPerTurn: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ExplosionIcon(size = 12.dp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            "${damage}",
            style = MaterialTheme.typography.bodySmall
        )
    }
    if (minRange > 0) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TargetIcon(size = 12.dp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "${minRange}-${range}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TargetIcon(size = 12.dp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "${range}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        LightningIcon(size = 12.dp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            actionsPerTurn.toString(),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun DefenderInfo(
    defender: Defender,
    gameState: GameState,
    onUpgradeDefender: (Int) -> Unit,
    onUndoTower: (Int) -> Unit,
    onSellTower: (Int) -> Unit,
    onMineAction: ((Int, MineAction) -> Unit)? = null,
    compactBuyPanel: Boolean = false,
    isMobile: Boolean = false  // Add platform parameter
) {
    // Use key to force recomposition when defender stats change
    key(
        defender.id,
        defender.level,
        defender.damage,
        defender.range,
        defender.actionsRemaining.value,
        defender.buildTimeRemaining.value,
        defender.isReady
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // Reduce padding on mobile to save space
            val cardPadding = if (isMobile) 4.dp else 8.dp
            Column(modifier = Modifier.padding(cardPadding)) {
                // Tower icon, name, and actions in one row
                Row(
                    // modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    // Tower icon - smaller on mobile to save space
                    val iconSize = if (isMobile) 32.dp else 48.dp
                    val iconInnerSize = if (isMobile) 28.dp else 44.dp
                    Box(
                        modifier = Modifier.size(iconSize),
                        contentAlignment = Alignment.Center
                    ) {
                        TowerIcon(defender = defender, modifier = Modifier.size(iconInnerSize), gameState = gameState)
                    }

                    val horizontalSpacing = if (isMobile) 4.dp else 8.dp
                    Spacer(modifier = Modifier.width(horizontalSpacing))

                    // Tower name and level
                    Column(modifier = Modifier.weight(0.7f)) {
                        val displayName = if (defender.type == DefenderType.DRAGONS_LAIR) {
                            // Check if the specific dragon from this lair is still alive
                            val dragonAlive = defender.dragonId.value?.let { dragonId ->
                                gameState.attackers.any {
                                    it.id == dragonId && !it.isDefeated.value
                                }
                            } ?: false
                            if (dragonAlive) "Dragon's Lair" else "Empty Dragon's Lair"
                        } else {
                            defender.type.displayName
                        }
                        Text(
                            displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Level ${defender.level.value}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF4CAF50)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SwordIcon(size = 12.dp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    defender.type.attackType.displayName,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(0.2f)) {
                        if (defender.type == DefenderType.DWARVEN_MINE) {
                            // Mine-specific UI with info dialog
                            var showMiningInfoDialog by remember { mutableStateOf(false) }

                            // Mining info dialog
                            if (showMiningInfoDialog) {
                                AlertDialog(
                                    onDismissRequest = { showMiningInfoDialog = false },
                                    title = { Text("Mining Probabilities") },
                                    text = { MiningOutcomeGrid() },
                                    confirmButton = {
                                        TextButton(onClick = { showMiningInfoDialog = false }) {
                                            Text("Close")
                                        }
                                    }
                                )
                            }

                            InfoIcon(
                                size = 16.dp,
                                modifier = Modifier
                                    .clickable { showMiningInfoDialog = true }
                                    .padding(4.dp)
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(0.5f)) {
                        DefenderActionsInfo(defender)
                    }
                    Spacer(modifier = Modifier.weight(6f))
                }

                // Reduce spacing on mobile
                val verticalSpacing = if (isMobile) 2.dp else 4.dp
                Spacer(modifier = Modifier.height(verticalSpacing))

                if (defender.isReady) {
                    if (defender.type == DefenderType.DRAGONS_LAIR) {
                        // Dragon's lair - no actions, can't be sold
                        Text(
                            "The dragon's lair... a reminder of greed's consequences.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    } else {

                        // Normal tower stats and buttons
                        val baseDamage =
                            if (defender.type == DefenderType.DWARVEN_MINE) defender.trapDamage else defender.damage
                        val nextLevelDamage = baseDamage + 5
                        val nextActualDamage = when (defender.type.attackType) {
                            AttackType.LASTING -> nextLevelDamage / 2
                            else -> nextLevelDamage
                        }
                        val nextLevel = defender.level.value + 1
                        val nextRangeCalculated = defender.type.baseRange + (nextLevel - 1) / 2
                        val nextRange = if (defender.type == DefenderType.SPIKE_TOWER && nextLevel >= 5) {
                            minOf(nextRangeCalculated, 2)
                        } else {
                            nextRangeCalculated
                        }
                        val nextActions =
                            if (defender.type == DefenderType.SPIKE_TOWER || defender.type == DefenderType.DWARVEN_MINE) {
                                val bonusActions = nextLevel / 5
                                minOf(1 + bonusActions, 3)
                            } else {
                                defender.type.actionsPerTurn
                            }

                        // Stats and upgrade button in columns
                        // On mobile, use more compact layout
                        if (isMobile) {
                            // Mobile: More compact horizontal layout
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Combined stats in one compact column
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = "Now",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
                                            )
                                            TowerStats(
                                                defender.type.minRange,
                                                if (defender.type == DefenderType.DWARVEN_MINE) defender.trapDamage else defender.actualDamage,
                                                defender.range,
                                                defender.actionsPerTurnCalculated
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "Up",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (gameState.canUpgradeDefender(defender)) Color(0xFF4CAF50) else Color.Gray
                                            )
                                            TowerStats(
                                                defender.type.minRange,
                                                nextActualDamage,
                                                nextRange,
                                                nextActions
                                            )
                                        }
                                    }
                                }

                                // Buttons side by side, more compact
                                Column(modifier = Modifier.weight(0.8f)) {
                                    UpgradeButton(defender, gameState, onUpgradeDefender = onUpgradeDefender, isMobile = isMobile)
                                }

                                Column(modifier = Modifier.weight(0.8f)) {
                                    UndoOrSellButton(
                                        defender = defender,
                                        gameState = gameState,
                                        onUndoTower = onUndoTower,
                                        onSellTower = onSellTower,
                                        isMobile = isMobile
                                    )
                                }

                                dwarvenMineActionButtonArea(
                                    defender.type,
                                    gameState,
                                    defender,
                                    onMineAction,
                                    compactBuyPanel
                                )
                            }
                        } else {
                            // Desktop: Original layout with 4 columns
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                // Current stats column
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Current:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    TowerStats(
                                        defender.type.minRange,
                                        if (defender.type == DefenderType.DWARVEN_MINE) defender.trapDamage else defender.actualDamage,
                                        defender.range,
                                        defender.actionsPerTurnCalculated
                                    )
                                }

                                // After upgrade stats column
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Upgrade:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (gameState.canUpgradeDefender(defender)) Color(0xFF4CAF50) else Color.Gray
                                    )
                                    TowerStats(
                                        defender.type.minRange,
                                        nextActualDamage,
                                        nextRange,
                                        nextActions
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    UpgradeButton(defender, gameState, onUpgradeDefender = onUpgradeDefender, isMobile = isMobile)
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    UndoOrSellButton(
                                        defender = defender,
                                        gameState = gameState,
                                        onUndoTower = onUndoTower,
                                        onSellTower = onSellTower,
                                        isMobile = isMobile
                                    )
                                }

                                dwarvenMineActionButtonArea(
                                    defender.type,
                                    gameState,
                                    defender,
                                    onMineAction,
                                    compactBuyPanel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.dwarvenMineActionButtonArea(
    type: DefenderType,
    gameState: GameState,
    defender: Defender,
    onMineAction: ((Int, MineAction) -> Unit)?,
    compactBuyPanel: Boolean = false

) {
    if (type == DefenderType.DWARVEN_MINE) {
        val mineActionsEnabled =
            gameState.phase.value != GamePhase.INITIAL_BUILDING && defender.actionsRemaining.value > 0

        if (mineActionsEnabled || gameState.phase.value == GamePhase.INITIAL_BUILDING) {
            // Dig button
            Column(modifier = Modifier.weight(0.5f)) {
                Button(
                    onClick = { onMineAction?.invoke(defender.id, MineAction.DIG) },
                    enabled = mineActionsEnabled,
                    modifier = Modifier.width(95.dp).height(60.dp)
                        .offset(y = (-12).dp),
                    contentPadding = PaddingValues(
                        horizontal = 4.dp,
                        vertical = 2.dp
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PickIcon(size = 24.dp)
                        Text("Dig", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Column(modifier = Modifier.weight(0.5f)) {
                // Trap button
                Button(
                    onClick = {
                        onMineAction?.invoke(
                            defender.id,
                            MineAction.BUILD_TRAP
                        )
                    },
                    enabled = mineActionsEnabled,
                    modifier = Modifier.width(95.dp).height(60.dp)
                        .offset(y = (-12).dp),
                    contentPadding = PaddingValues(
                        horizontal = 4.dp,
                        vertical = 2.dp
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        HoleIcon(size = 24.dp)
                        Text("Trap", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.weight(
            if (compactBuyPanel) 1.41f else 3f))
    } else {
        Spacer(modifier = Modifier.weight(if (compactBuyPanel) 2.41f else 4f))
    }
}

@Composable
fun MiningOutcomeGrid() {
    Column {
        // Header row
        Row {
            Text("Name", Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text("Chance (%)", Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text("Reward", Modifier.weight(1f), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        // Data rows
        DigOutcome.values().forEach { outcome ->
            Row {
                Text(outcome.displayName, Modifier.weight(1f))
                Text("${outcome.probability}", Modifier.weight(1f))
                Text("${outcome.coins}", Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun UpgradeButton(
    defender: Defender,
    gameState: GameState,
    modifier: Modifier = Modifier
        .offset(y = (-12).dp)
        .width(200.dp)
        .height(60.dp),
    onUpgradeDefender: (Int) -> Unit,
    isMobile: Boolean = false  // Add platform parameter
) {
    // Increase height for mobile to make buttons more usable
    val buttonModifier = if (isMobile) {
        Modifier
            .offset(y = (-6).dp)  // Less offset on mobile
            .width(200.dp)
            .height(100.dp)  // Taller on mobile (becomes 50dp with 0.5x scaling)
    } else {
        modifier
    }

    Button(
        onClick = { onUpgradeDefender(defender.id) },
        enabled = gameState.canUpgradeDefender(defender),
        modifier = buttonModifier,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Upgrade", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MoneyIcon(size = 14.dp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "${defender.upgradeCost}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun UndoOrSellButton(
    defender: Defender,
    gameState: GameState,
    onUndoTower: (Int) -> Unit,
    onSellTower: (Int) -> Unit,
    modifier: Modifier = Modifier
        .offset(y = (-12).dp)
        .width(200.dp)
        .height(60.dp),
    isMobile: Boolean = false  // Add platform parameter
) {
    var showSellConfirmation by remember { mutableStateOf(false) }

    // Increase height for mobile to make buttons more usable
    val buttonModifier = if (isMobile) {
        Modifier
            .offset(y = (-6).dp)  // Less offset on mobile
            .width(200.dp)
            .height(100.dp)  // Taller on mobile (becomes 50dp with 0.5x scaling)
    } else {
        modifier
    }

    // Determine if undo or sell is available
    val canUndo = defender.placedOnTurn == gameState.turnNumber.value && !defender.hasBeenUsed.value
    val canSell = defender.isReady && defender.actionsRemaining.value > 0

    if (canUndo) {
        // Show Undo button (100% refund)
        Button(
            onClick = { onUndoTower(defender.id) },
            enabled = true,
            modifier = buttonModifier,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50)  // Green for undo
            ),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Undo", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MoneyIcon(size = 14.dp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${defender.totalCost}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    } else if (canSell) {
        // Show Sell button (75% refund)
        val sellAmount = (defender.totalCost * 0.75).toInt()
        Button(
            onClick = { showSellConfirmation = true },
            enabled = true,
            modifier = buttonModifier,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF9800)  // Orange for sell
            ),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Sell", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MoneyIcon(size = 14.dp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "$sellAmount",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Confirmation dialog
        if (showSellConfirmation) {
            AlertDialog(
                onDismissRequest = { showSellConfirmation = false },
                title = { Text("Sell Tower?") },
                text = {
                    Text("Do you really want to sell the ${defender.type.displayName} for $sellAmount coins?")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onSellTower(defender.id)
                            showSellConfirmation = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF9800)
                        )
                    ) {
                        Text("Sell")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showSellConfirmation = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    } else {
        // Show disabled button when neither undo nor sell is available
        Button(
            onClick = { },
            enabled = false,
            modifier = buttonModifier,
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Sell", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "not enough Actions",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun DefenderActionsInfo(defender: Defender) {
    if (!defender.isReady) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimerIcon(size = 16.dp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "Building: ${defender.buildTimeRemaining.value}T",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFFF9800)
            )
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            LightningIcon(size = 16.dp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "${defender.actionsRemaining.value}/${defender.actionsPerTurnCalculated}",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
fun EnemyTurnInfo() {
    // The ViewModel automatically handles the delays and phase progression
    // This composable displays the enemy turn indicator with animation
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFCDD2))
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Enemy Turn",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                CircularProgressIndicator(color = Color.Red)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Enemies are spawning and moving...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Red
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Watch the grid for changes!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFD32F2F),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

@Composable
fun DefenderButton(
    type: DefenderType,
    isSelected: Boolean,
    canAfford: Boolean,
    coinsState: State<Int>,  // Accept State instead of Int
    onClick: () -> Unit
) {
    // Recalculate canAfford based on current coins.value to ensure reactivity
    val actuallyCanAfford = coinsState.value >= type.baseCost

    Button(
        onClick = onClick,
        enabled = actuallyCanAfford,  // Use recalculated value
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF1976D2) else MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier.fillMaxWidth().height(70.dp),
        contentPadding = PaddingValues(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // Tower icon on the left
            Box(
                modifier = Modifier.size(30.dp),
                contentAlignment = Alignment.Center
            ) {
                TowerTypeIcon(defenderType = type, modifier = Modifier.size(28.dp))
            }

            Spacer(modifier = Modifier.width(2.dp))

            // Stats on the right
            Row {
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        type.displayName
                            .replace(" Tower", ""),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1
                    )

                    Text(
                        type.attackType.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = Color(0xFFFFEB3B)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TimerIcon(size = 15.dp)
                        Text(
                            "${type.buildTime}T",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(start = 8.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    TowerStats(type.minRange, type.baseDamage, type.baseRange, type.actionsPerTurn)
                }
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(start = 8.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MoneyIcon(size = 14.dp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${type.baseCost}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GameLegend(modifier: Modifier = Modifier) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header with expand/collapse button
            Row(
                modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Legend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (isExpanded) {
                    TriangleDownIcon(size = 20.dp)
                } else {
                    TriangleLeftIcon(size = 20.dp)
                }
            }

            if (isExpanded) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Areas:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    item {
                        LegendItemHex(
                            color = Color(0xFF8BC34A),
                            label = "⬡",
                            description = "Build Island",
                            border = Color.Gray
                        )
                    }
                    item {
                        LegendItemHex(
                            color = Color(0xFFA5D6A7),
                            label = "⬡",
                            description = "Build Strip",
                            border = Color.Gray
                        )
                    }
                    item {
                        LegendItemHex(
                            color = Color(0xFFFFF8DC),
                            label = "⬡",
                            description = "Enemy Path",
                            border = Color.Gray
                        )
                    }
                    item {
                        LegendItemHex(
                            color = Color(0xFFE0E0E0),
                            label = "⬡",
                            description = "Non-Playable",
                            border = Color.Gray
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Special:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    item {
                        LegendItemHex(
                            color = Color(0xFFFFF8DC),
                            label = "Spawn",
                            description = "Spawn (3 points)",
                            border = Color(0xFFFF9800),
                            borderWidth = 3.dp
                        )
                    }
                    item {
                        LegendItemHex(
                            color = Color(0xFFFFF8DC),
                            label = "Target",
                            description = "Target (Defend!)",
                            border = Color(0xFF4CAF50),
                            borderWidth = 3.dp
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Units:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    item {
                        LegendItemHex(
                            color = Color(0xFF2196F3),
                            label = "⬡",
                            description = "Tower (Ready)",
                            border = Color(0xFF2196F3),
                            borderWidth = 3.dp
                        )
                    }
                    item {
                        LegendItemHex(
                            color = Color(0xFF9E9E9E),
                            label = "⬡",
                            description = "Tower (Building)",
                            border = Color(0xFF9E9E9E),
                            borderWidth = 3.dp
                        )
                    }
                    item {
                        LegendItemHex(
                            color = Color(0xFF7986CB),
                            label = "⬡",
                            description = "Tower (No Actions)",
                            border = Color(0xFF2196F3),
                            borderWidth = 3.dp
                        )
                    }
                    item {
                        LegendItemHex(
                            color = Color(0xFFF44336),
                            label = "⬡",
                            description = "Enemy Unit",
                            border = Color(0xFFF44336),
                            borderWidth = 3.dp
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Info:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    item {
                        Text(
                            "• Ballista: min range 3",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFF6F00)
                        )
                    }
                    item {
                        Text("• Icons show tower/enemy type", style = MaterialTheme.typography.bodySmall)
                    }
                    item {
                        Text(
                            "• Level & actions shown on towers",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    item {
                        Text("• Health shown on enemies", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItemHex(
    color: Color,
    label: String,
    description: String,
    border: Color = Color.Gray,
    borderWidth: androidx.compose.ui.unit.Dp = 1.dp
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp, 28.dp)
                .clip(HexagonShape())
                .background(color)
                .border(borderWidth, border, HexagonShape()),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontSize = 14.sp,
                color = if (color.luminance() > 0.5f) Color.Black else Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(description, style = MaterialTheme.typography.bodySmall)
    }
}

// Extension function to calculate color luminance
private fun Color.luminance(): Float {
    return (0.299f * red + 0.587f * green + 0.114f * blue)
}

@Composable
fun EnemyListPanel(gameState: GameState, modifier: Modifier = Modifier) {
    // Expand by default during initial building phase, collapsed otherwise
    // This state is remembered per GameState instance (each level has its own GameState)
    // so it will auto-expand when a new level is loaded
    var isExpanded by remember { mutableStateOf(gameState.phase.value == GamePhase.INITIAL_BUILDING) }

    // LazyListState to control scrolling
    val listState = rememberLazyListState()

    // Scroll to top when turn changes
    val currentTurn = gameState.turnNumber.value
    LaunchedEffect(currentTurn) {
        if (isExpanded && currentTurn > 0) {
            listState.animateScrollToItem(0)
        }
    }

    // Compute values directly - parent GamePlayScreen's key() will trigger recomposition
    val activeEnemies = gameState.attackers.filter { !it.isDefeated.value }.sortedBy { it.id }

    // Calculate how many enemies have spawned from the spawn plan
    // nextAttackerId starts at 1, so (nextAttackerId - 1) gives us the count of spawned enemies
    val totalSpawned = gameState.nextAttackerId.value - 1

    // Get the remaining planned spawns (those that haven't spawned yet)
    val plannedSpawns = gameState.spawnPlan.drop(totalSpawned)//.take(15)

    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enemies", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (isExpanded) {
                    TriangleDownIcon(size = 20.dp)
                } else {
                    TriangleLeftIcon(size = 20.dp)
                }
            }
            Text(
                "Active: ${activeEnemies.size} | Planned: ${plannedSpawns.size}",
                style = MaterialTheme.typography.bodySmall
            )

            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)
                ) {
                    // Active enemies on the map
                    if (activeEnemies.isNotEmpty()) {
                        item(key = "header-active") {
                            Text(
                                "On Map:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD32F2F)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                    items(
                        items = activeEnemies,
                        key = { attacker -> "active-${attacker.id}" }
                    ) { attacker ->
                        // Key by id, position and health to force recomposition when enemy moves or takes damage
                        key(
                            attacker.id,
                            attacker.position.value.x,
                            attacker.position.value.y,
                            attacker.currentHealth.value
                        ) {
                            EnemyItemDetailed(attacker, showPosition = true)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Planned enemy spawns (show what's left to spawn with turn information)
                    if (plannedSpawns.isNotEmpty()) {
                        item(key = "header-planned") {
                            if (activeEnemies.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Text(
                                "Planned Spawns:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF9800)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        itemsIndexed(
                            items = plannedSpawns,
                            key = { index, _ -> "planned-$index" }
                        ) { index, plannedSpawn ->
                            PlannedEnemyItem(plannedSpawn, gameState.turnNumber.value)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EnemyItemDetailed(attacker: Attacker, showPosition: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        )
    ) {
        Row(
            modifier = Modifier.padding(6.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Enemy icon (small version)
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                EnemyIcon(attacker = attacker, modifier = Modifier.size(28.dp), healthTextColor = Color.Black)
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Enemy details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    attacker.type.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "HP: ${attacker.currentHealth.value}/${attacker.maxHealth}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp
                    )
                    if (showPosition) {
                        Text(
                            "Pos: (${attacker.position.value.x},${attacker.position.value.y})",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = Color(0xFF1976D2)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UpcomingEnemyItem(attackerType: AttackerType) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0)
        )
    ) {
        Row(
            modifier = Modifier.padding(6.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Enemy type icon using graphical representation
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                EnemyTypeIcon(attackerType = attackerType, modifier = Modifier.size(28.dp))
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Enemy details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    attackerType.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "HP: ${attackerType.health}",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun PlannedEnemyItem(plannedSpawn: PlannedEnemySpawn, currentTurn: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0)
        )
    ) {
        Row(
            modifier = Modifier.padding(6.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Enemy type icon using graphical representation
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                EnemyTypeIcon(attackerType = plannedSpawn.attackerType, modifier = Modifier.size(28.dp))
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Enemy details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        plannedSpawn.attackerType.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (plannedSpawn.level > 1) {
                        Text(
                            "Lv${plannedSpawn.level}",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F)
                        )
                    }
                }
                Text(
                    "HP: ${plannedSpawn.healthPoints}",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp
                )
            }

            // Spawn turn
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "Turn ${plannedSpawn.spawnTurn}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = if (plannedSpawn.spawnTurn == currentTurn + 1) Color(0xFFFF5722) else Color(
                        0xFFFF9800
                    )
                )
                if (plannedSpawn.spawnTurn > currentTurn) {
                    Text(
                        "in ${plannedSpawn.spawnTurn - currentTurn} turns",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 9.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun EnemyItem(attacker: Attacker) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    attacker.type.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Text(
                    "ID: ${attacker.id}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                "HP: ${attacker.currentHealth.value}/${attacker.maxHealth}",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                "Reward: ${attacker.type.reward} coins",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFF9800)
            )

            Text(
                "Position: (${attacker.position.value.x}, ${attacker.position.value.y})",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
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

