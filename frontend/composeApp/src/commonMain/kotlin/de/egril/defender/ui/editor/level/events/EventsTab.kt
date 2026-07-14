package de.egril.defender.ui.editor.level.events

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
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
import com.hyperether.resources.stringResource
import de.egril.defender.model.AttackerType
import de.egril.defender.model.EventAction
import de.egril.defender.model.EventActionType
import de.egril.defender.model.EventCondition
import de.egril.defender.model.EventConditionType
import de.egril.defender.model.LevelEvent
import de.egril.defender.model.LevelEvents
import de.egril.defender.model.Position
import de.egril.defender.model.SpellType
import de.egril.defender.model.SupportObjectType
import de.egril.defender.ui.getLocalizedName
import de.egril.defender.ui.icon.TriangleDownIcon
import de.egril.defender.ui.icon.TriangleUpIcon
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.add_action
import defender_of_egril.composeapp.generated.resources.add_event
import defender_of_egril.composeapp.generated.resources.delete_action
import defender_of_egril.composeapp.generated.resources.delete_event
import defender_of_egril.composeapp.generated.resources.event_act_destroy_mine
import defender_of_egril.composeapp.generated.resources.event_act_give_coins
import defender_of_egril.composeapp.generated.resources.event_act_give_mana
import defender_of_egril.composeapp.generated.resources.event_act_give_support_object
import defender_of_egril.composeapp.generated.resources.event_act_give_support_spell
import defender_of_egril.composeapp.generated.resources.event_actions_count
import defender_of_egril.composeapp.generated.resources.event_actions_label
import defender_of_egril.composeapp.generated.resources.event_amount_label
import defender_of_egril.composeapp.generated.resources.event_any_enemy
import defender_of_egril.composeapp.generated.resources.event_cond_coins_at_or_below
import defender_of_egril.composeapp.generated.resources.event_cond_enemies_killed
import defender_of_egril.composeapp.generated.resources.event_cond_enemy_turn_start
import defender_of_egril.composeapp.generated.resources.event_cond_enemy_type_killed
import defender_of_egril.composeapp.generated.resources.event_cond_health_at_or_below
import defender_of_egril.composeapp.generated.resources.event_cond_mana_at_or_below
import defender_of_egril.composeapp.generated.resources.event_cond_turn_start
import defender_of_egril.composeapp.generated.resources.event_cond_unit_reached
import defender_of_egril.composeapp.generated.resources.event_condition_label
import defender_of_egril.composeapp.generated.resources.event_enemy_type_label
import defender_of_egril.composeapp.generated.resources.event_from_turn_label
import defender_of_egril.composeapp.generated.resources.event_message_label
import defender_of_egril.composeapp.generated.resources.event_message_none
import defender_of_egril.composeapp.generated.resources.event_no_actions
import defender_of_egril.composeapp.generated.resources.event_position_label
import defender_of_egril.composeapp.generated.resources.event_repeatable_help
import defender_of_egril.composeapp.generated.resources.event_repeatable_label
import defender_of_egril.composeapp.generated.resources.event_summary_coins
import defender_of_egril.composeapp.generated.resources.event_summary_enemy_turn
import defender_of_egril.composeapp.generated.resources.event_summary_killed
import defender_of_egril.composeapp.generated.resources.event_summary_label
import defender_of_egril.composeapp.generated.resources.event_summary_mana
import defender_of_egril.composeapp.generated.resources.event_summary_support_object
import defender_of_egril.composeapp.generated.resources.event_summary_support_spell
import defender_of_egril.composeapp.generated.resources.event_summary_turn
import defender_of_egril.composeapp.generated.resources.event_support_object_label
import defender_of_egril.composeapp.generated.resources.event_support_spell_label
import defender_of_egril.composeapp.generated.resources.event_threshold_label
import defender_of_egril.composeapp.generated.resources.events_intro
import defender_of_egril.composeapp.generated.resources.x_coordinate
import defender_of_egril.composeapp.generated.resources.y_coordinate

/**
 * Level editor tab for scripting events. Each event pairs a condition with a list of actions and an
 * optional predefined story message. See [de.egril.defender.model.LevelEvent].
 */
@Composable
fun EventsTab(
    events: LevelEvents,
    onEventsChange: (LevelEvents) -> Unit,
) {
    fun updateEvent(
        index: Int,
        newEvent: LevelEvent,
    ) {
        val updated = events.events.toMutableList()
        updated[index] = newEvent
        onEventsChange(events.copy(events = updated))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = stringResource(Res.string.events_intro),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Button(
                onClick = {
                    val newEvent =
                        LevelEvent(
                            id = generateEventId(events.events),
                            condition = EventCondition(type = EventConditionType.TURN_START),
                        )
                    onEventsChange(events.copy(events = events.events + newEvent))
                },
            ) {
                Text(stringResource(Res.string.add_event))
            }
        }
        itemsIndexed(events.events) { index, event ->
            EventCard(
                index = index,
                event = event,
                onEventChange = { updateEvent(index, it) },
                onDelete = {
                    val updated = events.events.toMutableList()
                    updated.removeAt(index)
                    onEventsChange(events.copy(events = updated))
                },
            )
        }
    }
}

private fun generateEventId(existing: List<LevelEvent>): String {
    var counter = existing.size + 1
    val ids = existing.map { it.id }.toSet()
    while (ids.contains("event_$counter")) counter++
    return "event_$counter"
}

@Composable
private fun EventCard(
    index: Int,
    event: LevelEvent,
    onEventChange: (LevelEvent) -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember(event.id) { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
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
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (expanded) {
                    TriangleUpIcon(size = 20.dp)
                } else {
                    TriangleDownIcon(size = 20.dp)
                }
                Column {
                    Text(
                        text = "${stringResource(Res.string.event_summary_label)} ${index + 1}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (!expanded) {
                        Text(
                            text = eventSummary(event),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            OutlinedButton(onClick = onDelete) {
                Text(stringResource(Res.string.delete_event))
            }
        }

        if (expanded) {
            HorizontalDivider()

            // Condition editor
            ConditionEditor(
                condition = event.condition,
                onConditionChange = { onEventChange(event.copy(condition = it)) },
            )

            HorizontalDivider()

            // Actions editor
            Text(
                text = stringResource(Res.string.event_actions_label),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            event.actions.forEachIndexed { actionIndex, action ->
                ActionEditor(
                    action = action,
                    onActionChange = { newAction ->
                        val updated = event.actions.toMutableList()
                        updated[actionIndex] = newAction
                        onEventChange(event.copy(actions = updated))
                    },
                    onDelete = {
                        val updated = event.actions.toMutableList()
                        updated.removeAt(actionIndex)
                        onEventChange(event.copy(actions = updated))
                    },
                )
            }
            OutlinedButton(
                onClick = {
                    onEventChange(
                        event.copy(actions = event.actions + EventAction(type = EventActionType.GIVE_COINS, amount = 50)),
                    )
                },
            ) {
                Text(stringResource(Res.string.add_action))
            }

            HorizontalDivider()

            // Message dropdown
            MessageDropdown(
                selectedKey = event.messageKey,
                onKeyChange = { onEventChange(event.copy(messageKey = it)) },
            )

            // Repeatable toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Checkbox(
                    checked = event.repeatable,
                    onCheckedChange = { onEventChange(event.copy(repeatable = it)) },
                )
                Text(stringResource(Res.string.event_repeatable_label))
            }
            Text(
                text = stringResource(Res.string.event_repeatable_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Short human-readable summary of an event shown in the collapsed card state:
 * the condition (with its amount/type) followed by the action count (only when more
 * than one) and the first three actions in abbreviated form.
 */
@Composable
private fun eventSummary(event: LevelEvent): String {
    val condition = conditionSummary(event.condition)
    val actions =
        if (event.actions.isEmpty()) {
            stringResource(Res.string.event_no_actions)
        } else {
            val names = mutableListOf<String>()
            event.actions.take(3).forEach { names.add(actionSummary(it)) }
            val preview = names.joinToString(", ")
            if (event.actions.size == 1) {
                preview
            } else {
                "${stringResource(Res.string.event_actions_count, event.actions.size)}: $preview"
            }
        }
    return "$condition • $actions"
}

/**
 * Compact condition description including the relevant amount/type where applicable.
 */
@Composable
private fun conditionSummary(condition: EventCondition): String =
    when (condition.type) {
        EventConditionType.TURN_START ->
            stringResource(Res.string.event_summary_turn, condition.fromTurn)

        EventConditionType.ENEMY_TURN_START ->
            stringResource(Res.string.event_summary_enemy_turn, condition.fromTurn)

        EventConditionType.ENEMIES_KILLED ->
            "${condition.threshold} ${condition.type.localizedName()}"

        EventConditionType.ENEMY_TYPE_KILLED -> {
            val typeName = condition.attackerType?.getLocalizedName() ?: stringResource(Res.string.event_any_enemy)
            "${condition.threshold} $typeName ${stringResource(Res.string.event_summary_killed)}"
        }

        EventConditionType.HEALTH_AT_OR_BELOW,
        EventConditionType.MANA_AT_OR_BELOW,
        EventConditionType.COINS_AT_OR_BELOW,
        -> "${condition.type.localizedName()} ${condition.threshold}"

        EventConditionType.UNIT_REACHED -> {
            val typeName = condition.attackerType?.getLocalizedName() ?: stringResource(Res.string.event_any_enemy)
            val position = condition.position
            val base = "$typeName ${condition.type.localizedName()}"
            if (position != null) "$base (${position.x},${position.y})" else base
        }
    }

/**
 * Compact action description including the relevant amount/type where applicable.
 */
@Composable
private fun actionSummary(action: EventAction): String =
    when (action.type) {
        EventActionType.GIVE_COINS -> stringResource(Res.string.event_summary_coins, action.amount)
        EventActionType.GIVE_MANA -> stringResource(Res.string.event_summary_mana, action.amount)
        EventActionType.GIVE_SUPPORT_OBJECT -> {
            val base = stringResource(Res.string.event_summary_support_object)
            action.supportObjectType?.let { "$base: ${it.name}" } ?: base
        }

        EventActionType.GIVE_SUPPORT_SPELL -> {
            val base = stringResource(Res.string.event_summary_support_spell)
            action.spellType?.let { "$base: ${it.getLocalizedName()}" } ?: base
        }

        EventActionType.DESTROY_MINE -> action.type.localizedName()
    }

@Composable
private fun ConditionEditor(
    condition: EventCondition,
    onConditionChange: (EventCondition) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        EnumDropdown(
            label = stringResource(Res.string.event_condition_label),
            options = EventConditionType.entries,
            selected = condition.type,
            optionLabel = { it.localizedName() },
            onSelected = { onConditionChange(condition.copy(type = it)) },
        )

        // From-turn applies to all conditions
        NumberField(
            label = stringResource(Res.string.event_from_turn_label),
            value = condition.fromTurn,
            onValueChange = { onConditionChange(condition.copy(fromTurn = it)) },
        )

        when (condition.type) {
            EventConditionType.ENEMIES_KILLED,
            EventConditionType.HEALTH_AT_OR_BELOW,
            EventConditionType.MANA_AT_OR_BELOW,
            EventConditionType.COINS_AT_OR_BELOW,
            ->
                NumberField(
                    label = stringResource(Res.string.event_threshold_label),
                    value = condition.threshold,
                    onValueChange = { onConditionChange(condition.copy(threshold = it)) },
                )

            EventConditionType.ENEMY_TYPE_KILLED -> {
                AttackerTypeDropdown(
                    selected = condition.attackerType,
                    allowAny = false,
                    onSelected = { onConditionChange(condition.copy(attackerType = it)) },
                )
                NumberField(
                    label = stringResource(Res.string.event_threshold_label),
                    value = condition.threshold,
                    onValueChange = { onConditionChange(condition.copy(threshold = it)) },
                )
            }

            EventConditionType.UNIT_REACHED -> {
                AttackerTypeDropdown(
                    selected = condition.attackerType,
                    allowAny = true,
                    onSelected = { onConditionChange(condition.copy(attackerType = it)) },
                )
                PositionField(
                    position = condition.position,
                    onPositionChange = { onConditionChange(condition.copy(position = it)) },
                )
            }

            EventConditionType.TURN_START,
            EventConditionType.ENEMY_TURN_START,
            -> {
                // No extra parameters
            }
        }
    }
}

@Composable
private fun ActionEditor(
    action: EventAction,
    onActionChange: (EventAction) -> Unit,
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
            Box(modifier = Modifier.weight(1f)) {
                EnumDropdown(
                    label = stringResource(Res.string.event_actions_label),
                    options = EventActionType.entries,
                    selected = action.type,
                    optionLabel = { it.localizedName() },
                    onSelected = { newType ->
                        // Ensure a concrete support object/spell is stored when the action type is
                        // switched to one that requires it, so the granted support is never null.
                        val updated =
                            when (newType) {
                                EventActionType.GIVE_SUPPORT_OBJECT ->
                                    action.copy(
                                        type = newType,
                                        supportObjectType = action.supportObjectType ?: SupportObjectType.entries.first(),
                                    )

                                EventActionType.GIVE_SUPPORT_SPELL ->
                                    action.copy(
                                        type = newType,
                                        spellType = action.spellType ?: SpellType.entries.first(),
                                    )

                                else -> action.copy(type = newType)
                            }
                        onActionChange(updated)
                    },
                )
            }
            OutlinedButton(onClick = onDelete) {
                Text(stringResource(Res.string.delete_action))
            }
        }

        when (action.type) {
            EventActionType.GIVE_COINS, EventActionType.GIVE_MANA ->
                NumberField(
                    label = stringResource(Res.string.event_amount_label),
                    value = action.amount,
                    onValueChange = { onActionChange(action.copy(amount = it)) },
                )

            EventActionType.GIVE_SUPPORT_OBJECT -> {
                EnumDropdown(
                    label = stringResource(Res.string.event_support_object_label),
                    options = SupportObjectType.entries,
                    selected = action.supportObjectType ?: SupportObjectType.entries.first(),
                    optionLabel = { it.name },
                    onSelected = { onActionChange(action.copy(supportObjectType = it)) },
                )
                NumberField(
                    label = stringResource(Res.string.event_amount_label),
                    value = action.amount,
                    onValueChange = { onActionChange(action.copy(amount = it)) },
                )
            }

            EventActionType.GIVE_SUPPORT_SPELL -> {
                EnumDropdown(
                    label = stringResource(Res.string.event_support_spell_label),
                    options = SpellType.entries,
                    selected = action.spellType ?: SpellType.entries.first(),
                    optionLabel = { it.getLocalizedName() },
                    onSelected = { onActionChange(action.copy(spellType = it)) },
                )
                NumberField(
                    label = stringResource(Res.string.event_amount_label),
                    value = action.amount,
                    onValueChange = { onActionChange(action.copy(amount = it)) },
                )
            }

            EventActionType.DESTROY_MINE ->
                PositionField(
                    position = action.position,
                    onPositionChange = { onActionChange(action.copy(position = it)) },
                )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageDropdown(
    selectedKey: String?,
    onKeyChange: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val noneLabel = stringResource(Res.string.event_message_none)
    val selectedLabel = selectedKey?.let { EventMessageCatalog.preview(it) } ?: noneLabel

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(Res.string.event_message_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(noneLabel) },
                onClick = {
                    onKeyChange(null)
                    expanded = false
                },
            )
            EventMessageCatalog.keys.forEach { key ->
                DropdownMenuItem(
                    text = { Text(EventMessageCatalog.preview(key)) },
                    onClick = {
                        onKeyChange(key)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> EnumDropdown(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttackerTypeDropdown(
    selected: AttackerType?,
    allowAny: Boolean,
    onSelected: (AttackerType?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val anyLabel = stringResource(Res.string.event_any_enemy)
    val label = stringResource(Res.string.event_enemy_type_label)
    val selectedLabel = selected?.getLocalizedName() ?: anyLabel

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (allowAny) {
                DropdownMenuItem(
                    text = { Text(anyLabel) },
                    onClick = {
                        onSelected(null)
                        expanded = false
                    },
                )
            }
            AttackerType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.getLocalizedName()) },
                    onClick = {
                        onSelected(type)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { text ->
            val filtered = text.filter { it.isDigit() }
            onValueChange(filtered.toIntOrNull() ?: 0)
        },
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.width(200.dp),
    )
}

@Composable
private fun PositionField(
    position: Position?,
    onPositionChange: (Position?) -> Unit,
) {
    val x = position?.x ?: 0
    val y = position?.y ?: 0
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "${stringResource(Res.string.event_position_label)}:",
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
            value = x.toString(),
            onValueChange = { text ->
                val newX = text.filter { it.isDigit() }.toIntOrNull() ?: 0
                onPositionChange(Position(newX, y))
            },
            label = { Text(stringResource(Res.string.x_coordinate)) },
            singleLine = true,
            modifier = Modifier.width(90.dp),
        )
        OutlinedTextField(
            value = y.toString(),
            onValueChange = { text ->
                val newY = text.filter { it.isDigit() }.toIntOrNull() ?: 0
                onPositionChange(Position(x, newY))
            },
            label = { Text(stringResource(Res.string.y_coordinate)) },
            singleLine = true,
            modifier = Modifier.width(90.dp),
        )
    }
}

@Composable
private fun EventConditionType.localizedName(): String =
    when (this) {
        EventConditionType.TURN_START -> stringResource(Res.string.event_cond_turn_start)
        EventConditionType.ENEMY_TURN_START -> stringResource(Res.string.event_cond_enemy_turn_start)
        EventConditionType.ENEMIES_KILLED -> stringResource(Res.string.event_cond_enemies_killed)
        EventConditionType.ENEMY_TYPE_KILLED -> stringResource(Res.string.event_cond_enemy_type_killed)
        EventConditionType.UNIT_REACHED -> stringResource(Res.string.event_cond_unit_reached)
        EventConditionType.HEALTH_AT_OR_BELOW -> stringResource(Res.string.event_cond_health_at_or_below)
        EventConditionType.MANA_AT_OR_BELOW -> stringResource(Res.string.event_cond_mana_at_or_below)
        EventConditionType.COINS_AT_OR_BELOW -> stringResource(Res.string.event_cond_coins_at_or_below)
    }

@Composable
private fun EventActionType.localizedName(): String =
    when (this) {
        EventActionType.GIVE_COINS -> stringResource(Res.string.event_act_give_coins)
        EventActionType.GIVE_MANA -> stringResource(Res.string.event_act_give_mana)
        EventActionType.GIVE_SUPPORT_OBJECT -> stringResource(Res.string.event_act_give_support_object)
        EventActionType.GIVE_SUPPORT_SPELL -> stringResource(Res.string.event_act_give_support_spell)
        EventActionType.DESTROY_MINE -> stringResource(Res.string.event_act_destroy_mine)
    }
