package de.egril.defender.ui.editor.level.initialsetup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.*
import de.egril.defender.model.AttackerType
import de.egril.defender.model.Position
import de.egril.defender.ui.getLocalizedName
import de.egril.defender.ui.icon.enemy.EnemyTypeIcon
import defender_of_egril.composeapp.generated.resources.*

/**
 * Section for managing initial attackers (enemies)
 */
@Composable
fun InitialAttackersSection(
    initialAttackers: List<InitialAttacker>,
    onInitialAttackersChange: (List<InitialAttacker>) -> Unit,
    map: EditorMap?
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAttacker by remember { mutableStateOf<Pair<Int, InitialAttacker>?>(null) }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(Res.string.add_initial_attacker))
        }
        
        Text(
            text = "${stringResource(Res.string.initial_attackers)}: ${initialAttackers.size}",
            style = MaterialTheme.typography.bodyLarge
        )
        
        if (initialAttackers.isEmpty()) {
            Text(
                text = stringResource(Res.string.no_initial_attackers),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(initialAttackers) { index, attacker ->
                    InitialAttackerCard(
                        attacker = attacker,
                        onEdit = { editingAttacker = Pair(index, attacker) },
                        onDelete = {
                            val newList = initialAttackers.toMutableList()
                            newList.removeAt(index)
                            onInitialAttackersChange(newList)
                        }
                    )
                }
            }
        }
    }
    
    if (showAddDialog || editingAttacker != null) {
        AddEditAttackerDialog(
            existingAttacker = editingAttacker?.second,
            map = map,
            onDismiss = {
                showAddDialog = false
                editingAttacker = null
            },
            onConfirm = { attacker ->
                if (editingAttacker != null) {
                    val newList = initialAttackers.toMutableList()
                    newList[editingAttacker!!.first] = attacker
                    onInitialAttackersChange(newList)
                } else {
                    onInitialAttackersChange(initialAttackers + attacker)
                }
                showAddDialog = false
                editingAttacker = null
            }
        )
    }
}

@Composable
fun InitialAttackerCard(
    attacker: InitialAttacker,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EnemyTypeIcon(attacker.type, modifier = Modifier.size(40.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attacker.type.getLocalizedName(),
                    style = MaterialTheme.typography.bodyLarge
                )
                val health = attacker.currentHealth ?: (attacker.type.health * attacker.level)
                Text(
                    text = "${stringResource(Res.string.level_label)}: ${attacker.level} | ${stringResource(Res.string.health)}: $health | ${stringResource(Res.string.position_label)}: (${attacker.position.x}, ${attacker.position.y})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (attacker.dragonName != null) {
                    Text(
                        text = "${stringResource(Res.string.dragon_name_label)}: ${attacker.dragonName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Button(onClick = onEdit, modifier = Modifier.width(80.dp)) {
                Text(stringResource(Res.string.edit))
            }
            
            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.width(80.dp)
            ) {
                Text(stringResource(Res.string.delete))
            }
        }
    }
}

@Composable
fun AddEditAttackerDialog(
    existingAttacker: InitialAttacker?,
    map: EditorMap?,
    onDismiss: () -> Unit,
    onConfirm: (InitialAttacker) -> Unit
) {
    var selectedType by remember { mutableStateOf(existingAttacker?.type ?: AttackerType.GOBLIN) }
    var level by remember { mutableStateOf(existingAttacker?.level?.toString() ?: "1") }
    var selectedPosition by remember { mutableStateOf(existingAttacker?.position ?: Position(0, 0)) }
    var customHealth by remember { mutableStateOf(existingAttacker?.currentHealth?.toString() ?: "") }
    var dragonName by remember { mutableStateOf(existingAttacker?.dragonName ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                if (existingAttacker != null) 
                    stringResource(Res.string.edit_initial_attacker) 
                else 
                    stringResource(Res.string.add_initial_attacker)
            ) 
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = stringResource(Res.string.enemy_type),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                items(AttackerType.entries.size) { index ->
                    val type = AttackerType.entries[index]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = selectedType == type,
                            onClick = { selectedType = type }
                        )
                        Box(modifier = Modifier.size(24.dp)) {
                            EnemyTypeIcon(type, modifier = Modifier.fillMaxSize())
                        }
                        Text(type.getLocalizedName())
                    }
                }
                
                item {
                    OutlinedTextField(
                        value = level,
                        onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 3) level = it },
                        label = { Text(stringResource(Res.string.level_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Position selection with map
                if (map != null) {
                    item {
                        Text(
                            text = stringResource(Res.string.select_position_on_map),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    item {
                        Text(
                            text = "${stringResource(Res.string.position_label)}: (${selectedPosition.x}, ${selectedPosition.y})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        ) {
                            InitialSetupMinimap(
                                map = map,
                                placementMode = PlacementMode.ATTACKER,
                                selectedPosition = selectedPosition,
                                onTileClick = { selectedPosition = it }
                            )
                        }
                    }
                }
                
                item {
                    OutlinedTextField(
                        value = customHealth,
                        onValueChange = { if (it.isEmpty() || (it.all { c -> c.isDigit() } && it.length <= 5)) customHealth = it },
                        label = { Text(stringResource(Res.string.custom_health_optional)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                if (selectedType == AttackerType.DRAGON) {
                    item {
                        OutlinedTextField(
                            value = dragonName,
                            onValueChange = { dragonName = it },
                            label = { Text(stringResource(Res.string.dragon_name_label)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val levelValue = level.toIntOrNull() ?: 1
                    val healthValue = customHealth.toIntOrNull()
                    
                    onConfirm(
                        InitialAttacker(
                            type = selectedType,
                            position = selectedPosition,
                            level = levelValue,
                            currentHealth = healthValue,
                            dragonName = if (selectedType == AttackerType.DRAGON && dragonName.isNotBlank()) dragonName else null
                        )
                    )
                }
            ) {
                Text(stringResource(Res.string.ok))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}
