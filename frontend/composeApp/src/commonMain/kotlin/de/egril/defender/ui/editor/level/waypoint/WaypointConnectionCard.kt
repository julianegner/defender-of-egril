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
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.EditorWaypoint
import de.egril.defender.model.Position
import de.egril.defender.ui.common.SelectableText
import de.egril.defender.ui.icon.RedCircleIcon
import de.egril.defender.ui.icon.RightArrowIcon
import de.egril.defender.ui.icon.TrashIcon
import de.egril.defender.ui.icon.WarningIcon
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.circular_dependency_warning
import defender_of_egril.composeapp.generated.resources.spawn_point_text
import defender_of_egril.composeapp.generated.resources.target_text
import defender_of_egril.composeapp.generated.resources.unconnected_waypoint_warning
import defender_of_egril.composeapp.generated.resources.waypoint
import defender_of_egril.composeapp.generated.resources.waypoint_position_format

/**
 * Card showing a single waypoint connection
 */
@Composable
fun WaypointConnectionCard(
    waypoint: EditorWaypoint,
    spawnPoints: List<Position>,
    validWaypointPositions: List<Position>,
    targets: List<Position>,
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
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header row with delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PositionText(waypoint.position, MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDelete) {
                    TrashIcon(size = 20.dp)
                }
            }

            // Source position info
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (spawnPoints.contains(waypoint.position)) {
                    SelectableText(
                        text = stringResource(Res.string.spawn_point_text),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (validWaypointPositions.contains(waypoint.position)) {
                    SelectableText(
                        text = stringResource(Res.string.waypoint),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Arrow indicator with warning circle if needed
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isInCircular) {
                    RedCircleIcon(size = 14.dp)
                }
                RightArrowIcon(
                    size = 20.dp,
                    tint = if (isInCircular) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                PositionText(waypoint.nextTargetPosition, MaterialTheme.typography.bodyLarge)

                // Warning icons next to target position
                if (isInCircular || isUnconnected) {
                    WarningIcon(size = 16.dp)
                }
            }

            // Target position type
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SelectableText(
                        text = stringResource(Res.string.waypoint_position_format,
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

                if (targets.contains(waypoint.nextTargetPosition)) {
                    SelectableText(
                        text = stringResource(Res.string.target_text),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                } else if (validWaypointPositions.contains(waypoint.nextTargetPosition)) {
                    SelectableText(
                        text = stringResource(Res.string.waypoint),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Warning messages section
            if (isInCircular || isUnconnected) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isInCircular) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            WarningIcon(size = 12.dp)
                            SelectableText(
                                text = stringResource(Res.string.circular_dependency_warning),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    if (isUnconnected) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            WarningIcon(size = 12.dp)
                            SelectableText(
                                text = stringResource(Res.string.unconnected_waypoint_warning),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Helper composable to display a position using the standard format
 */
@Composable
private fun PositionText(position: Position, style: androidx.compose.ui.text.TextStyle) {
    SelectableText(
        text = stringResource(Res.string.waypoint_position_format, position.x, position.y),
        style = style
    )
}
