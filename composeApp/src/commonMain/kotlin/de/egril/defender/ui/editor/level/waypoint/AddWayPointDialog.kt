package de.egril.defender.ui.editor.level.waypoint

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.EditorWaypoint
import de.egril.defender.model.Position
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.add_waypoint
import defender_of_egril.composeapp.generated.resources.cancel
import defender_of_egril.composeapp.generated.resources.select_source_position
import defender_of_egril.composeapp.generated.resources.select_target_position
import defender_of_egril.composeapp.generated.resources.spawn_point_text
import defender_of_egril.composeapp.generated.resources.target_text
import defender_of_egril.composeapp.generated.resources.waypoint
import defender_of_egril.composeapp.generated.resources.waypoint_exists_error

/**
 * Dialog for adding a new waypoint connection
 */
@Composable
fun AddWaypointDialog(
    waypointTiles: List<Position>,
    spawnPoints: List<Position>,
    target: Position?,
    existingWaypoints: List<EditorWaypoint>,
    onDismiss: () -> Unit,
    onAdd: (Position, Position) -> Unit
) {
    var selectedSource by remember { mutableStateOf<Position?>(null) }
    var selectedTarget by remember { mutableStateOf<Position?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Valid source positions: spawn points + waypoint tiles
    val validSources = remember(spawnPoints, waypointTiles) {
        (spawnPoints + waypointTiles).distinct()
    }

    // Valid target positions: waypoint tiles + target
    val validTargets = remember(waypointTiles, target) {
        val targets = waypointTiles.toMutableList()
        if (target != null) targets.add(target)
        targets.distinct()
    }

    // Get error message strings in composable context
    val waypointExistsErrorMsg = stringResource(Res.string.waypoint_exists_error)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.add_waypoint)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Source selection
                Text(
                    text = stringResource(Res.string.select_source_position),
                    style = MaterialTheme.typography.labelMedium
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(validSources) { pos ->
                        val isSpawn = spawnPoints.contains(pos)
                        val label = if (isSpawn) {
                            "${stringResource(Res.string.spawn_point_text)} (${pos.x}, ${pos.y})"
                        } else {
                            "${stringResource(Res.string.waypoint)} (${pos.x}, ${pos.y})"
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedSource = pos
                                errorMessage = null
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedSource == pos,
                                onClick = {
                                    selectedSource = pos
                                    errorMessage = null
                                }
                            )
                            Text(label)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Target selection
                Text(
                    text = stringResource(Res.string.select_target_position),
                    style = MaterialTheme.typography.labelMedium
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(validTargets) { pos ->
                        val isTarget = target == pos
                        val label = if (isTarget) {
                            "${stringResource(Res.string.target_text)} (${pos.x}, ${pos.y})"
                        } else {
                            "${stringResource(Res.string.waypoint)} (${pos.x}, ${pos.y})"
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedTarget = pos
                                errorMessage = null
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedTarget == pos,
                                onClick = {
                                    selectedTarget = pos
                                    errorMessage = null
                                }
                            )
                            Text(label)
                        }
                    }
                }

                // Error message
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedSource == null || selectedTarget == null) {
                        errorMessage = "Please select both source and target positions"
                        return@Button
                    }

                    // Check if waypoint already exists for this source
                    val exists = existingWaypoints.any { it.position == selectedSource }
                    if (exists) {
                        errorMessage = waypointExistsErrorMsg
                        return@Button
                    }

                    onAdd(selectedSource!!, selectedTarget!!)
                }
            ) {
                Text(stringResource(Res.string.add_waypoint))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}
