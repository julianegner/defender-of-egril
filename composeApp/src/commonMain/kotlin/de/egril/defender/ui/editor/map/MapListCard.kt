package de.egril.defender.ui.editor.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.egril.defender.editor.EditorMap
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import de.egril.defender.ui.icon.CheckmarkIcon
import de.egril.defender.ui.icon.CrossIcon
import de.egril.defender.ui.hexagon.HexagonMinimapFromEditorMap
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*

/**
 * Card displaying a map in the map list
 */
@Composable
fun MapListCard(
    map: EditorMap,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = map.name.ifEmpty { "Map ${map.id}" },
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (map.readyToUse) {
                        CheckmarkIcon(
                            size = 16.dp,
                            tint = Color.Green
                        )
                    } else {
                        CrossIcon(
                            size = 16.dp,
                            tint = Color.Red
                        )
                    }
                }
                Text(
                    text = "File: ${map.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Size: ${map.width}x${map.height}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = if (map.readyToUse) stringResource(Res.string.ready_to_use) else stringResource(Res.string.not_ready),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (map.readyToUse) Color.Green else Color.Red
                )
            }
            
            // Minimap preview
            Box(
                modifier = Modifier
                    .weight(1f)
                    .width(120.dp)
                    .height(80.dp)
                    .padding(4.dp)
            ) {
                // Create a dummy level for the minimap (we only need it for the grid dimensions)
                val dummyLevel = remember(map.id) {
                    Level(
                        id = 0,
                        name = map.name,
                        gridWidth = map.width,
                        gridHeight = map.height,
                        startPositions = emptyList(),
                        targetPositions = listOf(Position(0, 0)),
                        pathCells = emptySet(),
                        buildIslands = emptySet(),
                        attackerWaves = emptyList()
                    )
                }

                // Use HexagonMinimap with a direct map reference
                HexagonMinimapFromEditorMap(
                    map = map,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.weight(3f))

            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(Res.string.delete))
            }
        }
    }
}
