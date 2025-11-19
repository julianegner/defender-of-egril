package de.egril.defender.ui.editor.level.waypoint

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.model.Position
import de.egril.defender.ui.icon.RedCircleIcon
import de.egril.defender.ui.icon.RightArrowIcon
import de.egril.defender.ui.icon.TrashIcon
import de.egril.defender.ui.icon.WarningIcon
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.circular_dependency_warning
import defender_of_egril.composeapp.generated.resources.connect
import defender_of_egril.composeapp.generated.resources.spawn_point_text
import defender_of_egril.composeapp.generated.resources.target_text
import defender_of_egril.composeapp.generated.resources.unconnected_waypoint_warning
import defender_of_egril.composeapp.generated.resources.waypoint
import defender_of_egril.composeapp.generated.resources.waypoint_position_format

/**
 * Single node in the waypoint chain tree
 */
@Composable
fun WaypointChainNode(
    position: Position,
    isSpawn: Boolean,
    isTarget: Boolean,
    isInCircular: Boolean,
    isUnconnected: Boolean,
    indentLevel: Int,
    onDelete: (() -> Unit)?,
    onConnect: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.Companion
            .fillMaxWidth()
            .padding(start = (indentLevel * 16).dp),
        verticalAlignment = Alignment.Companion.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DepthIndicator(indentLevel, isInCircular)

        Column(modifier = Modifier.Companion.weight(1f)) {
            PositionAndTypeRow(position, isInCircular, isUnconnected)

            WaypointTypeLabel(isSpawn, isTarget)

            // Warning messages
            if (isInCircular) {
                WarningRow(
                    message = stringResource(Res.string.circular_dependency_warning)
                )
            }
            if (isUnconnected) {
                if (!isSpawn) {
                    WarningRow(
                        message = stringResource(Res.string.unconnected_waypoint_warning)
                    )
                }

                ConnectButton(onConnect) // Connect button (if unconnected)
            }
        }

        // Delete button (if allowed)
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                TrashIcon(size = 20.dp)
            }
        }
    }
}

@Composable
private fun DepthIndicator(indentLevel: Int, isInCircular: Boolean) {
    // Arrow indicator (except for first node)
    if (indentLevel > 0) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Companion.CenterVertically
        ) {
            if (isInCircular) {
                RedCircleIcon(size = 12.dp)
            }
            RightArrowIcon(
                size = 14.dp,
                tint = if (isInCircular) Color.Companion.Red else Color.Companion.Black
            )
        }
    }
}

@Composable
private fun PositionAndTypeRow(
    position: Position,
    isInCircular: Boolean,
    isUnconnected: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Companion.CenterVertically
    ) {
        Text(
            text = stringResource(Res.string.waypoint_position_format, position.x, position.y),
            style = MaterialTheme.typography.bodyMedium
        )

        // Warning icons
        if (isInCircular || isUnconnected) {
            WarningIcon(
                size = 14.dp
            )
        }
    }
}

@Composable
private fun ConnectButton(onConnect: (() -> Unit)?) {
    if (onConnect != null) {
        Button(
            onClick = onConnect,
            modifier = Modifier.Companion.height(32.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = stringResource(Res.string.connect),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun WarningRow(
    message: String,
    color: Color = MaterialTheme.colorScheme.error
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Companion.CenterVertically
    ) {
        WarningIcon(size = 12.dp)
        Text(
            text = message,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
private fun WaypointTypeLabel(isSpawn: Boolean, isTarget: Boolean) {
    if (isSpawn) {
        Text(
            text = stringResource(Res.string.spawn_point_text),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    } else if (isTarget) {
        Text(
            text = stringResource(Res.string.target_text),
            style = MaterialTheme.typography.labelSmall,
            color = Color.Companion.Green
        )
    } else {
        Text(
            text = stringResource(Res.string.waypoint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}
