@file:OptIn(ExperimentalMaterial3Api::class)

package de.egril.defender.ui.editor.level.enemies

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.EditorEnemySpawn
import de.egril.defender.editor.EditorEnemyTemplateKind
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.EditorSpawnTimeline
import de.egril.defender.editor.EditorStorage
import de.egril.defender.editor.SpawnGroupIssue
import de.egril.defender.editor.createLoop
import de.egril.defender.editor.deleteTurn
import de.egril.defender.editor.resolveLoopNesting
import de.egril.defender.editor.SpawnPointUtils
import de.egril.defender.editor.SpawnTurnTemplateDefinition
import de.egril.defender.editor.SpawnTurnTemplateEntry
import de.egril.defender.editor.SpawnTurnTemplateVariant
import de.egril.defender.model.AttackerType
import de.egril.defender.ui.common.SelectableText
import de.egril.defender.ui.editor.level.ChangeAllSpawnPointsDialog
import de.egril.defender.ui.editor.level.ChangeLevelDialog
import de.egril.defender.ui.editor.level.ChangeSpawnPointDialog
import de.egril.defender.ui.editor.level.ChangeTurnLevelDialog
import de.egril.defender.ui.editor.level.SpawnTurnSection
import de.egril.defender.ui.icon.PlusIcon
import de.egril.defender.ui.icon.WarningIcon
import defender_of_egril.composeapp.generated.resources.*

/**
 * Tab 2: Enemy Spawns
 */
@Composable
internal fun EnemySpawnsTab(
    timeline: EditorSpawnTimeline,
    issues: Set<SpawnGroupIssue>,
    onTimelineChange: (EditorSpawnTimeline) -> Unit,
    onShowEnemyDialog: (Int) -> Unit,
    onShowRemoveAllTurnsDialog: () -> Unit,
    map: EditorMap?,
    onApplyTemplate: (SpawnTurnTemplateDefinition, EditorEnemyTemplateKind, Int) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    requestedTurnToOpen: Int?,
    turnOpenRequestNonce: Int,
) {
    val enemySpawns = timeline.spawns.toMutableList()
    val maxTurnNumber = timeline.maxTurn

    fun onEnemySpawnsChange(newSpawns: MutableList<EditorEnemySpawn>) = onTimelineChange(timeline.copy(spawns = newSpawns))

    // Loop headers are collapsed by default; expanding one shows the loop's own settings.
    var expandedLoops by remember { mutableStateOf(setOf<String>()) }
    var spawnForLoopOptions by remember { mutableStateOf<EditorEnemySpawn?>(null) }
    val loopNodes = remember(timeline.loops) { resolveLoopNesting(timeline.loops) }
    val rows = remember(loopNodes, maxTurnNumber) { buildSpawnEditorRows(loopNodes, maxTurnNumber) }

    // Track the last added turn to keep it expanded
    var lastAddedTurn by remember { mutableStateOf<Int?>(null) }
    val listState = rememberLazyListState()
    val templates = EditorStorage.getSpawnTurnTemplates()
    var selectedEnemyKind by remember { mutableStateOf(EditorEnemyTemplateKind.MIXED) }
    var selectedEnemyLevel by remember { mutableStateOf("1") }
    var kindExpanded by remember { mutableStateOf(false) }
    val visibleTemplates =
        remember(templates, selectedEnemyKind) {
            templates.filter { template -> template.variantFor(selectedEnemyKind) != null }
        }

    // Track spawn point change dialog
    var spawnToChange by remember { mutableStateOf<EditorEnemySpawn?>(null) }

    // Track level change dialog
    var spawnToChangeLevel by remember { mutableStateOf<EditorEnemySpawn?>(null) }

    // Track turn level change dialog
    var turnToChangeLevel by remember { mutableStateOf<Int?>(null) }

    // Track bulk spawn point change dialog
    var showChangeAllSpawnPointsDialog by remember { mutableStateOf(false) }
    var tableMode by remember { mutableStateOf(false) }
    var turnToSaveTemplate by remember { mutableStateOf<Int?>(null) }
    var templateName by remember { mutableStateOf("") }
    var templateDescription by remember { mutableStateOf("") }

    // Check if any enemies are spawned outside valid spawn points
    val mapSpawnPoints = remember(map) { map?.getSpawnPoints()?.toSet() ?: emptySet() }
    val hasEnemiesOutsideSpawnPoints =
        remember(enemySpawns, mapSpawnPoints) {
            enemySpawns.any { spawn ->
                spawn.spawnPoint != null && spawn.spawnPoint !in mapSpawnPoints
            }
        }

    LaunchedEffect(turnOpenRequestNonce) {
        val targetTurn = requestedTurnToOpen ?: return@LaunchedEffect
        val leadingItems = 3 + (if (hasEnemiesOutsideSpawnPoints) 1 else 0)
        val rowIndex = rows.indexOfFirst { it is SpawnEditorRow.Turn && it.turn == targetTurn }
        if (rowIndex >= 0) listState.scrollToItem(leadingItems + rowIndex)
    }

    // Items are not spaced by the list itself so the loop lines on the left stay continuous.
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        // Add turn and remove all turns buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${stringResource(Res.string.enemies)} (${enemySpawns.size}):",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(onClick = {
                        // Add a new empty turn without opening dialog
                        val newTurn = maxTurnNumber + 1
                        onTimelineChange(timeline.copy(maxTurn = newTurn))
                        lastAddedTurn = newTurn
                    }) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            PlusIcon(size = 16.dp)
                            Text(stringResource(Res.string.add_turn))
                        }
                    }

                    Button(
                        onClick = onShowRemoveAllTurnsDialog,
                        enabled = enemySpawns.isNotEmpty() || maxTurnNumber > 0,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    if (enemySpawns.isNotEmpty() || maxTurnNumber > 0) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                            ),
                    ) {
                        Text(stringResource(Res.string.remove_all_turns))
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(Res.string.spawn_turn_templates),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    ExposedDropdownMenuBox(
                        expanded = kindExpanded,
                        onExpandedChange = { kindExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = selectedEnemyKind.localizedLabel(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(Res.string.enemy_kind)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = kindExpanded) },
                            modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true),
                        )
                        ExposedDropdownMenu(
                            expanded = kindExpanded,
                            onDismissRequest = { kindExpanded = false },
                        ) {
                            EditorEnemyTemplateKind.entries.forEach { kind ->
                                DropdownMenuItem(
                                    text = { Text(kind.localizedLabel()) },
                                    onClick = {
                                        selectedEnemyKind = kind
                                        kindExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = selectedEnemyLevel,
                        onValueChange = { if (it.all(Char::isDigit)) selectedEnemyLevel = it },
                        label = { Text(stringResource(Res.string.enemy_level)) },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(onClick = onUndo, enabled = canUndo, modifier = Modifier.weight(1f)) {
                        Text(stringResource(Res.string.undo))
                    }
                    Button(onClick = onRedo, enabled = canRedo, modifier = Modifier.weight(1f)) {
                        Text(stringResource(Res.string.redo))
                    }
                    Button(
                        onClick = { tableMode = !tableMode },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            if (tableMode) {
                                stringResource(Res.string.turn_mode)
                            } else {
                                stringResource(Res.string.table_mode)
                            },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (visibleTemplates.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.no_spawn_templates_available),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        visibleTemplates.forEach { template ->
                            Button(onClick = { onApplyTemplate(template, selectedEnemyKind, selectedEnemyLevel.toIntOrNull() ?: 1) }) {
                                Text(template.name)
                            }
                        }
                    }
                }
            }
        }

        // Warning card and button if enemies are outside spawn points
        if (hasEnemiesOutsideSpawnPoints) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            WarningIcon(size = 20.dp)
                            Text(
                                text = stringResource(Res.string.spawn_point_warning),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        Button(
                            onClick = { showChangeAllSpawnPointsDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                ),
                        ) {
                            Text(stringResource(Res.string.change_all_spawn_points))
                        }
                    }
                }
            }
        }

        item {
            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SelectableText(stringResource(Res.string.spawn_loops_help))
                SpawnLoopIssues(issues)
            }
        }

        // Group enemies by spawn turn
        val turnGroups = enemySpawns.groupBy { it.spawnTurn }

        if (tableMode) {
            item {
                EnemySpawnTableEditor(
                    enemySpawns = enemySpawns,
                    map = map,
                    onEnemySpawnsChange = { onEnemySpawnsChange(it) },
                )
            }
        } else {
            rows.forEach { row ->
                when (row) {
                    is SpawnEditorRow.LoopHeader ->
                        item(key = "loop_${row.node.loop.id}") {
                            SpawnLoopLines(depth = row.node.depth + 1, ownLineStartsHere = true) {
                                SpawnLoopHeader(
                                    node = row.node,
                                    timeline = timeline,
                                    expanded = row.node.loop.id in expandedLoops,
                                    onToggle = {
                                        val id = row.node.loop.id
                                        expandedLoops = if (id in expandedLoops) expandedLoops - id else expandedLoops + id
                                    },
                                    onTimelineChange = onTimelineChange,
                                    onLoopRenamed = { oldId, newId ->
                                        if (oldId in expandedLoops) expandedLoops = expandedLoops - oldId + newId
                                    },
                                )
                            }
                        }
                    is SpawnEditorRow.Turn -> {
                        val turn = row.turn
                        val spawnsInTurn = turnGroups[turn].orEmpty()
                        item(key = "turn_$turn") {
                            SpawnLoopLines(depth = row.depth) {
                                SpawnTurnSection(
                                    turn = turn,
                                    spawns = spawnsInTurn,
                                    initiallyExpanded = turn == lastAddedTurn,
                                    expandRequestKey = if (requestedTurnToOpen == turn) turnOpenRequestNonce else null,
                                    onRemoveEnemy = { spawn ->
                                        val newSpawns = enemySpawns.toMutableList().apply { remove(spawn) }
                                        onEnemySpawnsChange(newSpawns)
                                    },
                                    onDeleteTurn = {
                                        if (turn == maxTurnNumber) {
                                            onTimelineChange(timeline.deleteTurn(turn))
                                        }
                                    },
                                    onClearTurn = {
                                        onEnemySpawnsChange(enemySpawns.filter { it.spawnTurn != turn }.toMutableList())
                                    },
                                    canDeleteTurn = turn == maxTurnNumber,
                                    onCopyTurn = {
                                        // Unit ids must stay unique, so the copy does not take them over.
                                        onTimelineChange(
                                            timeline.copy(
                                                spawns = enemySpawns + spawnsInTurn.map { it.copy(spawnTurn = maxTurnNumber + 1, unitId = null) },
                                                maxTurn = maxTurnNumber + 1,
                                            ),
                                        )
                                    },
                                    onAddEnemy = { onShowEnemyDialog(turn) },
                                    onMoveTurnUp = {
                                        if (turn > 1) onEnemySpawnsChange(swapTurns(enemySpawns, turn, turn - 1))
                                    },
                                    onMoveTurnDown = {
                                        if (turn < maxTurnNumber) onEnemySpawnsChange(swapTurns(enemySpawns, turn, turn + 1))
                                    },
                                    canMoveUp = turn > 1,
                                    canMoveDown = turn < maxTurnNumber,
                                    onChangeSpawnPoint = { spawn ->
                                        spawnToChange = spawn
                                    },
                                    onChangeLevel = { spawn ->
                                        spawnToChangeLevel = spawn
                                    },
                                    onChangeTurnLevel = {
                                        turnToChangeLevel = turn
                                    },
                                    onSaveAsTemplate = {
                                        turnToSaveTemplate = turn
                                        templateName = "Turn $turn"
                                        templateDescription = ""
                                    },
                                    onCreateLoop = { onTimelineChange(timeline.createLoop(turn)) },
                                    onEditLoopOptions =
                                        if (timeline.loops.isNotEmpty()) {
                                            { spawn -> spawnForLoopOptions = spawn }
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

    spawnForLoopOptions?.let { spawn ->
        SpawnLoopOptionsDialog(
            spawn = spawn,
            otherUnitIds = enemySpawns.filter { it !== spawn }.mapNotNull { it.loopUnitId }.toSet(),
            onDismiss = { spawnForLoopOptions = null },
            onApply = { unitId, firstIterationOnly ->
                onEnemySpawnsChange(
                    enemySpawns
                        .map {
                            if (it === spawn) it.copy(unitId = unitId, firstIterationOnly = firstIterationOnly) else it
                        }.toMutableList(),
                )
                spawnForLoopOptions = null
            },
        )
    }

    // Change spawn point dialog
    spawnToChange?.let { spawn ->
        ChangeSpawnPointDialog(
            spawn = spawn,
            map = map,
            onDismiss = { spawnToChange = null },
            onChange = { newSpawnPoint ->
                val newSpawns =
                    enemySpawns
                        .map {
                            if (it === spawn) {
                                it.copy(spawnPoint = newSpawnPoint)
                            } else {
                                it
                            }
                        }.toMutableList()
                onEnemySpawnsChange(newSpawns)
                spawnToChange = null
            },
        )
    }

    // Change level dialog
    spawnToChangeLevel?.let { spawn ->
        ChangeLevelDialog(
            spawn = spawn,
            onDismiss = { spawnToChangeLevel = null },
            onChange = { newLevel ->
                val newSpawns =
                    enemySpawns
                        .map {
                            if (it === spawn) {
                                it.copy(level = newLevel)
                            } else {
                                it
                            }
                        }.toMutableList()
                onEnemySpawnsChange(newSpawns)
                spawnToChangeLevel = null
            },
        )
    }

    // Change turn level dialog
    turnToChangeLevel?.let { turn ->
        val spawnsInTurn = enemySpawns.filter { it.spawnTurn == turn }
        ChangeTurnLevelDialog(
            turn = turn,
            spawns = spawnsInTurn,
            onDismiss = { turnToChangeLevel = null },
            onChange = { newLevel ->
                val newSpawns =
                    enemySpawns
                        .map {
                            if (it.spawnTurn == turn) {
                                it.copy(level = newLevel)
                            } else {
                                it
                            }
                        }.toMutableList()
                onEnemySpawnsChange(newSpawns)
                turnToChangeLevel = null
            },
        )
    }

    // Change all spawn points dialog
    if (showChangeAllSpawnPointsDialog) {
        ChangeAllSpawnPointsDialog(
            enemySpawns = enemySpawns,
            map = map,
            onDismiss = { showChangeAllSpawnPointsDialog = false },
            onApply = { remappings ->
                // Apply remappings in correct order to avoid conflicts
                // We need to handle cases where a "from" position is also a "to" position
                val orderedRemappings = SpawnPointUtils.computeRemappingOrder(remappings)

                // Apply remappings
                val newSpawns =
                    enemySpawns
                        .map { spawn ->
                            spawn.spawnPoint?.let { spawnPoint ->
                                val newPoint = orderedRemappings[spawnPoint]
                                if (newPoint != null && newPoint != spawnPoint) {
                                    spawn.copy(spawnPoint = newPoint)
                                } else {
                                    spawn
                                }
                            } ?: spawn
                        }.toMutableList()

                onEnemySpawnsChange(newSpawns)
                showChangeAllSpawnPointsDialog = false
            },
        )
    }

    turnToSaveTemplate?.let { turn ->
        val spawnsInTurn = enemySpawns.filter { it.spawnTurn == turn }
        AlertDialog(
            onDismissRequest = { turnToSaveTemplate = null },
            title = { Text(stringResource(Res.string.save_spawn_template)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = templateName,
                        onValueChange = { templateName = it },
                        label = { Text(stringResource(Res.string.template_name)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = templateDescription,
                        onValueChange = { templateDescription = it },
                        label = { Text(stringResource(Res.string.template_description)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val templateId =
                            templateName
                                .trim()
                                .lowercase()
                                .replace(" ", "_")
                                .replace(Regex("[^a-z0-9_]"), "")
                                .ifBlank { "spawn_template_$turn" }
                        val baseLevel = spawnsInTurn.minOfOrNull { it.level } ?: 1
                        EditorStorage.saveSpawnTurnTemplate(
                            SpawnTurnTemplateDefinition(
                                id = templateId,
                                name = templateName.ifBlank { "Turn $turn" },
                                description = templateDescription,
                                variants =
                                    listOf(
                                        SpawnTurnTemplateVariant(
                                            kind = selectedEnemyKind,
                                            entries =
                                                spawnsInTurn.map { spawn ->
                                                    SpawnTurnTemplateEntry(
                                                        attackerType = spawn.attackerType,
                                                        turnOffset = 0,
                                                        amount = 1,
                                                        levelOffset = spawn.level - baseLevel,
                                                    )
                                                },
                                        ),
                                    ),
                            ),
                        )
                        turnToSaveTemplate = null
                    },
                    enabled = spawnsInTurn.isNotEmpty(),
                ) {
                    Text(stringResource(Res.string.save))
                }
            },
            dismissButton = {
                Button(onClick = { turnToSaveTemplate = null }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun EnemySpawnTableEditor(
    enemySpawns: MutableList<EditorEnemySpawn>,
    map: EditorMap?,
    onEnemySpawnsChange: (MutableList<EditorEnemySpawn>) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        enemySpawns.sortedWith(compareBy(EditorEnemySpawn::spawnTurn, { it.attackerType.displayName })).forEach { spawn ->
            EnemySpawnTableRow(
                spawn = spawn,
                map = map,
                onChange = { updatedSpawn ->
                    onEnemySpawnsChange(
                        enemySpawns
                            .map {
                                if (it === spawn) updatedSpawn else it
                            }.toMutableList(),
                    )
                },
                onDelete = {
                    onEnemySpawnsChange(enemySpawns.toMutableList().apply { remove(spawn) })
                },
            )
        }
    }
}

@Composable
private fun EnemySpawnTableRow(
    spawn: EditorEnemySpawn,
    map: EditorMap?,
    onChange: (EditorEnemySpawn) -> Unit,
    onDelete: () -> Unit,
) {
    var typeExpanded by remember { mutableStateOf(false) }
    var spawnExpanded by remember { mutableStateOf(false) }
    val compatibleSpawnPoints = remember(map, spawn.attackerType) { map?.getCompatibleSpawnPoints(spawn.attackerType).orEmpty() }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                OutlinedTextField(
                    value = spawn.attackerType.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(Res.string.enemy)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    modifier = Modifier.weight(1f).menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true),
                )
                ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                    AttackerType.entries.forEach { attackerType ->
                        val points = map?.getCompatibleSpawnPoints(attackerType).orEmpty()
                        DropdownMenuItem(
                            text = { Text(attackerType.displayName) },
                            enabled = points.isNotEmpty(),
                            onClick = {
                                onChange(
                                    spawn.copy(
                                        attackerType = attackerType,
                                        spawnPoint = spawn.spawnPoint?.let { it.takeIf { point -> point in points } ?: points.first() },
                                    ),
                                )
                                typeExpanded = false
                            },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = spawn.spawnTurn.toString(),
                onValueChange = { value -> value.toIntOrNull()?.let { onChange(spawn.copy(spawnTurn = it.coerceAtLeast(1))) } },
                label = { Text(stringResource(Res.string.turn_label)) },
                modifier = Modifier.weight(0.6f),
                singleLine = true,
            )
            OutlinedTextField(
                value = spawn.level.toString(),
                onValueChange = { value -> value.toIntOrNull()?.let { onChange(spawn.copy(level = it.coerceAtLeast(1))) } },
                label = { Text(stringResource(Res.string.level)) },
                modifier = Modifier.weight(0.6f),
                singleLine = true,
            )
            ExposedDropdownMenuBox(expanded = spawnExpanded, onExpandedChange = { spawnExpanded = it }) {
                OutlinedTextField(
                    value = spawn.spawnPoint?.let { "${it.x},${it.y}" } ?: stringResource(Res.string.auto_spawn_point),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(Res.string.spawn_point)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = spawnExpanded) },
                    modifier = Modifier.weight(1f).menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true),
                )
                ExposedDropdownMenu(expanded = spawnExpanded, onDismissRequest = { spawnExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.auto_spawn_point)) },
                        onClick = {
                            onChange(spawn.copy(spawnPoint = null))
                            spawnExpanded = false
                        },
                    )
                    compatibleSpawnPoints.forEach { spawnPoint ->
                        DropdownMenuItem(
                            text = { Text("${spawnPoint.x},${spawnPoint.y}") },
                            onClick = {
                                onChange(spawn.copy(spawnPoint = spawnPoint))
                                spawnExpanded = false
                            },
                        )
                    }
                }
            }
            Button(onClick = onDelete, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text(stringResource(Res.string.remove))
            }
        }
    }
}

@Composable
private fun EditorEnemyTemplateKind.localizedLabel(): String =
    when (this) {
        EditorEnemyTemplateKind.MIXED -> stringResource(Res.string.all_enemy_kinds)
        EditorEnemyTemplateKind.HORDE -> stringResource(Res.string.horde)
        EditorEnemyTemplateKind.UNDEAD -> stringResource(Res.string.undead)
        EditorEnemyTemplateKind.DARK_MAGIC -> stringResource(Res.string.dark_magic)
        EditorEnemyTemplateKind.DEMONIC -> stringResource(Res.string.demonic)
        EditorEnemyTemplateKind.PIRATES -> stringResource(Res.string.pirates)
        EditorEnemyTemplateKind.VILLAINS -> stringResource(Res.string.villains)
    }

/** Swaps the enemies of two turns. */
private fun swapTurns(
    spawns: List<EditorEnemySpawn>,
    first: Int,
    second: Int,
): MutableList<EditorEnemySpawn> =
    spawns
        .map { spawn ->
            when (spawn.spawnTurn) {
                first -> spawn.copy(spawnTurn = second)
                second -> spawn.copy(spawnTurn = first)
                else -> spawn
            }
        }.toMutableList()
