package de.egril.defender.ui.editor.level

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.egril.defender.editor.EditorEnemySpawn
import de.egril.defender.editor.EditorMap
import de.egril.defender.model.AttackerType
import de.egril.defender.model.Position
import de.egril.defender.ui.*
import de.egril.defender.ui.icon.DownArrowIcon
import de.egril.defender.ui.icon.ReloadIcon
import de.egril.defender.ui.icon.TrashIcon
import de.egril.defender.ui.icon.TriangleDownIcon
import de.egril.defender.ui.icon.TriangleRightIcon
import de.egril.defender.ui.icon.UpArrowIcon
import de.egril.defender.ui.icon.CheckmarkIcon
import de.egril.defender.ui.icon.PushpinIcon
import com.hyperether.resources.stringResource
import de.egril.defender.ui.common.SelectableText
import de.egril.defender.ui.hexagon.EnemyIconOnHexagon
import defender_of_egril.composeapp.generated.resources.*

/**
 * Dialog for adding an enemy to a specific turn
 */
@Composable
fun AddEnemyDialog(
    ewhadCount: Int = 0,
    turn: Int,
    map: EditorMap?,
    onDismiss: () -> Unit,
    onAdd: (AttackerType, Int, Int, Position?) -> Unit  // Added spawnPoint parameter
) {
    var selectedType by remember { mutableStateOf(AttackerType.GOBLIN) }
    var level by remember { mutableStateOf("1") }
    var amount by remember { mutableStateOf("1") }
    
    // Get available spawn points from the map
    val spawnPoints = remember(map) { map?.getSpawnPoints() ?: emptyList() }
    var selectedSpawnPoint by remember { mutableStateOf<Position?>(spawnPoints.firstOrNull()) }
    
    // State for spawn point selection dialog
    var showSpawnPointDialog by remember { mutableStateOf(false) }
    
    // Check if trying to add Ewhad when one already exists
    val canAddEwhad = selectedType != AttackerType.EWHAD || ewhadCount == 0
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { SelectableText(stringResource(Res.string.add_enemy_to_turn, turn)) },
        text = {
            Column {
                Text(stringResource(Res.string.enemy_type), modifier = Modifier.padding(bottom = 4.dp))
                LazyColumn(
                    modifier = Modifier.height(150.dp).padding(bottom = 8.dp)
                ) {
                    items(AttackerType.entries) { type ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { selectedType = type }.padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = selectedType == type,
                                onClick = { selectedType = type }
                            )
                            // Add enemy icon in hexagon
                            Box(modifier = Modifier.size(24.dp)) {
                                EnemyIconOnHexagon(
                                    attackerType = type,
                                    size = 24.dp
                                )
                            }
                            Text("${type.getLocalizedName()} (${stringResource(Res.string.hp_label)}: ${type.health})")
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = level,
                        onValueChange = { if (it.all { c -> c.isDigit() }) level = it },
                        label = { Text(stringResource(Res.string.enemy_level)) },
                        modifier = Modifier.weight(1f).padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { if (it.all { c -> c.isDigit() } && it.isNotEmpty()) amount = it },
                        label = { Text(stringResource(Res.string.amount)) },
                        modifier = Modifier.weight(1f).padding(bottom = 8.dp)
                    )
                }
                
                // Spawn point selection button (opens dialog with minimap)
                if (spawnPoints.isNotEmpty()) {
                    Text(
                        text = stringResource(Res.string.spawn_point),
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                    
                    OutlinedButton(
                        onClick = { showSpawnPointDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = selectedSpawnPoint?.let { "Position (${it.x}, ${it.y})" } 
                                ?: stringResource(Res.string.select_spawn_point)
                        )
                    }
                }
                
                Text(stringResource(Res.string.hp_with_level, selectedType.health * (level.toIntOrNull() ?: 1)), fontSize = 12.sp)
                if (!canAddEwhad) {
                    Text(
                        text = stringResource(Res.string.ewhad_warning),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountValue = amount.toIntOrNull() ?: 1
                    onAdd(selectedType, level.toIntOrNull() ?: 1, amountValue, selectedSpawnPoint)
                },
                enabled = canAddEwhad
            ) {
                SelectableText(stringResource(Res.string.add))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
    
    // Spawn point selection dialog with minimap
    if (showSpawnPointDialog) {
        SelectSpawnPointDialog(
            selectedType = selectedType,
            level = level.toIntOrNull() ?: 1,
            map = map,
            currentSelection = selectedSpawnPoint,
            onDismiss = { showSpawnPointDialog = false },
            onSelect = { point ->
                selectedSpawnPoint = point
                showSpawnPointDialog = false
            }
        )
    }
}

/**
 * Dialog for selecting or changing a spawn point with minimap visualization.
 * Can be used both when adding new enemies and when changing existing enemy spawn points.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpawnPointSelectionDialog(
    attackerType: AttackerType,
    level: Int,
    healthPoints: Int = attackerType.health * level,
    map: EditorMap?,
    currentSelection: Position?,
    title: String,
    confirmButtonText: String,
    onDismiss: () -> Unit,
    onConfirm: (Position) -> Unit
) {
    val spawnPoints = remember(map) { map?.getSpawnPoints() ?: emptyList() }
    var selectedSpawnPoint by remember { mutableStateOf(currentSelection ?: spawnPoints.firstOrNull()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            SelectableText(title)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Enemy info
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(32.dp)) {
                        EnemyIconOnHexagon(
                            attackerType = attackerType,
                            size = 32.dp
                        )
                    }
                    Column {
                        Text(
                            text = "${attackerType.getLocalizedName()} Lvl $level",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${stringResource(Res.string.hp_short)}: $healthPoints",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
                
                HorizontalDivider()
                
                // Minimap showing spawn points
                if (map != null && spawnPoints.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .background(Color(0xCC000000))
                            .border(2.dp, Color.White)
                            .padding(4.dp)
                    ) {
                        SpawnPointMinimap(
                            map = map,
                            selectedSpawnPoint = selectedSpawnPoint
                        )
                    }
                }
                
                // Instructions
                Text(
                    text = stringResource(Res.string.select_spawn_point_for_enemy),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Spawn point selection chips
                if (spawnPoints.isNotEmpty()) {
                    Text(
                        text = stringResource(Res.string.spawn_point),
                        style = MaterialTheme.typography.titleSmall
                    )
                    
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        spawnPoints.forEach { point ->
                            FilterChip(
                                modifier = Modifier.height(32.dp),
                                selected = selectedSpawnPoint == point,
                                onClick = { selectedSpawnPoint = point },
                                label = {
                                    Text(
                                        text = "S(${point.x},${point.y})",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                leadingIcon = if (selectedSpawnPoint == point) {
                                    { CheckmarkIcon(size = 14.dp) }
                                } else null
                            )
                        }
                    }
                } else {
                    Text(
                        text = stringResource(Res.string.no_spawn_points_available),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedSpawnPoint?.let { onConfirm(it) }
                },
                enabled = selectedSpawnPoint != null
            ) {
                SelectableText(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

/**
 * Convenience wrapper for selecting spawn point when adding new enemies
 */
@Composable
fun SelectSpawnPointDialog(
    selectedType: AttackerType,
    level: Int,
    map: EditorMap?,
    currentSelection: Position?,
    onDismiss: () -> Unit,
    onSelect: (Position) -> Unit
) {
    SpawnPointSelectionDialog(
        attackerType = selectedType,
        level = level,
        map = map,
        currentSelection = currentSelection,
        title = stringResource(Res.string.select_spawn_point),
        confirmButtonText = stringResource(Res.string.select),
        onDismiss = onDismiss,
        onConfirm = onSelect
    )
}

/**
 * Convenience wrapper for changing spawn point of existing enemies
 */
@Composable
fun ChangeSpawnPointDialog(
    spawn: EditorEnemySpawn,
    map: EditorMap?,
    onDismiss: () -> Unit,
    onChange: (Position) -> Unit
) {
    SpawnPointSelectionDialog(
        attackerType = spawn.attackerType,
        level = spawn.level,
        healthPoints = spawn.healthPoints,
        map = map,
        currentSelection = spawn.spawnPoint,
        title = stringResource(Res.string.change_spawn_point),
        confirmButtonText = stringResource(Res.string.change),
        onDismiss = onDismiss,
        onConfirm = onChange
    )
}

/**
 * Dialog for changing the level of an existing enemy spawn
 */
@Composable
fun ChangeLevelDialog(
    spawn: EditorEnemySpawn,
    onDismiss: () -> Unit,
    onChange: (Int) -> Unit
) {
    var newLevel by remember { mutableStateOf(spawn.level.toString()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { SelectableText(stringResource(Res.string.change_enemy_level)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Enemy info
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(32.dp)) {
                        EnemyIconOnHexagon(
                            attackerType = spawn.attackerType,
                            size = 32.dp
                        )
                    }
                    Column {
                        Text(
                            text = spawn.attackerType.getLocalizedName(),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${stringResource(Res.string.level)}: ${spawn.level}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
                
                HorizontalDivider()
                
                // New level input
                OutlinedTextField(
                    value = newLevel,
                    onValueChange = { if (it.all { c -> c.isDigit() } || it.isEmpty()) newLevel = it },
                    label = { Text(stringResource(Res.string.new_level)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Show new HP calculation
                val newLevelValue = newLevel.toIntOrNull() ?: spawn.level
                Text(
                    text = stringResource(Res.string.hp_with_level, spawn.attackerType.health * newLevelValue),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val levelValue = newLevel.toIntOrNull()
                    if (levelValue != null && levelValue > 0) {
                        onChange(levelValue)
                    }
                },
                enabled = newLevel.toIntOrNull()?.let { it > 0 } == true
            ) {
                SelectableText(stringResource(Res.string.change))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

/**
 * Dialog for changing the level of all enemies in a spawn turn.
 * Shows a confirmation dialog if units have different levels.
 */
@Composable
fun ChangeTurnLevelDialog(
    turn: Int,
    spawns: List<EditorEnemySpawn>,
    onDismiss: () -> Unit,
    onChange: (Int) -> Unit
) {
    var newLevel by remember { mutableStateOf("") }
    var showMixedLevelsConfirmation by remember { mutableStateOf(false) }
    
    // Get unique levels in this turn
    val uniqueLevels = spawns.map { it.level }.distinct().sorted()
    val hasMixedLevels = uniqueLevels.size > 1
    
    // Initialize with the first level if all same, otherwise leave empty
    LaunchedEffect(spawns) {
        if (!hasMixedLevels && spawns.isNotEmpty()) {
            newLevel = uniqueLevels.first().toString()
        }
    }
    
    if (showMixedLevelsConfirmation) {
        // Confirmation dialog for mixed levels
        val levelsList = uniqueLevels.joinToString(", ")
        val targetLevel = newLevel.toIntOrNull() ?: 1
        
        AlertDialog(
            onDismissRequest = { showMixedLevelsConfirmation = false },
            title = { SelectableText(stringResource(Res.string.change_turn_level)) },
            text = {
                Text(stringResource(Res.string.mixed_levels_warning, levelsList, targetLevel))
            },
            confirmButton = {
                Button(
                    onClick = {
                        onChange(targetLevel)
                        showMixedLevelsConfirmation = false
                    }
                ) {
                    SelectableText(stringResource(Res.string.apply))
                }
            },
            dismissButton = {
                TextButton(onClick = { showMixedLevelsConfirmation = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { SelectableText(stringResource(Res.string.change_all_levels_in_turn, turn)) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Show current level distribution
                    Text(
                        text = "${stringResource(Res.string.enemies)}: ${spawns.size}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    if (hasMixedLevels) {
                        Text(
                            text = "${stringResource(Res.string.level)}: ${uniqueLevels.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    } else if (spawns.isNotEmpty()) {
                        Text(
                            text = "${stringResource(Res.string.level)}: ${uniqueLevels.first()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    
                    HorizontalDivider()
                    
                    // New level input
                    OutlinedTextField(
                        value = newLevel,
                        onValueChange = { if (it.all { c -> c.isDigit() } || it.isEmpty()) newLevel = it },
                        label = { Text(stringResource(Res.string.new_level)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val levelValue = newLevel.toIntOrNull()
                        if (levelValue != null && levelValue > 0) {
                            if (hasMixedLevels) {
                                showMixedLevelsConfirmation = true
                            } else {
                                onChange(levelValue)
                            }
                        }
                    },
                    enabled = newLevel.toIntOrNull()?.let { it > 0 } == true
                ) {
                    SelectableText(stringResource(Res.string.apply))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }
}

/**
 * Collapsible section showing enemies spawning in a specific turn
 */
@Composable
fun SpawnTurnSection(
    turn: Int,
    spawns: List<EditorEnemySpawn>,
    initiallyExpanded: Boolean = false,
    onRemoveEnemy: (EditorEnemySpawn) -> Unit,
    onDeleteTurn: () -> Unit,
    onClearTurn: () -> Unit,
    canDeleteTurn: Boolean,
    onCopyTurn: () -> Unit,
    onAddEnemy: () -> Unit,
    onMoveTurnUp: () -> Unit,
    onMoveTurnDown: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    ewhadCount: Int,
    onChangeSpawnPoint: (EditorEnemySpawn) -> Unit,
    onChangeLevel: (EditorEnemySpawn) -> Unit,
    onChangeTurnLevel: () -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            // Turn header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (expanded) {
                        TriangleDownIcon(size = 16.dp)
                    } else {
                        TriangleRightIcon(size = 16.dp)
                    }
                    ReloadIcon(size = 14.dp)
                    SelectableText(
                        text = "Turn $turn",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    SelectableText(
                        text = "(${spawns.size} enemies)",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = onMoveTurnUp,
                        enabled = canMoveUp,
                        modifier = Modifier.height(32.dp)
                    ) {
                        UpArrowIcon(size = 12.dp, tint = Color.White)
                    }
                    Button(
                        onClick = onMoveTurnDown,
                        enabled = canMoveDown,
                        modifier = Modifier.height(32.dp)
                    ) {
                        DownArrowIcon(size = 12.dp, tint = Color.White)
                    }
                    // Change Turn Level button - only visible if there are units
                    if (spawns.isNotEmpty()) {
                        Button(
                            onClick = onChangeTurnLevel,
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            SelectableText(
                                text = stringResource(Res.string.level),
                                fontSize = 12.sp
                            )
                        }
                    }
                    Button(
                        onClick = onCopyTurn,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        SelectableText(
                            text = "Copy Turn", 
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                    // Show either Clear or Delete button depending on whether it's the last turn
                    // Make sure both buttons have same size by using consistent width
                    if (canDeleteTurn) {
                        Button(
                            onClick = onDeleteTurn,
                            modifier = Modifier.height(32.dp).width(80.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TrashIcon(size = 12.dp)
                            }
                        }
                    } else {
                        Button(
                            onClick = onClearTurn,
                            enabled = spawns.isNotEmpty(),
                            modifier = Modifier.height(32.dp).width(80.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            SelectableText(
                                text = stringResource(Res.string.clear_turn),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
            
            // Enemy list (collapsible)
            if (expanded) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Add Enemy button at the top
                    Button(
                        onClick = onAddEnemy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SelectableText(stringResource(Res.string.add_enemy_button, turn))
                    }
                    
                    if (spawns.isEmpty()) {
                        // Show message for empty turn
                        SelectableText(
                            text = stringResource(Res.string.no_enemies_in_turn),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(8.dp)
                        )
                    } else {
                        spawns.forEach { spawn ->
                            EnemySpawnRow(
                                spawn = spawn,
                                onRemoveEnemy = { onRemoveEnemy(spawn) },
                                onChangeSpawnPoint = onChangeSpawnPoint,
                                onChangeLevel = onChangeLevel
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Row displaying a single enemy spawn with its information and actions
 */
@Composable
private fun EnemySpawnRow(
    spawn: EditorEnemySpawn,
    onRemoveEnemy: () -> Unit,
    onChangeSpawnPoint: (EditorEnemySpawn) -> Unit,
    onChangeLevel: (EditorEnemySpawn) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.LightGray.copy(alpha = 0.2f))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Enemy icon
            Box(modifier = Modifier.size(24.dp)) {
                EnemyIconOnHexagon(
                    attackerType = spawn.attackerType,
                    size = 24.dp
                )
            }
            
            // Enemy info in horizontal layout
            SelectableText(
                text = "${spawn.attackerType.displayName} Lv${spawn.level}",
                fontSize = 14.sp
            )
            SelectableText(
                text = "${stringResource(Res.string.hp_short)}: ${spawn.healthPoints}",
                fontSize = 11.sp,
                color = Color.Gray
            )
            
            // Display spawn point - always show it
            val spawnPointText = spawn.spawnPoint?.let { spawnPoint ->
                "${stringResource(Res.string.spawn_point)}: (${spawnPoint.x}, ${spawnPoint.y})"
            } ?: stringResource(Res.string.no_spawn_point_set)
            SelectableText(
                text = spawnPointText,
                fontSize = 10.sp,
                color = if (spawn.spawnPoint != null) MaterialTheme.colorScheme.primary else Color.Gray,
                fontWeight = if (spawn.spawnPoint != null) FontWeight.SemiBold else FontWeight.Normal
            )
            
            // Change spawn point button - right after spawn point info
            Button(
                onClick = { onChangeSpawnPoint(spawn) },
                modifier = Modifier.height(28.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
            ) {
                PushpinIcon(size = 10.dp)
            }
            
            // Change level button
            Button(
                onClick = { onChangeLevel(spawn) },
                modifier = Modifier.height(28.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
            ) {
                SelectableText(stringResource(Res.string.level), fontSize = 10.sp)
            }
        }
        
        // Remove button on the far right
        Button(
            onClick = onRemoveEnemy,
            modifier = Modifier.height(36.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TrashIcon(size = 12.dp)
                SelectableText(stringResource(Res.string.remove), fontSize = 11.sp)
            }
        }
    }
}

/**
 * Dialog for bulk changing spawn points of all enemies
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChangeAllSpawnPointsDialog(
    enemySpawns: List<EditorEnemySpawn>,
    map: EditorMap?,
    onDismiss: () -> Unit,
    onApply: (Map<Position, Position>) -> Unit
) {
    val mapSpawnPoints = remember(map) { map?.getSpawnPoints() ?: emptyList() }
    
    // Get all unique spawn points currently used by enemies
    val usedSpawnPoints = remember(enemySpawns) {
        enemySpawns.mapNotNull { it.spawnPoint }.distinct()
    }
    
    // Create a mutable state map for spawn point remapping
    val remappings = remember {
        mutableStateMapOf<Position, Position>().apply {
            // Initialize each used position to first available spawn point or itself
            usedSpawnPoints.forEach { pos ->
                put(pos, mapSpawnPoints.firstOrNull() ?: pos)
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { SelectableText(stringResource(Res.string.change_all_spawn_points_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Minimap at the top (non-scrolling)
                if (map != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .background(Color(0xCC000000))
                            .border(2.dp, Color.White)
                            .padding(4.dp)
                    ) {
                        SpawnPointMinimap(
                            map = map,
                            selectedSpawnPoint = null
                        )
                    }
                    
                    HorizontalDivider()
                }
                
                // Description
                Text(
                    text = stringResource(Res.string.change_all_spawn_points_description),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                HorizontalDivider()
                
                // Count of affected enemies
                val affectedCount = enemySpawns.count { spawn ->
                    spawn.spawnPoint?.let { remappings[it] != it } ?: false
                }
                Text(
                    text = stringResource(Res.string.enemies_affected_count, affectedCount),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                
                HorizontalDivider()
                
                // Remapping table (scrollable)
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(250.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(usedSpawnPoints) { fromPos ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // From position
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(Res.string.from_spawn_point),
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "(${fromPos.x}, ${fromPos.y})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                // Count of enemies at this position
                                val count = enemySpawns.count { it.spawnPoint == fromPos }
                                Text(
                                    text = stringResource(Res.string.enemies_affected_count, count),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                                
                                // Arrow
                                Text(
                                    text = "↓",
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                                
                                // To position selector
                                Column {
                                    Text(
                                        text = stringResource(Res.string.to_spawn_point),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                    ) {
                                        mapSpawnPoints.forEach { toPos ->
                                            FilterChip(
                                                modifier = Modifier.height(32.dp),
                                                selected = remappings[fromPos] == toPos,
                                                onClick = { remappings[fromPos] = toPos },
                                                label = {
                                                    Text(
                                                        text = "(${toPos.x},${toPos.y})",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = if (remappings[fromPos] == toPos) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                leadingIcon = if (remappings[fromPos] == toPos) {
                                                    { CheckmarkIcon(size = 14.dp) }
                                                } else null
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApply(remappings.toMap())
                },
                enabled = mapSpawnPoints.isNotEmpty()
            ) {
                SelectableText(stringResource(Res.string.apply_changes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}
