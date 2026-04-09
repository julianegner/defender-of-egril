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
import de.egril.defender.ui.common.SelectableText
import de.egril.defender.ui.loadgame.SavefileLocationChip
import defender_of_egril.composeapp.generated.resources.*

/**
 * Card displaying a map in the map list
 */
@Composable
fun MapListCard(
    map: EditorMap,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit
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
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp).padding(top = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(4f)) {
                    SelectableText(
                        text = map.name.ifEmpty { "Map ${map.id}" },
                        style = MaterialTheme.typography.titleSmall
                    )
                    SelectableText(
                        text = "File: ${map.id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SelectableText(
                        text = "Size: ${map.width}x${map.height}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    SelectableText(
                        text = if (map.readyToUse) stringResource(Res.string.ready_to_use) else stringResource(Res.string.not_ready),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (map.readyToUse) Color.Green else Color.Red
                    )
                    if (map.isCommunity && map.communityAuthorUsername.isNotEmpty()) {
                        SelectableText(
                            text = map.communityAuthorUsername,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Minimap preview - 4x width
                Box(
                    modifier = Modifier
                        .weight(6f)
                        .width(480.dp)
                        .height(160.dp)
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

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDelete,
                        enabled = !map.isOfficial || de.egril.defender.OfficialEditMode.enabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        SelectableText(stringResource(Res.string.delete))
                    }
                    
                    Button(
                        onClick = onCopy
                    ) {
                        Text(stringResource(Res.string.copy_map))
                    }
                }
            }
            
            // Badges in upper right corner
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Ready/not ready check indicator
                if (map.readyToUse) {
                    CheckmarkIcon(
                        size = 20.dp,
                        tint = Color.Green
                    )
                } else {
                    CrossIcon(
                        size = 20.dp,
                        tint = Color.Red
                    )
                }
                // Official badge below the check
                if (map.isOfficial) {
                    SelectableText(
                        text = stringResource(Res.string.official),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                // Community badges: Local and/or Online
                if (map.isCommunity) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Community maps stored locally always also have an online counterpart
                        SavefileLocationChip(
                            label = stringResource(Res.string.savefile_chip_local),
                            color = MaterialTheme.colorScheme.tertiary,
                            onColor = MaterialTheme.colorScheme.onTertiary,
                            isMobile = false
                        )
                        SavefileLocationChip(
                            label = stringResource(Res.string.savefile_chip_remote),
                            color = MaterialTheme.colorScheme.primary,
                            onColor = MaterialTheme.colorScheme.onPrimary,
                            isMobile = false
                        )
                    }
                }
            }
        }
    }
}
