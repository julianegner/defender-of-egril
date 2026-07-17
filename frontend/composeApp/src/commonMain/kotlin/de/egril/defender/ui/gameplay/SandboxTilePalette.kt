package de.egril.defender.ui.gameplay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.TileType
import de.egril.defender.model.RiverFlow
import de.egril.defender.ui.editor.RiverFlowIndicator
import de.egril.defender.ui.editor.TileTypeButton
import de.egril.defender.ui.icon.PencilIcon
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.flow_direction
import defender_of_egril.composeapp.generated.resources.flow_speed
import defender_of_egril.composeapp.generated.resources.river_properties
import defender_of_egril.composeapp.generated.resources.sandbox_done_editing
import defender_of_egril.composeapp.generated.resources.sandbox_edit_map

/**
 * Persistent map-tile selector for sandbox levels, reusing the map editor's [TileTypeButton].
 * It is always available while playing a sandbox level: selecting a tile type activates painting
 * (tapping a map tile repaints it), and "Done editing" (or reselecting the active type) turns
 * painting off so normal interactions resume.
 *
 * When the [TileType.RIVER] tile is selected, a river-properties section lets the user choose the
 * water flow direction and speed used for painted river tiles.
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
                    onClick = { onSelectTileType(if (selectedTileType == type) null else type) },
                )
            }

            // River flow selector: only shown while the RIVER tile is the active paint type.
            if (selectedTileType == TileType.RIVER) {
                RiverFlowSelector(
                    selectedRiverFlow = selectedRiverFlow,
                    onSelectRiverFlow = onSelectRiverFlow,
                    selectedRiverSpeed = selectedRiverSpeed,
                    onSelectRiverSpeed = onSelectRiverSpeed,
                )
            }

            if (selectedTileType != null) {
                OutlinedButton(onClick = { onSelectTileType(null) }) {
                    Text(stringResource(Res.string.sandbox_done_editing))
                }
            }
        }
    }
}

/**
 * Flow direction (all [RiverFlow] values) and speed (1 or 2) selector for painted river tiles.
 */
@Composable
private fun RiverFlowSelector(
    selectedRiverFlow: RiverFlow,
    onSelectRiverFlow: (RiverFlow) -> Unit,
    selectedRiverSpeed: Int,
    onSelectRiverSpeed: (Int) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.river_properties),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(Res.string.flow_direction),
            style = MaterialTheme.typography.labelSmall,
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.heightIn(max = 180.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(RiverFlow.entries) { flow ->
                OutlinedButton(onClick = { onSelectRiverFlow(flow) }) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .background(
                                        if (selectedRiverFlow == flow) Color(0xFF4682B4) else Color(0xFF4682B4).copy(alpha = 0.4f),
                                        RoundedCornerShape(3.dp),
                                    ).padding(2.dp),
                        ) {
                            RiverFlowIndicator(flowDirection = flow, flowSpeed = 1, size = 14.dp)
                        }
                        Text(flow.name.replace("_", " "), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        Text(
            text = stringResource(Res.string.flow_speed),
            style = MaterialTheme.typography.labelSmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(1, 2).forEach { speed ->
                OutlinedButton(onClick = { onSelectRiverSpeed(speed) }) {
                    Text(
                        text = speed.toString(),
                        fontWeight = if (selectedRiverSpeed == speed) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}
