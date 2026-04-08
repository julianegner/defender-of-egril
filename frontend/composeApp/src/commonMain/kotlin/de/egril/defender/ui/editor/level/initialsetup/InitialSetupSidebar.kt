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
import de.egril.defender.model.DefenderType
import de.egril.defender.ui.getLocalizedName
import de.egril.defender.ui.hexagon.TowerIconOnHexagon
import de.egril.defender.ui.icon.enemy.EnemyTypeIcon
import de.egril.defender.ui.icon.TrapIcon
import de.egril.defender.ui.icon.PentagramIcon
import de.egril.defender.ui.icon.WoodIcon
import defender_of_egril.composeapp.generated.resources.*

/**
 * Sidebar for configuring and placing initial setup elements
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InitialSetupSidebar(
    placementMode: PlacementMode?,
    onPlacementModeChange: (PlacementMode?) -> Unit,
    selectedDefenderType: DefenderType,
    onSelectedDefenderTypeChange: (DefenderType) -> Unit,
    selectedDefenderLevel: Int,
    onSelectedDefenderLevelChange: (Int) -> Unit,
    showAllTowers: Boolean,
    onShowAllTowersChange: (Boolean) -> Unit,
    dragonName: String,
    onDragonNameChange: (String) -> Unit,
    selectedAttackerType: AttackerType,
    onSelectedAttackerTypeChange: (AttackerType) -> Unit,
    selectedAttackerLevel: Int,
    onSelectedAttackerLevelChange: (Int) -> Unit,
    customHealth: Int?,
    onCustomHealthChange: (Int?) -> Unit,
    attackerDragonName: String,
    onAttackerDragonNameChange: (String) -> Unit,
    selectedTrapType: String,
    onSelectedTrapTypeChange: (String) -> Unit,
    trapDamage: Int,
    onTrapDamageChange: (Int) -> Unit,
    barricadeHealthPoints: Int,
    onBarricadeHealthPointsChange: (Int) -> Unit,
    barricadeName: String,
    onBarricadeNameChange: (String) -> Unit,
    barricadeIsGate: Boolean,
    onBarricadeIsGateChange: (Boolean) -> Unit,
    availableTowers: Set<DefenderType>,
    initialData: InitialData,
    onRemoveDefender: (Int) -> Unit,
    onRemoveAttacker: (Int) -> Unit,
    onRemoveTrap: (Int) -> Unit,
    onRemoveBarricade: (Int) -> Unit,
    selectedElement: SelectedElement?,
    onSelectedElementChange: (SelectedElement?) -> Unit
) {
    Card(
        modifier = Modifier
            .width(400.dp)
            .fillMaxHeight()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            item {
                Text(
                    text = stringResource(Res.string.initial_setup_configuration),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            // Info card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        de.egril.defender.ui.icon.InfoIcon(size = 16.dp)
                        Text(
                            text = stringResource(Res.string.initial_setup_info),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            // Element type buttons
            item {
                Text(
                    text = stringResource(Res.string.element_type),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onPlacementModeChange(PlacementMode.DEFENDER) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (placementMode == PlacementMode.DEFENDER)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text(stringResource(Res.string.towers))
                    }
                    Button(
                        onClick = { onPlacementModeChange(PlacementMode.ATTACKER) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (placementMode == PlacementMode.ATTACKER)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text(stringResource(Res.string.enemies))
                    }
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onPlacementModeChange(PlacementMode.TRAP) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (placementMode == PlacementMode.TRAP)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text(stringResource(Res.string.traps))
                    }
                    Button(
                        onClick = { onPlacementModeChange(PlacementMode.BARRICADE) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (placementMode == PlacementMode.BARRICADE)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text(stringResource(Res.string.barricades))
                    }
                }
            }
            
            // Clear mode button
            item {
                Button(
                    onClick = { onPlacementModeChange(null) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (placementMode == null)
                            MaterialTheme.colorScheme.tertiary
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(stringResource(Res.string.selection_mode))
                }
            }
            
            item {
                HorizontalDivider()
            }
            
            // Configuration panel based on placement mode
            when (placementMode) {
                PlacementMode.DEFENDER -> {
                    item {
                        DefenderConfigPanel(
                            selectedType = selectedDefenderType,
                            onTypeChange = onSelectedDefenderTypeChange,
                            level = selectedDefenderLevel,
                            onLevelChange = onSelectedDefenderLevelChange,
                            showAllTowers = showAllTowers,
                            onShowAllTowersChange = onShowAllTowersChange,
                            dragonName = dragonName,
                            onDragonNameChange = onDragonNameChange,
                            availableTowers = availableTowers
                        )
                    }
                }
                PlacementMode.ATTACKER -> {
                    item {
                        AttackerConfigPanel(
                            selectedType = selectedAttackerType,
                            onTypeChange = onSelectedAttackerTypeChange,
                            level = selectedAttackerLevel,
                            onLevelChange = onSelectedAttackerLevelChange,
                            customHealth = customHealth,
                            onCustomHealthChange = onCustomHealthChange,
                            dragonName = attackerDragonName,
                            onDragonNameChange = onAttackerDragonNameChange
                        )
                    }
                }
                PlacementMode.TRAP -> {
                    item {
                        TrapConfigPanel(
                            selectedType = selectedTrapType,
                            onTypeChange = onSelectedTrapTypeChange,
                            damage = trapDamage,
                            onDamageChange = onTrapDamageChange
                        )
                    }
                }
                PlacementMode.BARRICADE -> {
                    item {
                        BarricadeConfigPanel(
                            healthPoints = barricadeHealthPoints,
                            onHealthPointsChange = onBarricadeHealthPointsChange,
                            name = barricadeName,
                            onNameChange = onBarricadeNameChange,
                            isGate = barricadeIsGate,
                            onIsGateChange = onBarricadeIsGateChange
                        )
                    }
                }
                null -> {
                    // Selection mode - show selected element details
                    if (selectedElement != null) {
                        item {
                            SelectedElementPanel(
                                selectedElement = selectedElement,
                                onRemove = {
                                    when (selectedElement) {
                                        is SelectedElement.Defender -> onRemoveDefender(selectedElement.index)
                                        is SelectedElement.Attacker -> onRemoveAttacker(selectedElement.index)
                                        is SelectedElement.Trap -> onRemoveTrap(selectedElement.index)
                                        is SelectedElement.Barricade -> onRemoveBarricade(selectedElement.index)
                                    }
                                },
                                onDeselect = { onSelectedElementChange(null) }
                            )
                        }
                    } else {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    text = stringResource(Res.string.selection_mode_info),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            item {
                HorizontalDivider()
            }
            
            // Placed elements summary
            item {
                Text(
                    text = stringResource(Res.string.placed_elements),
                    style = MaterialTheme.typography.titleSmall
                )
            }
            
            item {
                PlacedElementsSummary(
                    defenders = initialData.defenders,
                    attackers = initialData.attackers,
                    traps = initialData.traps,
                    barricades = initialData.barricades
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefenderConfigPanel(
    selectedType: DefenderType,
    onTypeChange: (DefenderType) -> Unit,
    level: Int,
    onLevelChange: (Int) -> Unit,
    showAllTowers: Boolean,
    onShowAllTowersChange: (Boolean) -> Unit,
    dragonName: String,
    onDragonNameChange: (String) -> Unit,
    availableTowers: Set<DefenderType>
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(Res.string.tower_configuration),
            style = MaterialTheme.typography.bodyMedium
        )
        
        // Show all towers toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(Res.string.show_all_towers),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall
            )
            Switch(
                checked = showAllTowers,
                onCheckedChange = onShowAllTowersChange
            )
        }
        
        // Tower type dropdown
        val towersToShow = if (showAllTowers) DefenderType.entries.filter { it != DefenderType.DRAGONS_LAIR } else availableTowers.filter { it != DefenderType.DRAGONS_LAIR }.toList()
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedType.getLocalizedName(),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(Res.string.tower_type)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                towersToShow.forEach { type ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TowerIconOnHexagon(defenderType = type, size = 24.dp)
                                Text(type.getLocalizedName())
                            }
                        },
                        onClick = {
                            onTypeChange(type)
                            expanded = false
                        }
                    )
                }
            }
        }
        
        // Level input
        OutlinedTextField(
            value = level.toString(),
            onValueChange = {
                val newLevel = it.toIntOrNull()
                if (newLevel != null && newLevel > 0 && newLevel <= 100) {
                    onLevelChange(newLevel)
                }
            },
            label = { Text(stringResource(Res.string.level_label)) },
            modifier = Modifier.fillMaxWidth()
        )
        
        // Dragon name (only for Dragon's Lair)
        if (selectedType == DefenderType.DRAGONS_LAIR) {
            OutlinedTextField(
                value = dragonName,
                onValueChange = onDragonNameChange,
                label = { Text(stringResource(Res.string.dragon_name_label)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttackerConfigPanel(
    selectedType: AttackerType,
    onTypeChange: (AttackerType) -> Unit,
    level: Int,
    onLevelChange: (Int) -> Unit,
    customHealth: Int?,
    onCustomHealthChange: (Int?) -> Unit,
    dragonName: String,
    onDragonNameChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(Res.string.enemy_configuration),
            style = MaterialTheme.typography.bodyMedium
        )
        
        // Enemy type dropdown
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedType.getLocalizedName(),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(Res.string.enemy_type)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                AttackerType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(modifier = Modifier.size(24.dp)) {
                                    EnemyTypeIcon(type, modifier = Modifier.fillMaxSize())
                                }
                                Text(type.getLocalizedName())
                            }
                        },
                        onClick = {
                            onTypeChange(type)
                            expanded = false
                        }
                    )
                }
            }
        }
        
        // Level input
        OutlinedTextField(
            value = level.toString(),
            onValueChange = {
                val newLevel = it.toIntOrNull()
                if (newLevel != null && newLevel > 0 && newLevel <= 100) {
                    onLevelChange(newLevel)
                }
            },
            label = { Text(stringResource(Res.string.level_label)) },
            modifier = Modifier.fillMaxWidth()
        )
        
        // Custom health
        OutlinedTextField(
            value = customHealth?.toString() ?: "",
            onValueChange = {
                if (it.isEmpty()) {
                    onCustomHealthChange(null)
                } else {
                    val newHealth = it.toIntOrNull()
                    if (newHealth != null && newHealth > 0) {
                        onCustomHealthChange(newHealth)
                    }
                }
            },
            label = { Text(stringResource(Res.string.custom_health_optional)) },
            modifier = Modifier.fillMaxWidth()
        )
        
        // Dragon name (only for Dragon)
        if (selectedType == AttackerType.DRAGON) {
            OutlinedTextField(
                value = dragonName,
                onValueChange = onDragonNameChange,
                label = { Text(stringResource(Res.string.dragon_name_label)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun TrapConfigPanel(
    selectedType: String,
    onTypeChange: (String) -> Unit,
    damage: Int,
    onDamageChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(Res.string.trap_configuration),
            style = MaterialTheme.typography.bodyMedium
        )
        
        // Trap type selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RadioButton(
                selected = selectedType == "DWARVEN",
                onClick = { onTypeChange("DWARVEN") }
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
                selected = selectedType == "MAGICAL",
                onClick = { onTypeChange("MAGICAL") }
            )
            PentagramIcon(size = 24.dp)
            Text(stringResource(Res.string.magical_trap))
        }
        
        // Damage input - only show for Dwarven traps (magical traps do no damage)
        if (selectedType == "DWARVEN") {
            OutlinedTextField(
                value = damage.toString(),
                onValueChange = {
                    val newDamage = it.toIntOrNull()
                    if (newDamage != null && newDamage > 0 && newDamage <= 9999) {
                        onDamageChange(newDamage)
                    }
                },
                label = { Text(stringResource(Res.string.damage_label)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun BarricadeConfigPanel(
    healthPoints: Int,
    onHealthPointsChange: (Int) -> Unit,
    name: String = "",
    onNameChange: (String) -> Unit = {},
    isGate: Boolean = false,
    onIsGateChange: (Boolean) -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(Res.string.barricade_configuration),
            style = MaterialTheme.typography.bodyMedium
        )
        
        // Health points input
        OutlinedTextField(
            value = healthPoints.toString(),
            onValueChange = {
                val newHP = it.toIntOrNull()
                if (newHP != null && newHP > 0 && newHP <= 9999) {
                    onHealthPointsChange(newHP)
                }
            },
            label = { Text(stringResource(Res.string.health_points)) },
            modifier = Modifier.fillMaxWidth()
        )

        // Optional name (for gates)
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(stringResource(Res.string.barricade_name_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Is gate toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(Res.string.is_gate_label),
                style = MaterialTheme.typography.bodyMedium
            )
            Switch(checked = isGate, onCheckedChange = onIsGateChange)
        }

        // Tower base hint (shown when HP >= TOWER_BASE_MIN_HP)
        if (healthPoints >= InitialBarricade.TOWER_BASE_MIN_HP) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    text = stringResource(Res.string.barricade_tower_base_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun SelectedElementPanel(
    selectedElement: SelectedElement,
    onRemove: () -> Unit,
    onDeselect: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(Res.string.selected_element),
                style = MaterialTheme.typography.bodyMedium
            )
            
            when (selectedElement) {
                is SelectedElement.Defender -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TowerIconOnHexagon(selectedElement.defender.type, size = 32.dp)
                        Column {
                            Text(
                                text = selectedElement.defender.type.getLocalizedName(),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "${stringResource(Res.string.level_label)}: ${selectedElement.defender.level}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "${stringResource(Res.string.position_label)}: (${selectedElement.defender.position.x}, ${selectedElement.defender.position.y})",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                is SelectedElement.Attacker -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.size(32.dp)) {
                            EnemyTypeIcon(selectedElement.attacker.type, modifier = Modifier.fillMaxSize())
                        }
                        Column {
                            Text(
                                text = selectedElement.attacker.type.getLocalizedName(),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "${stringResource(Res.string.level_label)}: ${selectedElement.attacker.level}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "${stringResource(Res.string.position_label)}: (${selectedElement.attacker.position.x}, ${selectedElement.attacker.position.y})",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                is SelectedElement.Trap -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (selectedElement.trap.type == "MAGICAL") {
                            PentagramIcon(size = 32.dp)
                        } else {
                            TrapIcon(size = 32.dp)
                        }
                        Column {
                            Text(
                                text = if (selectedElement.trap.type == "MAGICAL") stringResource(Res.string.magical_trap) else stringResource(Res.string.dwarven_trap),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            // Only show damage for Dwarven traps (magical traps do no damage)
                            if (selectedElement.trap.type == "DWARVEN") {
                                Text(
                                    text = "${stringResource(Res.string.damage_label)}: ${selectedElement.trap.damage}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                text = "${stringResource(Res.string.position_label)}: (${selectedElement.trap.position.x}, ${selectedElement.trap.position.y})",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                is SelectedElement.Barricade -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WoodIcon(size = 32.dp)
                        Column {
                            Text(
                                text = stringResource(Res.string.barricade),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "${stringResource(Res.string.health_points)}: ${selectedElement.barricade.healthPoints}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "${stringResource(Res.string.position_label)}: (${selectedElement.barricade.position.x}, ${selectedElement.barricade.position.y})",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRemove,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(Res.string.remove))
                }
                Button(
                    onClick = onDeselect,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(Res.string.deselect))
                }
            }
        }
    }
}

@Composable
fun PlacedElementsSummary(
    defenders: List<InitialDefender>,
    attackers: List<InitialAttacker>,
    traps: List<InitialTrap>,
    barricades: List<InitialBarricade>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "${stringResource(Res.string.towers)}: ${defenders.size}",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "${stringResource(Res.string.enemies)}: ${attackers.size}",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "${stringResource(Res.string.traps)}: ${traps.size}",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "${stringResource(Res.string.barricades)}: ${barricades.size}",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
