package de.egril.defender.ui.editor.level

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import de.egril.defender.model.AttackerType
import de.egril.defender.model.Position
import de.egril.defender.ui.*
import de.egril.defender.ui.hexagon.EnemyIconOnHexagon
import de.egril.defender.ui.icon.CheckmarkIcon
import defender_of_egril.composeapp.generated.resources.*

/**
 * Dialog for selecting or changing a spawn point with minimap visualization.
 * Can be used both when adding new enemies and when changing existing enemy spawn points.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpawnPointSelectionDialog(
    attackerType: AttackerType,
    level: Int,
    healthPoints: Int = attackerType.health * level,
    map: EditorMap?,
    currentSelection: Position?,
    title: String,
    confirmButtonText: String,
    onDismiss: () -> Unit,
    onConfirm: (Position) -> Unit,
) {
    val compatibleSpawnPoints = remember(map, attackerType) { map?.getCompatibleSpawnPoints(attackerType) ?: emptyList() }
    var selectedSpawnPoint by remember(attackerType, map, currentSelection) {
        mutableStateOf(currentSelection?.takeIf { it in compatibleSpawnPoints } ?: compatibleSpawnPoints.firstOrNull())
    }
    val compatibleSpawnPointSet = remember(compatibleSpawnPoints) { compatibleSpawnPoints.toSet() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Enemy info
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.size(32.dp)) {
                        EnemyIconOnHexagon(
                            attackerType = attackerType,
                            size = 32.dp,
                        )
                    }
                    Column {
                        Text(
                            text = "${attackerType.getLocalizedName()} Lvl $level",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "${stringResource(Res.string.hp_short)}: $healthPoints",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                        )
                    }
                }

                HorizontalDivider()

                // Minimap showing spawn points
                if (map != null && compatibleSpawnPoints.isNotEmpty()) {
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
                            selectedSpawnPoint = selectedSpawnPoint,
                            visibleSpawnPoints = compatibleSpawnPointSet,
                        )
                    }
                }

                // Instructions
                Text(
                    text = stringResource(Res.string.select_spawn_point_for_enemy),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Spawn point selection chips
                if (compatibleSpawnPoints.isNotEmpty()) {
                    Text(
                        text = stringResource(Res.string.spawn_point),
                        style = MaterialTheme.typography.titleSmall,
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        compatibleSpawnPoints.forEach { point ->
                            val isWaterSpawn = map?.getSpawnPointType(point) == de.egril.defender.model.SpawnPointType.WATER
                            FilterChip(
                                modifier = Modifier.height(32.dp),
                                selected = selectedSpawnPoint == point,
                                onClick = { selectedSpawnPoint = point },
                                colors =
                                    if (isWaterSpawn) {
                                        FilterChipDefaults.filterChipColors(
                                            containerColor = Color(0xFFB3D9FF),
                                            labelColor = Color(0xFF0D47A1),
                                            selectedContainerColor = Color(0xFF1565C0),
                                            selectedLabelColor = Color.White,
                                            selectedLeadingIconColor = Color.White,
                                        )
                                    } else {
                                        FilterChipDefaults.filterChipColors()
                                    },
                                label = {
                                    Text(
                                        text = "S(${point.x},${point.y})",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                },
                                leadingIcon =
                                    if (selectedSpawnPoint == point) {
                                        { CheckmarkIcon(size = 14.dp) }
                                    } else {
                                        null
                                    },
                            )
                        }
                    }
                } else {
                    Text(
                        text = stringResource(Res.string.no_compatible_spawn_points_available),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedSpawnPoint?.let { onConfirm(it) }
                },
                enabled = selectedSpawnPoint != null,
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}

/**
 * Convenience wrapper for selecting spawn point when adding new enemies
 */
@Composable
fun SelectSpawnPointDialog(
    selectedType: AttackerType,
    level: Int,
    map: EditorMap?,
    currentSelection: Position?,
    onDismiss: () -> Unit,
    onSelect: (Position) -> Unit,
) {
    SpawnPointSelectionDialog(
        attackerType = selectedType,
        level = level,
        map = map,
        currentSelection = currentSelection,
        title = stringResource(Res.string.select_spawn_point),
        confirmButtonText = stringResource(Res.string.select),
        onDismiss = onDismiss,
        onConfirm = onSelect,
    )
}

/**
 * Convenience wrapper for changing spawn point of existing enemies
 */
@Composable
fun ChangeSpawnPointDialog(
    spawn: EditorEnemySpawn,
    map: EditorMap?,
    onDismiss: () -> Unit,
    onChange: (Position) -> Unit,
) {
    SpawnPointSelectionDialog(
        attackerType = spawn.attackerType,
        level = spawn.level,
        healthPoints = spawn.healthPoints,
        map = map,
        currentSelection = spawn.spawnPoint,
        title = stringResource(Res.string.change_spawn_point),
        confirmButtonText = stringResource(Res.string.change),
        onDismiss = onDismiss,
        onConfirm = onChange,
    )
}

/**
 * Dialog for changing the level of an existing enemy spawn
 */
@Composable
fun ChangeLevelDialog(
    spawn: EditorEnemySpawn,
    onDismiss: () -> Unit,
    onChange: (Int) -> Unit,
) {
    var newLevel by remember { mutableStateOf(spawn.level.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.change_enemy_level)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Enemy info
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.size(32.dp)) {
                        EnemyIconOnHexagon(
                            attackerType = spawn.attackerType,
                            size = 32.dp,
                        )
                    }
                    Column {
                        Text(
                            text = spawn.attackerType.getLocalizedName(),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "${stringResource(Res.string.level)}: ${spawn.level}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                        )
                    }
                }

                HorizontalDivider()

                // New level input
                OutlinedTextField(
                    value = newLevel,
                    onValueChange = { if (it.all { c -> c.isDigit() } || it.isEmpty()) newLevel = it },
                    label = { Text(stringResource(Res.string.new_level)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                // Show new HP calculation
                val newLevelValue = newLevel.toIntOrNull() ?: spawn.level
                Text(
                    text = stringResource(Res.string.hp_with_level, spawn.attackerType.health * newLevelValue),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val levelValue = newLevel.toIntOrNull()
                    if (levelValue != null && levelValue > 0) {
                        onChange(levelValue)
                    }
                },
                enabled = newLevel.toIntOrNull()?.let { it > 0 } == true,
            ) {
                Text(stringResource(Res.string.change))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}

/**
 * Dialog for changing the level of all enemies in a spawn turn.
 * Shows a confirmation dialog if units have different levels.
 */
@Composable
fun ChangeTurnLevelDialog(
    turn: Int,
    spawns: List<EditorEnemySpawn>,
    onDismiss: () -> Unit,
    onChange: (Int) -> Unit,
) {
    var newLevel by remember { mutableStateOf("") }
    var showMixedLevelsConfirmation by remember { mutableStateOf(false) }

    // Get unique levels in this turn
    val uniqueLevels = spawns.map { it.level }.distinct().sorted()
    val hasMixedLevels = uniqueLevels.size > 1

    // Initialize with the first level if all same, otherwise leave empty
    LaunchedEffect(spawns) {
        if (!hasMixedLevels && spawns.isNotEmpty()) {
            newLevel = uniqueLevels.first().toString()
        }
    }

    if (showMixedLevelsConfirmation) {
        // Confirmation dialog for mixed levels
        val levelsList = uniqueLevels.joinToString(", ")
        val targetLevel = newLevel.toIntOrNull() ?: 1

        AlertDialog(
            onDismissRequest = { showMixedLevelsConfirmation = false },
            title = { Text(stringResource(Res.string.change_turn_level)) },
            text = {
                Text(stringResource(Res.string.mixed_levels_warning, levelsList, targetLevel))
            },
            confirmButton = {
                Button(
                    onClick = {
                        onChange(targetLevel)
                        showMixedLevelsConfirmation = false
                    },
                ) {
                    Text(stringResource(Res.string.apply))
                }
            },
            dismissButton = {
                TextButton(onClick = { showMixedLevelsConfirmation = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(Res.string.change_all_levels_in_turn, turn)) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Show current level distribution
                    Text(
                        text = "${stringResource(Res.string.enemies)}: ${spawns.size}",
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    if (hasMixedLevels) {
                        Text(
                            text = "${stringResource(Res.string.level)}: ${uniqueLevels.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                        )
                    } else if (spawns.isNotEmpty()) {
                        Text(
                            text = "${stringResource(Res.string.level)}: ${uniqueLevels.first()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                        )
                    }

                    HorizontalDivider()

                    // New level input
                    OutlinedTextField(
                        value = newLevel,
                        onValueChange = { if (it.all { c -> c.isDigit() } || it.isEmpty()) newLevel = it },
                        label = { Text(stringResource(Res.string.new_level)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val levelValue = newLevel.toIntOrNull()
                        if (levelValue != null && levelValue > 0) {
                            if (hasMixedLevels) {
                                showMixedLevelsConfirmation = true
                            } else {
                                onChange(levelValue)
                            }
                        }
                    },
                    enabled = newLevel.toIntOrNull()?.let { it > 0 } == true,
                ) {
                    Text(stringResource(Res.string.apply))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }
}
