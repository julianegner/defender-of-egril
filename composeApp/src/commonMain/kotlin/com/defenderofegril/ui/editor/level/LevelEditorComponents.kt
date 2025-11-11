package com.defenderofegril.ui.editor.level

import androidx.compose.foundation.background
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
import com.defenderofegril.editor.EditorEnemySpawn
import com.defenderofegril.model.AttackerType
import com.defenderofegril.ui.*
import com.defenderofegril.ui.icon.DownArrowIcon
import com.defenderofegril.ui.icon.ReloadIcon
import com.defenderofegril.ui.icon.TrashIcon
import com.defenderofegril.ui.icon.TriangleDownIcon
import com.defenderofegril.ui.icon.TriangleRightIcon
import com.defenderofegril.ui.icon.UpArrowIcon
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*

/**
 * Dialog for adding an enemy to a specific turn
 */
@Composable
fun AddEnemyDialog(
    ewhadCount: Int = 0,
    turn: Int,
    onDismiss: () -> Unit,
    onAdd: (AttackerType, Int) -> Unit
) {
    var selectedType by remember { mutableStateOf(AttackerType.GOBLIN) }
    var level by remember { mutableStateOf("1") }
    
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
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedType == type,
                                onClick = { selectedType = type }
                            )
                            Text("${type.getLocalizedName()} (${stringResource(Res.string.hp_label)}: ${type.health})")
                        }
                    }
                }
                OutlinedTextField(
                    value = level,
                    onValueChange = { if (it.all { c -> c.isDigit() }) level = it },
                    label = { Text(stringResource(Res.string.enemy_level)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
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
                    onAdd(selectedType, level.toIntOrNull() ?: 1)
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
}

/**
 * Collapsible section showing enemies spawning in a specific turn
 */
@Composable
fun SpawnTurnSection(
    turn: Int,
    spawns: List<EditorEnemySpawn>,
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
    ewhadCount: Int
) {
    var expanded by remember { mutableStateOf(false) }
    
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
                    if (canDeleteTurn) {
                        Button(
                            onClick = onDeleteTurn,
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            TrashIcon(size = 12.dp)
                        }
                    } else {
                        Button(
                            onClick = onClearTurn,
                            enabled = spawns.isNotEmpty(),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
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
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Enemy icon
                                    Box(modifier = Modifier.size(24.dp)) {
                                        EnemyIconOnHexagon(
                                            attackerType = spawn.attackerType,
                                            size = 24.dp
                                        )
                                    }
                                    
                                    Column {
                                        Text(
                                            text = "${spawn.attackerType.displayName} Lv${spawn.level}",
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "${stringResource(Res.string.hp_short)}: ${spawn.healthPoints}",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                
                                Button(
                                    onClick = { onRemoveEnemy(spawn) },
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
                    }
                }
            }
        }
    }
}
