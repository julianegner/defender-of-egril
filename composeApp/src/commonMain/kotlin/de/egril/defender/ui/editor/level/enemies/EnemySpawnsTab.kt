package de.egril.defender.ui.editor.level.enemies

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.EditorEnemySpawn
import de.egril.defender.editor.EditorMap
import de.egril.defender.ui.editor.level.ChangeLevelDialog
import de.egril.defender.ui.editor.level.ChangeSpawnPointDialog
import de.egril.defender.ui.editor.level.SpawnTurnSection
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.add_turn
import defender_of_egril.composeapp.generated.resources.enemies
import defender_of_egril.composeapp.generated.resources.remove_all_turns

/**
 * Tab 2: Enemy Spawns
 */
@Composable
fun EnemySpawnsTab(
    enemySpawns: MutableList<EditorEnemySpawn>,
    maxTurnNumber: Int,
    onMaxTurnNumberChange: (Int) -> Unit,
    onEnemySpawnsChange: (MutableList<EditorEnemySpawn>) -> Unit,
    ewhadCount: Int,
    onShowEnemyDialog: (Int) -> Unit,
    onShowRemoveAllTurnsDialog: () -> Unit,
    map: EditorMap?
) {
    // Track the last added turn to keep it expanded
    var lastAddedTurn by remember { mutableStateOf<Int?>(null) }
    
    // Track spawn point change dialog
    var spawnToChange by remember { mutableStateOf<EditorEnemySpawn?>(null) }
    
    // Track level change dialog
    var spawnToChangeLevel by remember { mutableStateOf<EditorEnemySpawn?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Add turn and remove all turns buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${stringResource(Res.string.enemies)} (${enemySpawns.size}):",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = {
                        // Add a new empty turn without opening dialog
                        val newTurn = maxTurnNumber + 1
                        onMaxTurnNumberChange(newTurn)
                        lastAddedTurn = newTurn
                    }) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("➕")
                            Text(stringResource(Res.string.add_turn))
                        }
                    }

                    Button(
                        onClick = onShowRemoveAllTurnsDialog,
                        enabled = enemySpawns.isNotEmpty() || maxTurnNumber > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (enemySpawns.isNotEmpty() || maxTurnNumber > 0)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(stringResource(Res.string.remove_all_turns))
                    }
                }
            }
        }

        // Group enemies by spawn turn and create list including empty turns
        val turnGroups = enemySpawns.groupBy { it.spawnTurn }.entries.sortedBy { it.key }

        // Create list of all turns from 1 to maxTurnNumber (including empty ones)
        val allTurns = (1..maxTurnNumber).map { turn ->
            turn to (turnGroups.find { it.key == turn }?.value ?: emptyList())
        }

        allTurns.forEachIndexed { index, (turn, spawnsInTurn) ->
            item {
                SpawnTurnSection(
                    turn = turn,
                    spawns = spawnsInTurn,
                    initiallyExpanded = turn == lastAddedTurn,
                    onRemoveEnemy = { spawn ->
                        val newSpawns = enemySpawns.toMutableList().apply { remove(spawn) }
                        onEnemySpawnsChange(newSpawns)
                    },
                    onDeleteTurn = {
                        // Check if this is the last turn
                        val isLastTurn = turn == maxTurnNumber
                        if (isLastTurn) {
                            // Remove all enemies from this turn and decrement maxTurnNumber
                            val newSpawns = enemySpawns.filter { it.spawnTurn != turn }.toMutableList()
                            onEnemySpawnsChange(newSpawns)
                            onMaxTurnNumberChange(maxTurnNumber - 1)
                        }
                    },
                    onClearTurn = {
                        // Clear all enemies from this turn but keep the turn
                        val newSpawns = enemySpawns.filter { it.spawnTurn != turn }.toMutableList()
                        onEnemySpawnsChange(newSpawns)
                    },
                    canDeleteTurn = turn == maxTurnNumber,
                    onCopyTurn = {
                        // Copy all enemies from this turn to a new turn (next available)
                        val newSpawns = enemySpawns.toMutableList().apply {
                            spawnsInTurn.forEach { spawn ->
                                add(spawn.copy(spawnTurn = maxTurnNumber + 1))
                            }
                        }
                        onEnemySpawnsChange(newSpawns)
                        onMaxTurnNumberChange(maxTurnNumber + 1)
                    },
                    onAddEnemy = {
                        // Show dialog to add enemy to this specific turn
                        onShowEnemyDialog(turn)
                    },
                    onMoveTurnUp = {
                        if (index > 0) {
                            val prevTurn = allTurns[index - 1].first
                            val newSpawns = enemySpawns.map { spawn ->
                                when (spawn.spawnTurn) {
                                    turn -> spawn.copy(spawnTurn = prevTurn)
                                    prevTurn -> spawn.copy(spawnTurn = turn)
                                    else -> spawn
                                }
                            }.toMutableList()
                            onEnemySpawnsChange(newSpawns)
                        }
                    },
                    onMoveTurnDown = {
                        if (index < allTurns.size - 1) {
                            val nextTurn = allTurns[index + 1].first
                            val newSpawns = enemySpawns.map { spawn ->
                                when (spawn.spawnTurn) {
                                    turn -> spawn.copy(spawnTurn = nextTurn)
                                    nextTurn -> spawn.copy(spawnTurn = turn)
                                    else -> spawn
                                }
                            }.toMutableList()
                            onEnemySpawnsChange(newSpawns)
                        }
                    },
                    canMoveUp = index > 0,
                    canMoveDown = index < allTurns.size - 1,
                    ewhadCount = ewhadCount,
                    onChangeSpawnPoint = { spawn ->
                        spawnToChange = spawn
                    },
                    onChangeLevel = { spawn ->
                        spawnToChangeLevel = spawn
                    }
                )
            }
        }
    }
    
    // Change spawn point dialog
    spawnToChange?.let { spawn ->
        ChangeSpawnPointDialog(
            spawn = spawn,
            map = map,
            onDismiss = { spawnToChange = null },
            onChange = { newSpawnPoint ->
                val newSpawns = enemySpawns.map {
                    if (it === spawn) {
                        it.copy(spawnPoint = newSpawnPoint)
                    } else {
                        it
                    }
                }.toMutableList()
                onEnemySpawnsChange(newSpawns)
                spawnToChange = null
            }
        )
    }
    
    // Change level dialog
    spawnToChangeLevel?.let { spawn ->
        ChangeLevelDialog(
            spawn = spawn,
            onDismiss = { spawnToChangeLevel = null },
            onChange = { newLevel ->
                val newSpawns = enemySpawns.map {
                    if (it === spawn) {
                        it.copy(level = newLevel)
                    } else {
                        it
                    }
                }.toMutableList()
                onEnemySpawnsChange(newSpawns)
                spawnToChangeLevel = null
            }
        )
    }
}
