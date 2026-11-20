package de.egril.defender.ui.editor.level

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.hyperether.resources.stringResource
import de.egril.defender.editor.*
import de.egril.defender.model.AttackerType
import de.egril.defender.model.DEFAULT_AUTO_TOWER_BASE_EXTRA_COST
import de.egril.defender.ui.editor.ConfirmationDialog
import de.egril.defender.ui.editor.SaveAsDialog
import defender_of_egril.composeapp.generated.resources.*
import kotlin.random.Random

@Composable
internal fun LevelEditorViewDialogs(
    state: LevelEditorViewState,
    level: EditorLevel,
    currentMap: EditorMap?,
    presentVillainTypes: Set<AttackerType>,
    spawnTimeline: EditorSpawnTimeline,
    onSave: (EditorLevel) -> Unit,
) {
    with(state) {
        if (showEnemyDialog) {
            AddEnemyDialog(
                presentVillainTypes = presentVillainTypes,
                turn = showEnemyDialogForTurn,
                map = currentMap,
                initialType = lastEnemyDialogType,
                initialLevel = lastEnemyDialogLevel,
                initialAmount = lastEnemyDialogAmount,
                initialSpawnPoint = lastEnemyDialogSpawnPoint,
                onDismiss = { showEnemyDialog = false },
                onAdd = { enemyType, level, amount, spawnPoint ->
                    lastEnemyDialogType = enemyType
                    lastEnemyDialogLevel = level.toString()
                    lastEnemyDialogAmount = amount.toString()
                    lastEnemyDialogSpawnPoint = spawnPoint
                    updateEnemySpawnState(
                        newEnemySpawns =
                            enemySpawns.toMutableList().apply {
                                repeat(amount) {
                                    add(EditorEnemySpawn(enemyType, level, showEnemyDialogForTurn, spawnPoint))
                                }
                            },
                    )
                    showEnemyDialog = false
                },
            )
        }

        if (showRemoveAllTurnsDialog) {
            ConfirmationDialog(
                title = stringResource(Res.string.remove_all_turns),
                message = stringResource(Res.string.confirm_remove_all_turns),
                onDismiss = { showRemoveAllTurnsDialog = false },
                onConfirm = {
                    updateEnemySpawnState(mutableListOf(), 0, emptyList())
                    showRemoveAllTurnsDialog = false
                },
            )
        }

        if (showSaveAsDialog) {
            SaveAsDialog(
                title = "Save Level As New",
                label = "Level Title",
                currentValue = title,
                onDismiss = { showSaveAsDialog = false },
                onSave = { newTitle ->
                    // Generate ID from title with underscores
                    val sanitizedTitle = newTitle.trim().replace(" ", "_").replace(Regex("[^a-zA-Z0-9_]"), "")
                    val newId =
                        if (sanitizedTitle.isNotEmpty()) {
                            "level_$sanitizedTitle"
                        } else {
                            "level_copy_${Random.nextInt(10000, 99999)}"
                        }
                    val newLevel =
                        level
                            .copy(
                                id = newId,
                                title = newTitle,
                                subtitle = subtitle,
                                author = author,
                                mapId = selectedMapId,
                                startCoins = startCoins.toIntOrNull() ?: 100,
                                startHealthPoints = startHP.toIntOrNull() ?: 10,
                                enemySpawns = emptyList(),
                                spawnGroups = null,
                                availableTowers = availableTowersState,
                                waypoints = waypointsState.toList(),
                                singleHitTargetOrder = singleHitTargetOrder,
                                testingOnly = testingOnly,
                                allowAutoAttack = allowAutoAttack,
                                connectedToPreviousLevel = connectedToPreviousLevel,
                                autoTowerBase = autoTowerBase,
                                autoTowerBaseExtraCost =
                                    autoTowerBaseExtraCost.toIntOrNull()
                                        ?: DEFAULT_AUTO_TOWER_BASE_EXTRA_COST,
                                isSandbox = isSandbox,
                                waaghEnabled = waaghEnabled,
                                supports = supportsState,
                                events = eventsState,
                                initialData = initialDataState,
                            ).let { if (isSandbox) it else it.withSpawnTimeline(spawnTimeline) }
                    onSave(newLevel)
                    showSaveAsDialog = false
                },
            )
        }

        // Warning dialog when saving official level
        if (showOfficialLevelSavedWarning) {
            AlertDialog(
                onDismissRequest = {
                    showOfficialLevelSavedWarning = false
                    pendingLevelToSave = null
                },
                title = { Text(stringResource(Res.string.official_level_saved_warning_title)) },
                text = { Text(stringResource(Res.string.official_level_saved_warning_message)) },
                confirmButton = {
                    Button(onClick = {
                        showOfficialLevelSavedWarning = false
                        // Save the pending level after user acknowledges the warning
                        pendingLevelToSave?.let { onSave(it) }
                        pendingLevelToSave = null
                    }) {
                        Text(stringResource(Res.string.ok))
                    }
                },
            )
        }
    }
}
