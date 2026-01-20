package de.egril.defender.ui.editor.level

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
import de.egril.defender.editor.EditorEnemySpawn
import de.egril.defender.editor.EditorLevel
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.EditorStorage
import de.egril.defender.editor.EditorWaypoint
import de.egril.defender.editor.WaypointValidationResult
import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import de.egril.defender.model.Position
import de.egril.defender.ui.*
import de.egril.defender.ui.editor.ConfirmationDialog
import de.egril.defender.ui.editor.CreateLevelDialog
import de.egril.defender.ui.editor.map.MapSelectionCard
import de.egril.defender.ui.editor.SaveAsDialog
import de.egril.defender.ui.icon.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.hyperether.resources.stringResource
import de.egril.defender.ui.editor.level.enemies.EnemySpawnsTab
import de.egril.defender.ui.editor.level.tower.TowersTab
import de.egril.defender.ui.editor.level.waypoint.WaypointsTab
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
    var levelToDelete by remember { mutableStateOf<EditorLevel?>(null) }
    
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
                            val sanitizedTitle = copyTitle.trim().lowercase()
                                .replace(" ", "_")
                                .replace(Regex("[^a-z0-9_]"), "")
                                .replace(Regex("_+"), "_")  // Collapse consecutive underscores
                            val newId = "${sanitizedTitle}_${Random.nextInt(1000, 9999)}"
                            val copiedLevel = level.copy(
                                id = newId,
                                title = copyTitle
                            )
                            EditorStorage.saveLevel(copiedLevel)
                            levels.value = EditorStorage.getAllLevels()
                        },
                        onDelete = {
                            levelToDelete = level
                        }
                    )
                }
            }
        }
    }
    
    // Confirmation dialog for delete level
    if (levelToDelete != null) {
        ConfirmationDialog(
            title = stringResource(Res.string.delete),
            message = stringResource(Res.string.confirm_delete_level),
            onDismiss = { levelToDelete = null },
            onConfirm = {
                EditorStorage.deleteLevel(levelToDelete!!.id)
                levels.value = EditorStorage.getAllLevels()
                if (selectedLevelId == levelToDelete!!.id) {
                    selectedLevelId = null
                }
                levelToDelete = null
            }
        )
    }
    
    if (showCreateDialog) {
        CreateLevelDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title ->
                // Generate ID from title with underscores (lowercase, no "level_" prefix)
                val sanitizedTitle = title.trim().lowercase()
                    .replace(" ", "_")
                    .replace(Regex("[^a-z0-9_]"), "")
                    .replace(Regex("_+"), "_")  // Collapse consecutive underscores
                val newId = if (sanitizedTitle.isNotEmpty()) {
                    sanitizedTitle
                } else {
                    "custom_${Random.nextInt(10000, 99999)}"
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
    // Check if any enemies are spawned outside valid spawn points
    val map = remember(level.mapId) { EditorStorage.getMap(level.mapId) }
    val hasEnemiesOutsideSpawnPoints = remember(level.enemySpawns, map) {
        val mapSpawnPoints = map?.getSpawnPoints()?.toSet() ?: emptySet()
        level.enemySpawns.any { spawn ->
            spawn.spawnPoint != null && spawn.spawnPoint !in mapSpawnPoints
        }
    }
    
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
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect() }
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = level.title,
                            style = MaterialTheme.typography.titleSmall
                        )
                        // Add ready/not ready indicator
                        if (EditorStorage.isLevelReadyToPlay(level)) {
                            CheckmarkIcon(
                                size = 16.dp,
                                tint = Color.Green
                            )
                        } else {
                            Text(
                                text = "✗",
                                color = Color.Red,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        // Add warning badge if enemies are outside spawn points
                        if (hasEnemiesOutsideSpawnPoints) {
                            WarningBadge()
                        }
                    }
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
                Text(
                    text = if (EditorStorage.isLevelReadyToPlay(level)) stringResource(Res.string.ready_to_use) else stringResource(Res.string.not_ready),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (EditorStorage.isLevelReadyToPlay(level)) Color.Green else Color.Red
                )
            }
            
            // Test Level badge in upper right corner
            if (level.testingOnly) {
                Text(
                    text = stringResource(Res.string.test_level),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Red,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            }
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
    var waypointsState by remember { mutableStateOf(level.waypoints.toMutableList()) }
    var testingOnly by remember { mutableStateOf(level.testingOnly) }
    var allowAutoAttack by remember { mutableStateOf(level.allowAutoAttack) }
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
    
    // Get current map to access waypoint tiles and target
    val currentMap = remember(selectedMapId) { EditorStorage.getMap(selectedMapId) }
    
    // Check if Ewhad is already in spawn list
    val ewhadCount = enemySpawns.count { it.attackerType == AttackerType.EWHAD }
    
    // Check if any enemies are spawned outside valid spawn points
    val mapSpawnPoints = remember(currentMap) { currentMap?.getSpawnPoints()?.toSet() ?: emptySet() }
    val hasEnemiesOutsideSpawnPoints = remember(enemySpawns, mapSpawnPoints) {
        enemySpawns.any { spawn ->
            spawn.spawnPoint != null && spawn.spawnPoint !in mapSpawnPoints
        }
    }
    
    // Check readiness for each tab
    val coinsInt = startCoins.toIntOrNull() ?: 0
    val hpInt = startHP.toIntOrNull() ?: 0
    val isLevelInfoReady = coinsInt > 0 && hpInt > 0
    val isEnemySpawnsReady = enemySpawns.isNotEmpty()
    val isTowersReady = availableTowersState.isNotEmpty()
    // Waypoints are optional, but if present they should be valid
    val isWaypointsValid = areWaypointsValid(waypointsState, currentMap, level)
    
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
        PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
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
                        } else if (hasEnemiesOutsideSpawnPoints) {
                            WarningBadge()
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
            Tab(
                selected = selectedTabIndex == 3,
                onClick = { selectedTabIndex = 3 },
                text = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(stringResource(Res.string.waypoints_tab))
                        if (!isWaypointsValid) {
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
                    onStartHPChange = { startHP = it },
                    testingOnly = testingOnly,
                    onTestingOnlyChange = { testingOnly = it },
                    allowAutoAttack = allowAutoAttack,
                    onAllowAutoAttackChange = { allowAutoAttack = it }
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
                    onShowRemoveAllTurnsDialog = { showRemoveAllTurnsDialog = true },
                    map = currentMap
                )
                2 -> TowersTab(
                    availableTowers = availableTowersState,
                    onAvailableTowersChange = { availableTowersState = it }
                )
                3 -> WaypointsTab(
                    waypoints = waypointsState.toList(),
                    onWaypointsChange = { waypointsState = it.toMutableList() },
                    map = currentMap,
                    isValid = isWaypointsValid
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
                            availableTowers = availableTowersState,
                            waypoints = waypointsState.toList(),
                            testingOnly = testingOnly,
                            allowAutoAttack = allowAutoAttack
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
            map = currentMap,
            onDismiss = { showEnemyDialog = false },
            onAdd = { enemyType, level, amount, spawnPoint ->
                enemySpawns = enemySpawns.toMutableList().apply {
                    // Add multiple enemies based on amount
                    repeat(amount) {
                        add(EditorEnemySpawn(enemyType, level, showEnemyDialogForTurn, spawnPoint))
                    }
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
                    availableTowers = availableTowersState,
                    waypoints = waypointsState.toList(),
                    testingOnly = testingOnly
                )
                onSave(newLevel)
                showSaveAsDialog = false
            }
        )
    }
}

private fun areWaypointsValid(
    waypointsState: MutableList<EditorWaypoint>,
    currentMap: EditorMap?,
    level: EditorLevel
): Boolean {
    val targets = currentMap?.getTargets() ?: emptyList()
    if (targets.isEmpty() || currentMap == null) {
        return false
    }
    
    val spawnPoints = currentMap.getSpawnPoints()
    val tempLevel = level.copy(waypoints = waypointsState.toList())
    val validationResult = tempLevel.validateWaypointsDetailed(targets, spawnPoints)
    
    // Valid if validation passes
    return validationResult.isValid
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

/**
 * Warning badge to indicate issues that don't prevent playability
 */
@Composable
private fun WarningBadge() {
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(Color(0xFFFFA500)) // Orange color
    )
}
