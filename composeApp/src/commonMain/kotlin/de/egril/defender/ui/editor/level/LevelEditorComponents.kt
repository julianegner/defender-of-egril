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
import com.hyperether.resources.stringResource
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
        title = { Text(stringResource(Res.string.add_enemy_to_turn).replace("%d", turn.toString())) },
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
                
                Text(stringResource(Res.string.hp_with_level).replace("%d", (selectedType.health * (level.toIntOrNull() ?: 1)).toString()), fontSize = 12.sp)
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
                Text(stringResource(Res.string.add))
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
 * Dialog for selecting a spawn point with minimap visualization
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectSpawnPointDialog(
    selectedType: AttackerType,
    level: Int,
    map: EditorMap?,
    currentSelection: Position?,
    onDismiss: () -> Unit,
    onSelect: (Position) -> Unit
) {
    val spawnPoints = remember(map) { map?.getSpawnPoints() ?: emptyList() }
    var selectedSpawnPoint by remember { mutableStateOf(currentSelection ?: spawnPoints.firstOrNull()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(Res.string.select_spawn_point))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Enemy info preview
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(32.dp)) {
                        EnemyIconOnHexagon(
                            attackerType = selectedType,
                            size = 32.dp
                        )
                    }
                    Column {
                        Text(
                            text = "${selectedType.getLocalizedName()} Lv$level",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${stringResource(Res.string.hp_short)}: ${selectedType.health * level}",
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
                                    { Text("✓", fontSize = 14.sp) }
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
                    selectedSpawnPoint?.let { onSelect(it) }
                },
                enabled = selectedSpawnPoint != null
            ) {
                Text(stringResource(Res.string.select))
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
    onChangeSpawnPoint: (EditorEnemySpawn) -> Unit
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
                    Text(
                        text = "Turn $turn",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
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
                    Button(
                        onClick = onCopyTurn,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
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
                            Text(
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
                        Text(stringResource(Res.string.add_enemy_button).replace("%d", turn.toString()))
                    }
                    
                    if (spawns.isEmpty()) {
                        // Show message for empty turn
                        Text(
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
                                onChangeSpawnPoint = onChangeSpawnPoint
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
    onChangeSpawnPoint: (EditorEnemySpawn) -> Unit
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
            Text(
                text = "${spawn.attackerType.displayName} Lv${spawn.level}",
                fontSize = 14.sp
            )
            Text(
                text = "${stringResource(Res.string.hp_short)}: ${spawn.healthPoints}",
                fontSize = 11.sp,
                color = Color.Gray
            )
            
            // Display spawn point - always show it
            val spawnPointText = spawn.spawnPoint?.let { spawnPoint ->
                "${stringResource(Res.string.spawn_point)}: (${spawnPoint.x}, ${spawnPoint.y})"
            } ?: stringResource(Res.string.no_spawn_point_set)
            Text(
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
                Text("📍", fontSize = 10.sp)
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
                Text(stringResource(Res.string.remove), fontSize = 11.sp)
            }
        }
    }
}

/**
 * Dialog for changing the spawn point of an enemy with a minimap
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChangeSpawnPointDialog(
    spawn: EditorEnemySpawn,
    map: EditorMap?,
    onDismiss: () -> Unit,
    onChange: (Position) -> Unit
) {
    val spawnPoints = remember(map) { map?.getSpawnPoints() ?: emptyList() }
    var selectedSpawnPoint by remember { mutableStateOf(spawn.spawnPoint ?: spawnPoints.firstOrNull()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(Res.string.change_spawn_point))
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
                            attackerType = spawn.attackerType,
                            size = 32.dp
                        )
                    }
                    Column {
                        Text(
                            text = "${spawn.attackerType.displayName} Lv${spawn.level}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${stringResource(Res.string.hp_short)}: ${spawn.healthPoints}",
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
                
                // Spawn point selection
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
                                    { Text("✓", fontSize = 14.sp) }
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
                    selectedSpawnPoint?.let { onChange(it) }
                },
                enabled = selectedSpawnPoint != null
            ) {
                Text(stringResource(Res.string.change))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}
