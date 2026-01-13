package de.egril.defender.ui.editor.map

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.egril.defender.editor.EditorMap
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import de.egril.defender.ui.icon.CheckmarkIcon
import de.egril.defender.ui.hexagon.HexagonMinimapFromEditorMap

/**
 * Card for selecting a map in the level editor
 */
@Composable
fun MapSelectionCard(
    map: EditorMap,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = map.name.ifEmpty { map.id },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${map.width}x${map.height}",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp
            )
            
            // Mini-map preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
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
            
            // Status indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (map.readyToUse) {
                    CheckmarkIcon(size = 12.dp, tint = Color.Green)
                } else {
                    Text("✗", color = Color.Red, fontSize = 12.sp)
                }
                Text(
                    text = if (map.readyToUse) "Ready" else "Not ready",
                    fontSize = 10.sp,
                    color = if (map.readyToUse) Color.Green else Color.Red
                )
            }
        }
    }
}
