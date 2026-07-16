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
import de.egril.defender.editor.TileType
import de.egril.defender.model.AttackerType
import de.egril.defender.ui.getLocalizedName
import de.egril.defender.ui.icon.TriangleDownIcon
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.close
import defender_of_egril.composeapp.generated.resources.enemy_level
import defender_of_egril.composeapp.generated.resources.enemy_path
import defender_of_egril.composeapp.generated.resources.sandbox_add_coins
import defender_of_egril.composeapp.generated.resources.sandbox_edit_map
import defender_of_egril.composeapp.generated.resources.sandbox_spawn_enemy
import defender_of_egril.composeapp.generated.resources.sandbox_tile_build_area
import defender_of_egril.composeapp.generated.resources.sandbox_tile_no_play
import defender_of_egril.composeapp.generated.resources.sandbox_tools
import defender_of_egril.composeapp.generated.resources.spawn
import defender_of_egril.composeapp.generated.resources.spawn_point
import defender_of_egril.composeapp.generated.resources.target

/**
 * Sandbox in-game tools dialog: send out an adjustable test enemy (type + level) and add coins.
 * Only shown for sandbox levels.
 */
@Composable
fun SandboxToolsDialog(
    onSpawnEnemy: (AttackerType, Int) -> Unit,
    onAddCoins: () -> Unit,
    onSelectPaintTile: (TileType) -> Unit,
    onDismiss: () -> Unit,
) {
    // Dragons are boss units with special spawn mechanics; keep the sandbox test roster to the
    // regular enemies so testing a stronghold is predictable.
    val spawnableTypes = remember { AttackerType.entries.filter { !it.isDragon } }
    var selectedType by remember { mutableStateOf(spawnableTypes.first()) }
    var enemyLevel by remember { mutableStateOf(1) }
    var typeMenuExpanded by remember { mutableStateOf(false) }

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

                Button(
                    onClick = { onSpawnEnemy(selectedType, enemyLevel) },
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

                HorizontalDivider()

                // Map tile editing: pick a tile type, then paint tiles on the map.
                Text(stringResource(Res.string.sandbox_edit_map))
                val tilePalette =
                    listOf(
                        TileType.PATH to stringResource(Res.string.enemy_path),
                        TileType.BUILD_AREA to stringResource(Res.string.sandbox_tile_build_area),
                        TileType.SPAWN_POINT to stringResource(Res.string.spawn_point),
                        TileType.TARGET to stringResource(Res.string.target),
                        TileType.NO_PLAY to stringResource(Res.string.sandbox_tile_no_play),
                    )
                tilePalette.forEach { (type, label) ->
                    OutlinedButton(
                        onClick = { onSelectPaintTile(type) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(label)
                    }
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
