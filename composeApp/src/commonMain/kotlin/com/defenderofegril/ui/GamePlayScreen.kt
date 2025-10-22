package com.defenderofegril.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.defenderofegril.model.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

@Composable
fun GamePlayScreen(
    gameState: GameState,
    coins: State<Int>,  // Add coins State parameter
    onPlaceDefender: (DefenderType, Position) -> Boolean,
    onUpgradeDefender: (Int) -> Boolean,
    onStartFirstPlayerTurn: () -> Unit,
    onDefenderAttack: (Int, Int) -> Boolean,
    onEndPlayerTurn: () -> Unit,
    onBackToMap: () -> Unit
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
            onBackToMap = onBackToMap
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
    onBackToMap: () -> Unit
) {
    var selectedDefenderType by remember { mutableStateOf<DefenderType?>(null) }
    var selectedDefenderId by remember { mutableStateOf<Int?>(null) }
    var selectedTargetId by remember { mutableStateOf<Int?>(null) }
    
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
                Text("Coins: ${gameState.coins}", style = MaterialTheme.typography.bodyLarge)
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
    
    // Hexagon dimensions - larger size for better visibility
    val hexSize = 55f // radius of hexagon (increased from 30 for better visibility)
    val hexWidth = hexSize * 2f
    val hexHeight = hexSize * kotlin.math.sqrt(3f)
    
    Column(
        modifier = modifier.fillMaxWidth().horizontalScroll(scrollState),
        horizontalAlignment = Alignment.Start
    ) {
        for (y in 0 until gameState.level.gridHeight) {
            Row(
                modifier = Modifier.offset(x = if (y % 2 == 1) (hexWidth * 0.75f).dp else 0.dp)
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

@Composable
fun GridCell(
    position: Position,
    gameState: GameState,
    isSelected: Boolean,
    isDefenderSelected: Boolean,
    isTargetSelected: Boolean,
    selectedDefenderId: Int?,
    onClick: () -> Unit,
    hexSize: Float = 55f
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
    
    // Base background color based on area type
    val baseBackgroundColor = when {
        isBuildIsland -> Color(0xFF8BC34A)  // Light green for build islands
        isBuildArea -> Color(0xFFA5D6A7)  // Medium green for strips adjacent to path
        isOnPath -> Color(0xFFFFF8DC)  // Cream/beige for enemy path
        else -> Color(0xFFE0E0E0)  // Light gray for off-path areas (non-playable)
    }
    
    // Apply selection states
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
        else -> baseBackgroundColor
    }
    
    // Border color
    val showRange = selectedDefenderId?.let { defenderId ->
        val selectedDefender = gameState.defenders.find { it.id == defenderId }
        selectedDefender?.isReady == true && selectedDefender.actionsRemaining > 0
    } ?: false
    
    val borderColor = when {
        cellIsInRange && isOnPath && showRange -> Color(0xFF4CAF50)  // Green border for tiles in range
        isDefenderSelected && gameState.phase != GamePhase.INITIAL_BUILDING -> Color(0xFFFFEB3B)  // Yellow border for selected defender
        isSpawnPoint -> Color(0xFFFF9800)  // Orange border for spawn
        isTarget -> Color(0xFF4CAF50)  // Green border for target
        attacker != null -> Color(0xFFF44336)  // Red border for enemies
        defender != null -> if (defender.isReady) Color(0xFF2196F3) else Color(0xFF9E9E9E)  // Blue/gray border for towers
        else -> Color.Transparent
    }
    
    val borderWidth = when {
        isDefenderSelected && gameState.phase != GamePhase.INITIAL_BUILDING -> 3f
        cellIsInRange && isOnPath && showRange -> 2.5f
        isSpawnPoint || isTarget -> 2f
        attacker != null || defender != null -> 2f
        else -> 0f
    }
    
    // Hexagon width and height for layout  
    val hexWidth = hexSize * 2f
    val hexHeight = hexSize * kotlin.math.sqrt(3f)
    
    // Box sized for proper honeycomb spacing, but hexagon drawn at full size
    Box(
        modifier = Modifier
            .size(width = (hexWidth * 0.75f).dp, height = hexHeight.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Draw hexagon at full size (will extend beyond Box bounds for proper tiling)
        Canvas(modifier = Modifier.size(width = hexWidth.dp, height = hexHeight.dp)) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            
            val hexPath = createHexagonPath(centerX, centerY, hexSize)
            
            // Fill hexagon
            drawPath(hexPath, backgroundColor, style = Fill)
            
            // Draw border
            if (borderWidth > 0f) {
                drawPath(hexPath, borderColor, style = Stroke(width = borderWidth))
            }
        }
        
        // Content overlay
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            when {
                attacker != null -> {
                    Text(
                        attacker.type.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = Color.White,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${attacker.currentHealth}/${attacker.maxHealth}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
                defender != null -> {
                    Text(
                        defender.type.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.65f,
                        textAlign = TextAlign.Center,
                        color = Color.White,
                        maxLines = 1
                    )
                    Text(
                        "Lvl ${defender.level}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.6f,
                        color = Color.White
                    )
                    if (!defender.isReady) {
                        Text(
                            "⏱${defender.buildTimeRemaining}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.6f,
                            color = Color(0xFFFFA500)
                        )
                    } else if (defender.actionsRemaining > 0) {
                        Text(
                            "⚡${defender.actionsRemaining}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.6f,
                            color = Color.Yellow
                        )
                    }
                }
                isSpawnPoint -> {
                    Text("S", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF9800))
                }
                isTarget -> {
                    Text("T", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
                }
            }
        }
    }
}

// Helper function to create hexagon path
private fun createHexagonPath(centerX: Float, centerY: Float, size: Float): Path {
    val path = Path()
    for (i in 0..6) {
        val angleDeg = 60f * i - 30f  // Start from top vertex
        val angleRad = (PI / 180f * angleDeg).toFloat()
        val x = centerX + size * cos(angleRad)
        val y = centerY + size * sin(angleRad)
        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()
    return path
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
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxWidth().height(170.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(DefenderType.entries.toTypedArray(), key = { type -> "${type.name}_${coinsState.value}" }) { type ->
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
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxWidth().height(150.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(DefenderType.entries.toTypedArray(), key = { type -> "${type.name}_${coinsState.value}" }) { type ->
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
        modifier = Modifier.fillMaxWidth().height(65.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(type.displayName.split(" ")[0], style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text("${type.baseCost}c ⏱${type.buildTime}", 
                 style = MaterialTheme.typography.labelSmall,
                 fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.75f)
            Text("Range:${if (type.minRange > 0) "${type.minRange}-" else ""}${type.baseRange} Actions:${type.actionsPerTurn}", 
                 style = MaterialTheme.typography.labelSmall,
                 fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.65f)
        }
    }
}

@Composable
fun GameLegend(modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Legend", style = MaterialTheme.typography.titleMedium)
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text("Areas:", style = MaterialTheme.typography.labelMedium)
            LegendItem(color = Color(0xFF8BC34A), label = "Island", description = "Build Zone", border = Color.Gray)
            LegendItem(color = Color(0xFFA5D6A7), label = "Strip", description = "Build Zone", border = Color.Gray)
            LegendItem(color = Color(0xFFFFF8DC), label = "Path", description = "Enemy Route", border = Color.Gray)
            LegendItem(color = Color(0xFFE0E0E0), label = "Off", description = "Non-Playable", border = Color.Gray)
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text("Special:", style = MaterialTheme.typography.labelMedium)
            LegendItem(color = Color(0xFFFFF8DC), label = "S", description = "Spawn Points (3)", border = Color(0xFFFF9800), borderWidth = 3.dp)
            LegendItem(color = Color(0xFFFFF8DC), label = "T", description = "Target (Defend!)", border = Color(0xFF4CAF50), borderWidth = 3.dp)
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text("Units:", style = MaterialTheme.typography.labelMedium)
            LegendItem(color = Color(0xFF2196F3), label = "Tower", description = "Ready Tower")
            LegendItem(color = Color(0xFF9E9E9E), label = "⏱", description = "Building Tower")
            LegendItem(color = Color(0xFFF44336), label = "Enemy", description = "Attacker")
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text("Towers:", style = MaterialTheme.typography.labelMedium)
            Text("Ballista: min range 3!", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFF6F00), fontWeight = FontWeight.Bold)
            Text("⚡ = Actions left", style = MaterialTheme.typography.bodySmall)
            Text("⏱ = Build time", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun LegendItem(
    color: Color, 
    label: String, 
    description: String, 
    border: Color = Color.Gray, 
    borderWidth: androidx.compose.ui.unit.Dp = 1.dp
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(color)
                .border(borderWidth, border),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label, 
                style = MaterialTheme.typography.labelSmall, 
                color = if (color.luminance() > 0.5f) Color.Black else Color.White
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
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Enemies", style = MaterialTheme.typography.titleMedium)
            Text(
                "Active: ${gameState.attackers.count { !it.isDefeated }} | Coming: ${gameState.attackersToSpawn.size}",
                style = MaterialTheme.typography.bodySmall
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(gameState.attackers.filter { !it.isDefeated }.sortedBy { it.id }) { attacker ->
                    EnemyItem(attacker)
                    Spacer(modifier = Modifier.height(4.dp))
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

