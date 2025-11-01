package com.defenderofegril.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.defenderofegril.editor.EditorStorage
import com.defenderofegril.editor.TileType
import com.defenderofegril.game.LevelData
import com.defenderofegril.model.AttackerType
import com.defenderofegril.model.DefenderType
import com.defenderofegril.model.Level
import com.defenderofegril.save.SaveGameMetadata
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

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
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    val dateStr = dateFormat.format(Date(saveGame.timestamp))
    
    // Get the level to access map for minimap
    val levels = remember { LevelData.createLevels() }
    val level = remember(saveGame.levelId) { 
        levels.find { it.id == saveGame.levelId }
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
            
            // Turn number with symbol
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = "⏱",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Turn ${saveGame.turnNumber}",
                    style = MaterialTheme.typography.bodyMedium
                )
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
                        ) {
                            val mapName = SaveGameMinimap(level)
                            Text(
                                text = mapName,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 12.sp,
                                modifier = Modifier.align(Alignment.TopStart)
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
                        Text("🗑️", style = MaterialTheme.typography.bodyLarge)
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

@Composable
fun SaveGameMinimap(level: Level): String {
    // Reuse the same minimap rendering logic from LevelMinimap
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
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Map Preview",
                fontSize = 8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return ""
    }
    
    // Render minimap
    val hexSize = 6.dp.value
    val hexWidth = sqrt(3.0) * hexSize
    val hexHeight = 2.0 * hexSize
    val verticalSpacing = hexHeight * 0.75
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        for (row in 0 until map.height) {
            for (col in 0 until map.width) {
                val tileType = map.tiles.getOrElse("$col,$row") { TileType.NO_PLAY }
                
                val offsetX = if (row % 2 == 1) hexWidth / 2 else 0.0
                val centerX = (col * hexWidth + offsetX + hexWidth / 2).toFloat()
                val centerY = (row * verticalSpacing + hexHeight / 2).toFloat()
                
                val color = when (tileType) {
                    TileType.PATH -> Color(0xFF8B4513)
                    TileType.BUILD_AREA -> Color(0xFF90EE90)
                    TileType.ISLAND -> Color(0xFF228B22)
                    TileType.SPAWN_POINT -> Color(0xFFDC143C)
                    TileType.TARGET -> Color(0xFF4169E1)
                    TileType.NO_PLAY -> Color(0xFF808080)
                    TileType.WAYPOINT -> Color(0xFFFFD700)
                }
                
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
fun InfoChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
