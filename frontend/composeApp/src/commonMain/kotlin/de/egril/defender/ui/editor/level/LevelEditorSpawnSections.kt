package de.egril.defender.ui.editor.level

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.hyperether.resources.stringResource
import de.egril.defender.editor.EditorEnemySpawn
import de.egril.defender.editor.EditorMap
import de.egril.defender.model.Position
import de.egril.defender.ui.*
import de.egril.defender.ui.hexagon.EnemyIconOnHexagon
import de.egril.defender.ui.icon.CheckmarkIcon
import de.egril.defender.ui.icon.DownArrowIcon
import de.egril.defender.ui.icon.PushpinIcon
import de.egril.defender.ui.icon.ReloadIcon
import de.egril.defender.ui.icon.TrashIcon
import de.egril.defender.ui.icon.TriangleDownIcon
import de.egril.defender.ui.icon.TriangleRightIcon
import de.egril.defender.ui.icon.UpArrowIcon
import defender_of_egril.composeapp.generated.resources.*

/**
 * Collapsible section showing enemies spawning in a specific turn
 */
@Composable
fun SpawnTurnSection(
    turn: Int,
    spawns: List<EditorEnemySpawn>,
    initiallyExpanded: Boolean = false,
    expandRequestKey: Int? = null,
    onRemoveEnemy: (EditorEnemySpawn) -> Unit,
    onDeleteTurn: () -> Unit,
    onClearTurn: () -> Unit,
    canDeleteTurn: Boolean,
    onCopyTurn: () -> Unit,
    onAddEnemy: () -> Unit,
    onMoveTurnUp: () -> Unit,
    onMoveTurnDown: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onChangeSpawnPoint: (EditorEnemySpawn) -> Unit,
    onChangeLevel: (EditorEnemySpawn) -> Unit,
    onChangeTurnLevel: () -> Unit,
    onSaveAsTemplate: () -> Unit,
    onCreateLoop: (() -> Unit)? = null,
    onEditLoopOptions: ((EditorEnemySpawn) -> Unit)? = null,
) {
    var expanded by remember(turn) { mutableStateOf(initiallyExpanded) }
    val villainSummary = remember(spawns) { spawns.presentVillainSummary { it.villainName ?: it.displayName } }
    val distinctTypes = remember(spawns) { spawns.map { it.attackerType }.distinct() }

    LaunchedEffect(initiallyExpanded, expandRequestKey) {
        if (initiallyExpanded || expandRequestKey != null) {
            expanded = true
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
        ) {
            // Turn header
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (expanded) {
                        TriangleDownIcon(size = 16.dp)
                    } else {
                        TriangleRightIcon(size = 16.dp)
                    }
                    ReloadIcon(size = 14.dp)
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "Turn $turn",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "(${spawns.size} enemies)",
                                fontSize = 12.sp,
                                color = Color.Gray,
                            )
                            distinctTypes.forEach { type ->
                                TooltipWrapper(text = type.getLocalizedName()) {
                                    EnemyIconOnHexagon(attackerType = type, size = 18.dp)
                                }
                            }
                        }
                        if (villainSummary.isNotEmpty()) {
                            Text(
                                text = villainSummary,
                                color = Color.Red,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Button(
                        onClick = onMoveTurnUp,
                        enabled = canMoveUp,
                        modifier = Modifier.height(32.dp),
                    ) {
                        UpArrowIcon(size = 12.dp, tint = Color.White)
                    }
                    Button(
                        onClick = onMoveTurnDown,
                        enabled = canMoveDown,
                        modifier = Modifier.height(32.dp),
                    ) {
                        DownArrowIcon(size = 12.dp, tint = Color.White)
                    }
                    // Change Turn Level button - only visible if there are units
                    if (spawns.isNotEmpty()) {
                        Button(
                            onClick = onChangeTurnLevel,
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        ) {
                            Text(
                                text = stringResource(Res.string.level),
                                fontSize = 12.sp,
                            )
                        }
                    }
                    Button(
                        onClick = onCopyTurn,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) {
                        Text(
                            text = "Copy Turn",
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.CenterVertically),
                        )
                    }
                    if (onCreateLoop != null) {
                        Button(
                            onClick = onCreateLoop,
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        ) {
                            Text(
                                text = stringResource(Res.string.spawn_create_loop),
                                fontSize = 12.sp,
                            )
                        }
                    }
                    Button(
                        onClick = onSaveAsTemplate,
                        enabled = spawns.isNotEmpty(),
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.save_as_template),
                            fontSize = 12.sp,
                        )
                    }
                    // Show either Clear or Delete button depending on whether it's the last turn
                    // Make sure both buttons have same size by using consistent width
                    if (canDeleteTurn) {
                        Button(
                            onClick = onDeleteTurn,
                            modifier = Modifier.height(32.dp).width(80.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                ),
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                TrashIcon(size = 12.dp)
                            }
                        }
                    } else {
                        Button(
                            onClick = onClearTurn,
                            enabled = spawns.isNotEmpty(),
                            modifier = Modifier.height(32.dp).width(80.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        ) {
                            Text(
                                text = stringResource(Res.string.clear_turn),
                                fontSize = 10.sp,
                            )
                        }
                    }
                }
            }

            // Enemy list (collapsible)
            if (expanded) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Add Enemy button at the top
                    Button(
                        onClick = onAddEnemy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(Res.string.add_enemy_button, turn))
                    }

                    if (spawns.isEmpty()) {
                        // Show message for empty turn
                        Text(
                            text = stringResource(Res.string.no_enemies_in_turn),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(8.dp),
                        )
                    } else {
                        spawns.forEach { spawn ->
                            EnemySpawnRow(
                                spawn = spawn,
                                onRemoveEnemy = { onRemoveEnemy(spawn) },
                                onChangeSpawnPoint = onChangeSpawnPoint,
                                onChangeLevel = onChangeLevel,
                                onEditLoopOptions = onEditLoopOptions,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Row displaying a single enemy spawn with its information and actions
 */
@Composable
private fun EnemySpawnRow(
    spawn: EditorEnemySpawn,
    onRemoveEnemy: () -> Unit,
    onChangeSpawnPoint: (EditorEnemySpawn) -> Unit,
    onChangeLevel: (EditorEnemySpawn) -> Unit,
    onEditLoopOptions: ((EditorEnemySpawn) -> Unit)? = null,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Color.LightGray.copy(alpha = 0.2f))
                .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            // Enemy icon
            Box(modifier = Modifier.size(24.dp)) {
                EnemyIconOnHexagon(
                    attackerType = spawn.attackerType,
                    size = 24.dp,
                )
            }

            // Enemy info in horizontal layout
            Text(
                text = "${spawn.attackerType.displayName} Lv${spawn.level}",
                fontSize = 14.sp,
            )
            Text(
                text = "${stringResource(Res.string.hp_short)}: ${spawn.healthPoints}",
                fontSize = 11.sp,
                color = Color.Gray,
            )

            // Display spawn point - always show it
            val spawnPointText =
                spawn.spawnPoint?.let { spawnPoint ->
                    "${stringResource(Res.string.spawn_point)}: (${spawnPoint.x}, ${spawnPoint.y})"
                } ?: stringResource(Res.string.no_spawn_point_set)
            Text(
                text = spawnPointText,
                fontSize = 10.sp,
                color = if (spawn.spawnPoint != null) MaterialTheme.colorScheme.primary else Color.Gray,
                fontWeight = if (spawn.spawnPoint != null) FontWeight.SemiBold else FontWeight.Normal,
            )

            // Change spawn point button - right after spawn point info
            Button(
                onClick = { onChangeSpawnPoint(spawn) },
                modifier = Modifier.height(28.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
            ) {
                PushpinIcon(size = 10.dp)
            }

            // Change level button
            Button(
                onClick = { onChangeLevel(spawn) },
                modifier = Modifier.height(28.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
            ) {
                Text(stringResource(Res.string.level), fontSize = 10.sp)
            }

            if (onEditLoopOptions != null) {
                Button(
                    onClick = { onEditLoopOptions(spawn) },
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                ) {
                    Text(stringResource(Res.string.spawn_loop_options), fontSize = 10.sp)
                }
            }
            spawn.loopUnitId?.takeIf { onEditLoopOptions != null }?.let { unitId ->
                Text(
                    text = "${stringResource(Res.string.spawn_unit_id_short)}: $unitId",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (onEditLoopOptions != null && spawn.spawnsInFirstIterationOnly) {
                Text(
                    text = stringResource(Res.string.spawn_first_only),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }

        // Remove button on the far right
        Button(
            onClick = onRemoveEnemy,
            modifier = Modifier.height(36.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TrashIcon(size = 12.dp)
                Text(stringResource(Res.string.remove), fontSize = 11.sp)
            }
        }
    }
}

/**
 * Dialog for bulk changing spawn points of all enemies
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChangeAllSpawnPointsDialog(
    enemySpawns: List<EditorEnemySpawn>,
    map: EditorMap?,
    onDismiss: () -> Unit,
    onApply: (Map<Position, Position>) -> Unit,
) {
    val mapSpawnPoints = remember(map) { map?.getSpawnPoints() ?: emptyList() }

    // Get all unique spawn points currently used by enemies
    val usedSpawnPoints =
        remember(enemySpawns) {
            enemySpawns.mapNotNull { it.spawnPoint }.distinct()
        }

    // Create a mutable state map for spawn point remapping
    val remappings =
        remember {
            mutableStateMapOf<Position, Position>().apply {
                // Initialize each used position to first available spawn point or itself
                usedSpawnPoints.forEach { pos ->
                    put(pos, mapSpawnPoints.firstOrNull() ?: pos)
                }
            }
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.change_all_spawn_points_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Minimap at the top (non-scrolling)
                if (map != null) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .background(Color(0xCC000000))
                                .border(2.dp, Color.White)
                                .padding(4.dp),
                    ) {
                        SpawnPointMinimap(
                            map = map,
                            selectedSpawnPoint = null,
                            visibleSpawnPoints = null,
                        )
                    }

                    HorizontalDivider()
                }

                // Description
                Text(
                    text = stringResource(Res.string.change_all_spawn_points_description),
                    style = MaterialTheme.typography.bodyMedium,
                )

                HorizontalDivider()

                // Count of affected enemies
                val affectedCount =
                    enemySpawns.count { spawn ->
                        spawn.spawnPoint?.let { remappings[it] != it } ?: false
                    }
                Text(
                    text = stringResource(Res.string.enemies_affected_count, affectedCount),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                )

                HorizontalDivider()

                // Remapping table (scrollable)
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(250.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(usedSpawnPoints) { fromPos ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                // From position
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = stringResource(Res.string.from_spawn_point),
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        text = "(${fromPos.x}, ${fromPos.y})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }

                                // Count of enemies at this position
                                val count = enemySpawns.count { it.spawnPoint == fromPos }
                                Text(
                                    text = stringResource(Res.string.enemies_affected_count, count),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                )

                                // Arrow
                                DownArrowIcon(
                                    size = 24.dp,
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
                                )

                                // To position selector
                                Column {
                                    Text(
                                        text = stringResource(Res.string.to_spawn_point),
                                        style = MaterialTheme.typography.labelMedium,
                                    )

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    ) {
                                        mapSpawnPoints.forEach { toPos ->
                                            FilterChip(
                                                modifier = Modifier.height(32.dp),
                                                selected = remappings[fromPos] == toPos,
                                                onClick = { remappings[fromPos] = toPos },
                                                label = {
                                                    Text(
                                                        text = "(${toPos.x},${toPos.y})",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight =
                                                            if (remappings[fromPos] ==
                                                                toPos
                                                            ) {
                                                                FontWeight.Bold
                                                            } else {
                                                                FontWeight.Normal
                                                            },
                                                    )
                                                },
                                                leadingIcon =
                                                    if (remappings[fromPos] == toPos) {
                                                        { CheckmarkIcon(size = 14.dp) }
                                                    } else {
                                                        null
                                                    },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApply(remappings.toMap())
                },
                enabled = mapSpawnPoints.isNotEmpty(),
            ) {
                Text(stringResource(Res.string.apply_changes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}
