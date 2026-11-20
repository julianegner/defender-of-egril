package de.egril.defender.ui.editor.level.initialsetup

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.*
import de.egril.defender.model.BridgeType
import de.egril.defender.model.FiefType
import de.egril.defender.ui.getLocalizedName
import de.egril.defender.ui.hexagon.TowerIconOnHexagon
import de.egril.defender.ui.icon.PentagramIcon
import de.egril.defender.ui.icon.TrapIcon
import de.egril.defender.ui.icon.WoodIcon
import de.egril.defender.ui.icon.enemy.EnemyTypeIcon
import defender_of_egril.composeapp.generated.resources.*

@Composable
fun SelectedElementPanel(
    selectedElement: SelectedElement,
    onRemove: () -> Unit,
    onDeselect: () -> Unit,
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.selected_element),
                style = MaterialTheme.typography.bodyMedium,
            )

            when (selectedElement) {
                is SelectedElement.Defender -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TowerIconOnHexagon(selectedElement.defender.type, size = 32.dp)
                        Column {
                            Text(
                                text = selectedElement.defender.type.getLocalizedName(),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = "${stringResource(Res.string.level_label)}: ${selectedElement.defender.level}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                text = "${stringResource(
                                    Res.string.position_label,
                                )}: (${selectedElement.defender.position.x}, ${selectedElement.defender.position.y})",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
                is SelectedElement.Attacker -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(modifier = Modifier.size(32.dp)) {
                            EnemyTypeIcon(selectedElement.attacker.type, modifier = Modifier.fillMaxSize())
                        }
                        Column {
                            Text(
                                text = selectedElement.attacker.type.getLocalizedName(),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = "${stringResource(Res.string.level_label)}: ${selectedElement.attacker.level}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                text = "${stringResource(
                                    Res.string.position_label,
                                )}: (${selectedElement.attacker.position.x}, ${selectedElement.attacker.position.y})",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
                is SelectedElement.Trap -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (selectedElement.trap.type == "MAGICAL") {
                            PentagramIcon(size = 32.dp)
                        } else {
                            TrapIcon(size = 32.dp)
                        }
                        Column {
                            Text(
                                text =
                                    if (selectedElement.trap.type ==
                                        "MAGICAL"
                                    ) {
                                        stringResource(Res.string.magical_trap)
                                    } else {
                                        stringResource(Res.string.dwarven_trap)
                                    },
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            // Only show damage for Dwarven traps (magical traps do no damage)
                            if (selectedElement.trap.type == "DWARVEN") {
                                Text(
                                    text = "${stringResource(Res.string.damage_label)}: ${selectedElement.trap.damage}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Text(
                                text = "${stringResource(
                                    Res.string.position_label,
                                )}: (${selectedElement.trap.position.x}, ${selectedElement.trap.position.y})",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
                is SelectedElement.Barricade -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        WoodIcon(size = 32.dp)
                        Column {
                            Text(
                                text = stringResource(Res.string.barricade),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = "${stringResource(Res.string.health_points)}: ${selectedElement.barricade.healthPoints}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                text = "${stringResource(
                                    Res.string.position_label,
                                )}: (${selectedElement.barricade.position.x}, ${selectedElement.barricade.position.y})",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
                is SelectedElement.Bridge -> {
                    Column {
                        Text(
                            text =
                                when (selectedElement.bridge.type) {
                                    BridgeType.WOODEN -> stringResource(Res.string.wooden_bridge)
                                    BridgeType.STONE -> stringResource(Res.string.stone_bridge)
                                    BridgeType.MAGICAL -> stringResource(Res.string.bridges)
                                },
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        if (!selectedElement.bridge.isIndestructible) {
                            Text(
                                text = "${stringResource(Res.string.health_points)}: ${selectedElement.bridge.healthPoints}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        } else {
                            Text(
                                text = stringResource(Res.string.indestructible),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Text(
                            text = "${stringResource(
                                Res.string.position_label,
                            )}: (${selectedElement.bridge.position.x}, ${selectedElement.bridge.position.y})",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                is SelectedElement.Fief -> {
                    Column {
                        Text(
                            text = selectedElement.fief.type.getLocalizedName(),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text =
                                "${stringResource(Res.string.stat_income)}: " +
                                    "+${selectedElement.fief.type.incomePerTurn}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = "${stringResource(
                                Res.string.position_label,
                            )}: (${selectedElement.fief.position.x}, ${selectedElement.fief.position.y})",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                is SelectedElement.Mushroom -> {
                    Column {
                        Text(
                            text = stringResource(Res.string.mushroom),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = "${stringResource(
                                Res.string.position_label,
                            )}: (${selectedElement.mushroom.position.x}, ${selectedElement.mushroom.position.y})",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                is SelectedElement.Gargoyle -> {
                    Column {
                        Text(
                            text = stringResource(Res.string.gargoyle_name),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = "${stringResource(
                                Res.string.level_label,
                            )}: ${selectedElement.gargoyle.level}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = "${stringResource(
                                Res.string.position_label,
                            )}: (${selectedElement.gargoyle.position.x}, ${selectedElement.gargoyle.position.y})",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                is SelectedElement.Portal -> {
                    Column {
                        Text(
                            text = stringResource(Res.string.initial_setup_portals),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = "${stringResource(Res.string.initial_setup_portal_entry)}: (${selectedElement.portal.entryPosition.x}, ${selectedElement.portal.entryPosition.y})",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = "${stringResource(Res.string.initial_setup_portal_exit)}: (${selectedElement.portal.exitPosition.x}, ${selectedElement.portal.exitPosition.y})",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onRemove,
                    modifier = Modifier.weight(1f),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        ),
                ) {
                    Text(stringResource(Res.string.remove))
                }
                Button(
                    onClick = onDeselect,
                    modifier = Modifier.weight(1f),
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
    barricades: List<InitialBarricade>,
    bridges: List<InitialBridge>,
    fiefs: List<InitialFief>,
    portals: List<InitialPortal> = emptyList(),
    gargoyles: List<InitialGargoyle> = emptyList(),
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "${stringResource(Res.string.towers)}: ${defenders.size}",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = "${stringResource(Res.string.enemies)}: ${attackers.size}",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = "${stringResource(Res.string.traps)}: ${traps.size}",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = "${stringResource(Res.string.barricades)}: ${barricades.size}",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = "${stringResource(Res.string.bridges)}: ${bridges.size}",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = "${stringResource(Res.string.initial_setup_fiefs)}: ${fiefs.size}",
            style = MaterialTheme.typography.bodySmall,
        )
        if (gargoyles.isNotEmpty()) {
            Text(
                text = "${stringResource(Res.string.gargoyle_name)}: ${gargoyles.size}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (portals.isNotEmpty()) {
            Text(
                text = "${stringResource(Res.string.initial_setup_portals)}: ${portals.size}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
internal fun FiefType.getLocalizedName(): String =
    when (this) {
        FiefType.FISHER -> stringResource(Res.string.fief_type_fisher)
        FiefType.FISHER_BOAT -> stringResource(Res.string.fief_type_fisher_boat)
        FiefType.WOODCUTTER -> stringResource(Res.string.fief_type_woodcutter)
        FiefType.QUARRY -> stringResource(Res.string.fief_type_quarry)
        FiefType.MARKETPLACE -> stringResource(Res.string.fief_type_marketplace)
    }
