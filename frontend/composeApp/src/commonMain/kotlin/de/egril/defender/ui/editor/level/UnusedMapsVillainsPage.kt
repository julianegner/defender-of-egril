package de.egril.defender.ui.editor.level

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hyperether.resources.currentLanguage
import com.hyperether.resources.stringResource
import de.egril.defender.editor.EditorEnemySpawn
import de.egril.defender.editor.EditorLevel
import de.egril.defender.editor.EditorMap
import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import de.egril.defender.model.isRealVillain
import de.egril.defender.ui.getLocalizedDescription
import de.egril.defender.ui.getLocalizedName
import de.egril.defender.ui.hexagon.HexagonMinimapFromEditorMap
import de.egril.defender.ui.icon.WarningIcon
import de.egril.defender.ui.icon.enemy.EnemyTypeIcon
import defender_of_egril.composeapp.generated.resources.*

/**
 * Lists maps not used by any level (left) and real villains not used by any level (right).
 * Selecting one of each and confirming creates a new level with that villain in its first spawn turn.
 */
@Composable
internal fun UnusedMapsVillainsPage(
    levels: List<EditorLevel>,
    maps: List<EditorMap>,
    onBack: () -> Unit,
    onCreateLevel: (map: EditorMap, villain: AttackerType, name: String) -> Unit,
) {
    val locale = currentLanguage.value
    val usedMapIds = remember(levels) { levels.map { it.mapId }.toSet() }
    val unusedMaps = remember(maps, usedMapIds) { maps.filter { it.readyToUse && it.id !in usedMapIds } }
    val unusedVillains =
        remember(levels) {
            villainUsageEntries(levels, onlyUnusedVillains = true).map { it.villainType }
        }
    var selectedMap by remember { mutableStateOf<EditorMap?>(null) }
    var selectedVillain by remember { mutableStateOf<AttackerType?>(null) }
    var showNameDialog by remember { mutableStateOf(false) }

    val pairIncompatible =
        selectedMap != null && selectedVillain != null &&
            selectedMap!!.getCompatibleSpawnPoints(selectedVillain!!).isEmpty()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.unused_maps_villains),
                style = MaterialTheme.typography.titleMedium,
            )
            Button(onClick = onBack) {
                Text(stringResource(Res.string.back_to_levels))
            }
        }

        Text(
            text = stringResource(Res.string.pair_selection_hint),
            style = MaterialTheme.typography.bodyMedium,
        )

        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.unused_maps),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(unusedMaps) { map ->
                        SelectableCard(
                            selected = selectedMap?.id == map.id,
                            onClick = { selectedMap = if (selectedMap?.id == map.id) null else map },
                        ) {
                            Text(
                                text = map.name.ifBlank { map.id },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = map.id,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Box(
                                modifier = Modifier.fillMaxWidth().height(120.dp).padding(top = 8.dp),
                            ) {
                                HexagonMinimapFromEditorMap(
                                    map = map,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.unused_villains),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(unusedVillains.filter { it.isRealVillain }) { villain ->
                        SelectableCard(
                            selected = selectedVillain == villain,
                            onClick = { selectedVillain = if (selectedVillain == villain) null else villain },
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(modifier = Modifier.size(36.dp)) {
                                    EnemyTypeIcon(attackerType = villain, modifier = Modifier.fillMaxSize())
                                }
                                Column {
                                    Text(
                                        text = villain.getLocalizedName(locale),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = villain.getLocalizedDescription(),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (pairIncompatible) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                WarningIcon(size = 14.dp)
                Text(
                    text = stringResource(Res.string.pair_incompatible_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        Button(
            onClick = { showNameDialog = true },
            enabled = selectedMap != null && selectedVillain != null && !pairIncompatible,
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(stringResource(Res.string.pair_level_title))
        }
    }

    if (showNameDialog && selectedMap != null && selectedVillain != null) {
        LevelNameDialog(
            onDismiss = { showNameDialog = false },
            onCreate = { name ->
                showNameDialog = false
                onCreateLevel(selectedMap!!, selectedVillain!!, name)
            },
        )
    }
}

/**
 * Builds a level on [map] whose only enemy is [villain] in spawn turn 1.
 * The villain is placed on the first spawn point compatible with its movement type.
 */
internal fun createPairedLevel(
    map: EditorMap,
    villain: AttackerType,
    title: String,
    author: String,
): EditorLevel {
    val spawnPoint = map.getCompatibleSpawnPoints(villain).firstOrNull()
    val id = levelIdFromTitle(title)
    return EditorLevel(
        id = id,
        mapId = map.id,
        title = title,
        subtitle = "",
        startCoins = 100,
        startHealthPoints = 10,
        enemySpawns = listOf(EditorEnemySpawn(attackerType = villain, level = 1, spawnTurn = 1, spawnPoint = spawnPoint)),
        availableTowers = DefenderType.entries.filter { it != DefenderType.DRAGONS_LAIR && it != DefenderType.ALTAR }.toSet(),
        author = author,
    )
}

/** Same id rules as the level creation dialog; falls back to a random id for titles with no usable characters. */
internal fun levelIdFromTitle(title: String): String {
    val sanitized =
        title
            .trim()
            .lowercase()
            .replace(" ", "_")
            .replace(Regex("[^a-z0-9_]"), "")
            .replace(Regex("_+"), "_")
    return sanitized.ifEmpty { "custom_${kotlin.random.Random.nextInt(10000, 99999)}" }
}

@Composable
private fun SelectableCard(
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
            ),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            content()
        }
    }
}

@Composable
private fun LevelNameDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    val trimmed = name.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.pair_level_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(Res.string.level_title)) },
                    isError = name.isNotEmpty() && trimmed.isEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (trimmed.isEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(Res.string.level_name_required),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onCreate(trimmed) }, enabled = trimmed.isNotEmpty()) {
                Text(stringResource(Res.string.create))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}
