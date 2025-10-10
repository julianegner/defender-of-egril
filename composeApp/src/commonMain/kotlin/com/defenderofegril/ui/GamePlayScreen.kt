package com.defenderofegril.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.defenderofegril.model.*

@Composable
fun GamePlayScreen(
    gameState: GameState,
    onPlaceDefender: (DefenderType, Position) -> Boolean,
    onUpgradeDefender: (Int) -> Boolean,
    onStartCombat: () -> Unit,
    onExecuteTurn: () -> Unit,
    onReturnToPlanning: () -> Unit,
    onBackToMap: () -> Unit
) {
    var selectedDefenderType by remember { mutableStateOf<DefenderType?>(null) }
    var selectedDefenderId by remember { mutableStateOf<Int?>(null) }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Level: ${gameState.level.name}", style = MaterialTheme.typography.titleLarge)
                Text("Coins: ${gameState.coins}", style = MaterialTheme.typography.bodyLarge)
                Text("Health: ${gameState.healthPoints}", style = MaterialTheme.typography.bodyLarge)
            }
            
            Button(onClick = onBackToMap) {
                Text("Back to Map")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Game Grid
        GameGrid(
            gameState = gameState,
            selectedDefenderType = selectedDefenderType,
            onCellClick = { position ->
                selectedDefenderType?.let { type ->
                    if (onPlaceDefender(type, position)) {
                        selectedDefenderType = null
                    }
                }
                // Check if there's a defender at this position
                val defender = gameState.defenders.find { it.position == position }
                selectedDefenderId = defender?.id
            },
            modifier = Modifier.weight(1f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Control Panel
        if (gameState.phase == GamePhase.PLANNING) {
            PlanningControls(
                gameState = gameState,
                selectedDefenderType = selectedDefenderType,
                selectedDefenderId = selectedDefenderId,
                onSelectDefenderType = { selectedDefenderType = it },
                onUpgradeDefender = { onUpgradeDefender(it) },
                onStartCombat = onStartCombat
            )
        } else {
            CombatControls(
                onExecuteTurn = onExecuteTurn,
                onReturnToPlanning = onReturnToPlanning
            )
        }
    }
}

@Composable
fun GameGrid(
    gameState: GameState,
    selectedDefenderType: DefenderType?,
    onCellClick: (Position) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (y in 0 until gameState.level.gridHeight) {
            Row {
                for (x in 0 until gameState.level.gridWidth) {
                    val position = Position(x, y)
                    GridCell(
                        position = position,
                        gameState = gameState,
                        isSelected = selectedDefenderType != null,
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
    onClick: () -> Unit
) {
    val isStart = position == gameState.level.startPosition
    val isTarget = position == gameState.level.targetPosition
    val defender = gameState.defenders.find { it.position == position }
    val attacker = gameState.attackers.find { it.position == position && !it.isDefeated }
    
    val backgroundColor = when {
        isStart -> Color(0xFFFF9800)
        isTarget -> Color(0xFF4CAF50)
        defender != null -> Color(0xFF2196F3)
        attacker != null -> Color(0xFFF44336)
        isSelected -> Color(0xFFE0E0E0)
        else -> Color.White
    }
    
    Box(
        modifier = Modifier
            .size(48.dp)
            .border(1.dp, Color.Gray)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when {
            isStart -> Text("S", style = MaterialTheme.typography.labelSmall)
            isTarget -> Text("T", style = MaterialTheme.typography.labelSmall)
            defender != null -> Text(
                defender.type.displayName.take(1) + defender.level,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
            attacker != null -> Text(
                attacker.type.displayName.take(1),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
    }
}

@Composable
fun PlanningControls(
    gameState: GameState,
    selectedDefenderType: DefenderType?,
    selectedDefenderId: Int?,
    onSelectDefenderType: (DefenderType?) -> Unit,
    onUpgradeDefender: (Int) -> Unit,
    onStartCombat: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Place Defenders", style = MaterialTheme.typography.titleMedium)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxWidth().height(150.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(DefenderType.entries.toTypedArray()) { type ->
                DefenderButton(
                    type = type,
                    isSelected = selectedDefenderType == type,
                    canAfford = gameState.canPlaceDefender(type),
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${defender.type.displayName} (Lvl ${defender.level})")
                    Button(
                        onClick = { onUpgradeDefender(id) },
                        enabled = gameState.canUpgradeDefender(defender)
                    ) {
                        Text("Upgrade (${defender.upgradeCost} coins)")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = onStartCombat,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start Combat")
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
            Text(type.displayName, style = MaterialTheme.typography.labelSmall)
            Text("${type.baseCost} coins", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun CombatControls(
    onExecuteTurn: () -> Unit,
    onReturnToPlanning: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onExecuteTurn,
            modifier = Modifier.weight(1f)
        ) {
            Text("Next Turn")
        }
        
        Button(
            onClick = onReturnToPlanning,
            modifier = Modifier.weight(1f)
        ) {
            Text("Return to Planning")
        }
    }
}
