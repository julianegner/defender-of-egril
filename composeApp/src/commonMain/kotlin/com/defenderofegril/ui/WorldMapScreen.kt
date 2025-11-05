package com.defenderofegril.ui

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
import androidx.compose.ui.unit.sp
import com.defenderofegril.model.AttackerType
import com.defenderofegril.model.Level
import com.defenderofegril.model.LevelStatus
import com.defenderofegril.model.WorldLevel
import com.defenderofegril.model.getEnemyTypeCounts
import com.defenderofegril.ui.icon.enemy.EnemyTypeIcon

@Composable
fun WorldMapScreen(
    worldLevels: List<WorldLevel>,
    onLevelSelected: (Int) -> Unit,
    onBackToMenu: () -> Unit,
    onShowRules: () -> Unit,
    onOpenEditor: () -> Unit,
    onLoadGame: () -> Unit,
    onCheatCode: ((String) -> Boolean)? = null  // Callback for processing cheat codes, returns true if code was valid
) {
    var showCheatDialog by remember { mutableStateOf(false) }
    var cheatCodeInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title text - clickable for cheat code access (less obvious than a button)
        Text(
            text = "World Map - Meadows of Egril",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .padding(bottom = 16.dp)
                .then(
                    if (onCheatCode != null) {
                        Modifier.clickable { showCheatDialog = true }
                    } else {
                        Modifier
                    }
                )
        )
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(worldLevels) { worldLevel ->
                LevelCard(
                    worldLevel = worldLevel,
                    onClick = {
                        if (worldLevel.status != LevelStatus.LOCKED) {
                            onLevelSelected(worldLevel.level.id)
                        }
                    }
                )
            }
            
            // Add Editor Button as a special card (only on desktop)
            if (isEditorAvailable()) {
                item {
                    EditorButtonCard(onClick = onOpenEditor)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = onLoadGame) {
                Text("Load Game")
            }
            
            Button(onClick = onShowRules) {
                Text("Rules")
            }
            
            Button(onClick = onBackToMenu) {
                Text("Back to Menu")
            }
        }
    }
    
    // Cheat code dialog
    if (showCheatDialog && onCheatCode != null) {
        AlertDialog(
            onDismissRequest = {
                showCheatDialog = false
                cheatCodeInput = ""
                errorMessage = ""
            },
            title = { Text("Cheat Code") },
            text = {
                Column {
                    Text("Enter cheat code:")
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = cheatCodeInput,
                        onValueChange = { 
                            cheatCodeInput = it
                            errorMessage = ""  // Clear error when user types
                        },
                        placeholder = { Text("unlock") },
                        isError = errorMessage.isNotEmpty()
                    )
                    if (errorMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = onCheatCode(cheatCodeInput)
                        if (success) {
                            showCheatDialog = false
                            cheatCodeInput = ""
                            errorMessage = ""
                        } else {
                            errorMessage = "Invalid cheat code"
                        }
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showCheatDialog = false
                        cheatCodeInput = ""
                        errorMessage = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun LevelCard(
    worldLevel: WorldLevel,
    onClick: () -> Unit
) {
    val backgroundColor = when (worldLevel.status) {
        LevelStatus.LOCKED -> Color(0xFF9E9E9E)
        LevelStatus.UNLOCKED -> Color(0xFF2196F3)
        LevelStatus.WON -> Color(0xFF4CAF50)
    }
    
    val statusText = when (worldLevel.status) {
        LevelStatus.LOCKED -> "Locked"
        LevelStatus.UNLOCKED -> "Available"
        LevelStatus.WON -> "Completed"
    }
    
    // Get enemy counts for this level
    val enemyCounts = worldLevel.level.getEnemyTypeCounts()
    val enemyList = enemyCounts.entries.toList()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable(enabled = worldLevel.status != LevelStatus.LOCKED, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left column: Level info, coins, health, and enemies
            Column(
                modifier = Modifier.weight(2f).fillMaxHeight(),
                verticalArrangement = Arrangement.Top
            ) {
                Row {
                    // Header: level number and name
                    Column {
                        Text(
                            text = "Level ${worldLevel.level.id}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontSize = 18.sp
                        )

                        Text(
                            text = worldLevel.level.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Coins and Health display
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                MoneyIcon(size = 12.dp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${worldLevel.level.initialCoins}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                HeartIcon(size = 12.dp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${worldLevel.level.healthPoints}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    // Enemy units display
                    if (enemyList.isNotEmpty()) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            enemyList.forEachIndexed { index, (attackerType, count) ->
                                if (index % 2 == 0) {
                                    EnemyUnitEntry(attackerType, count)
                                }
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            enemyList.forEachIndexed { index, (attackerType, count) ->
                                if (index % 2 == 1) {
                                    EnemyUnitEntry(attackerType, count)
                                }
                            }
                        }
                    }
                }
            }

            // Right column: Minimap and status
            Column(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                // horizontalAlignment = Alignment.End
            ) {
                // Minimap preview
                Box(
                    modifier = Modifier
                        .width(300.dp)
                        .height(120.dp)
                        .padding(top = 20.dp)
                ) {
                    val mapName = HexagonMinimap(
                        level = worldLevel.level,
                        config = MinimapConfig(
                            showSpawnPoints = true,
                            showTarget = true,
                            showTowers = false,
                            showEnemies = false,
                            showViewport = false,
                            backgroundColor = Color.Transparent,
                            borderColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxSize()
                    )
                    Text(
                        text = mapName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.absoluteOffset(x = 0.dp, y = (-20).dp)
                    )
                }
                
                // Status at the bottom
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (worldLevel.status) {
                        LevelStatus.LOCKED -> LockIcon(size = 13.dp)
                        LevelStatus.UNLOCKED -> SwordIcon(size = 13.dp)
                        LevelStatus.WON -> CheckmarkIcon(size = 13.dp, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        textAlign = TextAlign.End,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun EnemyUnitEntry(attackerType: AttackerType, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier.size(24.dp)
        ) {
            EnemyTypeIcon(attackerType = attackerType)
        }

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = "${attackerType.displayName}: ${count}",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            fontSize = 11.sp
        )
    }
}

@Composable
fun EditorButtonCard(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFF9800)  // Distinctive orange color
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(12.dp),
        ) {
            // Distinctive symbol - wrench/hammer icon
            ToolsIcon(size = 64.dp)
            
            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "Level Editor",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontSize = 20.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Create & Edit Maps and Levels",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }
}
