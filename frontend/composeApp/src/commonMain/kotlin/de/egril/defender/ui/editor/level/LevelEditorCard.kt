package de.egril.defender.ui.editor.level

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.*
import de.egril.defender.ui.icon.*
import de.egril.defender.ui.loadgame.SavefileLocationChip
import defender_of_egril.composeapp.generated.resources.*

@Composable
internal fun LevelCard(
    level: EditorLevel,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    // Check if any enemies are spawned outside valid spawn points
    val map = remember(level.mapId) { EditorStorage.getMap(level.mapId) }
    val spawnEntries = remember(level.enemySpawns, level.spawnGroups) { level.configuredSpawnEntries() }
    val villainTypes = remember(spawnEntries) { spawnEntries.presentVillainTypes() }
    val villainSummary = remember(spawnEntries) { spawnEntries.presentVillainSummary { it.villainName ?: it.displayName } }
    val hasEnemiesOutsideSpawnPoints =
        remember(spawnEntries, map) {
            val mapSpawnPoints = map?.getSpawnPoints()?.toSet() ?: emptySet()
            spawnEntries.any { spawn ->
                spawn.spawnPoint != null && spawn.spawnPoint !in mapSpawnPoints
            }
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect() }
                            .padding(12.dp)
                            .padding(top = 24.dp), // Add top padding for the badges
                ) {
                    Text(
                        text = level.title,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    // Add warning badge if enemies are outside spawn points
                    if (hasEnemiesOutsideSpawnPoints) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            WarningBadge()
                        }
                    }
                    if (level.subtitle.isNotEmpty()) {
                        Text(
                            text = level.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text(
                        text = "${stringResource(Res.string.file)}: ${level.id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${stringResource(
                            Res.string.map_label,
                        )}: ${level.mapId} | ${stringResource(
                            Res.string.coins,
                        )}: ${level.startCoins} | ${stringResource(Res.string.hp_short)}: ${level.startHealthPoints}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = "${stringResource(Res.string.enemies)}: ${level.configuredSpawnCount()}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (villainSummary.isNotEmpty()) {
                        Text(
                            text = "${stringResource(if (villainTypes.size > 1) Res.string.villains else Res.string.villain)}: $villainSummary",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Red,
                        )
                    }
                    Text(
                        text =
                            if (EditorStorage.isLevelReadyToPlay(
                                    level,
                                )
                            ) {
                                stringResource(Res.string.ready_to_use)
                            } else {
                                stringResource(Res.string.not_ready)
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (EditorStorage.isLevelReadyToPlay(level)) Color.Green else Color.Red,
                    )
                }

                // Badges in upper right corner
                Column(
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Test Level badge
                        if (level.testingOnly) {
                            Text(
                                text = stringResource(Res.string.test_level),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        // Ready/not ready check indicator
                        if (EditorStorage.isLevelReadyToPlay(level)) {
                            CheckmarkIcon(
                                size = 20.dp,
                                tint = Color.Green,
                            )
                        } else {
                            CrossIcon(
                                size = 20.dp,
                                tint = Color.Red,
                            )
                        }
                    }
                    // Official/User badge below the check
                    if (level.isOfficial) {
                        Text(
                            text = stringResource(Res.string.official_level),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    // Community badge: shown for levels downloaded from the community backend
                    if (level.isCommunity) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SavefileLocationChip(
                                label = stringResource(Res.string.savefile_chip_local),
                                color = MaterialTheme.colorScheme.tertiary,
                                onColor = MaterialTheme.colorScheme.onTertiary,
                                isMobile = false,
                            )
                            SavefileLocationChip(
                                label = stringResource(Res.string.savefile_chip_remote),
                                color = MaterialTheme.colorScheme.primary,
                                onColor = MaterialTheme.colorScheme.onPrimary,
                                isMobile = false,
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onCopy,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(Res.string.copy_level))
                }
                Button(
                    onClick = onDelete,
                    enabled = !level.isOfficial || de.egril.defender.OfficialEditMode.enabled,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        ),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(Res.string.delete))
                }
            }
        }
    }
}
