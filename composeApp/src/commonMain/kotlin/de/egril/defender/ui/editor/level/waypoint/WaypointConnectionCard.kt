package de.egril.defender.ui.editor.level.waypoint

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.EditorWaypoint
import de.egril.defender.model.Position
import de.egril.defender.ui.icon.RedCircleIcon
import de.egril.defender.ui.icon.RightArrowIcon
import de.egril.defender.ui.icon.TrashIcon
import de.egril.defender.ui.icon.WarningIcon
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.circular_dependency_warning
import defender_of_egril.composeapp.generated.resources.spawn_point_text
import defender_of_egril.composeapp.generated.resources.target_text
import defender_of_egril.composeapp.generated.resources.unconnected_waypoint_warning
import defender_of_egril.composeapp.generated.resources.waypoint_position_format

/**
 * Card showing a single waypoint connection
 */
@Composable
fun WaypointConnectionCard(
    waypoint: EditorWaypoint,
    spawnPoints: List<Position>,
    waypointTiles: List<Position>,
    target: Position?,
    isInCircular: Boolean,
    isUnconnected: Boolean,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isInCircular) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Source position
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Position (${waypoint.position.x}, ${waypoint.position.y})",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (spawnPoints.contains(waypoint.position)) {
                    Text(
                        text = stringResource(Res.string.spawn_point_text),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (waypointTiles.contains(waypoint.position)) {
                    Text(
                        text = "WAYPOINT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Arrow
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                if (isInCircular) {
                    RedCircleIcon(size = 12.dp)
                }
                RightArrowIcon(
                    size = 16.dp,
                    tint = if (isInCircular) Color.Red else Color.Black
                )
            }

            // Target position
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.waypoint_position_format).format(
                            waypoint.nextTargetPosition.x,
                            waypoint.nextTargetPosition.y
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Warning icons
                    if (isInCircular) {
                        WarningIcon(size = 14.dp)
                    }
                    if (isUnconnected) {
                        WarningIcon(size = 14.dp)
                    }
                }

                if (target == waypoint.nextTargetPosition) {
                    Text(
                        text = stringResource(Res.string.target_text),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Green
                    )
                } else if (waypointTiles.contains(waypoint.nextTargetPosition)) {
                    Text(
                        text = "WAYPOINT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                // Show warning messages
                if (isInCircular) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WarningIcon(size = 12.dp)
                        Text(
                            text = stringResource(Res.string.circular_dependency_warning),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Red
                        )
                    }
                }
                if (isUnconnected) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WarningIcon(size = 12.dp)
                        Text(
                            text = stringResource(Res.string.unconnected_waypoint_warning),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Delete button
            IconButton(onClick = onDelete) {
                TrashIcon(size = 20.dp)
            }
        }
    }
}
