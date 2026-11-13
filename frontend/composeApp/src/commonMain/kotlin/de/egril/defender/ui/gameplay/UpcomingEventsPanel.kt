package de.egril.defender.ui.gameplay

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperether.resources.stringResource
import de.egril.defender.model.GameState
import de.egril.defender.model.LevelEvent
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.in_x_turns
import defender_of_egril.composeapp.generated.resources.turn
import defender_of_egril.composeapp.generated.resources.upcoming_events
import org.jetbrains.compose.resources.painterResource

/**
 * Collapsible list of scripted events flagged for the upcoming list. Shows nothing when no event is listed.
 */
@Composable
fun UpcomingEventsPanel(
    gameState: GameState,
    modifier: Modifier = Modifier,
    forceExpanded: Boolean = false,
) {
    val currentTurn = gameState.turnNumber.value
    val upcomingEvents =
        gameState.level.events.events
            .filter { it.isListedAsUpcoming(gameState.triggeredEventIds) }
            .sortedBy { it.scheduledTurn(currentTurn) ?: Int.MAX_VALUE }
    if (upcomingEvents.isEmpty()) return

    // Same gap as between the enemy list and the legend; it only exists together with this panel.
    Column(modifier = modifier) {
        ExpandableCard(
            title = stringResource(Res.string.upcoming_events),
            modifier = Modifier.fillMaxWidth(),
            defaultExpanded = true,
            forceExpanded = forceExpanded,
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(
                    items = upcomingEvents,
                    key = { event -> "upcoming-event-${event.id}" },
                ) { event ->
                    UpcomingEventItem(event, currentTurn)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun UpcomingEventItem(
    event: LevelEvent,
    currentTurn: Int,
) {
    val scheduledTurn = event.scheduledTurn(currentTurn)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GamePlayColors.UpcomingCardBackground),
    ) {
        Row(
            modifier = Modifier.padding(6.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EventUpcomingSymbols.resource(event.upcomingSymbol)?.let { symbol ->
                    Image(
                        painter = painterResource(symbol),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = event.upcomingName?.takeIf { it.isNotBlank() } ?: event.id,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (scheduledTurn != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${stringResource(Res.string.turn)} $scheduledTurn",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (scheduledTurn == currentTurn + 1) GamePlayColors.WarningDeep else GamePlayColors.Warning,
                    )
                    if (scheduledTurn > currentTurn) {
                        Text(
                            stringResource(Res.string.in_x_turns, (scheduledTurn - currentTurn).toString()),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 9.sp,
                            color = Color.Gray,
                        )
                    }
                }
            }
        }
    }
}
