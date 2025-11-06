package com.defenderofegril.ui.editor.level

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.defenderofegril.editor.EditorEnemySpawn
import com.defenderofegril.editor.EditorLevel
import com.defenderofegril.editor.EditorStorage
import com.defenderofegril.model.AttackerType
import com.defenderofegril.model.DefenderType
import com.defenderofegril.ui.*
import com.defenderofegril.ui.editor.CreateLevelDialog
import com.defenderofegril.ui.editor.map.MapSelectionCard
import com.defenderofegril.ui.editor.SaveAsDialog
import kotlin.random.Random

/**
 * Main content for the Level Editor tab
 */
@Composable
fun LevelEditorContent() {
    val levels = remember { mutableStateOf(EditorStorage.getAllLevels()) }
    var selectedLevelId by remember { mutableStateOf<String?>(null) }
    var editingLevel by remember { mutableStateOf<EditorLevel?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    
    if (editingLevel != null) {
        // Level editing view
        LevelEditorView(
            level = editingLevel!!,
            onSave = { updatedLevel ->
                EditorStorage.saveLevel(updatedLevel)
                levels.value = EditorStorage.getAllLevels()
                editingLevel = null
            },
            onCancel = { editingLevel = null }
        )
    } else {
        // Level list view
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Levels",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Button(onClick = { showCreateDialog = true }) {
                    Text("Create New Level")
                }
            }
            
            Text(
                text = "Select a level to edit:",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(levels.value) { level ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedLevelId == level.id) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { 
                                        selectedLevelId = level.id
                                        editingLevel = level
                                    }
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = level.title,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                if (level.subtitle.isNotEmpty()) {
                                    Text(
                                        text = level.subtitle,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Text(
                                    text = "File: ${level.id}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Map: ${level.mapId} | Coins: ${level.startCoins} | HP: ${level.startHealthPoints}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "Enemies: ${level.enemySpawns.size}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Button(
                                onClick = {
                                    EditorStorage.deleteLevel(level.id)
                                    levels.value = EditorStorage.getAllLevels()
                                    if (selectedLevelId == level.id) {
                                        selectedLevelId = null
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showCreateDialog) {
        CreateLevelDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title ->
                // Generate ID from title with underscores
                val sanitizedTitle = title.trim().replace(" ", "_").replace(Regex("[^a-zA-Z0-9_]"), "")
                val newId = if (sanitizedTitle.isNotEmpty()) {
                    "level_$sanitizedTitle"
                } else {
                    "level_custom_${Random.nextInt(10000, 99999)}"
                }
                // Get first ready-to-use map
                val firstReadyMap = EditorStorage.getAllMaps().filter { it.readyToUse }.firstOrNull()
                val newLevel = EditorLevel(
                    id = newId,
                    mapId = firstReadyMap?.id ?: "map_30x8",
                    title = title,
                    subtitle = "",
                    startCoins = 100,
                    startHealthPoints = 10,
                    enemySpawns = emptyList(),
                    availableTowers = DefenderType.entries.filter {
                        it != DefenderType.DRAGONS_LAIR
                    }.toSet()
                )
                EditorStorage.saveLevel(newLevel)
                levels.value = EditorStorage.getAllLevels()
                showCreateDialog = false
                editingLevel = newLevel
            }
        )
    }
}

/**
 * View for editing a level
 */
@Composable
fun LevelEditorView(
    level: EditorLevel,
    onSave: (EditorLevel) -> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf(level.title) }
    var subtitle by remember { mutableStateOf(level.subtitle) }
    var startCoins by remember { mutableStateOf(level.startCoins.toString()) }
    var startHP by remember { mutableStateOf(level.startHealthPoints.toString()) }
    var selectedMapId by remember { mutableStateOf(level.mapId) }
    var enemySpawns by remember { mutableStateOf(level.enemySpawns.toMutableList()) }
    var availableTowers by remember { mutableStateOf(level.availableTowers.toMutableSet()) }
    var showEnemyDialog by remember { mutableStateOf(false) }
    var showEnemyDialogForTurn by remember { mutableStateOf(1) }
    var showSaveAsDialog by remember { mutableStateOf(false) }
    
    // Get only ready-to-use maps for selection
    val maps = remember { EditorStorage.getAllMaps().filter { it.readyToUse } }
    
    // Check if Ewhad is already in spawn list
    val ewhadCount = enemySpawns.count { it.attackerType == AttackerType.EWHAD }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Editing Level: ${level.title}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Title and subtitle
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Level Title") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            item {
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("Subtitle (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Map selection with mini-maps
            item {
                Text(
                    text = "Map:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(maps) { map ->
                        MapSelectionCard(
                            map = map,
                            isSelected = selectedMapId == map.id,
                            onClick = { selectedMapId = map.id }
                        )
                    }
                }
            }
            
            // Start coins and HP
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startCoins,
                        onValueChange = { if (it.all { c -> c.isDigit() }) startCoins = it },
                        label = { Text("Start Coins") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = startHP,
                        onValueChange = { if (it.all { c -> c.isDigit() }) startHP = it },
                        label = { Text("Start HP") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Enemies section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enemies (${enemySpawns.size}):",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = { 
                        // Add a new turn (next available)
                        val nextTurn = (enemySpawns.maxOfOrNull { it.spawnTurn } ?: 0) + 1
                        enemySpawns = enemySpawns.toMutableList().apply {
                            add(
                                EditorEnemySpawn(
                                    AttackerType.GOBLIN,
                                    1,
                                    nextTurn
                                )
                            )
                        }
                    }) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("➕")
                            Text("Add Turn")
                        }
                    }
                }
            }
            
            // Group enemies by spawn turn
            val turnGroups = enemySpawns.groupBy { it.spawnTurn }.entries.sortedBy { it.key }
            val turns = turnGroups.map { it.key }
            
            turnGroups.forEachIndexed { index, entry ->
                val turn = entry.key
                val spawnsInTurn = entry.value
                item {
                    val turnIndex = index
                    SpawnTurnSection(
                        turn = turn,
                        spawns = spawnsInTurn,
                        onRemoveEnemy = { spawn ->
                            enemySpawns = enemySpawns.toMutableList().apply { remove(spawn) }
                        },
                        onCopyTurn = {
                            // Copy all enemies from this turn to a new turn (next available)
                            val maxTurn = enemySpawns.maxOfOrNull { it.spawnTurn } ?: 0
                            enemySpawns = enemySpawns.toMutableList().apply {
                                spawnsInTurn.forEach { spawn ->
                                    add(spawn.copy(spawnTurn = maxTurn + 1))
                                }
                            }
                        },
                        onAddEnemy = {
                            // Show dialog to add enemy to this specific turn
                            showEnemyDialog = true
                            showEnemyDialogForTurn = turn
                        },
                        onMoveTurnUp = {
                            if (turnIndex > 0) {
                                val prevTurn = turns[turnIndex - 1]
                                enemySpawns = enemySpawns.map { spawn ->
                                    when (spawn.spawnTurn) {
                                        turn -> spawn.copy(spawnTurn = prevTurn)
                                        prevTurn -> spawn.copy(spawnTurn = turn)
                                        else -> spawn
                                    }
                                }.toMutableList()
                            }
                        },
                        onMoveTurnDown = {
                            if (turnIndex < turns.size - 1) {
                                val nextTurn = turns[turnIndex + 1]
                                enemySpawns = enemySpawns.map { spawn ->
                                    when (spawn.spawnTurn) {
                                        turn -> spawn.copy(spawnTurn = nextTurn)
                                        nextTurn -> spawn.copy(spawnTurn = turn)
                                        else -> spawn
                                    }
                                }.toMutableList()
                            }
                        },
                        canMoveUp = turnIndex > 0,
                        canMoveDown = turnIndex < turns.size - 1,
                        ewhadCount = ewhadCount
                    )
                }
            }
            
            // Towers section
            item {
                Text(
                    text = "Available Towers:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }
            
            items(DefenderType.entries.filter { it != DefenderType.DRAGONS_LAIR }) { tower ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = availableTowers.contains(tower),
                        onCheckedChange = { checked ->
                            if (checked) availableTowers.add(tower)
                            else availableTowers.remove(tower)
                        }
                    )
                    TowerIconOnHexagon(defenderType = tower)
                    Text("${tower.displayName} (Cost: ${tower.baseCost}, Damage: ${tower.baseDamage})")
                }
            }
        }
        
        // Save/Cancel buttons
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val updatedLevel = level.copy(
                            title = title,
                            subtitle = subtitle,
                            mapId = selectedMapId,
                            startCoins = startCoins.toIntOrNull() ?: 100,
                            startHealthPoints = startHP.toIntOrNull() ?: 10,
                            enemySpawns = enemySpawns.toList(),
                            availableTowers = availableTowers.toSet()
                        )
                        onSave(updatedLevel)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save Level")
                }
                
                Button(
                    onClick = { showSaveAsDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save As New")
                }
            }
            
            Button(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    }
    
    if (showEnemyDialog) {
        AddEnemyDialog(
            ewhadCount = ewhadCount,
            turn = showEnemyDialogForTurn,
            onDismiss = { showEnemyDialog = false },
            onAdd = { enemyType, level ->
                enemySpawns = enemySpawns.toMutableList().apply {
                    add(EditorEnemySpawn(enemyType, level, showEnemyDialogForTurn))
                }
                showEnemyDialog = false
            }
        )
    }
    
    if (showSaveAsDialog) {
        SaveAsDialog(
            title = "Save Level As New",
            label = "Level Title",
            currentValue = title,
            onDismiss = { showSaveAsDialog = false },
            onSave = { newTitle ->
                // Generate ID from title with underscores
                val sanitizedTitle = newTitle.trim().replace(" ", "_").replace(Regex("[^a-zA-Z0-9_]"), "")
                val newId = if (sanitizedTitle.isNotEmpty()) {
                    "level_$sanitizedTitle"
                } else {
                    "level_copy_${Random.nextInt(10000, 99999)}"
                }
                val newLevel = level.copy(
                    id = newId,
                    title = newTitle,
                    subtitle = subtitle,
                    mapId = selectedMapId,
                    startCoins = startCoins.toIntOrNull() ?: 100,
                    startHealthPoints = startHP.toIntOrNull() ?: 10,
                    enemySpawns = enemySpawns.toList(),
                    availableTowers = availableTowers.toSet()
                )
                onSave(newLevel)
                showSaveAsDialog = false
            }
        )
    }
}
