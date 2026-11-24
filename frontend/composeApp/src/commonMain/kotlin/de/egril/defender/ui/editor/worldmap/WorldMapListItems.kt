package de.egril.defender.ui.editor.worldmap

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.egril.defender.editor.ConnectionType
import de.egril.defender.editor.WorldMapData
import de.egril.defender.editor.WorldMapLocationData
import de.egril.defender.editor.WorldMapPathData
import de.egril.defender.ui.icon.PencilIcon
import de.egril.defender.ui.icon.RightArrowIcon
import de.egril.defender.ui.icon.WarningIcon

/**
 * List item for a location
 */
@Composable
internal fun LocationListItem(
    location: WorldMapLocationData,
    worldMapData: WorldMapData,
    allLevels: List<de.egril.defender.editor.EditorLevel>,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    // Check if location has unfulfilled prerequisites
    val hasUnfulfilledPrereqs = worldMapData.hasLocationWithUnfulfilledPrerequisites(location.id, allLevels)

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick() },
        colors =
            CardDefaults.cardColors(
                containerColor =
                    when {
                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
            ),
    ) {
        Row(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Warning icon if location has unfulfilled prerequisites
                    if (hasUnfulfilledPrereqs) {
                        WarningIcon(
                            size = 16.dp,
                        )
                    }
                    Text(
                        text = location.name,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    text = "Position: ${location.position.x}, ${location.position.y} | ${location.levelIds.size} levels",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row {
                // Edit button
                TextButton(
                    onClick = onEdit,
                    contentPadding = PaddingValues(4.dp),
                ) {
                    PencilIcon(
                        size = 16.dp,
                    )
                }

                // Delete button
                TextButton(
                    onClick = onDelete,
                    contentPadding = PaddingValues(4.dp),
                ) {
                    Text(
                        text = "X",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Red,
                    )
                }
            }
        }
    }
}

/**
 * List item for a path
 */
@Composable
internal fun PathListItem(
    path: WorldMapPathData,
    locations: List<WorldMapLocationData>,
    allLevels: List<de.egril.defender.editor.EditorLevel>,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val fromName = locations.find { it.id == path.fromLocationId }?.name ?: path.fromLocationId
    val toName = locations.find { it.id == path.toLocationId }?.name ?: path.toLocationId

    // Check if connection is valid based on level prerequisites
    val isValid = path.isValidConnection(locations, allLevels)

    // Determine connection type display
    val connectionTypeText =
        when (path.type) {
            ConnectionType.ROAD -> "Road"
            ConnectionType.SEA_ROUTE -> "Sea"
        }
    val connectionTypeColor =
        when (path.type) {
            ConnectionType.ROAD -> Color(0xFFA0522D) // Brown
            ConnectionType.SEA_ROUTE -> Color(0xFF00CED1) // Lighter cyan/aqua for visibility
        }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Warning icon if connection is invalid
                    if (!isValid) {
                        WarningIcon(
                            size = 16.dp,
                        )
                    }
                    Text(
                        text = fromName,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    RightArrowIcon(size = 14.dp)
                    Text(
                        text = toName,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${path.controlPoints.size} waypoints",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = connectionTypeColor.copy(alpha = 0.2f),
                    ) {
                        Text(
                            text = connectionTypeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = connectionTypeColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            // Delete button
            TextButton(
                onClick = onDelete,
                contentPadding = PaddingValues(4.dp),
            ) {
                Text(
                    text = "X",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Red,
                )
            }
        }
    }
}
