package de.egril.defender.ui.editor.level.initialsetup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
    selectedBridgeType: BridgeType,
    onSelectedBridgeTypeChange: (BridgeType) -> Unit,
    bridgeHealthPoints: Int,
    onBridgeHealthPointsChange: (Int) -> Unit,
    bridgeIsIndestructible: Boolean,
    onBridgeIsIndestructibleChange: (Boolean) -> Unit,
    selectedFiefType: FiefType,
    onSelectedFiefTypeChange: (FiefType) -> Unit,
    selectedGargoyleLevel: Int,
    onSelectedGargoyleLevelChange: (Int) -> Unit,
    availableTowers: Set<DefenderType>,
    initialData: InitialData,
    onRemoveDefender: (Int) -> Unit,
    onRemoveAttacker: (Int) -> Unit,
    onRemoveTrap: (Int) -> Unit,
    onRemoveBarricade: (Int) -> Unit,
    onRemoveBridge: (Int) -> Unit,
    onRemoveFief: (Int) -> Unit,
    onRemoveMushroom: (Int) -> Unit,
    onRemoveGargoyle: (Int) -> Unit,
    onRemovePortal: (Int) -> Unit,
    selectedElement: SelectedElement?,
    onSelectedElementChange: (SelectedElement?) -> Unit,
) {
    Card(
        modifier =
            Modifier
                .width(400.dp)
                .fillMaxHeight(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Header
            item {
                Text(
                    text = stringResource(Res.string.initial_setup_configuration),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            // Info card
            item {
                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        de.egril.defender.ui.icon
                            .InfoIcon(size = 16.dp)
                        Text(
                            text = stringResource(Res.string.initial_setup_info),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            // Element type buttons
            item {
                Text(
                    text = stringResource(Res.string.element_type),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { onPlacementModeChange(PlacementMode.DEFENDER) },
                        modifier = Modifier.weight(1f),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    if (placementMode == PlacementMode.DEFENDER) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.secondary
                                    },
                            ),
                    ) {
                        Text(stringResource(Res.string.towers))
                    }
                    Button(
                        onClick = { onPlacementModeChange(PlacementMode.ATTACKER) },
                        modifier = Modifier.weight(1f),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    if (placementMode == PlacementMode.ATTACKER) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.secondary
                                    },
                            ),
                    ) {
                        Text(stringResource(Res.string.enemies))
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { onPlacementModeChange(PlacementMode.TRAP) },
                        modifier = Modifier.weight(1f),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    if (placementMode == PlacementMode.TRAP) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.secondary
                                    },
                            ),
                    ) {
                        Text(stringResource(Res.string.traps))
                    }
                    Button(
                        onClick = { onPlacementModeChange(PlacementMode.BARRICADE) },
                        modifier = Modifier.weight(1f),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    if (placementMode == PlacementMode.BARRICADE) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.secondary
                                    },
                            ),
                    ) {
                        Text(stringResource(Res.string.barricades))
                    }
                    Button(
                        onClick = { onPlacementModeChange(PlacementMode.BRIDGE) },
                        modifier = Modifier.weight(1f),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    if (placementMode == PlacementMode.BRIDGE) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.secondary
                                    },
                            ),
                    ) {
                        Text(stringResource(Res.string.bridges))
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { onPlacementModeChange(PlacementMode.FIEF) },
                        modifier = Modifier.weight(1f),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    if (placementMode == PlacementMode.FIEF) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.secondary
                                    },
                            ),
                    ) {
                        Text(stringResource(Res.string.initial_setup_fiefs))
                    }
                    Button(
                        onClick = { onPlacementModeChange(PlacementMode.MUSHROOM) },
                        modifier = Modifier.weight(1f),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    if (placementMode == PlacementMode.MUSHROOM) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.secondary
                                    },
                            ),
                    ) {
                        Text(stringResource(Res.string.initial_setup_mushrooms))
                    }
                    Button(
                        onClick = { onPlacementModeChange(PlacementMode.PORTAL) },
                        modifier = Modifier.weight(1f),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    if (placementMode == PlacementMode.PORTAL) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.secondary
                                    },
                            ),
                    ) {
                        Text(stringResource(Res.string.initial_setup_portals))
                    }
                }
            }

            // Neutral units: creatures that belong to no side and are not controlled by the player
            item {
                Text(
                    text = stringResource(Res.string.initial_setup_neutral_units),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { onPlacementModeChange(PlacementMode.GARGOYLE) },
                        modifier = Modifier.weight(1f),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    if (placementMode == PlacementMode.GARGOYLE) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.secondary
                                    },
                            ),
                    ) {
                        Text(stringResource(Res.string.gargoyle_name))
                    }
                }
            }

            // Clear mode button
            item {
                Button(
                    onClick = { onPlacementModeChange(null) },
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                if (placementMode == null) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                        ),
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
                            availableTowers = availableTowers,
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
                            onDragonNameChange = onAttackerDragonNameChange,
                        )
                    }
                }
                PlacementMode.TRAP -> {
                    item {
                        TrapConfigPanel(
                            selectedType = selectedTrapType,
                            onTypeChange = onSelectedTrapTypeChange,
                            damage = trapDamage,
                            onDamageChange = onTrapDamageChange,
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
                            onIsGateChange = onBarricadeIsGateChange,
                        )
                    }
                }
                PlacementMode.BRIDGE -> {
                    item {
                        BridgeConfigPanel(
                            selectedType = selectedBridgeType,
                            onTypeChange = onSelectedBridgeTypeChange,
                            healthPoints = bridgeHealthPoints,
                            onHealthPointsChange = onBridgeHealthPointsChange,
                            isIndestructible = bridgeIsIndestructible,
                            onIsIndestructibleChange = onBridgeIsIndestructibleChange,
                        )
                    }
                }
                PlacementMode.FIEF -> {
                    item {
                        FiefConfigPanel(
                            selectedType = selectedFiefType,
                            onTypeChange = onSelectedFiefTypeChange,
                        )
                    }
                }
                PlacementMode.MUSHROOM -> {
                    // No configuration needed for mushrooms - they are simply placed on path tiles
                }
                PlacementMode.GARGOYLE -> {
                    item {
                        GargoyleConfigPanel(
                            level = selectedGargoyleLevel,
                            onLevelChange = onSelectedGargoyleLevelChange,
                        )
                    }
                }
                PlacementMode.PORTAL -> {
                    item {
                        Card(
                            colors =
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                ),
                        ) {
                            Text(
                                text = stringResource(Res.string.initial_setup_portal_hint),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
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
                                        is SelectedElement.Bridge -> onRemoveBridge(selectedElement.index)
                                        is SelectedElement.Fief -> onRemoveFief(selectedElement.index)
                                        is SelectedElement.Mushroom -> onRemoveMushroom(selectedElement.index)
                                        is SelectedElement.Gargoyle -> onRemoveGargoyle(selectedElement.index)
                                        is SelectedElement.Portal -> onRemovePortal(selectedElement.index)
                                    }
                                },
                                onDeselect = { onSelectedElementChange(null) },
                            )
                        }
                    } else {
                        item {
                            Card(
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    ),
                            ) {
                                Text(
                                    text = stringResource(Res.string.selection_mode_info),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp),
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
                    style = MaterialTheme.typography.titleSmall,
                )
            }

            item {
                PlacedElementsSummary(
                    defenders = initialData.defenders,
                    attackers = initialData.attackers,
                    traps = initialData.traps,
                    barricades = initialData.barricades,
                    bridges = initialData.bridges,
                    fiefs = initialData.fiefs,
                    portals = initialData.portals,
                    gargoyles = initialData.gargoyles,
                )
            }
        }
    }
}
