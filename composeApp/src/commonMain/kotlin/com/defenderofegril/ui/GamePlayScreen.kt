package com.defenderofegril.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.defenderofegril.model.*
import kotlinx.coroutines.delay
import kotlin.math.sqrt

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
    coins: State<Int>,  // Add coins State parameter
    onPlaceDefender: (DefenderType, Position) -> Boolean,
    onUpgradeDefender: (Int) -> Boolean,
    onStartFirstPlayerTurn: () -> Unit,
    onDefenderAttack: (Int, Int) -> Boolean,
    onEndPlayerTurn: () -> Unit,
    onBackToMap: () -> Unit,
    onCheatCode: ((String) -> Boolean)? = null  // Add cheat code callback
) {
    // Force recomposition when game state changes by using key properties
    key(gameState.turnNumber, gameState.phase, gameState.attackers.size, gameState.defenders.size, gameState.coins) {
        GamePlayScreenContent(
            gameState = gameState,
            coins = coins,  // Pass coins State
            onPlaceDefender = onPlaceDefender,
            onUpgradeDefender = onUpgradeDefender,
            onStartFirstPlayerTurn = onStartFirstPlayerTurn,
            onDefenderAttack = onDefenderAttack,
            onEndPlayerTurn = onEndPlayerTurn,
            onBackToMap = onBackToMap,
            onCheatCode = onCheatCode
        )
    }
}

@Composable
private fun GamePlayScreenContent(
    gameState: GameState,
    coins: State<Int>,  // Add coins State parameter
    onPlaceDefender: (DefenderType, Position) -> Boolean,
    onUpgradeDefender: (Int) -> Boolean,
    onStartFirstPlayerTurn: () -> Unit,
    onDefenderAttack: (Int, Int) -> Boolean,
    onEndPlayerTurn: () -> Unit,
    onBackToMap: () -> Unit,
    onCheatCode: ((String) -> Boolean)? = null
) {
    var selectedDefenderType by remember { mutableStateOf<DefenderType?>(null) }
    var selectedDefenderId by remember { mutableStateOf<Int?>(null) }
    var selectedTargetId by remember { mutableStateOf<Int?>(null) }
    var showCheatDialog by remember { mutableStateOf(false) }
    var cheatCodeInput by remember { mutableStateOf("") }
    
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
                    "Coins: ${gameState.coins}", 
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.clickable(
                        onClick = { 
                            if (onCheatCode != null) {
                                showCheatDialog = true
                            }
                        }
                    )
                )
                Text("Health: ${gameState.healthPoints}", style = MaterialTheme.typography.bodyLarge)
                Text("Turn: ${gameState.turnNumber}", style = MaterialTheme.typography.bodyMedium)
                val activeEnemies = gameState.attackers.count { !it.isDefeated }
                val remainingEnemies = gameState.attackersToSpawn.size
                Text("Enemies: $activeEnemies active, $remainingEnemies to come", 
                     style = MaterialTheme.typography.bodyMedium,
                     color = Color(0xFFF44336))
            }
            
            // Prominent phase indicator
            val phaseText = when(gameState.phase) {
                GamePhase.INITIAL_BUILDING -> "Initial Building Phase"
                GamePhase.PLAYER_TURN -> "YOUR TURN"
                GamePhase.ENEMY_TURN -> "ENEMY TURN"
            }
            val phaseColor = when(gameState.phase) {
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
            
            Button(onClick = onBackToMap) {
                Text("Back to Map")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Game Grid with Legend and Enemy List
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Scrollable Game Grid
            GameGrid(
                gameState = gameState,
                selectedDefenderType = selectedDefenderType,
                selectedDefenderId = selectedDefenderId,
                selectedTargetId = selectedTargetId,
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
                        return@GameGrid
                    }
                    
                    // Check if there's an attacker at this position (for targeting)
                    val attacker = gameState.attackers.find { it.position == position && !it.isDefeated }
                    if (attacker != null && selectedDefenderId != null) {
                        selectedTargetId = attacker.id
                    }
                },
                modifier = Modifier.weight(2f)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Side panel with Legend and Enemy List
            Column(modifier = Modifier.width(250.dp)) {
                // Legend
                GameLegend(modifier = Modifier.fillMaxWidth())
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Enemy List
                EnemyListPanel(gameState = gameState, modifier = Modifier.fillMaxWidth().weight(1f))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Control Panel based on phase
        when (gameState.phase) {
            GamePhase.INITIAL_BUILDING -> {
                InitialBuildingControls(
                    gameState = gameState,
                    coinsState = coins,
                    selectedDefenderType = selectedDefenderType,
                    selectedDefenderId = selectedDefenderId,
                    onSelectDefenderType = { selectedDefenderType = it },
                    onUpgradeDefender = { onUpgradeDefender(it) },
                    onStartFirstPlayerTurn = onStartFirstPlayerTurn
                )
            }
            GamePhase.PLAYER_TURN -> {
                PlayerTurnControls(
                    gameState = gameState,
                    coinsState = coins,
                    selectedDefenderType = selectedDefenderType,
                    selectedDefenderId = selectedDefenderId,
                    selectedTargetId = selectedTargetId,
                    onSelectDefenderType = { selectedDefenderType = it },
                    onUpgradeDefender = { onUpgradeDefender(it) },
                    onDefenderAttack = { defenderId, targetId ->
                        if (onDefenderAttack(defenderId, targetId)) {
                            selectedTargetId = null
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
                            isDefenderSelected = gameState.defenders.find { it.position == position }?.id == selectedDefenderId,
                            isTargetSelected = gameState.attackers.find { it.position == position }?.id == selectedTargetId,
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
    val attacker = gameState.attackers.find { it.position == position && !it.isDefeated }
    
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
    val backgroundColor = when {
        attacker != null -> Color(0xFFF44336)  // Red background for enemies
        defender != null -> {
            when {
                !defender.isReady -> Color(0xFF9E9E9E)  // Gray for building
                defender.actionsRemaining <= 0 -> Color(0xFF7986CB)  // Blue-gray mix for used up actions
                else -> Color(0xFF2196F3)  // Blue for ready with actions
            }
        }
        isDefenderSelected && gameState.phase != GamePhase.INITIAL_BUILDING -> baseBackgroundColor.copy(alpha = 0.7f)
        isTargetSelected && gameState.phase != GamePhase.INITIAL_BUILDING -> baseBackgroundColor.copy(alpha = 0.8f)
        else -> baseBackgroundColor  // No selection highlighting during placement or in initial phase
    }
    
    // Border color - use borders to indicate entities instead of background
    // For range visualization, show green border on path tiles in range (only if tower has actions)
    val showRange = selectedDefenderId?.let { defenderId ->
        val selectedDefender = gameState.defenders.find { it.id == defenderId }
        selectedDefender?.isReady == true && selectedDefender.actionsRemaining > 0
    } ?: false
    
    val borderColor = when {
        cellIsInRange && isOnPath && showRange -> Color(0xFF4CAF50)  // Green border for tiles in range (only on path, only if actions available)
        isDefenderSelected && gameState.phase != GamePhase.INITIAL_BUILDING -> Color(0xFFFFEB3B)  // Yellow border for selected defender (not during initial building)
        isSpawnPoint -> Color(0xFFFF9800)  // Orange border for spawn
        isTarget -> Color(0xFF4CAF50)  // Green border for target
        attacker != null -> Color(0xFFF44336)  // Red border for enemies
        defender != null -> if (defender.isReady) Color(0xFF2196F3) else Color(0xFF9E9E9E)  // Blue/gray border for towers
        else -> Color.Transparent  // No borders for empty cells
    }
    
    // Thicker borders for important elements
    val borderWidth = when {
        isDefenderSelected && gameState.phase != GamePhase.INITIAL_BUILDING -> 5.dp  // Extra thick border for selected defender (not during initial building)
        cellIsInRange && isOnPath && showRange -> 4.dp  // Thick border for cells in range
        isSpawnPoint || isTarget -> 3.dp
        attacker != null || defender != null -> 3.dp
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
                // Key by both id and currentHealth to force recomposition when health changes
                key(attacker.id, attacker.currentHealth) {
                    EnemyIcon(attacker = attacker)
                }
            }
            defender != null -> {
                // Use graphical icon for towers
                // Key by id, level and actionsRemaining to force recomposition when these change
                key(defender.id, defender.level, defender.actionsRemaining, defender.buildTimeRemaining) {
                    TowerIcon(defender = defender)
                }
            }
            isSpawnPoint -> {
                // Show spawn indicator when cell is empty
                Text("S", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF9800))
            }
            isTarget -> {
                // Show target indicator when cell is empty
                Text("T", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
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
            modifier = Modifier.fillMaxWidth().height(85.dp),
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
    onSelectDefenderType: (DefenderType?) -> Unit,
    onUpgradeDefender: (Int) -> Unit,
    onDefenderAttack: (Int, Int) -> Unit,
    onEndPlayerTurn: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Your Turn - Place towers and attack enemies", 
             style = MaterialTheme.typography.titleMedium)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Defender placement buttons
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            modifier = Modifier.fillMaxWidth().height(75.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(DefenderType.entries.toTypedArray(), key = { type -> "${type.name}_${coinsState.value}_${gameState.defenders.count { it.type == type }}" }) { type ->
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
                DefenderInfo(defender, gameState, onUpgradeDefender)
                
                if (defender.isReady && defender.actionsRemaining > 0 && selectedTargetId != null) {
                    val target = gameState.attackers.find { it.id == selectedTargetId }
                    if (target != null && defender.canAttack(target)) {
                        Button(
                            onClick = { onDefenderAttack(defenderId, selectedTargetId) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Attack ${target.type.displayName} (${target.currentHealth}/${target.maxHealth} HP)")
                        }
                    }
                }
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
fun DefenderInfo(
    defender: Defender,
    gameState: GameState,
    onUpgradeDefender: (Int) -> Unit
) {
    // Use key to force recomposition when defender stats change
    key(defender.id, defender.level, defender.damage, defender.range, defender.actionsRemaining, defender.buildTimeRemaining, defender.isReady) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("${defender.type.displayName} (Lvl ${defender.level})")
                if (!defender.isReady) {
                    Text("Building: ${defender.buildTimeRemaining} turns", 
                         style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("Actions: ${defender.actionsRemaining}/${defender.type.actionsPerTurn}",
                         style = MaterialTheme.typography.bodySmall)
                    if (defender.type.minRange > 0) {
                        Text("Damage: ${defender.damage}, Range: ${defender.type.minRange}-${defender.range}",
                             style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text("Damage: ${defender.damage}, Range: ${defender.range}",
                             style = MaterialTheme.typography.bodySmall)
                    }
                    
                    if (gameState.canUpgradeDefender(defender)) {
                        val nextDamage = defender.damage + 5
                        val nextRange = defender.range + (if (defender.level % 2 == 0) 1 else 0)
                        Text("After upgrade: Damage ${nextDamage}, Range ${nextRange}",
                             style = MaterialTheme.typography.bodySmall,
                             color = Color(0xFF4CAF50))
                    }
                    
                    Button(
                        onClick = { onUpgradeDefender(defender.id) },
                        enabled = gameState.canUpgradeDefender(defender),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Upgrade (${defender.upgradeCost} coins)")
                    }
                }
            }
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
        modifier = Modifier.fillMaxWidth().height(65.dp),
        contentPadding = PaddingValues(2.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(type.displayName.split(" ")[0], style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontSize = 9.sp)
            Text("${type.baseCost}c", 
                 style = MaterialTheme.typography.labelSmall,
                 fontSize = 8.sp)
            Text("R:${if (type.minRange > 0) "${type.minRange}-" else ""}${type.baseRange}", 
                 style = MaterialTheme.typography.labelSmall,
                 fontSize = 7.sp)
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
                        Text("Areas:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    
                    item {
                        LegendItemHex(color = Color(0xFF8BC34A), label = "⬡", description = "Build Island", border = Color.Gray)
                    }
                    item {
                        LegendItemHex(color = Color(0xFFA5D6A7), label = "⬡", description = "Build Strip", border = Color.Gray)
                    }
                    item {
                        LegendItemHex(color = Color(0xFFFFF8DC), label = "⬡", description = "Enemy Path", border = Color.Gray)
                    }
                    item {
                        LegendItemHex(color = Color(0xFFE0E0E0), label = "⬡", description = "Non-Playable", border = Color.Gray)
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Special:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    
                    item {
                        LegendItemHex(color = Color(0xFFFFF8DC), label = "S", description = "Spawn (3 points)", border = Color(0xFFFF9800), borderWidth = 3.dp)
                    }
                    item {
                        LegendItemHex(color = Color(0xFFFFF8DC), label = "T", description = "Target (Defend!)", border = Color(0xFF4CAF50), borderWidth = 3.dp)
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Units:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    
                    item {
                        LegendItemHex(color = Color(0xFF2196F3), label = "⬡", description = "Tower (Ready)", border = Color(0xFF2196F3), borderWidth = 3.dp)
                    }
                    item {
                        LegendItemHex(color = Color(0xFF9E9E9E), label = "⬡", description = "Tower (Building)", border = Color(0xFF9E9E9E), borderWidth = 3.dp)
                    }
                    item {
                        LegendItemHex(color = Color(0xFF7986CB), label = "⬡", description = "Tower (No Actions)", border = Color(0xFF2196F3), borderWidth = 3.dp)
                    }
                    item {
                        LegendItemHex(color = Color(0xFFF44336), label = "⬡", description = "Enemy Unit", border = Color(0xFFF44336), borderWidth = 3.dp)
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Info:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    
                    item {
                        Text("• Ballista: min range 3", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFF6F00))
                    }
                    item {
                        Text("• Icons show tower/enemy type", style = MaterialTheme.typography.bodySmall)
                    }
                    item {
                        Text("• Level & actions shown on towers", style = MaterialTheme.typography.bodySmall)
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
    var isExpanded by remember { mutableStateOf(false) }
    
    // Direct observation - Compose will track changes
    val activeEnemies = gameState.attackers.filter { !it.isDefeated }.sortedBy { it.id }
    val toSpawnList = gameState.attackersToSpawn.take(15)
    
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
                "Active: ${activeEnemies.size} | To Spawn: ${toSpawnList.size}",
                style = MaterialTheme.typography.bodySmall
            )
            
            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
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
                        EnemyItemDetailed(attacker, showPosition = true)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    
                    // Planned enemy spawns (show what's left to spawn)
                    if (toSpawnList.isNotEmpty()) {
                        item(key = "header-tospawn") {
                            if (activeEnemies.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Text(
                                "To Spawn:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF9800)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        itemsIndexed(
                            items = toSpawnList,  // Show up to 15 upcoming enemies
                            key = { index, _ -> "tospawn-$index" }
                        ) { index, attackerType ->
                            UpcomingEnemyItem(attackerType)
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
                        "HP: ${attacker.currentHealth}/${attacker.maxHealth}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp
                    )
                    if (showPosition) {
                        Text(
                            "Pos: (${attacker.position.x},${attacker.position.y})",
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
            // Enemy type icon placeholder (small colored box)
            Box(
                modifier = Modifier.size(32.dp).background(Color(0xFFFF5722), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    attackerType.displayName.take(1),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
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
            // Enemy type icon placeholder (small colored box)
            Box(
                modifier = Modifier.size(32.dp).background(Color(0xFFFF5722), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    plannedSpawn.attackerType.displayName.take(1),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
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
                    color = if (plannedSpawn.spawnTurn == currentTurn + 1) Color(0xFFFF5722) else Color(0xFFFF9800)
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
                "HP: ${attacker.currentHealth}/${attacker.maxHealth}",
                style = MaterialTheme.typography.bodySmall
            )
            
            Text(
                "Reward: ${attacker.type.reward} coins",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFF9800)
            )
            
            Text(
                "Position: (${attacker.position.x}, ${attacker.position.y})",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

