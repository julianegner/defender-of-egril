package de.egril.defender.ui.editor.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.TileType
import de.egril.defender.ui.icon.MagnifyingGlassIcon
import de.egril.defender.ui.editor.TileTypeButton
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*

/**
 * Header for the map editor with controls
 */
@Composable
fun MapEditorHeader(
    map: EditorMap,
    mapName: String,
    onMapNameChange: (String) -> Unit,
    selectedTileType: TileType,
    onTileTypeChange: (TileType) -> Unit,
    zoomLevel: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp)
        ) {
            // Header
            Text(
                text = stringResource(Res.string.editing_map).replace("%s", map.name.ifEmpty { map.id }),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Map name input
            OutlinedTextField(
                value = mapName,
                onValueChange = onMapNameChange,
                label = { Text(stringResource(Res.string.map_name)) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            
            // Tile type selector
            Text(
                text = stringResource(Res.string.select_tile_type),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // All tile types are selectable
                items(TileType.entries) { tileType ->
                    TileTypeButton(
                        tileType = tileType,
                        selected = selectedTileType == tileType,
                        onClick = { onTileTypeChange(tileType) }
                    )
                }
            }

            ZoomControls(
                map = map,
                zoomLevel = zoomLevel,
                onZoomIn = onZoomIn,
                onZoomOut = onZoomOut
            )
        }
    }
}

@Composable
fun ZoomControls(
    map: EditorMap,
    zoomLevel: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit
) {
    // Zoom controls
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${stringResource(Res.string.click_hexagons_to_paint)} (${map.width}x${map.height})",
            style = MaterialTheme.typography.bodySmall
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onZoomOut,
                modifier = Modifier.height(32.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    MagnifyingGlassIcon(size = 14.dp, tint = Color.White)
                    Text("-", fontSize = 12.sp)
                }
            }
            Text(
                text = "${(zoomLevel * 100).toInt()}%",
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Button(
                onClick = onZoomIn,
                modifier = Modifier.height(32.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    MagnifyingGlassIcon(size = 14.dp, tint = Color.White)
                    Text("+", fontSize = 12.sp)
                }
            }
        }
    }
}
