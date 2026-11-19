package de.egril.defender.ui.editor.level.events

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.model.EventAction
import de.egril.defender.model.EventActionType
import de.egril.defender.model.EventCondition
import de.egril.defender.model.EventConditionType
import de.egril.defender.model.EventMapImage
import de.egril.defender.model.LevelEvent
import de.egril.defender.model.MIN_ROTATABLE_REGION_TILES
import de.egril.defender.model.Position
import de.egril.defender.model.SpellType
import de.egril.defender.model.SupportObjectType
import de.egril.defender.model.TileZone
import de.egril.defender.ui.getLocalizedName
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.delete_action
import defender_of_egril.composeapp.generated.resources.event_actions_count
import defender_of_egril.composeapp.generated.resources.event_actions_label
import defender_of_egril.composeapp.generated.resources.event_amount_label
import defender_of_egril.composeapp.generated.resources.event_any_enemy
import defender_of_egril.composeapp.generated.resources.event_condition_label
import defender_of_egril.composeapp.generated.resources.event_destroy_mine_no_mine_warning
import defender_of_egril.composeapp.generated.resources.event_from_turn_label
import defender_of_egril.composeapp.generated.resources.event_move_dx_label
import defender_of_egril.composeapp.generated.resources.event_move_dy_label
import defender_of_egril.composeapp.generated.resources.event_move_rotation_label
import defender_of_egril.composeapp.generated.resources.event_move_rotation_min_size_warning
import defender_of_egril.composeapp.generated.resources.event_no_actions
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
internal fun eventSummary(event: LevelEvent): String {
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
        EventActionType.MOVE_TILE_ZONE,
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
internal fun ConditionEditor(
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

            EventActionType.MOVE_TILE_ZONE -> {
                val movableZones = context.tileZones.filter { it.movable }
                IdDropdown(
                    label = stringResource(Res.string.event_zone_label),
                    options = movableZones.map { it.id to it.displayName },
                    selectedId = action.zoneId,
                    onSelected = { onActionChange(action.copy(zoneId = it)) },
                )
                if (movableZones.none { it.id == action.zoneId }) {
                    Text(
                        text = stringResource(Res.string.event_zone_missing_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                NumberField(
                    label = stringResource(Res.string.event_move_rotation_label),
                    value = action.rotationSteps,
                    onValueChange = { onActionChange(action.copy(rotationSteps = it)) },
                    allowNegative = true,
                )
                if (action.rotationSteps.mod(6) != 0 && (movableZones.firstOrNull { it.id == action.zoneId }?.tiles?.size ?: 0) < MIN_ROTATABLE_REGION_TILES) {
                    Text(
                        text = stringResource(Res.string.event_move_rotation_min_size_warning, MIN_ROTATABLE_REGION_TILES),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                NumberField(
                    label = stringResource(Res.string.event_move_dx_label),
                    value = action.moveDx,
                    onValueChange = { onActionChange(action.copy(moveDx = it)) },
                    allowNegative = true,
                )
                NumberField(
                    label = stringResource(Res.string.event_move_dy_label),
                    value = action.moveDy,
                    onValueChange = { onActionChange(action.copy(moveDy = it)) },
                    allowNegative = true,
                )
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
