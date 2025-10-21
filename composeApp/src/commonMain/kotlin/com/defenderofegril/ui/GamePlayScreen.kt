package com.defenderofegril.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.defenderofegril.model.*
import kotlinx.coroutines.delay

@Composable
fun GamePlayScreen(
    gameState: GameState,
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
    
    Column(
        modifier = modifier.fillMaxWidth().horizontalScroll(scrollState),
        horizontalAlignment = Alignment.Start
    ) {
        for (y in 0 until gameState.level.gridHeight) {
            Row {
                for (x in 0 until gameState.level.gridWidth) {
                    val position = Position(x, y)
                    GridCell(
                        position = position,
                        gameState = gameState,
                        isSelected = selectedDefenderType != null,
                        isDefenderSelected = gameState.defenders.find { it.position == position }?.id == selectedDefenderId,
                        isTargetSelected = gameState.attackers.find { it.position == position }?.id == selectedTargetId,
                        onClick = { onCellClick(position) }
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
    onClick: () -> Unit
) {
    val isSpawnPoint = gameState.level.isSpawnPoint(position)
    val isTarget = position == gameState.level.targetPosition
    val isOnPath = gameState.level.isOnPath(position)
    val isBuildIsland = gameState.level.isBuildIsland(position)
    val isBuildArea = gameState.level.isBuildArea(position)
    val defender = gameState.defenders.find { it.position == position }
    val attacker = gameState.attackers.find { it.position == position && !it.isDefeated }
    
    // Base background color based on area type - ALWAYS visible
    // Build islands + strips adjacent to path allow tower placement
    val baseBackgroundColor = when {
        isBuildIsland -> Color(0xFF8BC34A)  // Light green for build islands
        isBuildArea -> Color(0xFFA5D6A7)  // Medium green for strips adjacent to path
        isOnPath -> Color(0xFFFFF8DC)  // Cream/beige for enemy path
        else -> Color(0xFFE0E0E0)  // Light gray for off-path areas (non-playable)
    }
    
    // Apply slight tint for selection states, but keep base color visible
    val backgroundColor = when {
        isDefenderSelected -> baseBackgroundColor.copy(alpha = 0.7f)
        isTargetSelected -> baseBackgroundColor.copy(alpha = 0.8f)
        isSelected -> baseBackgroundColor.copy(alpha = 0.9f)
        else -> baseBackgroundColor
    }
    
    // Border color - use borders to indicate entities instead of background
    val borderColor = when {
        isSpawnPoint -> Color(0xFFFF9800)  // Orange border for spawn
        isTarget -> Color(0xFF4CAF50)  // Green border for target
        attacker != null -> Color(0xFFF44336)  // Red border for enemies
        defender != null -> if (defender.isReady) Color(0xFF2196F3) else Color(0xFF9E9E9E)  // Blue/gray border for towers
        else -> Color.Gray
    }
    
    // Thicker borders for important elements
    val borderWidth = when {
        isSpawnPoint || isTarget -> 3.dp
        attacker != null || defender != null -> 3.dp
        else -> 1.dp
    }
    
    Box(
        modifier = Modifier
            .size(48.dp)
            .border(borderWidth, borderColor)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            when {
                isSpawnPoint && attacker == null && defender == null -> {
                    // Show spawn indicator when cell is empty
                    Text("S", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF9800))
                }
                isTarget && attacker == null && defender == null -> {
                    // Show target indicator when cell is empty
                    Text("T", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
                }
                defender != null -> {
                    Text(
                        defender.type.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.65f,
                        textAlign = TextAlign.Center,
                        color = if (defender.isReady) Color.White else Color.Black,
                        maxLines = 1
                    )
                    Text(
                        "Lvl ${defender.level}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.6f,
                        color = if (defender.isReady) Color.White else Color.Black
                    )
                    if (!defender.isReady) {
                        Text(
                            "⏱${defender.buildTimeRemaining}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.6f,
                            color = Color.Orange
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
                attacker != null -> {
                    Text(
                        attacker.type.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.7f,
                        color = Color.White,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "${attacker.currentHealth}/${attacker.maxHealth} HP",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.6f,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun InitialBuildingControls(
    gameState: GameState,
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
            modifier = Modifier.fillMaxWidth().height(150.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(DefenderType.entries.toTypedArray()) { type ->
                val canAfford = gameState.canPlaceDefender(type) // Recalculate on every recomposition
                DefenderButton(
                    type = type,
                    isSelected = selectedDefenderType == type,
                    canAfford = canAfford,
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
            modifier = Modifier.fillMaxWidth().height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(DefenderType.entries.toTypedArray()) { type ->
                val canAfford = gameState.canPlaceDefender(type) // Recalculate on every recomposition
                DefenderButton(
                    type = type,
                    isSelected = selectedDefenderType == type,
                    canAfford = canAfford,
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
    // The ViewModel automatically handles the 1.5s delay and phase progression
    // This composable just displays the enemy turn indicator
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
                    color = Color.Red
                )
                Spacer(modifier = Modifier.height(8.dp))
                CircularProgressIndicator(color = Color.Red)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Enemies are moving...", 
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Red
                )
            }
        }
    }
}
        }
    }
}

@Composable
fun DefenderButton(
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
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(type.displayName.split(" ")[0], style = MaterialTheme.typography.labelSmall)
            Text("${type.baseCost}c ⏱${type.buildTime}", 
                 style = MaterialTheme.typography.labelSmall,
                 fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.8f)
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

