package de.egril.defender.ui.editor.level.events

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import de.egril.defender.model.EventAction
import de.egril.defender.model.EventActionType
import de.egril.defender.model.EventCondition
import de.egril.defender.model.EventConditionType
import de.egril.defender.model.LevelEvent
import de.egril.defender.model.LevelEvents
import de.egril.defender.model.Position
import de.egril.defender.model.TileZone
import de.egril.defender.ui.icon.TriangleDownIcon
import de.egril.defender.ui.icon.TriangleUpIcon
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.add_action
import defender_of_egril.composeapp.generated.resources.add_event
import defender_of_egril.composeapp.generated.resources.delete_event
import defender_of_egril.composeapp.generated.resources.event_actions_label
import defender_of_egril.composeapp.generated.resources.event_repeatable_help
import defender_of_egril.composeapp.generated.resources.event_repeatable_label
import defender_of_egril.composeapp.generated.resources.event_summary_label
import defender_of_egril.composeapp.generated.resources.event_upcoming_help
import defender_of_egril.composeapp.generated.resources.event_upcoming_label
import defender_of_egril.composeapp.generated.resources.event_upcoming_name_label
import defender_of_egril.composeapp.generated.resources.events_intro

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

            // Upcoming list toggle: the event is listed with its symbol and name while it is still to come
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Checkbox(
                    checked = event.showInUpcomingList,
                    onCheckedChange = { onEventChange(event.copy(showInUpcomingList = it)) },
                )
                Text(stringResource(Res.string.event_upcoming_label))
            }
            Text(
                text = stringResource(Res.string.event_upcoming_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (event.showInUpcomingList) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    UpcomingSymbolDropdown(
                        selectedSymbol = event.upcomingSymbol,
                        onSymbolChange = { onEventChange(event.copy(upcomingSymbol = it)) },
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = event.upcomingName.orEmpty(),
                        // Quotes and backslashes are not escaped in level files, so they are not accepted.
                        onValueChange = { onEventChange(event.copy(upcomingName = it.filterNot { c -> c == '"' || c == '\\' })) },
                        label = { Text(stringResource(Res.string.event_upcoming_name_label)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            HorizontalDivider()

            EventLoopSection(
                loop = event.loop,
                onLoopChange = { onEventChange(event.copy(loop = it)) },
                context = context,
            )
        }
    }
}
