package com.defenderofegril.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.defenderofegril.game.LevelData
import com.defenderofegril.model.*
import com.defenderofegril.save.SaveGameMetadata
import com.defenderofegril.utils.formatTimestamp

@Composable
fun LoadGameScreen(
    savedGames: List<SaveGameMetadata>,
    onLoadGame: (String) -> Unit,
    onDeleteGame: (String) -> Unit,
    onBack: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Load Game",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (savedGames.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No saved games found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(savedGames) { saveGame ->
                    SavedGameCard(
                        saveGame = saveGame,
                        onLoad = { onLoadGame(saveGame.id) },
                        onDelete = { showDeleteDialog = saveGame.id }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = onBack) {
            Text("Back to World Map")
        }
    }
    
    // Delete confirmation dialog
    showDeleteDialog?.let { saveId ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Save Game") },
            text = { Text("Are you sure you want to delete this saved game?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteGame(saveId)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SavedGameCard(
    saveGame: SaveGameMetadata,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = formatTimestamp(saveGame.timestamp)
    
    // Get the level to access map for minimap
    val levels = remember { LevelData.createLevels() }
    val level = remember(saveGame.levelId) { 
        levels.find { it.id == saveGame.levelId }
    }
    
    // Create a minimal GameState for minimap rendering
    val minimapGameState = remember(saveGame.id) {
        if (level != null && (saveGame.defenderPositions.isNotEmpty() || saveGame.attackerPositions.isNotEmpty())) {
            // Create minimal Defender/Attacker objects for minimap display
            val defenders = saveGame.defenderPositions.map { saved ->
                Defender(
                    id = saved.id,
                    type = saved.type,
                    position = saved.position,
                    level = mutableStateOf(saved.level),
                    buildTimeRemaining = mutableStateOf(saved.buildTimeRemaining),
                    actionsRemaining = mutableStateOf(0),
                    placedOnTurn = saved.placedOnTurn,
                    hasBeenUsed = mutableStateOf(false),
                    dragonId = mutableStateOf(null)
                )
            }
            val attackers = saveGame.attackerPositions.filter { !it.isDefeated }.map { saved ->
                Attacker(
                    id = saved.id,
                    type = saved.type,
                    position = mutableStateOf(saved.position),
                    level = saved.level,
                    currentHealth = mutableStateOf(saved.currentHealth),
                    isDefeated = mutableStateOf(false)
                )
            }
            // Create a minimal GameState with just the data needed for rendering
            GameState(
                level = level,
                defenders = androidx.compose.runtime.snapshots.SnapshotStateList<Defender>().apply { addAll(defenders) },
                attackers = androidx.compose.runtime.snapshots.SnapshotStateList<Attacker>().apply { addAll(attackers) }
            )
        } else {
            null
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLoad() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // Header row with level name, date, and turn
            Text(
                text = saveGame.levelName,
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = dateStr,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Turn number and coins with symbols
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Turn number
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimerIcon(
                        size = 16.dp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Turn ${saveGame.turnNumber}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Coins
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MoneyIcon(size = 16.dp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${saveGame.coins}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // Display comment if present
            if (!saveGame.comment.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "💬",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = saveGame.comment,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Three-column layout: Towers | Enemies | Minimap
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Column 1: Built towers
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Top
                ) {
                    if (saveGame.defenderCounts.isNotEmpty()) {
                        Text(
                            text = "Built Towers:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            saveGame.defenderCounts.entries.forEach { (type, count) ->
                                UnitEntry(
                                    icon = { DefenderTypeIconSimple(type) },
                                    name = type.displayName,
                                    count = count
                                )
                            }
                        }
                    }
                }
                
                // Column 2: Enemies (current and to come)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Top
                ) {
                    // Current enemies on map
                    if (saveGame.attackerCounts.isNotEmpty()) {
                        Text(
                            text = "Enemies on Map:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            saveGame.attackerCounts.entries.forEach { (type, count) ->
                                UnitEntry(
                                    icon = { EnemyTypeIcon(attackerType = type) },
                                    name = type.displayName,
                                    count = count
                                )
                            }
                        }
                    }
                    
                    if (saveGame.attackerCounts.isNotEmpty() && saveGame.remainingSpawnCounts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // Remaining spawns
                    if (saveGame.remainingSpawnCounts.isNotEmpty()) {
                        Text(
                            text = "Enemies to Come:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            saveGame.remainingSpawnCounts.entries.forEach { (type, count) ->
                                UnitEntry(
                                    icon = { EnemyTypeIcon(attackerType = type) },
                                    name = type.displayName,
                                    count = count
                                )
                            }
                        }
                    }
                }
                
                // Column 3: Minimap and delete button
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    // Minimap
                    if (level != null) {
                        Box(
                            modifier = Modifier
                                .width(300.dp)
                                .height(120.dp)
                                .padding(top = 20.dp)
                        ) {
                            val mapName = HexagonMinimap(
                                level = level,
                                config = MinimapConfig(
                                    showSpawnPoints = true,
                                    showTarget = true,
                                    showTowers = true,
                                    showEnemies = true,
                                    showViewport = false,
                                    backgroundColor = Color.Transparent,
                                    borderColor = Color.Transparent
                                ),
                                gameState = minimapGameState,  // Pass the minimap game state with unit positions
                                modifier = Modifier.fillMaxSize()
                            )
                            Text(
                                text = mapName,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 12.sp,
                                modifier = Modifier.absoluteOffset(x = 0.dp, y = (-20).dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Delete button with text
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.clickable { onDelete() }
                    ) {
                        Text(
                            text = "Delete Savegame",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        TrashIcon(size = 20.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun UnitEntry(
    icon: @Composable () -> Unit,
    name: String,
    count: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(modifier = Modifier.size(32.dp)) {
            icon()
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$name: $count",
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun DefenderTypeIconSimple(defenderType: DefenderType) {
    // Use the proper tower icon with hexagon shape and game color
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(HexagonShape())
            .background(Color(0xFF2196F3)) // same color as in the game
    ) {
        TowerTypeIcon(defenderType = defenderType)
    }
}
