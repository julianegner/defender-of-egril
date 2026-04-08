package de.egril.defender.ui.editor.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.TileType
import de.egril.defender.ui.icon.MagnifyingGlassIcon
import de.egril.defender.ui.editor.TileTypeButton
import de.egril.defender.ui.editor.getTileColor
import de.egril.defender.ui.editor.RiverFlowIndicator
import com.hyperether.resources.stringResource
import de.egril.defender.ui.common.SelectableText
import defender_of_egril.composeapp.generated.resources.*

/**
 * Header for the map editor with controls
 */
@Composable
fun MapEditorHeader(
    map: EditorMap,
    mapName: String,
    onMapNameChange: (String) -> Unit,
    mapAuthor: String,
    onMapAuthorChange: (String) -> Unit,
    selectedTileType: TileType,
    onTileTypeChange: (TileType) -> Unit,
    selectedRiverFlow: de.egril.defender.model.RiverFlow,
    onRiverFlowChange: (de.egril.defender.model.RiverFlow) -> Unit,
    selectedRiverSpeed: Int,
    onRiverSpeedChange: (Int) -> Unit,
    zoomLevel: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onChangeAllNoPlayToPath: () -> Unit,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    selectedTargetName: String = "",
    onTargetNameChange: (String) -> Unit = {},
    selectedTargetType: de.egril.defender.model.TargetType = de.egril.defender.model.TargetType.STANDARD,
    onTargetTypeChange: (de.egril.defender.model.TargetType) -> Unit = {}
) {
    if (isExpanded) {
        ExpandedMapEditorHeader(
            map = map,
            mapName = mapName,
            onMapNameChange = onMapNameChange,
            mapAuthor = mapAuthor,
            onMapAuthorChange = onMapAuthorChange,
            selectedTileType = selectedTileType,
            onTileTypeChange = onTileTypeChange,
            selectedRiverFlow = selectedRiverFlow,
            onRiverFlowChange = onRiverFlowChange,
            selectedRiverSpeed = selectedRiverSpeed,
            onRiverSpeedChange = onRiverSpeedChange,
            zoomLevel = zoomLevel,
            onZoomIn = onZoomIn,
            onZoomOut = onZoomOut,
            onChangeAllNoPlayToPath = onChangeAllNoPlayToPath,
            onCollapse = onToggleExpanded,
            selectedTargetName = selectedTargetName,
            onTargetNameChange = onTargetNameChange,
            selectedTargetType = selectedTargetType,
            onTargetTypeChange = onTargetTypeChange
        )
    } else {
        CollapsedMapEditorHeader(
            selectedTileType = selectedTileType,
            onTileTypeChange = onTileTypeChange,
            selectedRiverFlow = selectedRiverFlow,
            onRiverFlowChange = onRiverFlowChange,
            selectedRiverSpeed = selectedRiverSpeed,
            onRiverSpeedChange = onRiverSpeedChange,
            onExpand = onToggleExpanded,
            selectedTargetName = selectedTargetName,
            onTargetNameChange = onTargetNameChange,
            selectedTargetType = selectedTargetType,
            onTargetTypeChange = onTargetTypeChange
        )
    }
}

/**
 * Expanded version of the map editor header (original full header)
 */
@Composable
private fun ExpandedMapEditorHeader(
    map: EditorMap,
    mapName: String,
    onMapNameChange: (String) -> Unit,
    mapAuthor: String,
    onMapAuthorChange: (String) -> Unit,
    selectedTileType: TileType,
    onTileTypeChange: (TileType) -> Unit,
    selectedRiverFlow: de.egril.defender.model.RiverFlow,
    onRiverFlowChange: (de.egril.defender.model.RiverFlow) -> Unit,
    selectedRiverSpeed: Int,
    onRiverSpeedChange: (Int) -> Unit,
    zoomLevel: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onChangeAllNoPlayToPath: () -> Unit,
    onCollapse: () -> Unit,
    selectedTargetName: String = "",
    onTargetNameChange: (String) -> Unit = {},
    selectedTargetType: de.egril.defender.model.TargetType = de.egril.defender.model.TargetType.STANDARD,
    onTargetTypeChange: (de.egril.defender.model.TargetType) -> Unit = {}
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
            // Official map info banner
            if (map.isOfficial) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        de.egril.defender.ui.icon.InfoIcon(size = 20.dp)
                        SelectableText(
                            text = stringResource(Res.string.official_map_info),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            // Header with collapse button
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SelectableText(
                        text = stringResource(Res.string.editing_map, map.name.ifEmpty { map.id }),
                        style = MaterialTheme.typography.titleMedium
                    )
                    // Official badge
                    if (map.isOfficial) {
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = stringResource(Res.string.official),
                                    fontSize = 10.sp
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                labelColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
                
                Button(
                    onClick = onCollapse,
                    modifier = Modifier.height(32.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        de.egril.defender.ui.icon.TriangleUpIcon(size = 12.dp)
                        SelectableText(stringResource(Res.string.collapse), fontSize = 12.sp)
                    }
                }
            }
            
            // Map name input
            OutlinedTextField(
                value = mapName,
                onValueChange = onMapNameChange,
                label = { Text(stringResource(Res.string.map_name)) },
                enabled = !map.isOfficial || de.egril.defender.OfficialEditMode.enabled,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            
            // Author input
            OutlinedTextField(
                value = mapAuthor,
                onValueChange = onMapAuthorChange,
                label = { Text(stringResource(Res.string.author_optional)) },
                enabled = !map.isOfficial || de.egril.defender.OfficialEditMode.enabled,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            
            // Tile type selector
            SelectableText(
                text = stringResource(Res.string.select_tile_type),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(TileType.entries.toList()) { tileType ->
                    TileTypeButton(
                        tileType = tileType,
                        selected = selectedTileType == tileType,
                        onClick = { onTileTypeChange(tileType) }
                    )
                }
            }
            
            // River properties (shown when RIVER tile is selected)
            if (selectedTileType == TileType.RIVER) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        SelectableText(
                            text = "River Properties",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        
                        // Flow direction selector
                        SelectableText(
                            text = "Flow Direction:",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(de.egril.defender.model.RiverFlow.entries) { flow ->
                                Button(
                                    onClick = { onRiverFlowChange(flow) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedRiverFlow == flow) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.secondary
                                    ),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    SelectableText(flow.name.replace("_", " "), fontSize = 10.sp)
                                }
                            }
                        }
                        
                        // Flow speed selector
                        SelectableText(
                            text = "Flow Speed:",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = { onRiverSpeedChange(1) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedRiverSpeed == 1) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.secondary
                                ),
                                modifier = Modifier.height(32.dp)
                            ) {
                                SelectableText(stringResource(Res.string.speed_slow), fontSize = 10.sp)
                            }
                            Button(
                                onClick = { onRiverSpeedChange(2) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedRiverSpeed == 2) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.secondary
                                ),
                                modifier = Modifier.height(32.dp)
                            ) {
                                SelectableText(stringResource(Res.string.speed_fast), fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
            
            // Target properties (shown when TARGET tile is selected)
            if (selectedTileType == TileType.TARGET) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SelectableText(
                            text = stringResource(Res.string.target_name_label),
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(
                            value = selectedTargetName,
                            onValueChange = onTargetNameChange,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                        SelectableText(
                            text = stringResource(Res.string.target_type_label),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            de.egril.defender.model.TargetType.entries.forEach { type ->
                                val label = when (type) {
                                    de.egril.defender.model.TargetType.STANDARD -> stringResource(Res.string.target_type_standard)
                                    de.egril.defender.model.TargetType.SINGLE_HIT -> stringResource(Res.string.target_type_single_hit)
                                }
                                Button(
                                    onClick = { onTargetTypeChange(type) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedTargetType == type)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.secondary
                                    ),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    SelectableText(label, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Change All NO_PLAY to PATH button
            Button(
                onClick = onChangeAllNoPlayToPath,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                SelectableText(stringResource(Res.string.change_all_no_play_to_path))
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

/**
 * Collapsed version of the map editor header - small card on the left side
 */
@Composable
private fun CollapsedMapEditorHeader(
    selectedTileType: TileType,
    onTileTypeChange: (TileType) -> Unit,
    selectedRiverFlow: de.egril.defender.model.RiverFlow,
    onRiverFlowChange: (de.egril.defender.model.RiverFlow) -> Unit,
    selectedRiverSpeed: Int,
    onRiverSpeedChange: (Int) -> Unit,
    onExpand: () -> Unit,
    selectedTargetName: String = "",
    onTargetNameChange: (String) -> Unit = {},
    selectedTargetType: de.egril.defender.model.TargetType = de.egril.defender.model.TargetType.STANDARD,
    onTargetTypeChange: (de.egril.defender.model.TargetType) -> Unit = {}
) {
    var showRiverPropertiesDialog by remember { mutableStateOf(false) }
    var showTargetPropertiesDialog by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .width(280.dp)
            .padding(top = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tile type dropdown - styled to look like a dropdown
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = getTileColor(selectedTileType).copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // Color indicator box
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(getTileColor(selectedTileType), shape = MaterialTheme.shapes.small)
                            )
                            // Tile type name
                            SelectableText(
                                text = selectedTileType.name,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            // River flow indicator if it's a river tile
                            if (selectedTileType == TileType.RIVER) {
                                RiverFlowIndicator(
                                    flowDirection = selectedRiverFlow,
                                    flowSpeed = selectedRiverSpeed,
                                    size = 14.dp
                                )
                            }
                            // Target type indicator if it's a target tile
                            if (selectedTileType == TileType.TARGET) {
                                val typeLabel = when (selectedTargetType) {
                                    de.egril.defender.model.TargetType.STANDARD -> stringResource(Res.string.target_type_standard)
                                    de.egril.defender.model.TargetType.SINGLE_HIT -> stringResource(Res.string.target_type_single_hit)
                                }
                                SelectableText(
                                    text = typeLabel,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        // Dropdown triangle
                        de.egril.defender.ui.icon.TriangleDownIcon(size = 10.dp)
                    }
                }
                
                // Dropdown menu
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    TileType.entries.forEach { tileType ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Color indicator box
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .background(getTileColor(tileType), shape = MaterialTheme.shapes.small)
                                    )
                                    // Tile type name
                                    Text(tileType.name)
                                }
                            },
                            onClick = {
                                onTileTypeChange(tileType)
                                expanded = false
                                // Show properties dialog if RIVER or TARGET is selected
                                if (tileType == TileType.RIVER) {
                                    showRiverPropertiesDialog = true
                                } else if (tileType == TileType.TARGET) {
                                    showTargetPropertiesDialog = true
                                }
                            }
                        )
                    }
                }
            }

            // Show target properties button when TARGET is already selected
            if (selectedTileType == TileType.TARGET) {
                IconButton(
                    onClick = { showTargetPropertiesDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    de.egril.defender.ui.icon.PencilIcon(size = 16.dp)
                }
            }
            
            // Expand button - just icon, no text
            IconButton(
                onClick = onExpand,
                modifier = Modifier.size(32.dp)
            ) {
                de.egril.defender.ui.icon.LeftArrowIcon(size = 16.dp)
            }
        }
    }
    
    // River properties dialog
    if (showRiverPropertiesDialog) {
        AlertDialog(
            onDismissRequest = { showRiverPropertiesDialog = false },
            title = { SelectableText(stringResource(Res.string.river_properties)) },
            text = {
                Column {
                    Text(stringResource(Res.string.flow_direction), style = MaterialTheme.typography.bodyMedium)
                    
                    // Display flow directions in 2 rows (4 items per row)
                    val flows = de.egril.defender.model.RiverFlow.entries
                    val firstRowFlows = flows.take(4)
                    val secondRowFlows = flows.drop(4)
                    
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // First row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            firstRowFlows.forEach { flow ->
                                Button(
                                    onClick = { onRiverFlowChange(flow) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedRiverFlow == flow) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.secondary
                                    ),
                                    modifier = Modifier.height(32.dp).weight(1f)
                                ) {
                                    Text(flow.name.replace("_", " "), fontSize = 10.sp)
                                }
                            }
                        }
                        
                        // Second row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            secondRowFlows.forEach { flow ->
                                Button(
                                    onClick = { onRiverFlowChange(flow) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedRiverFlow == flow) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.secondary
                                    ),
                                    modifier = Modifier.height(32.dp).weight(1f)
                                ) {
                                    Text(flow.name.replace("_", " "), fontSize = 10.sp)
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(stringResource(Res.string.flow_speed), style = MaterialTheme.typography.bodyMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = { onRiverSpeedChange(1) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedRiverSpeed == 1) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(stringResource(Res.string.speed_slow), fontSize = 10.sp)
                        }
                        Button(
                            onClick = { onRiverSpeedChange(2) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedRiverSpeed == 2) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(stringResource(Res.string.speed_fast), fontSize = 10.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showRiverPropertiesDialog = false }) {
                    Text(stringResource(Res.string.ok))
                }
            }
        )
    }

    // Target properties dialog
    if (showTargetPropertiesDialog) {
        var localName by remember(showTargetPropertiesDialog) { mutableStateOf(selectedTargetName) }
        AlertDialog(
            onDismissRequest = { showTargetPropertiesDialog = false },
            title = { SelectableText(stringResource(Res.string.target_name_label)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = localName,
                        onValueChange = { localName = it },
                        label = { Text(stringResource(Res.string.target_name_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Text(stringResource(Res.string.target_type_label), style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        de.egril.defender.model.TargetType.entries.forEach { type ->
                            val label = when (type) {
                                de.egril.defender.model.TargetType.STANDARD -> stringResource(Res.string.target_type_standard)
                                de.egril.defender.model.TargetType.SINGLE_HIT -> stringResource(Res.string.target_type_single_hit)
                            }
                            Button(
                                onClick = { onTargetTypeChange(type) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedTargetType == type)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.secondary
                                ),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text(label, fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onTargetNameChange(localName)
                    showTargetPropertiesDialog = false
                }) {
                    SelectableText(stringResource(Res.string.ok))
                }
            },
            dismissButton = {
                Button(onClick = { showTargetPropertiesDialog = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
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
        SelectableText(
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
                    SelectableText("-", fontSize = 12.sp)
                }
            }
            SelectableText(
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
                    SelectableText("+", fontSize = 12.sp)
                }
            }
        }
    }
}
