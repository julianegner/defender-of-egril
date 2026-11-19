package de.egril.defender.ui.editor.level.initialsetup

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.*
import de.egril.defender.model.AttackerType
import de.egril.defender.model.BridgeType
import de.egril.defender.model.DefenderType
import de.egril.defender.model.FiefType
import de.egril.defender.ui.getLocalizedName
import de.egril.defender.ui.hexagon.TowerIconOnHexagon
import de.egril.defender.ui.icon.PentagramIcon
import de.egril.defender.ui.icon.TrapIcon
import de.egril.defender.ui.icon.enemy.EnemyTypeIcon
import defender_of_egril.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GargoyleConfigPanel(
    level: Int,
    onLevelChange: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.initial_setup_gargoyle_configuration),
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = level.toString(),
            onValueChange = {
                val newLevel = it.toIntOrNull()
                if (newLevel != null && newLevel > 0 && newLevel <= 100) {
                    onLevelChange(newLevel)
                }
            },
            label = { Text(stringResource(Res.string.level_label)) },
            modifier = Modifier.fillMaxWidth(),
        )
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
    availableTowers: Set<DefenderType>,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.tower_configuration),
            style = MaterialTheme.typography.bodyMedium,
        )

        // Show all towers toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.show_all_towers),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
            )
            Switch(
                checked = showAllTowers,
                onCheckedChange = onShowAllTowersChange,
            )
        }

        // Tower type dropdown
        val towersToShow =
            if (showAllTowers) {
                DefenderType.entries.filter { it != DefenderType.DRAGONS_LAIR && it != DefenderType.ALTAR }
            } else {
                availableTowers
                    .filter {
                        it !=
                            DefenderType.DRAGONS_LAIR
                    }.toList()
            }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = selectedType.getLocalizedName(),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(Res.string.tower_type)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                towersToShow.forEach { type ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                TowerIconOnHexagon(defenderType = type, size = 24.dp)
                                Text(type.getLocalizedName())
                            }
                        },
                        onClick = {
                            onTypeChange(type)
                            expanded = false
                        },
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
            modifier = Modifier.fillMaxWidth(),
        )

        // Dragon name (only for Dragon's Lair)
        if (selectedType == DefenderType.DRAGONS_LAIR) {
            OutlinedTextField(
                value = dragonName,
                onValueChange = onDragonNameChange,
                label = { Text(stringResource(Res.string.dragon_name_label)) },
                modifier = Modifier.fillMaxWidth(),
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
    onDragonNameChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.enemy_configuration),
            style = MaterialTheme.typography.bodyMedium,
        )

        // Enemy type dropdown
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = selectedType.getLocalizedName(),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(Res.string.enemy_type)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                AttackerType.entries.filterNot { it.isMirrorImage }.forEach { type ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                        },
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
            modifier = Modifier.fillMaxWidth(),
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
            modifier = Modifier.fillMaxWidth(),
        )

        // Dragon name (only for Dragon)
        if (selectedType == AttackerType.DRAGON) {
            OutlinedTextField(
                value = dragonName,
                onValueChange = onDragonNameChange,
                label = { Text(stringResource(Res.string.dragon_name_label)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun TrapConfigPanel(
    selectedType: String,
    onTypeChange: (String) -> Unit,
    damage: Int,
    onDamageChange: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.trap_configuration),
            style = MaterialTheme.typography.bodyMedium,
        )

        // Trap type selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RadioButton(
                selected = selectedType == "DWARVEN",
                onClick = { onTypeChange("DWARVEN") },
            )
            TrapIcon(size = 24.dp)
            Text(stringResource(Res.string.dwarven_trap))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RadioButton(
                selected = selectedType == "MAGICAL",
                onClick = { onTypeChange("MAGICAL") },
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
                modifier = Modifier.fillMaxWidth(),
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
    onIsGateChange: (Boolean) -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.barricade_configuration),
            style = MaterialTheme.typography.bodyMedium,
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
            modifier = Modifier.fillMaxWidth(),
        )

        // Optional name (for gates)
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(stringResource(Res.string.barricade_name_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        // Is gate toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(Res.string.is_gate_label),
                style = MaterialTheme.typography.bodyMedium,
            )
            Switch(checked = isGate, onCheckedChange = onIsGateChange)
        }

        // Tower base hint (shown when HP >= TOWER_BASE_MIN_HP)
        if (healthPoints >= InitialBarricade.TOWER_BASE_MIN_HP) {
            Card(
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
            ) {
                Text(
                    text = stringResource(Res.string.barricade_tower_base_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
    }
}

@Composable
fun BridgeConfigPanel(
    selectedType: BridgeType,
    onTypeChange: (BridgeType) -> Unit,
    healthPoints: Int,
    onHealthPointsChange: (Int) -> Unit,
    isIndestructible: Boolean,
    onIsIndestructibleChange: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.bridge_configuration),
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RadioButton(
                selected = selectedType == BridgeType.WOODEN,
                onClick = { onTypeChange(BridgeType.WOODEN) },
            )
            Text(stringResource(Res.string.wooden_bridge))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RadioButton(
                selected = selectedType == BridgeType.STONE,
                onClick = { onTypeChange(BridgeType.STONE) },
            )
            Text(stringResource(Res.string.stone_bridge))
        }
        OutlinedTextField(
            value = healthPoints.toString(),
            onValueChange = {
                val newHP = it.toIntOrNull()
                if (newHP != null && newHP > 0 && newHP <= 9999) {
                    onHealthPointsChange(newHP)
                }
            },
            label = { Text(stringResource(Res.string.health_points)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isIndestructible,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(Res.string.indestructible),
                style = MaterialTheme.typography.bodyMedium,
            )
            Switch(
                checked = isIndestructible,
                onCheckedChange = onIsIndestructibleChange,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiefConfigPanel(
    selectedType: FiefType,
    onTypeChange: (FiefType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.fief_configuration),
            style = MaterialTheme.typography.bodyMedium,
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = selectedType.getLocalizedName(),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(Res.string.element_type)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                FiefType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.getLocalizedName()) },
                        onClick = {
                            onTypeChange(type)
                            expanded = false
                        },
                    )
                }
            }
        }

        Text(
            text = "${stringResource(Res.string.stat_income)}: +${selectedType.incomePerTurn}",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
