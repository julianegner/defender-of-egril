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
import de.egril.defender.model.DefenderType
import de.egril.defender.model.Position
import de.egril.defender.ui.getLocalizedName
import de.egril.defender.ui.hexagon.TowerIconOnHexagon
import defender_of_egril.composeapp.generated.resources.*

/**
 * Section for managing initial defenders (towers)
 */
@Composable
fun InitialDefendersSection(
    initialDefenders: List<InitialDefender>,
    onInitialDefendersChange: (List<InitialDefender>) -> Unit,
    map: EditorMap?,
    availableTowers: Set<DefenderType>
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingDefender by remember { mutableStateOf<Pair<Int, InitialDefender>?>(null) }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Add button
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(Res.string.add_initial_defender))
        }
        
        // List of initial defenders
        Text(
            text = "${stringResource(Res.string.initial_defenders)}: ${initialDefenders.size}",
            style = MaterialTheme.typography.bodyLarge
        )
        
        if (initialDefenders.isEmpty()) {
            Text(
                text = stringResource(Res.string.no_initial_defenders),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(initialDefenders) { index, defender ->
                    InitialDefenderCard(
                        defender = defender,
                        onEdit = { editingDefender = Pair(index, defender) },
                        onDelete = {
                            val newList = initialDefenders.toMutableList()
                            newList.removeAt(index)
                            onInitialDefendersChange(newList)
                        }
                    )
                }
            }
        }
    }
    
    // Add/Edit dialog
    if (showAddDialog || editingDefender != null) {
        AddEditDefenderDialog(
            existingDefender = editingDefender?.second,
            map = map,
            availableTowers = availableTowers,
            onDismiss = {
                showAddDialog = false
                editingDefender = null
            },
            onConfirm = { defender ->
                if (editingDefender != null) {
                    // Update existing
                    val newList = initialDefenders.toMutableList()
                    newList[editingDefender!!.first] = defender
                    onInitialDefendersChange(newList)
                } else {
                    // Add new
                    onInitialDefendersChange(initialDefenders + defender)
                }
                showAddDialog = false
                editingDefender = null
            }
        )
    }
}

/**
 * Card displaying a single initial defender
 */
@Composable
fun InitialDefenderCard(
    defender: InitialDefender,
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
            TowerIconOnHexagon(
                defenderType = defender.type,
                size = 40.dp
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = defender.type.getLocalizedName(),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "${stringResource(Res.string.level_label)}: ${defender.level} | ${stringResource(Res.string.position_label)}: (${defender.position.x}, ${defender.position.y})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (defender.dragonName != null) {
                    Text(
                        text = "${stringResource(Res.string.dragon_name_label)}: ${defender.dragonName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Button(
                onClick = onEdit,
                modifier = Modifier.width(80.dp)
            ) {
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

/**
 * Dialog for adding or editing an initial defender
 */
@Composable
fun AddEditDefenderDialog(
    existingDefender: InitialDefender?,
    map: EditorMap?,
    availableTowers: Set<DefenderType>,
    onDismiss: () -> Unit,
    onConfirm: (InitialDefender) -> Unit
) {
    var selectedType by remember { mutableStateOf(existingDefender?.type ?: DefenderType.SPIKE_TOWER) }
    var level by remember { mutableStateOf(existingDefender?.level?.toString() ?: "1") }
    var x by remember { mutableStateOf(existingDefender?.position?.x?.toString() ?: "0") }
    var y by remember { mutableStateOf(existingDefender?.position?.y?.toString() ?: "0") }
    var dragonName by remember { mutableStateOf(existingDefender?.dragonName ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                if (existingDefender != null) 
                    stringResource(Res.string.edit_initial_defender) 
                else 
                    stringResource(Res.string.add_initial_defender)
            ) 
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tower type selection
                item {
                    Text(
                        text = stringResource(Res.string.tower_type),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                // Show only available towers
                val towerTypes = DefenderType.entries.filter { 
                    it != DefenderType.DRAGONS_LAIR && availableTowers.contains(it) 
                }
                
                items(towerTypes.size) { index ->
                    val type = towerTypes[index]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = selectedType == type,
                            onClick = { selectedType = type }
                        )
                        TowerIconOnHexagon(defenderType = type, size = 32.dp)
                        Text(type.getLocalizedName())
                    }
                }
                
                // Level input
                item {
                    OutlinedTextField(
                        value = level,
                        onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 3) level = it },
                        label = { Text(stringResource(Res.string.level_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Position inputs
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = x,
                            onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 3) x = it },
                            label = { Text("X") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = y,
                            onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 3) y = it },
                            label = { Text("Y") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // Dragon name input (only for Dragon's Lair)
                if (selectedType == DefenderType.DRAGONS_LAIR) {
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
                    val xValue = x.toIntOrNull() ?: 0
                    val yValue = y.toIntOrNull() ?: 0
                    
                    onConfirm(
                        InitialDefender(
                            type = selectedType,
                            position = Position(xValue, yValue),
                            level = levelValue,
                            dragonName = if (selectedType == DefenderType.DRAGONS_LAIR && dragonName.isNotBlank()) dragonName else null
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
