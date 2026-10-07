package de.egril.defender.ui.editor.level.enemies

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.EditorEnemySpawn
import de.egril.defender.editor.EditorSpawnLoop
import de.egril.defender.editor.EditorSpawnLoopNode
import de.egril.defender.editor.EditorSpawnTimeline
import de.egril.defender.editor.SpawnGroupIssue
import de.egril.defender.editor.addTurnToLoop
import de.egril.defender.editor.copyLoop
import de.egril.defender.editor.deleteLoopWithTurns
import de.egril.defender.editor.resizeLoop
import de.egril.defender.editor.unwrapLoop
import de.egril.defender.model.SpawnCondition
import de.egril.defender.model.SpawnRepeatMode
import de.egril.defender.model.isRealVillain
import de.egril.defender.ui.editor.level.events.IdDropdown
import de.egril.defender.ui.getLocalizedName
import de.egril.defender.ui.common.SelectableText
import de.egril.defender.ui.icon.TriangleDownIcon
import de.egril.defender.ui.icon.TriangleRightIcon
import defender_of_egril.composeapp.generated.resources.*

/** Width of one nesting level in the spawn list; the loop line is drawn at its left edge. */
private val LoopIndentWidth = 16.dp
private val LoopLineWidth = 4.dp

private val loopLineColors =
    listOf(
        Color(0xFF1E88E5),
        Color(0xFF43A047),
        Color(0xFFFB8C00),
        Color(0xFF8E24AA),
        Color(0xFFE53935),
        Color(0xFF00ACC1),
    )

/** Color of the loop line for a nesting depth (0 = outermost loop). */
internal fun loopLineColor(depth: Int): Color = loopLineColors[depth % loopLineColors.size]

/** One row of the spawn list: a loop header placed before the loop's first turn, or a turn. */
internal sealed interface SpawnEditorRow {
    data class LoopHeader(
        val node: EditorSpawnLoopNode,
    ) : SpawnEditorRow

    data class Turn(
        val turn: Int,
        val depth: Int,
    ) : SpawnEditorRow
}

/** Builds the mixed list of loop headers and turns; a turn's depth is the number of loops containing it. */
internal fun buildSpawnEditorRows(
    nodes: List<EditorSpawnLoopNode>,
    maxTurn: Int,
): List<SpawnEditorRow> =
    buildList {
        for (turn in 1..maxTurn) {
            nodes.filter { it.loop.startTurn == turn }.forEach { add(SpawnEditorRow.LoopHeader(it)) }
            add(SpawnEditorRow.Turn(turn, nodes.count { it.loop.contains(turn) }))
        }
    }

/**
 * Indents [content] by [depth] nesting levels and draws one colored vertical line per level.
 * Rows have no outer spacing, so the lines of consecutive rows join into continuous loop bars.
 */
@Composable
internal fun SpawnLoopLines(
    depth: Int,
    ownLineStartsHere: Boolean = false,
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    val lineWidth = LoopLineWidth.toPx()
                    val indent = LoopIndentWidth.toPx()
                    for (level in 0 until depth) {
                        // The line of a loop starts at its header instead of above it.
                        val top = if (ownLineStartsHere && level == depth - 1) 4.dp.toPx() else 0f
                        drawRect(
                            color = loopLineColor(level),
                            topLeft = Offset(level * indent, top),
                            size = Size(lineWidth, size.height - top),
                        )
                    }
                }.padding(start = LoopIndentWidth * depth, top = 4.dp, bottom = 4.dp),
    ) {
        content()
    }
}

@Composable
internal fun SpawnLoopIssues(issues: Set<SpawnGroupIssue>) {
    spawnIssueTexts(issues).forEach { SelectableText(it, color = MaterialTheme.colorScheme.error) }
}

/** Localized descriptions of [issues]. */
@Composable
internal fun spawnIssueTexts(issues: Set<SpawnGroupIssue>): List<String> =
    issues.map { issue ->
        val resource =
            when (issue) {
                SpawnGroupIssue.STRUCTURE -> Res.string.spawn_error_structure
                SpawnGroupIssue.GROUP_ID -> Res.string.spawn_error_group_id
                SpawnGroupIssue.TURNS -> Res.string.spawn_error_turns
                SpawnGroupIssue.REPEAT_COUNT -> Res.string.spawn_error_repeat
                SpawnGroupIssue.SPAWN_VALUES -> Res.string.spawn_error_values
                SpawnGroupIssue.UNIT_ID -> Res.string.spawn_error_unit_id
                SpawnGroupIssue.CONDITION_TARGET -> Res.string.spawn_error_target
                SpawnGroupIssue.SPAWN_POINT -> Res.string.spawn_error_point
                SpawnGroupIssue.VILLAIN -> Res.string.spawn_error_villain
                SpawnGroupIssue.INFINITE_NOT_STOPPED -> Res.string.spawn_error_infinite_not_stopped
            }
        stringResource(resource)
    }

@Composable
internal fun loopSummary(loop: EditorSpawnLoop): String =
    when (loop.repeatMode) {
        SpawnRepeatMode.COUNT -> stringResource(Res.string.spawn_loop_summary_count, loop.repeatCount)
        SpawnRepeatMode.CONDITION -> stringResource(Res.string.spawn_loop_summary_condition, loop.targetUnitId.orEmpty())
        SpawnRepeatMode.INFINITE -> stringResource(Res.string.spawn_repeat_infinite)
    }

/** Header of a loop in the spawn list; collapsed it shows a summary, expanded all loop settings. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SpawnLoopHeader(
    node: EditorSpawnLoopNode,
    timeline: EditorSpawnTimeline,
    expanded: Boolean,
    onToggle: () -> Unit,
    onTimelineChange: (EditorSpawnTimeline) -> Unit,
    onLoopRenamed: (oldId: String, newId: String) -> Unit,
) {
    val loop = node.loop
    val color = loopLineColor(node.depth)

    fun updateLoop(updated: EditorSpawnLoop) {
        onTimelineChange(timeline.copy(loops = timeline.loops.map { if (it.id == loop.id) updated else it }))
    }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("spawn_loop_header_${loop.id}"),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
    ) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onToggle() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (expanded) TriangleDownIcon(size = 16.dp) else TriangleRightIcon(size = 16.dp)
                Text(
                    text = stringResource(Res.string.spawn_loop_title, loop.id),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = color,
                )
                Text(
                    text = "${loopSummary(loop)} | ${stringResource(Res.string.spawn_loop_turn_range, loop.startTurn, loop.endTurn)}",
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = { onTimelineChange(timeline.copyLoop(loop.id)) },
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                ) {
                    Text(stringResource(Res.string.spawn_loop_copy), fontSize = 12.sp)
                }
            }
            if (expanded) {
                SpawnLoopSettings(loop, timeline, ::updateLoop, onTimelineChange, onLoopRenamed)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpawnLoopSettings(
    loop: EditorSpawnLoop,
    timeline: EditorSpawnTimeline,
    updateLoop: (EditorSpawnLoop) -> Unit,
    onTimelineChange: (EditorSpawnTimeline) -> Unit,
    onLoopRenamed: (oldId: String, newId: String) -> Unit,
) {
    OutlinedTextField(
        value = loop.id,
        onValueChange = { newId ->
            updateLoop(loop.copy(id = newId))
            onLoopRenamed(loop.id, newId)
        },
        label = { Text(stringResource(Res.string.spawn_loop_id)) },
        singleLine = true,
        isError = loop.id.isBlank() || timeline.loops.count { it.id == loop.id } > 1,
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SpawnRepeatMode.entries.forEach { mode ->
            val label =
                when (mode) {
                    SpawnRepeatMode.COUNT -> Res.string.spawn_repeat_count
                    SpawnRepeatMode.CONDITION -> Res.string.spawn_repeat_condition
                    SpawnRepeatMode.INFINITE -> Res.string.spawn_repeat_infinite
                }
            FilterChip(
                selected = loop.repeatMode == mode,
                onClick = {
                    updateLoop(
                        loop.copy(
                            repeatMode = mode,
                            condition = if (mode == SpawnRepeatMode.CONDITION) SpawnCondition.UNIT_ALIVE else null,
                        ),
                    )
                },
                label = { Text(stringResource(label)) },
            )
        }
    }
    when (loop.repeatMode) {
        SpawnRepeatMode.COUNT ->
            SpawnNumberField(loop.repeatCount, stringResource(Res.string.spawn_iterations)) {
                updateLoop(loop.copy(repeatCount = it))
            }
        SpawnRepeatMode.CONDITION -> {
            SelectableText(stringResource(Res.string.spawn_condition_help))
            val unitOptions =
                timeline.spawns
                    .mapNotNull { spawn -> spawn.loopUnitId?.let { it to spawn.attackerType } }
                    .distinctBy { it.first }
                    .sortedBy { it.first }
                    .map { (unitId, type) -> unitId to "$unitId (${type.getLocalizedName()})" }
            Box(modifier = Modifier.testTag("spawn_target_unit_dropdown")) {
                IdDropdown(
                    label = stringResource(Res.string.spawn_target_unit),
                    options = unitOptions,
                    selectedId = loop.targetUnitId,
                    onSelected = { updateLoop(loop.copy(targetUnitId = it)) },
                )
            }
            if (unitOptions.isEmpty()) {
                SelectableText(stringResource(Res.string.spawn_no_unit_ids), color = MaterialTheme.colorScheme.error)
            }
        }
        SpawnRepeatMode.INFINITE -> SelectableText(stringResource(Res.string.spawn_infinite_help))
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LoopBoundStepper(
            label = stringResource(Res.string.spawn_loop_start, loop.startTurn),
            onDecrease = timeline.resizeLoop(loop.id, loop.startTurn - 1, loop.endTurn),
            onIncrease = timeline.resizeLoop(loop.id, loop.startTurn + 1, loop.endTurn),
            onTimelineChange = onTimelineChange,
        )
        LoopBoundStepper(
            label = stringResource(Res.string.spawn_loop_end, loop.endTurn),
            onDecrease = timeline.resizeLoop(loop.id, loop.startTurn, loop.endTurn - 1),
            onIncrease = timeline.resizeLoop(loop.id, loop.startTurn, loop.endTurn + 1),
            onTimelineChange = onTimelineChange,
        )
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(onClick = { onTimelineChange(timeline.addTurnToLoop(loop.id)) }) {
            Text(stringResource(Res.string.spawn_loop_add_turn))
        }
        OutlinedButton(onClick = { onTimelineChange(timeline.unwrapLoop(loop.id)) }) {
            Text(stringResource(Res.string.spawn_loop_unwrap))
        }
        Button(
            onClick = { onTimelineChange(timeline.deleteLoopWithTurns(loop.id)) },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
            Text(stringResource(Res.string.spawn_loop_delete))
        }
    }
}

/** Moves a loop boundary by one turn; a direction is disabled if it would break the nesting. */
@Composable
private fun LoopBoundStepper(
    label: String,
    onDecrease: EditorSpawnTimeline?,
    onIncrease: EditorSpawnTimeline?,
    onTimelineChange: (EditorSpawnTimeline) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, modifier = Modifier.width(120.dp))
        OutlinedButton(
            onClick = { onDecrease?.let(onTimelineChange) },
            enabled = onDecrease != null,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        ) { Text("-") }
        OutlinedButton(
            onClick = { onIncrease?.let(onTimelineChange) },
            enabled = onIncrease != null,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        ) { Text("+") }
    }
}

@Composable
internal fun SpawnNumberField(
    value: Int,
    label: String,
    modifier: Modifier = Modifier,
    onChange: (Int) -> Unit,
) {
    var text by remember { mutableStateOf(value.toString()) }
    LaunchedEffect(value) {
        if (text.toIntOrNull() != value && !(value == 0 && text.toIntOrNull() == null)) text = value.toString()
    }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onChange(it.toIntOrNull() ?: 0)
        },
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        isError = value < 1,
    )
}

/** Edits the loop-related options of a single enemy: its unit id and whether it only spawns in the first iteration. */
@Composable
internal fun SpawnLoopOptionsDialog(
    spawn: EditorEnemySpawn,
    otherUnitIds: Set<String>,
    onDismiss: () -> Unit,
    onApply: (unitId: String?, firstIterationOnly: Boolean) -> Unit,
) {
    var unitId by remember(spawn) { mutableStateOf(spawn.loopUnitId.orEmpty()) }
    var firstOnly by remember(spawn) { mutableStateOf(spawn.spawnsInFirstIterationOnly) }
    val isVillain = spawn.attackerType.isRealVillain
    val validId = unitId.isEmpty() || unitId.isNotBlank() && unitId == unitId.trim() && unitId !in otherUnitIds

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.spawn_loop_options)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = unitId,
                    onValueChange = { unitId = it },
                    readOnly = isVillain,
                    enabled = !isVillain,
                    label = { Text(stringResource(Res.string.spawn_unit_id)) },
                    singleLine = true,
                    isError = !validId,
                    modifier = Modifier.testTag("spawn_unit_id_field"),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = firstOnly || isVillain,
                        onCheckedChange = { firstOnly = it },
                        enabled = !isVillain,
                        modifier = Modifier.testTag("spawn_first_only_checkbox"),
                    )
                    Text(stringResource(Res.string.spawn_first_only))
                }
                if (!validId) {
                    Text(stringResource(Res.string.spawn_error_unit_id), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = validId,
                onClick = { onApply(unitId.takeIf { it.isNotEmpty() }, firstOnly || isVillain) },
            ) { Text(stringResource(Res.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}
