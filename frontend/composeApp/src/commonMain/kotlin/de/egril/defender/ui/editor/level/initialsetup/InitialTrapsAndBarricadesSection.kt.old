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
import de.egril.defender.model.Position
import de.egril.defender.model.TrapType
import de.egril.defender.ui.icon.TrapIcon
import de.egril.defender.ui.icon.PentagramIcon
import de.egril.defender.ui.icon.WoodIcon
import defender_of_egril.composeapp.generated.resources.*

/**
 * Section for managing initial traps
 */
@Composable
fun InitialTrapsSection(
    initialTraps: List<InitialTrap>,
    onInitialTrapsChange: (List<InitialTrap>) -> Unit,
    map: EditorMap?
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTrap by remember { mutableStateOf<Pair<Int, InitialTrap>?>(null) }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(Res.string.add_initial_trap))
        }
        
        Text(
            text = "${stringResource(Res.string.initial_traps)}: ${initialTraps.size}",
            style = MaterialTheme.typography.bodyLarge
        )
        
        if (initialTraps.isEmpty()) {
            Text(
                text = stringResource(Res.string.no_initial_traps),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(initialTraps) { index, trap ->
                    InitialTrapCard(
                        trap = trap,
                        onEdit = { editingTrap = Pair(index, trap) },
                        onDelete = {
                            val newList = initialTraps.toMutableList()
                            newList.removeAt(index)
                            onInitialTrapsChange(newList)
                        }
                    )
                }
            }
        }
    }
    
    if (showAddDialog || editingTrap != null) {
        AddEditTrapDialog(
            existingTrap = editingTrap?.second,
            map = map,
            onDismiss = {
                showAddDialog = false
                editingTrap = null
            },
            onConfirm = { trap ->
                if (editingTrap != null) {
                    val newList = initialTraps.toMutableList()
                    newList[editingTrap!!.first] = trap
                    onInitialTrapsChange(newList)
                } else {
                    onInitialTrapsChange(initialTraps + trap)
                }
                showAddDialog = false
                editingTrap = null
            }
        )
    }
}

@Composable
fun InitialTrapCard(
    trap: InitialTrap,
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
            // Trap icon
            if (trap.type == "MAGICAL") {
                PentagramIcon(size = 40.dp)
            } else {
                TrapIcon(size = 40.dp)
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${if (trap.type == "MAGICAL") stringResource(Res.string.magical_trap) else stringResource(Res.string.dwarven_trap)}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "${stringResource(Res.string.damage_label)}: ${trap.damage} | ${stringResource(Res.string.position_label)}: (${trap.position.x}, ${trap.position.y})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
fun AddEditTrapDialog(
    existingTrap: InitialTrap?,
    map: EditorMap?,
    onDismiss: () -> Unit,
    onConfirm: (InitialTrap) -> Unit
) {
    var trapType by remember { mutableStateOf(existingTrap?.type ?: "DWARVEN") }
    var damage by remember { mutableStateOf(existingTrap?.damage?.toString() ?: "10") }
    var selectedPosition by remember { mutableStateOf(existingTrap?.position ?: Position(0, 0)) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                if (existingTrap != null) 
                    stringResource(Res.string.edit_initial_trap) 
                else 
                    stringResource(Res.string.add_initial_trap)
            ) 
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Trap type selection
                Text(
                    text = stringResource(Res.string.trap_type),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RadioButton(
                        selected = trapType == "DWARVEN",
                        onClick = { trapType = "DWARVEN" }
                    )
                    TrapIcon(size = 24.dp)
                    Text(stringResource(Res.string.dwarven_trap))
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RadioButton(
                        selected = trapType == "MAGICAL",
                        onClick = { trapType = "MAGICAL" }
                    )
                    PentagramIcon(size = 24.dp)
                    Text(stringResource(Res.string.magical_trap))
                }
                
                OutlinedTextField(
                    value = damage,
                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 4) damage = it },
                    label = { Text(stringResource(Res.string.damage_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Position selection with map
                if (map != null) {
                    Text(
                        text = stringResource(Res.string.select_position_on_map),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = "${stringResource(Res.string.position_label)}: (${selectedPosition.x}, ${selectedPosition.y})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        InitialSetupMinimap(
                            map = map,
                            placementMode = PlacementMode.TRAP,
                            selectedPosition = selectedPosition,
                            onTileClick = { selectedPosition = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val damageValue = damage.toIntOrNull() ?: 10
                    
                    onConfirm(
                        InitialTrap(
                            position = selectedPosition,
                            damage = damageValue,
                            type = trapType
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

/**
 * Section for managing initial barricades
 */
@Composable
fun InitialBarricadesSection(
    initialBarricades: List<InitialBarricade>,
    onInitialBarricadesChange: (List<InitialBarricade>) -> Unit,
    map: EditorMap?
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingBarricade by remember { mutableStateOf<Pair<Int, InitialBarricade>?>(null) }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(Res.string.add_initial_barricade))
        }
        
        Text(
            text = "${stringResource(Res.string.initial_barricades)}: ${initialBarricades.size}",
            style = MaterialTheme.typography.bodyLarge
        )
        
        if (initialBarricades.isEmpty()) {
            Text(
                text = stringResource(Res.string.no_initial_barricades),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(initialBarricades) { index, barricade ->
                    InitialBarricadeCard(
                        barricade = barricade,
                        onEdit = { editingBarricade = Pair(index, barricade) },
                        onDelete = {
                            val newList = initialBarricades.toMutableList()
                            newList.removeAt(index)
                            onInitialBarricadesChange(newList)
                        }
                    )
                }
            }
        }
    }
    
    if (showAddDialog || editingBarricade != null) {
        AddEditBarricadeDialog(
            existingBarricade = editingBarricade?.second,
            map = map,
            onDismiss = {
                showAddDialog = false
                editingBarricade = null
            },
            onConfirm = { barricade ->
                if (editingBarricade != null) {
                    val newList = initialBarricades.toMutableList()
                    newList[editingBarricade!!.first] = barricade
                    onInitialBarricadesChange(newList)
                } else {
                    onInitialBarricadesChange(initialBarricades + barricade)
                }
                showAddDialog = false
                editingBarricade = null
            }
        )
    }
}

@Composable
fun InitialBarricadeCard(
    barricade: InitialBarricade,
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
            // Barricade icon
            WoodIcon(size = 40.dp)
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.barricade),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "${stringResource(Res.string.health)}: ${barricade.healthPoints} | ${stringResource(Res.string.position_label)}: (${barricade.position.x}, ${barricade.position.y})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
fun AddEditBarricadeDialog(
    existingBarricade: InitialBarricade?,
    map: EditorMap?,
    onDismiss: () -> Unit,
    onConfirm: (InitialBarricade) -> Unit
) {
    var healthPoints by remember { mutableStateOf(existingBarricade?.healthPoints?.toString() ?: "10") }
    var selectedPosition by remember { mutableStateOf(existingBarricade?.position ?: Position(0, 0)) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                if (existingBarricade != null) 
                    stringResource(Res.string.edit_initial_barricade) 
                else 
                    stringResource(Res.string.add_initial_barricade)
            ) 
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = healthPoints,
                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 4) healthPoints = it },
                    label = { Text(stringResource(Res.string.health_points)) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Position selection with map
                if (map != null) {
                    Text(
                        text = stringResource(Res.string.select_position_on_map),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = "${stringResource(Res.string.position_label)}: (${selectedPosition.x}, ${selectedPosition.y})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        InitialSetupMinimap(
                            map = map,
                            placementMode = PlacementMode.BARRICADE,
                            selectedPosition = selectedPosition,
                            onTileClick = { selectedPosition = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val hpValue = healthPoints.toIntOrNull() ?: 10
                    
                    onConfirm(
                        InitialBarricade(
                            position = selectedPosition,
                            healthPoints = hpValue
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
