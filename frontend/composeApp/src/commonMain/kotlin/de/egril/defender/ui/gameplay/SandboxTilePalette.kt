package de.egril.defender.ui.gameplay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.TileType
import de.egril.defender.model.RiverFlow
import de.egril.defender.ui.editor.RiverFlowIndicator
import de.egril.defender.ui.editor.TileTypeButton
import de.egril.defender.ui.icon.PencilIcon
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.flow_direction
import defender_of_egril.composeapp.generated.resources.flow_speed
import defender_of_egril.composeapp.generated.resources.ok
import defender_of_egril.composeapp.generated.resources.river_properties
import defender_of_egril.composeapp.generated.resources.sandbox_done_editing
import defender_of_egril.composeapp.generated.resources.sandbox_edit_map
import defender_of_egril.composeapp.generated.resources.speed_fast
import defender_of_egril.composeapp.generated.resources.speed_slow

/**
 * Persistent map-tile selector for sandbox levels, reusing the map editor's [TileTypeButton].
 * It is always available while playing a sandbox level: selecting a tile type activates painting
 * (tapping a map tile repaints it), and "Done editing" (or reselecting the active type) turns
 * painting off so normal interactions resume.
 *
 * When the [TileType.RIVER] tile is selected, a small indicator row shows the current flow
 * direction and opens a compact dialog to change it, matching the map-editor behaviour.
 */
@Composable
fun SandboxTilePalette(
    selectedTileType: TileType?,
    onSelectTileType: (TileType?) -> Unit,
    selectedRiverFlow: RiverFlow,
    onSelectRiverFlow: (RiverFlow) -> Unit,
    selectedRiverSpeed: Int,
    onSelectRiverSpeed: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showRiverDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 3.dp,
        shadowElevation = 3.dp,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .padding(8.dp)
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                PencilIcon(size = 16.dp)
                Text(
                    text = stringResource(Res.string.sandbox_edit_map),
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            TileType.entries.forEach { type ->
                TileTypeButton(
                    tileType = type,
                    selected = selectedTileType == type,
                    // Reselecting the active tile toggles painting off.
                    onClick = {
                        val toggled = if (selectedTileType == type) null else type
                        onSelectTileType(toggled)
                        if (toggled == TileType.RIVER) {
                            showRiverDialog = true
                        }
                    },
                )
            }

            // River flow indicator: only shown while the RIVER tile is the active paint type.
            // Clicking it reopens the compact direction dialog.
            if (selectedTileType == TileType.RIVER) {
                OutlinedButton(onClick = { showRiverDialog = true }) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        RiverFlowIndicator(
                            flowDirection = selectedRiverFlow,
                            flowSpeed = selectedRiverSpeed,
                            size = 14.dp,
                        )
                        Text(
                            text = selectedRiverFlow.name.replace("_", " "),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }

            if (selectedTileType != null) {
                OutlinedButton(onClick = { onSelectTileType(null) }) {
                    Text(stringResource(Res.string.sandbox_done_editing))
                }
            }
        }
    }

    if (showRiverDialog) {
        RiverPropertiesDialog(
            selectedRiverFlow = selectedRiverFlow,
            onSelectRiverFlow = onSelectRiverFlow,
            selectedRiverSpeed = selectedRiverSpeed,
            onSelectRiverSpeed = onSelectRiverSpeed,
            onDismiss = { showRiverDialog = false },
        )
    }
}

/**
 * Compact dialog for selecting the flow direction and speed of painted river tiles.
 * Mirrors the dialog used in the map editor's [CollapsedMapEditorHeader].
 */
@Composable
private fun RiverPropertiesDialog(
    selectedRiverFlow: RiverFlow,
    onSelectRiverFlow: (RiverFlow) -> Unit,
    selectedRiverSpeed: Int,
    onSelectRiverSpeed: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val flows = RiverFlow.entries
    val firstRowFlows = flows.take(4)
    val secondRowFlows = flows.drop(4)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.river_properties)) },
        text = {
            Column {
                Text(stringResource(Res.string.flow_direction), style = MaterialTheme.typography.bodyMedium)
                Column(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        firstRowFlows.forEach { flow ->
                            Button(
                                onClick = { onSelectRiverFlow(flow) },
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor =
                                            if (selectedRiverFlow == flow) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.secondary
                                            },
                                    ),
                                modifier = Modifier.height(32.dp).weight(1f),
                            ) {
                                Text(flow.name.replace("_", " "), fontSize = 10.sp)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        secondRowFlows.forEach { flow ->
                            Button(
                                onClick = { onSelectRiverFlow(flow) },
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor =
                                            if (selectedRiverFlow == flow) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.secondary
                                            },
                                    ),
                                modifier = Modifier.height(32.dp).weight(1f),
                            ) {
                                Text(flow.name.replace("_", " "), fontSize = 10.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(Res.string.flow_speed), style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = { onSelectRiverSpeed(1) },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    if (selectedRiverSpeed == 1) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.secondary
                                    },
                            ),
                        modifier = Modifier.height(32.dp),
                    ) {
                        Text(stringResource(Res.string.speed_slow), fontSize = 10.sp)
                    }
                    Button(
                        onClick = { onSelectRiverSpeed(2) },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    if (selectedRiverSpeed == 2) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.secondary
                                    },
                            ),
                        modifier = Modifier.height(32.dp),
                    ) {
                        Text(stringResource(Res.string.speed_fast), fontSize = 10.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(Res.string.ok))
            }
        },
    )
}
