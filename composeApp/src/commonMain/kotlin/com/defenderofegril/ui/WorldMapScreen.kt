package com.defenderofegril.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.defenderofegril.editor.EditorStorage
import com.defenderofegril.editor.TileType
import com.defenderofegril.model.AttackerType
import com.defenderofegril.model.Level
import com.defenderofegril.model.LevelStatus
import com.defenderofegril.model.WorldLevel
import com.defenderofegril.model.getEnemyTypeCounts
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

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
                    val mapName = LevelMinimap(worldLevel.level)
                    Text(text = mapName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.absoluteOffset(x = 0.dp, y = (-20).dp)
                    )
                }
                
                // Status at the bottom
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 13.sp
                )
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
fun LevelMinimap(level: Level): String {
    // Cache the editor data to avoid redundant lookups on recomposition
    val sequence = remember { EditorStorage.getLevelSequence() }
    val editorLevelId = remember(level.id) {
        if (level.id > 0 && level.id <= sequence.sequence.size) {
            sequence.sequence[level.id - 1]
        } else {
            null
        }
    }
    
    val editorLevel = remember(editorLevelId) { 
        editorLevelId?.let { EditorStorage.getLevel(it) }
    }
    val map = remember(editorLevel?.mapId) { 
        editorLevel?.let { EditorStorage.getMap(it.mapId) }
    }
    
    if (map == null) {
        // Fallback display if map is not found
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Map Preview",
                fontSize = 8.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
        return ""  // Return empty string if map is not found
    }
    
    // Render minimap similar to MapMiniPreview from LevelEditorScreen
    val hexSize = 6.dp.value
    val hexWidth = sqrt(3.0) * hexSize
    val hexHeight = 2.0 * hexSize
    val verticalSpacing = hexHeight * 0.75
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        for (row in 0 until map.height) {
            for (col in 0 until map.width) {
                val tileType = map.tiles.getOrElse("$col,$row") { TileType.NO_PLAY }
                
                // Calculate hex center position
                val offsetX = if (row % 2 == 1) hexWidth / 2 else 0.0
                val centerX = (col * hexWidth + offsetX + hexWidth / 2).toFloat()
                val centerY = (row * verticalSpacing + hexHeight / 2).toFloat()
                
                // Get color for tile type
                val color = when (tileType) {
                    TileType.PATH -> Color(0xFF8B4513)
                    TileType.BUILD_AREA -> Color(0xFF90EE90)
                    TileType.ISLAND -> Color(0xFF228B22)
                    TileType.SPAWN_POINT -> Color(0xFFDC143C)
                    TileType.TARGET -> Color(0xFF4169E1)
                    TileType.NO_PLAY -> Color(0xFF808080)
                    TileType.WAYPOINT -> Color(0xFFFFD700)
                }
                
                // Draw hexagon
                val path = Path().apply {
                    for (i in 0 until 6) {
                        val angle = PI * (60.0 * i - 30.0) / 180.0
                        val x = centerX + (hexSize * cos(angle)).toFloat()
                        val y = centerY + (hexSize * sin(angle)).toFloat()
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }
                drawPath(path, color)
            }
        }
    }
    return map.name
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
            Text(
                text = "🛠️",
                style = MaterialTheme.typography.displayLarge,
                fontSize = 64.sp
            )
            
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
