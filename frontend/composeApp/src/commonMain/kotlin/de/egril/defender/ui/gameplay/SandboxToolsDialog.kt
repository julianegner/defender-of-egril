package de.egril.defender.ui.gameplay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.model.AttackerType
import de.egril.defender.model.Position
import de.egril.defender.ui.getLocalizedName
import de.egril.defender.ui.icon.TriangleDownIcon
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.close
import defender_of_egril.composeapp.generated.resources.enemy_level
import defender_of_egril.composeapp.generated.resources.sandbox_add_coins
import defender_of_egril.composeapp.generated.resources.sandbox_spawn_enemy
import defender_of_egril.composeapp.generated.resources.sandbox_spawn_point_auto
import defender_of_egril.composeapp.generated.resources.sandbox_tools
import defender_of_egril.composeapp.generated.resources.select_spawn_point
import defender_of_egril.composeapp.generated.resources.spawn

/**
 * Sandbox in-game tools dialog: send out an adjustable test enemy (type + level + spawn point) and
 * add coins. Only shown for sandbox levels.
 */
@Composable
fun SandboxToolsDialog(
    spawnPoints: List<Position>,
    onSpawnEnemy: (AttackerType, Int, Position?) -> Unit,
    onAddCoins: () -> Unit,
    onDismiss: () -> Unit,
) {
    // Dragons are boss units with special spawn mechanics; keep the sandbox test roster to the
    // regular enemies so testing a stronghold is predictable.
    val spawnableTypes = remember { AttackerType.entries.filter { !it.isDragon && !it.isMirrorImage } }
    var selectedType by remember { mutableStateOf(spawnableTypes.first()) }
    var enemyLevel by remember { mutableStateOf(1) }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    // null = automatic (first free spawn point); otherwise the chosen spawn point.
    var selectedSpawnPoint by remember { mutableStateOf<Position?>(null) }
    var spawnPointMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.sandbox_tools)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(Res.string.sandbox_spawn_enemy))

                // Enemy type selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { typeMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(selectedType.getLocalizedName())
                            TriangleDownIcon(size = 10.dp)
                        }
                    }
                    DropdownMenu(
                        expanded = typeMenuExpanded,
                        onDismissRequest = { typeMenuExpanded = false },
                    ) {
                        spawnableTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.getLocalizedName()) },
                                onClick = {
                                    selectedType = type
                                    typeMenuExpanded = false
                                },
                            )
                        }
                    }
                }

                // Enemy level stepper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${stringResource(Res.string.enemy_level)}: $enemyLevel")
                    OutlinedButton(onClick = { if (enemyLevel > 1) enemyLevel-- }) {
                        Text("-", style = MaterialTheme.typography.titleLarge)
                    }
                    OutlinedButton(onClick = { if (enemyLevel < 20) enemyLevel++ }) {
                        Text("+", style = MaterialTheme.typography.titleLarge)
                    }
                }

                // Spawn point selector: only meaningful when the map has more than one spawn point.
                if (spawnPoints.size > 1) {
                    Text(stringResource(Res.string.select_spawn_point))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { spawnPointMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val sp = selectedSpawnPoint
                                Text(if (sp == null) stringResource(Res.string.sandbox_spawn_point_auto) else spawnPointLabel(sp))
                                TriangleDownIcon(size = 10.dp)
                            }
                        }
                        DropdownMenu(
                            expanded = spawnPointMenuExpanded,
                            onDismissRequest = { spawnPointMenuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.sandbox_spawn_point_auto)) },
                                onClick = {
                                    selectedSpawnPoint = null
                                    spawnPointMenuExpanded = false
                                },
                            )
                            spawnPoints.forEach { point ->
                                DropdownMenuItem(
                                    text = { Text(spawnPointLabel(point)) },
                                    onClick = {
                                        selectedSpawnPoint = point
                                        spawnPointMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = { onSpawnEnemy(selectedType, enemyLevel, selectedSpawnPoint) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.spawn))
                }

                HorizontalDivider()

                Button(
                    onClick = onAddCoins,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.sandbox_add_coins))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.close))
            }
        },
    )
}

/** Human-readable label for a chosen spawn point option using its grid coordinates. */
private fun spawnPointLabel(point: Position): String = "(${point.x}, ${point.y})"
