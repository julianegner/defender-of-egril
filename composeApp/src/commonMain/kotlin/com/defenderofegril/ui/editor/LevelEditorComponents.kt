package com.defenderofegril.ui.editor

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
import com.defenderofegril.ui.*

/**
 * Dialog for adding an enemy to a specific turn
 */
@Composable
fun AddEnemyDialog(
    ewhadCount: Int = 0,
    turn: Int,
    onDismiss: () -> Unit,
    onAdd: (com.defenderofegril.model.AttackerType, Int) -> Unit
) {
    var selectedType by remember { mutableStateOf(com.defenderofegril.model.AttackerType.GOBLIN) }
    var level by remember { mutableStateOf("1") }
    
    // Check if trying to add Ewhad when one already exists
    val canAddEwhad = selectedType != com.defenderofegril.model.AttackerType.EWHAD || ewhadCount == 0
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Enemy to Turn $turn") },
        text = {
            Column {
                Text("Enemy Type:", modifier = Modifier.padding(bottom = 4.dp))
                LazyColumn(
                    modifier = Modifier.height(150.dp).padding(bottom = 8.dp)
                ) {
                    items(com.defenderofegril.model.AttackerType.entries) { type ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { selectedType = type }.padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedType == type,
                                onClick = { selectedType = type }
                            )
                            Text("${type.displayName} (HP: ${type.health})")
                        }
                    }
                }
                OutlinedTextField(
                    value = level,
                    onValueChange = { if (it.all { c -> c.isDigit() }) level = it },
                    label = { Text("Enemy Level") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                Text("HP: ${selectedType.health * (level.toIntOrNull() ?: 1)}", fontSize = 12.sp)
                if (!canAddEwhad) {
                    Text(
                        text = "⚠️ Ewhad can only be spawned once per level!",
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
                Text("Add")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
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
    spawns: List<com.defenderofegril.editor.EditorEnemySpawn>,
    onRemoveEnemy: (com.defenderofegril.editor.EditorEnemySpawn) -> Unit,
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
                    Text(
                        text = if (expanded) "▼" else "▶",
                        fontSize = 16.sp
                    )
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
                        Text("➕ Add Enemy to Turn $turn")
                    }
                    
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
                                        text = "HP: ${spawn.healthPoints}",
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
                                    Text("Remove", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
