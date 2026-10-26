package de.egril.defender.ui.editor.level.events

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.model.EventAction
import de.egril.defender.model.EventActionType
import de.egril.defender.model.EventLoop
import de.egril.defender.model.EventLoopStep
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.add_action
import defender_of_egril.composeapp.generated.resources.event_loop_add_step
import defender_of_egril.composeapp.generated.resources.event_loop_delete_step
import defender_of_egril.composeapp.generated.resources.event_loop_enabled_label
import defender_of_egril.composeapp.generated.resources.event_loop_endless_label
import defender_of_egril.composeapp.generated.resources.event_loop_help
import defender_of_egril.composeapp.generated.resources.event_loop_invalid_error
import defender_of_egril.composeapp.generated.resources.event_loop_nested_label
import defender_of_egril.composeapp.generated.resources.event_loop_repeat_label
import defender_of_egril.composeapp.generated.resources.event_loop_step_label
import defender_of_egril.composeapp.generated.resources.event_loop_unreachable_hint
import defender_of_egril.composeapp.generated.resources.event_loop_wait_label

/** Maximum nesting depth offered in the editor; keeps the UI usable (the runtime has no limit). */
private const val MAX_EDITOR_LOOP_DEPTH = 4

/**
 * Loop section of an event card: a toggle that adds/removes the event's [EventLoop] and, when
 * present, the recursive loop editor with validation feedback.
 */
@Composable
internal fun EventLoopSection(
    loop: EventLoop?,
    onLoopChange: (EventLoop?) -> Unit,
    context: EventEditorContext,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Checkbox(
                checked = loop != null,
                onCheckedChange = { enabled -> onLoopChange(if (enabled) defaultLoop() else null) },
            )
            Text(stringResource(Res.string.event_loop_enabled_label))
        }
        Text(
            text = stringResource(Res.string.event_loop_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (loop != null) {
            if (!loop.isValid()) {
                Text(
                    text = stringResource(Res.string.event_loop_invalid_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (loop.hasUnreachableSteps()) {
                Text(
                    text = stringResource(Res.string.event_loop_unreachable_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            EventLoopEditor(loop = loop, onLoopChange = { onLoopChange(it) }, context = context, depth = 0)
        }
    }
}

private fun defaultLoop(): EventLoop = EventLoop(steps = listOf(EventLoopStep(waitTurns = 1)), repeatCount = 0)

@Composable
private fun EventLoopEditor(
    loop: EventLoop,
    onLoopChange: (EventLoop) -> Unit,
    context: EventEditorContext,
    depth: Int,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Checkbox(
                checked = loop.isEndless,
                onCheckedChange = { endless -> onLoopChange(loop.copy(repeatCount = if (endless) 0 else 2)) },
            )
            Text(stringResource(Res.string.event_loop_endless_label))
            if (!loop.isEndless) {
                NumberField(
                    label = stringResource(Res.string.event_loop_repeat_label),
                    value = loop.repeatCount,
                    // 0 would silently switch to endless; keep at least one repetition while the switch is off.
                    onValueChange = { onLoopChange(loop.copy(repeatCount = it.coerceAtLeast(1))) },
                )
            }
        }

        loop.steps.forEachIndexed { stepIndex, step ->
            EventLoopStepEditor(
                stepNumber = stepIndex + 1,
                step = step,
                context = context,
                depth = depth,
                onStepChange = { newStep ->
                    val updated = loop.steps.toMutableList()
                    updated[stepIndex] = newStep
                    onLoopChange(loop.copy(steps = updated))
                },
                onDelete = {
                    val updated = loop.steps.toMutableList()
                    updated.removeAt(stepIndex)
                    onLoopChange(loop.copy(steps = updated))
                },
            )
        }

        OutlinedButton(onClick = { onLoopChange(loop.copy(steps = loop.steps + EventLoopStep(waitTurns = 1))) }) {
            Text(stringResource(Res.string.event_loop_add_step))
        }
    }
}

@Composable
private fun EventLoopStepEditor(
    stepNumber: Int,
    step: EventLoopStep,
    context: EventEditorContext,
    depth: Int,
    onStepChange: (EventLoopStep) -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(Res.string.event_loop_step_label, stepNumber),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            OutlinedButton(onClick = onDelete) {
                Text(stringResource(Res.string.event_loop_delete_step))
            }
        }

        NumberField(
            label = stringResource(Res.string.event_loop_wait_label),
            value = step.waitTurns,
            onValueChange = { onStepChange(step.copy(waitTurns = it)) },
        )

        step.actions.forEachIndexed { actionIndex, action ->
            ActionEditor(
                action = action,
                context = context,
                onActionChange = { newAction ->
                    val updated = step.actions.toMutableList()
                    updated[actionIndex] = newAction
                    onStepChange(step.copy(actions = updated))
                },
                onDelete = {
                    val updated = step.actions.toMutableList()
                    updated.removeAt(actionIndex)
                    onStepChange(step.copy(actions = updated))
                },
            )
        }
        OutlinedButton(onClick = { onStepChange(step.copy(actions = step.actions + defaultStepAction(context))) }) {
            Text(stringResource(Res.string.add_action))
        }

        MessageDropdown(
            selectedKey = step.messageKey,
            onKeyChange = { onStepChange(step.copy(messageKey = it)) },
            selectedFrame = step.messageFrame,
            onFrameChange = { onStepChange(step.copy(messageFrame = it)) },
        )

        if (depth + 1 < MAX_EDITOR_LOOP_DEPTH || step.nestedLoop != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Checkbox(
                    checked = step.nestedLoop != null,
                    onCheckedChange = { enabled ->
                        onStepChange(step.copy(nestedLoop = if (enabled) defaultLoop().copy(repeatCount = 2) else null))
                    },
                )
                Text(stringResource(Res.string.event_loop_nested_label))
            }
            step.nestedLoop?.let { nested ->
                EventLoopEditor(
                    loop = nested,
                    onLoopChange = { onStepChange(step.copy(nestedLoop = it)) },
                    context = context,
                    depth = depth + 1,
                )
            }
        }
    }
}

/** Loop steps are mostly used for map changes, so a zone action is the default when zones exist. */
private fun defaultStepAction(context: EventEditorContext): EventAction {
    val zone = context.tileZones.firstOrNull()
    return if (zone != null) {
        EventAction(type = EventActionType.TOGGLE_TILE_ZONE, zoneId = zone.id)
    } else {
        EventAction(type = EventActionType.GIVE_COINS, amount = 50)
    }
}
