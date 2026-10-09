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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.model.AttackerType
import de.egril.defender.model.EventAction
import de.egril.defender.model.EventActionType
import de.egril.defender.model.EventCondition
import de.egril.defender.model.EventConditionType
import de.egril.defender.model.EventMapImage
import de.egril.defender.model.EventMessageFrameId
import de.egril.defender.model.LevelEvent
import de.egril.defender.model.LevelEvents
import de.egril.defender.model.Position
import de.egril.defender.model.SpellType
import de.egril.defender.model.SupportObjectType
import de.egril.defender.model.TileZone
import de.egril.defender.ui.common.SelectableText
import de.egril.defender.ui.gameplay.EventMessageFrames
import de.egril.defender.ui.getLocalizedName
import de.egril.defender.ui.icon.TriangleDownIcon
import de.egril.defender.ui.icon.TriangleUpIcon
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.add_action
import defender_of_egril.composeapp.generated.resources.add_event
import defender_of_egril.composeapp.generated.resources.delete_action
import defender_of_egril.composeapp.generated.resources.delete_event
import defender_of_egril.composeapp.generated.resources.event_act_apply_tile_zone
import defender_of_egril.composeapp.generated.resources.event_act_destroy_mine
import defender_of_egril.composeapp.generated.resources.event_act_hide_altar_link
import defender_of_egril.composeapp.generated.resources.event_act_show_altar_link
import defender_of_egril.composeapp.generated.resources.event_altar_link_help
import defender_of_egril.composeapp.generated.resources.event_altar_first
import defender_of_egril.composeapp.generated.resources.event_altar_link_id
import defender_of_egril.composeapp.generated.resources.event_altar_second
import defender_of_egril.composeapp.generated.resources.event_act_give_coins
import defender_of_egril.composeapp.generated.resources.event_act_give_mana
import defender_of_egril.composeapp.generated.resources.event_act_give_support_object
import defender_of_egril.composeapp.generated.resources.event_act_give_support_spell
import defender_of_egril.composeapp.generated.resources.event_act_hide_map_image
import defender_of_egril.composeapp.generated.resources.event_act_revert_tile_zone
import defender_of_egril.composeapp.generated.resources.event_act_show_map_image
import defender_of_egril.composeapp.generated.resources.event_act_stop_event_loop
import defender_of_egril.composeapp.generated.resources.event_act_stop_spawn_loop
import defender_of_egril.composeapp.generated.resources.event_act_toggle_tile_zone
import defender_of_egril.composeapp.generated.resources.event_act_win_level
import defender_of_egril.composeapp.generated.resources.event_actions_count
import defender_of_egril.composeapp.generated.resources.event_actions_label
import defender_of_egril.composeapp.generated.resources.event_amount_label
import defender_of_egril.composeapp.generated.resources.event_any_enemy
import defender_of_egril.composeapp.generated.resources.event_cond_altars_activated
import defender_of_egril.composeapp.generated.resources.event_cond_coins_at_or_below
import defender_of_egril.composeapp.generated.resources.event_cond_enemies_killed
import defender_of_egril.composeapp.generated.resources.event_cond_enemy_turn_start
import defender_of_egril.composeapp.generated.resources.event_cond_enemy_type_killed
import defender_of_egril.composeapp.generated.resources.event_cond_health_at_or_below
import defender_of_egril.composeapp.generated.resources.event_cond_mana_at_or_below
import defender_of_egril.composeapp.generated.resources.event_cond_turn_start
import defender_of_egril.composeapp.generated.resources.event_cond_unit_reached
import defender_of_egril.composeapp.generated.resources.event_condition_label
import defender_of_egril.composeapp.generated.resources.event_destroy_mine_no_mine_warning
import defender_of_egril.composeapp.generated.resources.event_enemy_type_label
import defender_of_egril.composeapp.generated.resources.event_from_turn_label
import defender_of_egril.composeapp.generated.resources.event_message_frame_label
import defender_of_egril.composeapp.generated.resources.event_message_frame_standard
import defender_of_egril.composeapp.generated.resources.event_message_label
import defender_of_egril.composeapp.generated.resources.event_message_none
import defender_of_egril.composeapp.generated.resources.event_no_actions
import defender_of_egril.composeapp.generated.resources.event_position_label
import defender_of_egril.composeapp.generated.resources.event_repeatable_help
import defender_of_egril.composeapp.generated.resources.event_repeatable_label
import defender_of_egril.composeapp.generated.resources.event_spawn_loop_label
import defender_of_egril.composeapp.generated.resources.event_spawn_loop_missing_warning
import defender_of_egril.composeapp.generated.resources.event_summary_coins
import defender_of_egril.composeapp.generated.resources.event_summary_enemy_turn
import defender_of_egril.composeapp.generated.resources.event_summary_killed
import defender_of_egril.composeapp.generated.resources.event_summary_label
import defender_of_egril.composeapp.generated.resources.event_summary_loop
import defender_of_egril.composeapp.generated.resources.event_summary_mana
import defender_of_egril.composeapp.generated.resources.event_summary_support_object
import defender_of_egril.composeapp.generated.resources.event_summary_support_spell
import defender_of_egril.composeapp.generated.resources.event_summary_turn
import defender_of_egril.composeapp.generated.resources.event_support_object_label
import defender_of_egril.composeapp.generated.resources.event_support_spell_label
import defender_of_egril.composeapp.generated.resources.event_target_event_label
import defender_of_egril.composeapp.generated.resources.event_target_event_missing_warning
import defender_of_egril.composeapp.generated.resources.event_threshold_label
import defender_of_egril.composeapp.generated.resources.event_zone_label
import defender_of_egril.composeapp.generated.resources.event_zone_missing_warning
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
    minePositions: Set<Position>,
    issueDescription: String? = null,
    tileZones: List<TileZone> = emptyList(),
    spawnLoops: List<Pair<String, String>> = emptyList(),
    issues: List<String> = emptyList(),
) {
    val context =
        EventEditorContext(
            minePositions = minePositions,
            tileZones = tileZones,
            spawnLoops = spawnLoops,
            loopEvents =
                events.events.mapIndexedNotNull { index, event ->
                    if (event.loop != null) event.id to index else null
                },
            imageFiles = rememberEventMapImageFiles(),
            imageIds =
                events.events
                    .flatMap { it.allActions() }
                    .filter { it.type == EventActionType.SHOW_MAP_IMAGE }
                    .mapNotNull { it.mapImage?.id }
                    .distinct(),
        )

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
        val allIssues = listOfNotNull(issueDescription) + issues
        if (allIssues.isNotEmpty()) {
            items(allIssues) { issue ->
                Text(
                    text = issue,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
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
                context = context,
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
    context: EventEditorContext,
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
                    context = context,
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
                selectedFrame = event.messageFrame,
                onFrameChange = { onEventChange(event.copy(messageFrame = it)) },
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

            HorizontalDivider()

            EventLoopSection(
                loop = event.loop,
                onLoopChange = { onEventChange(event.copy(loop = it)) },
                context = context,
            )
        }
    }
}

/**
 * Data the action editors need to offer valid choices: the level's mines, the tile zones of the
 * level's map and the events that own a loop (targets for [EventActionType.STOP_EVENT_LOOP]).
 *
 * @param loopEvents Pairs of event id and its index in the event list (used for the display label).
 * @param spawnLoops Pairs of spawn loop id and display label (targets for [EventActionType.STOP_SPAWN_LOOP]).
 */
internal data class EventEditorContext(
    val minePositions: Set<Position>,
    val tileZones: List<TileZone>,
    val loopEvents: List<Pair<String, Int>>,
    val spawnLoops: List<Pair<String, String>> = emptyList(),
    val imageFiles: List<String> = emptyList(),
    val imageIds: List<String> = emptyList(),
)

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
    val loop = if (event.loop != null) " • ${stringResource(Res.string.event_summary_loop)}" else ""
    return "$condition • $actions$loop"
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

        EventConditionType.ENEMIES_KILLED,
        EventConditionType.ALTARS_ACTIVATED,
        ->
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
internal fun actionSummary(action: EventAction): String =
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

        EventActionType.DESTROY_MINE,
        EventActionType.WIN_LEVEL,
        -> action.type.localizedName()

        EventActionType.APPLY_TILE_ZONE,
        EventActionType.REVERT_TILE_ZONE,
        EventActionType.TOGGLE_TILE_ZONE,
        -> action.zoneId?.let { "${action.type.localizedName()}: $it" } ?: action.type.localizedName()

        EventActionType.STOP_EVENT_LOOP ->
            action.targetEventId?.let { "${action.type.localizedName()}: $it" } ?: action.type.localizedName()
        EventActionType.STOP_SPAWN_LOOP ->
            action.spawnLoopId?.let { "${action.type.localizedName()}: $it" } ?: action.type.localizedName()
        EventActionType.SHOW_MAP_IMAGE ->
            action.mapImage?.let { "${action.type.localizedName()}: ${it.id} (${it.fileName})" } ?: action.type.localizedName()
        EventActionType.HIDE_MAP_IMAGE ->
            action.imageId?.let { "${action.type.localizedName()}: $it" } ?: action.type.localizedName()
        EventActionType.SHOW_ALTAR_LINK, EventActionType.HIDE_ALTAR_LINK ->
            action.linkId?.let { "${action.type.localizedName()}: $it" } ?: action.type.localizedName()
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
            EventConditionType.ALTARS_ACTIVATED,
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
internal fun ActionEditor(
    action: EventAction,
    onActionChange: (EventAction) -> Unit,
    onDelete: () -> Unit,
    context: EventEditorContext,
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

                                EventActionType.SHOW_MAP_IMAGE ->
                                    action.copy(
                                        type = newType,
                                        mapImage =
                                            action.mapImage ?: EventMapImage(
                                                id =
                                                    generateSequence(1) { it + 1 }
                                                        .map { "map_image_$it" }
                                                        .first { it !in context.imageIds },
                                                fileName = context.imageFiles.firstOrNull().orEmpty(),
                                            ),
                                    )
                                EventActionType.HIDE_MAP_IMAGE ->
                                    action.copy(
                                        type = newType,
                                        imageId = action.imageId ?: action.mapImage?.id ?: context.imageIds.firstOrNull(),
                                    )
                                EventActionType.STOP_SPAWN_LOOP ->
                                    action.copy(type = newType, spawnLoopId = action.spawnLoopId ?: context.spawnLoops.firstOrNull()?.first)
                                EventActionType.SHOW_ALTAR_LINK, EventActionType.HIDE_ALTAR_LINK ->
                                    action.copy(type = newType, linkId = action.linkId ?: DEFAULT_ALTAR_LINK_ID)
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
            EventActionType.WIN_LEVEL -> Unit
            EventActionType.SHOW_MAP_IMAGE, EventActionType.HIDE_MAP_IMAGE ->
                EventMapImageEditor(action, onActionChange, context)
            EventActionType.SHOW_ALTAR_LINK, EventActionType.HIDE_ALTAR_LINK ->
                AltarLinkEditor(action, onActionChange)
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

            EventActionType.DESTROY_MINE -> {
                PositionField(
                    position = action.position,
                    onPositionChange = { onActionChange(action.copy(position = it)) },
                )
                val position = action.position
                if (position == null || !context.minePositions.contains(position)) {
                    Text(
                        text = stringResource(Res.string.event_destroy_mine_no_mine_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            EventActionType.APPLY_TILE_ZONE,
            EventActionType.REVERT_TILE_ZONE,
            EventActionType.TOGGLE_TILE_ZONE,
            -> {
                IdDropdown(
                    label = stringResource(Res.string.event_zone_label),
                    options = context.tileZones.map { it.id to it.displayName },
                    selectedId = action.zoneId,
                    onSelected = { onActionChange(action.copy(zoneId = it)) },
                )
                if (context.tileZones.none { it.id == action.zoneId }) {
                    Text(
                        text = stringResource(Res.string.event_zone_missing_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            EventActionType.STOP_EVENT_LOOP -> {
                val eventLabel = stringResource(Res.string.event_summary_label)
                IdDropdown(
                    label = stringResource(Res.string.event_target_event_label),
                    options = context.loopEvents.map { (id, index) -> id to "$eventLabel ${index + 1}" },
                    selectedId = action.targetEventId,
                    onSelected = { onActionChange(action.copy(targetEventId = it)) },
                )
                if (context.loopEvents.none { it.first == action.targetEventId }) {
                    Text(
                        text = stringResource(Res.string.event_target_event_missing_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            EventActionType.STOP_SPAWN_LOOP -> {
                Box(modifier = Modifier.testTag("event_spawn_loop_dropdown")) {
                    IdDropdown(
                        label = stringResource(Res.string.event_spawn_loop_label),
                        options = context.spawnLoops,
                        selectedId = action.spawnLoopId,
                        onSelected = { onActionChange(action.copy(spawnLoopId = it)) },
                    )
                }
                if (context.spawnLoops.none { it.first == action.spawnLoopId }) {
                    Text(
                        text = stringResource(Res.string.event_spawn_loop_missing_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

/** Link id used when a new altar-line action is created; any non-blank id works. */
private const val DEFAULT_ALTAR_LINK_ID = "altar_link"

/**
 * Editor for [EventActionType.SHOW_ALTAR_LINK] / [EventActionType.HIDE_ALTAR_LINK].
 * The id identifies the line; the two altar tiles define which altars are connected.
 */
@Composable
private fun AltarLinkEditor(
    action: EventAction,
    onActionChange: (EventAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = action.linkId.orEmpty(),
            onValueChange = { onActionChange(action.copy(linkId = it)) },
            label = { Text(stringResource(Res.string.event_altar_link_id)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (action.type == EventActionType.SHOW_ALTAR_LINK) {
            AltarTileFields(stringResource(Res.string.event_altar_first), action.altarFrom) {
                onActionChange(action.copy(altarFrom = it))
            }
            AltarTileFields(stringResource(Res.string.event_altar_second), action.altarTo) {
                onActionChange(action.copy(altarTo = it))
            }
        }
        SelectableText(
            stringResource(Res.string.event_altar_link_help),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/** X and Y tile inputs for one altar; [onChange] is called with a complete position once both are set. */
@Composable
private fun AltarTileFields(
    label: String,
    position: Position?,
    onChange: (Position) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        AltarCoordinateField(stringResource(Res.string.x_coordinate), position?.x) {
            onChange(Position(it, position?.y ?: 0))
        }
        AltarCoordinateField(stringResource(Res.string.y_coordinate), position?.y) {
            onChange(Position(position?.x ?: 0, it))
        }
    }
}

@Composable
private fun AltarCoordinateField(
    label: String,
    value: Int?,
    onChange: (Int) -> Unit,
) {
    var text by remember { mutableStateOf(value?.toString().orEmpty()) }
    LaunchedEffect(value) {
        if (text.toIntOrNull() != value) text = value?.toString().orEmpty()
    }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            it.toIntOrNull()?.takeIf { n -> n >= 0 }?.let(onChange)
        },
        label = { Text(label) },
        isError = text.toIntOrNull() == null,
        singleLine = true,
        modifier = Modifier.width(96.dp),
    )
}

/** Dropdown over (id, label) pairs; shows the raw id when the stored id is not among the options. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun IdDropdown(
    label: String,
    options: List<Pair<String, String>>,
    selectedId: String?,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selectedId }?.second ?: selectedId.orEmpty()
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
            options.forEach { (id, optionLabel) ->
                DropdownMenuItem(
                    text = { Text(optionLabel) },
                    onClick = {
                        onSelected(id)
                        expanded = false
                    },
                )
            }
        }
    }
}

/**
 * Message selection for an event or loop step: the predefined story text and – when a text is
 * selected – the visual frame its popup uses. "No message" means no popup is shown at all.
 */
@Composable
internal fun MessageDropdown(
    selectedKey: String?,
    onKeyChange: (String?) -> Unit,
    selectedFrame: EventMessageFrameId? = null,
    onFrameChange: ((EventMessageFrameId?) -> Unit)? = null,
) {
    MessageKeyDropdown(selectedKey = selectedKey, onKeyChange = onKeyChange)
    if (selectedKey != null && onFrameChange != null) {
        MessageFrameDropdown(selectedFrame = selectedFrame, onFrameChange = onFrameChange)
    }
}

/**
 * Label of a message frame: the standard story frame gets a localized name, all other frames are
 * named after the villain (or artwork) they belong to.
 */
@Composable
internal fun eventMessageFrameLabel(frameId: EventMessageFrameId?): String =
    if (EventMessageFrames.isStandard(frameId)) {
        stringResource(Res.string.event_message_frame_standard)
    } else {
        EventMessageFrames.label(frameId)
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageFrameDropdown(
    selectedFrame: EventMessageFrameId?,
    onFrameChange: (EventMessageFrameId?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = eventMessageFrameLabel(selectedFrame),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(Res.string.event_message_frame_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            EventMessageFrames.frameIds.forEach { frameId ->
                DropdownMenuItem(
                    text = { Text(eventMessageFrameLabel(frameId)) },
                    onClick = {
                        // The standard frame is stored as "no frame" so level files stay unchanged.
                        onFrameChange(frameId.takeUnless { EventMessageFrames.isStandard(it) })
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageKeyDropdown(
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
            AttackerType.entries.filterNot { it.isMirrorImage }.forEach { type ->
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
internal fun NumberField(
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
        EventConditionType.ALTARS_ACTIVATED -> stringResource(Res.string.event_cond_altars_activated)
    }

@Composable
private fun EventActionType.localizedName(): String =
    when (this) {
        EventActionType.GIVE_COINS -> stringResource(Res.string.event_act_give_coins)
        EventActionType.GIVE_MANA -> stringResource(Res.string.event_act_give_mana)
        EventActionType.GIVE_SUPPORT_OBJECT -> stringResource(Res.string.event_act_give_support_object)
        EventActionType.GIVE_SUPPORT_SPELL -> stringResource(Res.string.event_act_give_support_spell)
        EventActionType.DESTROY_MINE -> stringResource(Res.string.event_act_destroy_mine)
        EventActionType.APPLY_TILE_ZONE -> stringResource(Res.string.event_act_apply_tile_zone)
        EventActionType.REVERT_TILE_ZONE -> stringResource(Res.string.event_act_revert_tile_zone)
        EventActionType.TOGGLE_TILE_ZONE -> stringResource(Res.string.event_act_toggle_tile_zone)
        EventActionType.STOP_EVENT_LOOP -> stringResource(Res.string.event_act_stop_event_loop)
        EventActionType.STOP_SPAWN_LOOP -> stringResource(Res.string.event_act_stop_spawn_loop)
        EventActionType.SHOW_MAP_IMAGE -> stringResource(Res.string.event_act_show_map_image)
        EventActionType.HIDE_MAP_IMAGE -> stringResource(Res.string.event_act_hide_map_image)
        EventActionType.SHOW_ALTAR_LINK -> stringResource(Res.string.event_act_show_altar_link)
        EventActionType.HIDE_ALTAR_LINK -> stringResource(Res.string.event_act_hide_altar_link)
        EventActionType.WIN_LEVEL -> stringResource(Res.string.event_act_win_level)
    }
