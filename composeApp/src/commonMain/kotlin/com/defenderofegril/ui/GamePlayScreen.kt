package com.defenderofegril.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.layout
import com.defenderofegril.model.*
import kotlinx.coroutines.delay
import kotlin.math.sqrt

// UI Constants
private const val ATTACK_ICON = "⚔️"
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
    onStartFirstPlayerTurn: () -> Unit,
    onDefenderAttack: (Int, Int) -> Boolean,
    onDefenderAttackPosition: (Int, Position) -> Boolean,
    onEndPlayerTurn: () -> Unit,
    onBackToMap: () -> Unit,
    onCheatCode: ((String) -> Boolean)? = null  // Add cheat code callback
) {
    GamePlayScreenContent(
        gameState = gameState,
        onPlaceDefender = onPlaceDefender,
        onUpgradeDefender = onUpgradeDefender,
        onStartFirstPlayerTurn = onStartFirstPlayerTurn,
        onDefenderAttack = onDefenderAttack,
        onDefenderAttackPosition = onDefenderAttackPosition,
        onEndPlayerTurn = onEndPlayerTurn,
        onBackToMap = onBackToMap,
        onCheatCode = onCheatCode
    )
}

@Composable
private fun GamePlayScreenContent(
    gameState: GameState,
    onPlaceDefender: (DefenderType, Position) -> Boolean,
    onUpgradeDefender: (Int) -> Boolean,
    onStartFirstPlayerTurn: () -> Unit,
    onDefenderAttack: (Int, Int) -> Boolean,
    onDefenderAttackPosition: (Int, Position) -> Boolean,
    onEndPlayerTurn: () -> Unit,
    onBackToMap: () -> Unit,
    onCheatCode: ((String) -> Boolean)? = null
) {
    var selectedDefenderType by remember { mutableStateOf<DefenderType?>(null) }
    var selectedDefenderId by remember { mutableStateOf<Int?>(null) }
    var selectedTargetId by remember { mutableStateOf<Int?>(null) }
    var selectedTargetPosition by remember { mutableStateOf<Position?>(null) }
    var showCheatDialog by remember { mutableStateOf(false) }
    var cheatCodeInput by remember { mutableStateOf("") }
    var showOverlay by remember { mutableStateOf(false) }  // MutableState for overlay visibility
    
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header with prominent phase indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Level: ${gameState.level.name}", style = MaterialTheme.typography.titleLarge)
                // Clickable coins display for cheat codes
                Text(
                    "Coins: ${gameState.coins.value}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.clickable(
                        onClick = { 
                            if (onCheatCode != null) {
                                showCheatDialog = true
                            }
                        }
                    )
                )
                Text("Health: ${gameState.healthPoints.value}", style = MaterialTheme.typography.bodyLarge)
                Text("Turn: ${gameState.turnNumber.value}", style = MaterialTheme.typography.bodyMedium)

                val activeEnemies = gameState.attackers.count { !it.isDefeated.value }
                // val remainingEnemies = gameState.attackersToSpawn.size

                // Calculate how many enemies have spawned from the spawn plan
                // nextAttackerId starts at 1, so (nextAttackerId - 1) gives us the count of spawned enemies
                val totalSpawned = gameState.nextAttackerId.value - 1

                // Get the remaining planned spawns (those that haven't spawned yet)
                val plannedSpawns = gameState.spawnPlan.drop(totalSpawned)//.take(15)

                val remainingEnemies = plannedSpawns.size

                Text("Enemies: $activeEnemies active, $remainingEnemies to come", 
                     style = MaterialTheme.typography.bodyMedium,
                     color = Color(0xFFF44336))
            }
            
            // Prominent phase indicator
            val phaseText = when(gameState.phase.value) {
                GamePhase.INITIAL_BUILDING -> "Initial Building Phase"
                GamePhase.PLAYER_TURN -> "YOUR TURN"
                GamePhase.ENEMY_TURN -> "ENEMY TURN"
            }
            val phaseColor = when(gameState.phase.value) {
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

                // Toggle button positioned above the map and far to the right
                Button(
                    onClick = { showOverlay = !showOverlay },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showOverlay) Color(0xFF4CAF50) else Color(0xFF2196F3)
                    )
                ) {
                    Text(if (showOverlay) "Hide Info  ◀" else "Show Info  ▶")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Game Grid with toggle button and overlay
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Scrollable Game Grid
            GameGrid(
                gameState = gameState,
                selectedDefenderType = selectedDefenderType,
                selectedDefenderId = selectedDefenderId,
                selectedTargetId = selectedTargetId,
                selectedTargetPosition = selectedTargetPosition,
                onCellClick = { position ->
                    // Try to place defender if one is selected
                    selectedDefenderType?.let { type ->
                        if (onPlaceDefender(type, position)) {
                            selectedDefenderType = null
                        }
                        return@GameGrid
                    }
                    
                    // Check if there's a defender at this position
                    val defender = gameState.defenders.find { it.position == position }
                    if (defender != null) {
                        selectedDefenderId = defender.id
                        selectedTargetId = null
                        selectedTargetPosition = null
                        return@GameGrid
                    }
                    
                    // Handle targeting for selected defender
                    if (selectedDefenderId != null) {
                        val selectedDefender = gameState.defenders.find { it.id == selectedDefenderId }
                        if (selectedDefender != null) {
                            // For AOE/DOT towers, allow targeting path tiles
                            if (selectedDefender.type.attackType == AttackType.AOE ||
                                selectedDefender.type.attackType == AttackType.DOT) {
                                // Check if position is on the path and in range
                                val distance = selectedDefender.position.distanceTo(position)
                                if (gameState.level.isOnPath(position) &&
                                    distance >= selectedDefender.type.minRange &&
                                    distance <= selectedDefender.range) {
                                    selectedTargetPosition = position
                                    // Also set targetId if there's an enemy at this position
                                    val enemyAtPosition = gameState.attackers.find { it.position.value == position && !it.isDefeated.value }
                                    selectedTargetId = enemyAtPosition?.id
                                }
                            } else {
                                // For single-target attacks, only allow targeting enemies
                                val attacker = gameState.attackers.find { it.position.value == position && !it.isDefeated.value }
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
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Control Panel based on phase
        when (gameState.phase.value) {
            GamePhase.INITIAL_BUILDING -> {
                InitialBuildingControls(
                    gameState = gameState,
                    coinsState = gameState.coins,
                    selectedDefenderType = selectedDefenderType,
                    selectedDefenderId = selectedDefenderId,
                    onSelectDefenderType = { selectedDefenderType = it },
                    onUpgradeDefender = { onUpgradeDefender(it) },
                    onStartFirstPlayerTurn = {
                        selectedDefenderType = null  // Clear defender type selection when starting battle
                        selectedDefenderId = null  // Clear defender selection when starting battle
                        onStartFirstPlayerTurn()
                    }
                )
            }
            GamePhase.PLAYER_TURN -> {
                PlayerTurnControls(
                    gameState = gameState,
                    coinsState = gameState.coins,
                    selectedDefenderType = selectedDefenderType,
                    selectedDefenderId = selectedDefenderId,
                    selectedTargetId = selectedTargetId,
                    selectedTargetPosition = selectedTargetPosition,
                    onSelectDefenderType = { selectedDefenderType = it },
                    onUpgradeDefender = { onUpgradeDefender(it) },
                    onDefenderAttack = { defenderId, targetId ->
                        if (onDefenderAttack(defenderId, targetId)) {
                            selectedTargetId = null
                            selectedTargetPosition = null
                        }
                    },
                    onDefenderAttackPosition = { defenderId, targetPos ->
                        if (onDefenderAttackPosition(defenderId, targetPos)) {
                            selectedTargetId = null
                            selectedTargetPosition = null
                        }
                    },
                    onEndPlayerTurn = onEndPlayerTurn
                )
            }
            GamePhase.ENEMY_TURN -> {
                EnemyTurnInfo()
            }
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
                        Text("• moneybags, 1000coins, cash - Get 1000 coins", style = MaterialTheme.typography.bodySmall)
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

@Composable
fun GameGrid(
    gameState: GameState,
    selectedDefenderType: DefenderType?,
    selectedDefenderId: Int?,
    selectedTargetId: Int?,
    selectedTargetPosition: Position?,
    onCellClick: (Position) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val hexSize = 40.dp  // Radius of hexagon (center to corner)
    
    // Calculate hex dimensions for pointy-top hexagons
    val sqrt3 = sqrt(3.0).toFloat()
    val hexWidth = hexSize.value * sqrt3  // Width of hexagon (flat-to-flat)
    val hexHeight = hexSize.value * 2f    // Height of hexagon (point-to-point)
    
    // For pointy-top hexagons, vertical spacing between centers is 3/4 of height
    val verticalSpacing = hexHeight * 0.75f
    
    // Calculate total grid width: each column takes hexWidth, plus offset for odd rows
    // Add significant padding at the end to ensure target is visible when scrolling
    val totalGridWidth = ((gameState.level.gridWidth) * hexWidth + hexWidth + 200f).dp
    
    Box(
        modifier = modifier.fillMaxWidth().horizontalScroll(scrollState)
    ) {
        Column(
            modifier = Modifier.width(totalGridWidth),  // Set explicit width for scrolling
            verticalArrangement = Arrangement.spacedBy((-hexHeight + verticalSpacing - 7f).dp)  // Extra tight spacing to eliminate all gaps
        ) {
            for (y in 0 until gameState.level.gridHeight) {
                Row(
                    modifier = Modifier.offset(x = if (y % 2 == 1) (hexWidth * 0.42f).dp else 0.dp),  // Offset odd rows 42% to eliminate final gaps
                    horizontalArrangement = Arrangement.spacedBy((-10).dp)  // Even tighter horizontal spacing
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
                            /* TODO from main
                            isDefenderSelected = gameState.defenders.find { it.position == position }?.id == selectedDefenderId,
                            isTargetSelected = gameState.attackers.find { it.position == position }?.id == selectedTargetId || position == selectedTargetPosition,
                             */
                            selectedDefenderId = selectedDefenderId,
                            onClick = { onCellClick(position) },
                            hexSize = hexSize
                        )
                    }
                }
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
                FieldEffectType.FIREBALL_AOE -> Color(0xFFFF9800).copy(alpha = 0.5f)  // Orange tint for fireball
                FieldEffectType.ACID_DOT -> Color(0xFF4CAF50).copy(alpha = 0.6f)  // Green tint for acid
            }
        }
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
    
    val borderColor = when {
        cellIsInRange && isOnPath && showRange -> Color(0xFF4CAF50)  // Green border for tiles in range (only on path, only if actions available)
        isDefenderSelected && gameState.phase.value != GamePhase.INITIAL_BUILDING -> Color(0xFFFFEB3B)  // Yellow border for selected defender (not during initial building)
        isSpawnPoint -> Color(0xFFFF9800)  // Orange border for spawn
        isTarget -> Color(0xFF4CAF50)  // Green border for target
        attacker != null -> Color(0xFFF44336)  // Red border for enemies
        defender != null -> if (defender.isReady) Color(0xFF2196F3) else Color(0xFF9E9E9E)  // Blue/gray border for towers
        fieldEffect != null -> {
            when (fieldEffect.type) {
                FieldEffectType.FIREBALL_AOE -> Color(0xFFFF5722)  // Deep orange border for fireball
                FieldEffectType.ACID_DOT -> Color(0xFF4CAF50)  // Green border for acid
            }
        }
        else -> Color.Transparent  // No borders for empty cells
    }
    
    // Thicker borders for important elements
    val borderWidth = when {
        isDefenderSelected && gameState.phase.value != GamePhase.INITIAL_BUILDING -> 5.dp  // Extra thick border for selected defender (not during initial building)
        cellIsInRange && isOnPath && showRange -> 4.dp  // Thick border for cells in range
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
                    TowerIcon(defender = defender)
                }
            }
            fieldEffect != null -> {
                // Show field effect info
                when (fieldEffect.type) {
                    FieldEffectType.FIREBALL_AOE -> {
                        // Show fireball symbol
                        Text(
                            "💥",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color(0xFFFF5722)
                        )
                    }
                    FieldEffectType.ACID_DOT -> {
                        // Show acid splash with damage and duration
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "🧪",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF4CAF50)
                            )
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
fun InitialBuildingControls(
    gameState: GameState,
    coinsState: State<Int>,
    selectedDefenderType: DefenderType?,
    selectedDefenderId: Int?,
    onSelectDefenderType: (DefenderType?) -> Unit,
    onUpgradeDefender: (Int) -> Unit,
    onStartFirstPlayerTurn: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Initial Building Phase - Place towers (no build time)", 
             style = MaterialTheme.typography.titleMedium)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            modifier = Modifier.fillMaxWidth().height(90.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(DefenderType.entries.toTypedArray(), key = { type -> "${type.name}_${coinsState.value}_${gameState.defenders.count { it.type == type }}" }) { type ->
                // Directly calculate canAfford using coinsState.value to ensure immediate reactivity
                val canAfford = coinsState.value >= type.baseCost
                println("DEBUG: InitialBuilding Button for ${type.displayName} - coins: ${coinsState.value}, cost: ${type.baseCost}, canAfford: $canAfford")
                DefenderButton(
                    type = type,
                    isSelected = selectedDefenderType == type,
                    canAfford = canAfford,
                    coinsState = coinsState,  // Pass State instead of Int
                    onClick = {
                        onSelectDefenderType(if (selectedDefenderType == type) null else type)
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        selectedDefenderId?.let { id ->
            val defender = gameState.defenders.find { it.id == id }
            if (defender != null) {
                DefenderInfo(defender, gameState, onUpgradeDefender)
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = onStartFirstPlayerTurn,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start Battle")
        }
    }
}

@Composable
fun PlayerTurnControls(
    gameState: GameState,
    coinsState: State<Int>,
    selectedDefenderType: DefenderType?,
    selectedDefenderId: Int?,
    selectedTargetId: Int?,
    selectedTargetPosition: Position?,
    onSelectDefenderType: (DefenderType?) -> Unit,
    onUpgradeDefender: (Int) -> Unit,
    onDefenderAttack: (Int, Int) -> Unit,
    onDefenderAttackPosition: (Int, Position) -> Unit,
    onEndPlayerTurn: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Your Turn - Place towers and attack enemies",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Defender placement buttons
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            modifier = Modifier.fillMaxWidth().height(90.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(
                DefenderType.entries.toTypedArray(),
                key = { type -> "${type.name}_${coinsState.value}_${gameState.defenders.count { it.type == type }}" }) { type ->
                // Directly calculate canAfford using coinsState.value to ensure immediate reactivity
                val canAfford = coinsState.value >= type.baseCost
                println("DEBUG: PlayerTurn Button for ${type.displayName} - coins: ${coinsState.value}, cost: ${type.baseCost}, canAfford: $canAfford")
                DefenderButton(
                    type = type,
                    isSelected = selectedDefenderType == type,
                    canAfford = canAfford,
                    coinsState = coinsState,  // Pass State instead of Int
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
                DefenderInfo(defender, gameState, onUpgradeDefender,)

                // Spacer(modifier = Modifier.height(8.dp))
                AttackButton(
                    defender = defender,
                    gameState = gameState,
                    selectedTargetId = selectedTargetId,
                    selectedTargetPosition = selectedTargetPosition,
                    onDefenderAttack = onDefenderAttack,
                    onDefenderAttackPosition = onDefenderAttackPosition,
                    modifier = Modifier
                        .layout { measurable, constraints ->
                            // Measure the tooltip but don't add it to the layout
                            val placeable = measurable.measure(constraints)
                            layout(0,0) { // Set the size to 0 to avoid taking up space and move other elements
                                placeable.place(0, 0)
                            }
                        }
                        .width(200.dp)
                        .height(100.dp)
                        .absoluteOffset(x = 1000.dp, y = (-130).dp) // .absoluteOffset(x = 700.dp, y = (-130).dp)

                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onEndPlayerTurn,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
        ) {
            Text("End Turn")
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
        if ((defender.type.attackType == AttackType.AOE || defender.type.attackType == AttackType.DOT) && selectedTargetPosition != null) {
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
                            Text(ATTACK_ICON, fontSize = 24.sp)
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
                        Text(ATTACK_ICON, fontSize = 24.sp)
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
                        Text(ATTACK_ICON, fontSize = 24.sp)
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
    Text(
        "💥 ${damage}",
        style = MaterialTheme.typography.bodySmall
    )
    if (minRange > 0) {
        Text(
            "🎯 ${minRange}-${range}",
            style = MaterialTheme.typography.bodySmall
        )
    } else {
        Text(
            "🎯 ${range}",
            style = MaterialTheme.typography.bodySmall
        )
    }
    Text(
        "⚡  ${actionsPerTurn}",
        style = MaterialTheme.typography.bodySmall
    )
}

            @Composable
            fun DefenderInfo(
                defender: Defender,
                gameState: GameState,
                onUpgradeDefender: (Int) -> Unit
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
                    Card(modifier = Modifier
                        .fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            // Tower icon, name, and actions in one row
                            Row(
                                // modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                // Tower icon
                                Box(
                                    modifier = Modifier.size(48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    TowerIcon(defender = defender, modifier = Modifier.size(44.dp))
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Tower name and level
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        defender.type.displayName,
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
                                        Text(
                                            "⚔️ ${defender.type.attackType.name}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(modifier = Modifier.weight(1f)) {
                                    DefenderActionsInfo(defender)
                                }
                                Spacer(modifier = Modifier.weight(6f))
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            if (defender.isReady) {
                                // Calculate next level stats for comparison
                                val nextLevelDamage = defender.damage + 5
                                val nextActualDamage = when (defender.type.attackType) {
                                    AttackType.DOT -> nextLevelDamage / 2
                                    else -> nextLevelDamage
                                }
                                val nextRange = defender.range + (if (defender.level.value % 2 == 0) 1 else 0)

                                // Stats and upgrade button in columns
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                            defender.actualDamage,
                                            defender.range,
                                            defender.type.actionsPerTurn
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
                                            defender.type.actionsPerTurn
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        UpgradeButton(defender, gameState, onUpgradeDefender = onUpgradeDefender)
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        // Text("placeholder for the sell/remove tower button")
                                        Button(
                                            modifier = Modifier
                                                .offset(y = (-24).dp)
                                                .width(200.dp)
                                                .height(100.dp),
                                            onClick = { /* TODO: Implement sell/remove tower */ },
                                            enabled = false
                                        ) {
                                            Text("Sell Tower (Placeholder)")
                                        }
                                    }
                                    Spacer(modifier = Modifier.weight(4f))
                                }
                            }
                        }
                    }
                }
            }

@Composable
fun UpgradeButton(
    defender: Defender,
    gameState: GameState,
    modifier: Modifier = Modifier
        .offset(y = (-24).dp)
        .width(200.dp)
        .height(100.dp),
    onUpgradeDefender: (Int) -> Unit) {
    Button(
        onClick = { onUpgradeDefender(defender.id) },
        enabled = gameState.canUpgradeDefender(defender),
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Upgrade", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "💰${defender.upgradeCost}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

            @Composable
            fun DefenderActionsInfo(defender: Defender) {
                if (!defender.isReady) {
                    Text(
                        "⏱ Building: ${defender.buildTimeRemaining.value}T",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFFF9800)
                    )
                } else {
                    Text(
                        "⚡ ${defender.actionsRemaining.value}/${defender.type.actionsPerTurn}",
                        style = MaterialTheme.typography.titleMedium,
                    )
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
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        // Tower icon on the left
                        Box(
                            modifier = Modifier.size(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            TowerTypeIcon(defenderType = type, modifier = Modifier.size(32.dp))
                        }
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        // Stats on the right
                        Row {
                            Column(
                                modifier = Modifier.fillMaxHeight(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    type.displayName.replace(" Tower", ""),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1
                                )

                                // Show special ability type
                                // "Long" indicates ranged with minimum range (e.g., Ballista can't shoot too close)
                                // "Range" indicates normal ranged attack without minimum range restriction
                                val special = when (type.attackType) {
                                    AttackType.AOE -> "Throws Fireball" //Area of Effect
                                    AttackType.DOT -> "Throws Acid" //Damage over Time
                                    AttackType.MELEE -> "Melee"
                                    AttackType.RANGED -> if (type.minRange > 0) "Long Range" else "Range"
                                }
                                Text(
                                    special,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 12.sp,
                                    color = Color(0xFFFFEB3B)
                                )
                                Text(
                                    "⏱ ${type.buildTime}T",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 12.sp
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(start = 16.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.Start
                            ) {
                                TowerStats(type.minRange, type.baseDamage, type.baseRange, type.actionsPerTurn)
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(start = 16.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    "💰${type.baseCost}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 20.sp
                                )
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
                            Text(if (isExpanded) "▼" else "▶", style = MaterialTheme.typography.titleMedium)
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
                            Text(if (isExpanded) "▼" else "▶", fontSize = 16.sp)
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
                            EnemyIcon(attacker = attacker, modifier = Modifier.size(28.dp))
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
                            Text(
                                plannedSpawn.attackerType.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "HP: ${plannedSpawn.attackerType.health}",
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

