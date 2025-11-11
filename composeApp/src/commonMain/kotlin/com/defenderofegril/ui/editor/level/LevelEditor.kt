package com.defenderofegril.ui.editor.level

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.defenderofegril.editor.EditorEnemySpawn
import com.defenderofegril.editor.EditorLevel
import com.defenderofegril.editor.EditorMap
import com.defenderofegril.editor.EditorStorage
import com.defenderofegril.model.AttackerType
import com.defenderofegril.model.DefenderType
import com.defenderofegril.ui.*
import com.defenderofegril.ui.editor.ConfirmationDialog
import com.defenderofegril.ui.editor.CreateLevelDialog
import com.defenderofegril.ui.editor.map.MapSelectionCard
import com.defenderofegril.ui.editor.SaveAsDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*
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
                    text = stringResource(Res.string.levels),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Button(onClick = { showCreateDialog = true }) {
                    Text(stringResource(Res.string.create_new_level))
                }
            }
            
            Text(
                text = stringResource(Res.string.select_level_to_edit),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 300.dp),
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(levels.value) { level ->
                    LevelCard(
                        level = level,
                        isSelected = selectedLevelId == level.id,
                        onSelect = { 
                            selectedLevelId = level.id
                            editingLevel = level
                        },
                        onCopy = {
                            // Copy the level with a new ID and " - Copy" suffix
                            val copyTitle = "${level.title} - Copy"
                            val sanitizedTitle = copyTitle.trim().replace(" ", "_").replace(Regex("[^a-zA-Z0-9_]"), "")
                            val newId = "level_${sanitizedTitle}_${Random.nextInt(1000, 9999)}"
                            val copiedLevel = level.copy(
                                id = newId,
                                title = copyTitle
                            )
                            EditorStorage.saveLevel(copiedLevel)
                            levels.value = EditorStorage.getAllLevels()
                        },
                        onDelete = {
                            EditorStorage.deleteLevel(level.id)
                            levels.value = EditorStorage.getAllLevels()
                            if (selectedLevelId == level.id) {
                                selectedLevelId = null
                            }
                        }
                    )
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

@Composable
private fun LevelCard(
    level: EditorLevel,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect() }
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
                    text = "${stringResource(Res.string.file)}: ${level.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${stringResource(Res.string.map_label)}: ${level.mapId} | ${stringResource(Res.string.coins)}: ${level.startCoins} | ${stringResource(Res.string.hp_short)}: ${level.startHealthPoints}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "${stringResource(Res.string.enemies)}: ${level.enemySpawns.size}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCopy,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(Res.string.copy_level))
                }
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(Res.string.delete))
                }
            }
        }
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
    var availableTowersState by remember { mutableStateOf(level.availableTowers.toSet()) }
    var showEnemyDialog by remember { mutableStateOf(false) }
    var showEnemyDialogForTurn by remember { mutableStateOf(1) }
    var showSaveAsDialog by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableStateOf(0) }
    var showRemoveAllTurnsDialog by remember { mutableStateOf(false) }
    // Track the maximum turn number explicitly to support empty turns
    var maxTurnNumber by remember { 
        mutableStateOf(level.enemySpawns.maxOfOrNull { it.spawnTurn } ?: 0) 
    }
    
    // Get only ready-to-use maps for selection
    val maps = remember { EditorStorage.getAllMaps().filter { it.readyToUse } }
    
    // Check if Ewhad is already in spawn list
    val ewhadCount = enemySpawns.count { it.attackerType == AttackerType.EWHAD }
    
    // Check readiness for each tab
    val coinsInt = startCoins.toIntOrNull() ?: 0
    val hpInt = startHP.toIntOrNull() ?: 0
    val isLevelInfoReady = coinsInt > 0 && hpInt > 0
    val isEnemySpawnsReady = enemySpawns.isNotEmpty()
    val isTowersReady = availableTowersState.isNotEmpty()
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Title above tabs
        Text(
            text = "${stringResource(Res.string.level_title)}: ${level.title}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Tab Row with badges
        TabRow(selectedTabIndex = selectedTabIndex) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(stringResource(Res.string.level_info_tab))
                        if (!isLevelInfoReady) {
                            RedDotBadge()
                        }
                    }
                }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(stringResource(Res.string.enemy_spawns_tab))
                        if (!isEnemySpawnsReady) {
                            RedDotBadge()
                        }
                    }
                }
            )
            Tab(
                selected = selectedTabIndex == 2,
                onClick = { selectedTabIndex = 2 },
                text = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(stringResource(Res.string.towers_tab))
                        if (!isTowersReady) {
                            RedDotBadge()
                        }
                    }
                }
            )
        }
        
        // Tab Content
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTabIndex) {
                0 -> LevelInfoTab(
                    title = title,
                    onTitleChange = { title = it },
                    subtitle = subtitle,
                    onSubtitleChange = { subtitle = it },
                    selectedMapId = selectedMapId,
                    onMapChange = { selectedMapId = it },
                    maps = maps,
                    startCoins = startCoins,
                    onStartCoinsChange = { startCoins = it },
                    startHP = startHP,
                    onStartHPChange = { startHP = it }
                )
                1 -> EnemySpawnsTab(
                    enemySpawns = enemySpawns,
                    maxTurnNumber = maxTurnNumber,
                    onMaxTurnNumberChange = { maxTurnNumber = it },
                    onEnemySpawnsChange = { enemySpawns = it },
                    ewhadCount = ewhadCount,
                    onShowEnemyDialog = { turn ->
                        showEnemyDialog = true
                        showEnemyDialogForTurn = turn
                    },
                    onShowRemoveAllTurnsDialog = { showRemoveAllTurnsDialog = true }
                )
                2 -> TowersTab(
                    availableTowers = availableTowersState,
                    onAvailableTowersChange = { availableTowersState = it }
                )
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
                            availableTowers = availableTowersState
                        )
                        onSave(updatedLevel)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(Res.string.save_level))
                }
                
                Button(
                    onClick = { showSaveAsDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(Res.string.save_as_new))
                }
            }
            
            Button(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(Res.string.cancel))
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
    
    if (showRemoveAllTurnsDialog) {
        ConfirmationDialog(
            title = stringResource(Res.string.remove_all_turns),
            message = stringResource(Res.string.confirm_remove_all_turns),
            onDismiss = { showRemoveAllTurnsDialog = false },
            onConfirm = {
                enemySpawns = mutableListOf()
                maxTurnNumber = 0
                showRemoveAllTurnsDialog = false
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
                    availableTowers = availableTowersState
                )
                onSave(newLevel)
                showSaveAsDialog = false
            }
        )
    }
}

/**
 * Tab 1: Level Info (title, subtitle, map, coins, HP)
 */
@Composable
private fun LevelInfoTab(
    title: String,
    onTitleChange: (String) -> Unit,
    subtitle: String,
    onSubtitleChange: (String) -> Unit,
    selectedMapId: String,
    onMapChange: (String) -> Unit,
    maps: List<EditorMap>,
    startCoins: String,
    onStartCoinsChange: (String) -> Unit,
    startHP: String,
    onStartHPChange: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Title and subtitle
        item {
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text(stringResource(Res.string.level_title)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        item {
            OutlinedTextField(
                value = subtitle,
                onValueChange = onSubtitleChange,
                label = { Text(stringResource(Res.string.subtitle_optional)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Map selection with mini-maps
        item {
            Column {
                Text(
                    text = "${stringResource(Res.string.map_label)}:",
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
                            onClick = { onMapChange(map.id) }
                        )
                    }
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
                    onValueChange = { if (it.all { c -> c.isDigit() }) onStartCoinsChange(it) },
                    label = { Text(stringResource(Res.string.start_coins)) },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = startHP,
                    onValueChange = { if (it.all { c -> c.isDigit() }) onStartHPChange(it) },
                    label = { Text(stringResource(Res.string.start_hp)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Tab 2: Enemy Spawns
 */
@Composable
private fun EnemySpawnsTab(
    enemySpawns: MutableList<EditorEnemySpawn>,
    maxTurnNumber: Int,
    onMaxTurnNumberChange: (Int) -> Unit,
    onEnemySpawnsChange: (MutableList<EditorEnemySpawn>) -> Unit,
    ewhadCount: Int,
    onShowEnemyDialog: (Int) -> Unit,
    onShowRemoveAllTurnsDialog: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Add turn and remove all turns buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${stringResource(Res.string.enemies)} (${enemySpawns.size}):",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = { 
                        // Add a new empty turn without opening dialog
                        onMaxTurnNumberChange(maxTurnNumber + 1)
                    }) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("➕")
                            Text(stringResource(Res.string.add_turn))
                        }
                    }
                    
                    Button(
                        onClick = onShowRemoveAllTurnsDialog,
                        enabled = enemySpawns.isNotEmpty() || maxTurnNumber > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (enemySpawns.isNotEmpty() || maxTurnNumber > 0) 
                                MaterialTheme.colorScheme.error 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(stringResource(Res.string.remove_all_turns))
                    }
                }
            }
        }
        
        // Group enemies by spawn turn and create list including empty turns
        val turnGroups = enemySpawns.groupBy { it.spawnTurn }.entries.sortedBy { it.key }
        
        // Create list of all turns from 1 to maxTurnNumber (including empty ones)
        val allTurns = (1..maxTurnNumber).map { turn ->
            turn to (turnGroups.find { it.key == turn }?.value ?: emptyList())
        }
        
        allTurns.forEachIndexed { index, (turn, spawnsInTurn) ->
            item {
                SpawnTurnSection(
                    turn = turn,
                    spawns = spawnsInTurn,
                    onRemoveEnemy = { spawn ->
                        val newSpawns = enemySpawns.toMutableList().apply { remove(spawn) }
                        onEnemySpawnsChange(newSpawns)
                    },
                    onDeleteTurn = {
                        // Check if this is the last turn
                        val isLastTurn = turn == maxTurnNumber
                        if (isLastTurn) {
                            // Remove all enemies from this turn and decrement maxTurnNumber
                            val newSpawns = enemySpawns.filter { it.spawnTurn != turn }.toMutableList()
                            onEnemySpawnsChange(newSpawns)
                            onMaxTurnNumberChange(maxTurnNumber - 1)
                        }
                    },
                    onClearTurn = {
                        // Clear all enemies from this turn but keep the turn
                        val newSpawns = enemySpawns.filter { it.spawnTurn != turn }.toMutableList()
                        onEnemySpawnsChange(newSpawns)
                    },
                    canDeleteTurn = turn == maxTurnNumber,
                    onCopyTurn = {
                        // Copy all enemies from this turn to a new turn (next available)
                        val newSpawns = enemySpawns.toMutableList().apply {
                            spawnsInTurn.forEach { spawn ->
                                add(spawn.copy(spawnTurn = maxTurnNumber + 1))
                            }
                        }
                        onEnemySpawnsChange(newSpawns)
                        onMaxTurnNumberChange(maxTurnNumber + 1)
                    },
                    onAddEnemy = {
                        // Show dialog to add enemy to this specific turn
                        onShowEnemyDialog(turn)
                    },
                    onMoveTurnUp = {
                        if (index > 0) {
                            val prevTurn = allTurns[index - 1].first
                            val newSpawns = enemySpawns.map { spawn ->
                                when (spawn.spawnTurn) {
                                    turn -> spawn.copy(spawnTurn = prevTurn)
                                    prevTurn -> spawn.copy(spawnTurn = turn)
                                    else -> spawn
                                }
                            }.toMutableList()
                            onEnemySpawnsChange(newSpawns)
                        }
                    },
                    onMoveTurnDown = {
                        if (index < allTurns.size - 1) {
                            val nextTurn = allTurns[index + 1].first
                            val newSpawns = enemySpawns.map { spawn ->
                                when (spawn.spawnTurn) {
                                    turn -> spawn.copy(spawnTurn = nextTurn)
                                    nextTurn -> spawn.copy(spawnTurn = turn)
                                    else -> spawn
                                }
                            }.toMutableList()
                            onEnemySpawnsChange(newSpawns)
                        }
                    },
                    canMoveUp = index > 0,
                    canMoveDown = index < allTurns.size - 1,
                    ewhadCount = ewhadCount
                )
            }
        }
    }
}

/**
 * Tab 3: Available Towers
 */
@Composable
private fun TowersTab(
    availableTowers: Set<DefenderType>,
    onAvailableTowersChange: (Set<DefenderType>) -> Unit
) {
    val allTowers = DefenderType.entries.filter { it != DefenderType.DRAGONS_LAIR }
    val hasUnselectedTowers = allTowers.any { !availableTowers.contains(it) }
    val hasSelectedTowers = availableTowers.isNotEmpty()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Add All / Remove All buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onAvailableTowersChange(allTowers.toSet())
                    },
                    enabled = hasUnselectedTowers,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(Res.string.add_all_towers))
                }
                Button(
                    onClick = {
                        onAvailableTowersChange(emptySet())
                    },
                    enabled = hasSelectedTowers,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(Res.string.remove_all_towers))
                }
            }
        }
        
        item {
            Text(
                text = stringResource(Res.string.available_towers),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }
        
        items(allTowers) { tower ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = availableTowers.contains(tower),
                    onCheckedChange = { checked ->
                        val newTowers = if (checked) {
                            availableTowers + tower
                        } else {
                            availableTowers - tower
                        }
                        onAvailableTowersChange(newTowers)
                    }
                )
                TowerIconOnHexagon(defenderType = tower)
                Text("${tower.getLocalizedName()} (${stringResource(Res.string.cost_label)}: ${tower.baseCost}, ${stringResource(Res.string.damage_label)}: ${tower.baseDamage})")
            }
        }
    }
}

/**
 * Red dot badge to indicate incomplete data
 */
@Composable
private fun RedDotBadge() {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(Color.Red)
    )
}
