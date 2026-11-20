package de.egril.defender.ui.editor.level.events

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.model.AttackerType
import de.egril.defender.model.EventAction
import de.egril.defender.model.EventActionType
import de.egril.defender.model.EventConditionType
import de.egril.defender.model.EventMessageFrameId
import de.egril.defender.model.Position
import de.egril.defender.ui.common.SelectableText
import de.egril.defender.ui.gameplay.EventMessageFrames
import de.egril.defender.ui.gameplay.EventUpcomingSymbols
import de.egril.defender.ui.getLocalizedName
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.event_act_apply_tile_zone
import defender_of_egril.composeapp.generated.resources.event_act_destroy_mine
import defender_of_egril.composeapp.generated.resources.event_act_give_coins
import defender_of_egril.composeapp.generated.resources.event_act_give_mana
import defender_of_egril.composeapp.generated.resources.event_act_give_support_object
import defender_of_egril.composeapp.generated.resources.event_act_give_support_spell
import defender_of_egril.composeapp.generated.resources.event_act_hide_altar_link
import defender_of_egril.composeapp.generated.resources.event_act_hide_map_image
import defender_of_egril.composeapp.generated.resources.event_act_move_tile_zone
import defender_of_egril.composeapp.generated.resources.event_act_revert_tile_zone
import defender_of_egril.composeapp.generated.resources.event_act_show_altar_link
import defender_of_egril.composeapp.generated.resources.event_act_show_map_image
import defender_of_egril.composeapp.generated.resources.event_act_stop_event_loop
import defender_of_egril.composeapp.generated.resources.event_act_stop_spawn_loop
import defender_of_egril.composeapp.generated.resources.event_act_toggle_tile_zone
import defender_of_egril.composeapp.generated.resources.event_act_win_level
import defender_of_egril.composeapp.generated.resources.event_altar_first
import defender_of_egril.composeapp.generated.resources.event_altar_link_help
import defender_of_egril.composeapp.generated.resources.event_altar_link_id
import defender_of_egril.composeapp.generated.resources.event_altar_second
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
import defender_of_egril.composeapp.generated.resources.event_enemy_type_label
import defender_of_egril.composeapp.generated.resources.event_message_frame_label
import defender_of_egril.composeapp.generated.resources.event_message_frame_standard
import defender_of_egril.composeapp.generated.resources.event_message_label
import defender_of_egril.composeapp.generated.resources.event_message_none
import defender_of_egril.composeapp.generated.resources.event_position_label
import defender_of_egril.composeapp.generated.resources.event_upcoming_symbol_label
import defender_of_egril.composeapp.generated.resources.event_upcoming_symbol_none
import defender_of_egril.composeapp.generated.resources.x_coordinate
import defender_of_egril.composeapp.generated.resources.y_coordinate
import org.jetbrains.compose.resources.painterResource

/** Link id used when a new altar-line action is created; any non-blank id works. */
internal const val DEFAULT_ALTAR_LINK_ID = "altar_link"

/**
 * Editor for [EventActionType.SHOW_ALTAR_LINK] / [EventActionType.HIDE_ALTAR_LINK].
 * The id identifies the line; the two altar tiles define which altars are connected.
 */
@Composable
internal fun AltarLinkEditor(
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
internal fun UpcomingSymbolDropdown(
    selectedSymbol: String?,
    onSymbolChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val noSymbolLabel = stringResource(Res.string.event_upcoming_symbol_none)
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selectedSymbol?.takeIf { EventUpcomingSymbols.resource(it) != null } ?: noSymbolLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(Res.string.event_upcoming_symbol_label)) },
            leadingIcon = {
                EventUpcomingSymbols.resource(selectedSymbol)?.let { symbol ->
                    Image(painter = painterResource(symbol), contentDescription = null, modifier = Modifier.size(24.dp))
                }
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(noSymbolLabel) },
                onClick = {
                    onSymbolChange(null)
                    expanded = false
                },
            )
            EventUpcomingSymbols.symbolIds.forEach { symbolId ->
                DropdownMenuItem(
                    text = { Text(symbolId) },
                    leadingIcon = {
                        EventUpcomingSymbols.resource(symbolId)?.let { symbol ->
                            Image(painter = painterResource(symbol), contentDescription = null, modifier = Modifier.size(24.dp))
                        }
                    },
                    onClick = {
                        onSymbolChange(symbolId)
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
internal fun <T> EnumDropdown(
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
internal fun AttackerTypeDropdown(
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
    allowNegative: Boolean = false,
) {
    // Keep the typed text so intermediate states such as "-" survive: they parse to 0 and would
    // otherwise be overwritten by the controlled value before the digits are typed.
    var text by remember { mutableStateOf(value.toString()) }
    if ((text.toIntOrNull() ?: 0) != value) text = value.toString()
    OutlinedTextField(
        value = text,
        onValueChange = { typed ->
            val filtered = typed.filterIndexed { index, c -> c.isDigit() || (allowNegative && index == 0 && c == '-') }
            text = filtered
            onValueChange(filtered.toIntOrNull() ?: 0)
        },
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.width(200.dp),
    )
}

@Composable
internal fun PositionField(
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
internal fun EventConditionType.localizedName(): String =
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
internal fun EventActionType.localizedName(): String =
    when (this) {
        EventActionType.GIVE_COINS -> stringResource(Res.string.event_act_give_coins)
        EventActionType.GIVE_MANA -> stringResource(Res.string.event_act_give_mana)
        EventActionType.GIVE_SUPPORT_OBJECT -> stringResource(Res.string.event_act_give_support_object)
        EventActionType.GIVE_SUPPORT_SPELL -> stringResource(Res.string.event_act_give_support_spell)
        EventActionType.DESTROY_MINE -> stringResource(Res.string.event_act_destroy_mine)
        EventActionType.APPLY_TILE_ZONE -> stringResource(Res.string.event_act_apply_tile_zone)
        EventActionType.REVERT_TILE_ZONE -> stringResource(Res.string.event_act_revert_tile_zone)
        EventActionType.TOGGLE_TILE_ZONE -> stringResource(Res.string.event_act_toggle_tile_zone)
        EventActionType.MOVE_TILE_ZONE -> stringResource(Res.string.event_act_move_tile_zone)
        EventActionType.STOP_EVENT_LOOP -> stringResource(Res.string.event_act_stop_event_loop)
        EventActionType.STOP_SPAWN_LOOP -> stringResource(Res.string.event_act_stop_spawn_loop)
        EventActionType.SHOW_MAP_IMAGE -> stringResource(Res.string.event_act_show_map_image)
        EventActionType.HIDE_MAP_IMAGE -> stringResource(Res.string.event_act_hide_map_image)
        EventActionType.SHOW_ALTAR_LINK -> stringResource(Res.string.event_act_show_altar_link)
        EventActionType.HIDE_ALTAR_LINK -> stringResource(Res.string.event_act_hide_altar_link)
        EventActionType.WIN_LEVEL -> stringResource(Res.string.event_act_win_level)
    }
