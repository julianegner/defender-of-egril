package de.egril.defender.ui.editor.map

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.TileType
import de.egril.defender.ui.editor.RiverFlowIndicator
import de.egril.defender.ui.editor.TileTypeButton
import de.egril.defender.ui.editor.getTileColor
import de.egril.defender.ui.icon.MagnifyingGlassIcon
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
    mapToolingInfo: String,
    onMapToolingInfoChange: (String) -> Unit,
    allowNoBuildableTiles: Boolean,
    onAllowNoBuildableTilesChange: (Boolean) -> Unit,
    allowNoDirectPath: Boolean,
    onAllowNoDirectPathChange: (Boolean) -> Unit,
    mapWidth: Int,
    mapHeight: Int,
    resizeLeft: String,
    onResizeLeftChange: (String) -> Unit,
    resizeRight: String,
    onResizeRightChange: (String) -> Unit,
    resizeTop: String,
    onResizeTopChange: (String) -> Unit,
    resizeBottom: String,
    onResizeBottomChange: (String) -> Unit,
    onApplyResize: () -> Unit,
    canApplyResize: Boolean,
    resultingMapWidth: Int,
    resultingMapHeight: Int,
    showUnsafeResizeWarning: Boolean,
    mapUsageLevelNames: List<String>,
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
    onTargetTypeChange: (de.egril.defender.model.TargetType) -> Unit = {},
    selectedSpawnPointType: de.egril.defender.model.SpawnPointType = de.egril.defender.model.SpawnPointType.LAND,
    onSpawnPointTypeChange: (de.egril.defender.model.SpawnPointType) -> Unit = {},
    backgroundImageLoaded: Boolean = false,
    onLoadBackgroundImage: () -> Unit = {},
    onClearBackgroundImage: () -> Unit = {},
    mapOverlayAlpha: Float = 0.7f,
    onMapOverlayAlphaChange: (Float) -> Unit = {},
    showMapFlowOverlay: Boolean = false,
    onToggleMapFlowOverlay: () -> Unit = {},
    showMapPathPreviewOverlay: Boolean = false,
    onToggleMapPathPreviewOverlay: () -> Unit = {},
    onUndo: () -> Unit = {},
    canUndo: Boolean = false,
    onRedo: () -> Unit = {},
    canRedo: Boolean = false,
    onOpenAreaClipboard: () -> Unit = {},
) {
    if (isExpanded) {
        ExpandedMapEditorHeader(
            map = map,
            mapName = mapName,
            onMapNameChange = onMapNameChange,
            mapAuthor = mapAuthor,
            onMapAuthorChange = onMapAuthorChange,
            mapToolingInfo = mapToolingInfo,
            onMapToolingInfoChange = onMapToolingInfoChange,
            allowNoBuildableTiles = allowNoBuildableTiles,
            onAllowNoBuildableTilesChange = onAllowNoBuildableTilesChange,
            allowNoDirectPath = allowNoDirectPath,
            onAllowNoDirectPathChange = onAllowNoDirectPathChange,
            mapWidth = mapWidth,
            mapHeight = mapHeight,
            resizeLeft = resizeLeft,
            onResizeLeftChange = onResizeLeftChange,
            resizeRight = resizeRight,
            onResizeRightChange = onResizeRightChange,
            resizeTop = resizeTop,
            onResizeTopChange = onResizeTopChange,
            resizeBottom = resizeBottom,
            onResizeBottomChange = onResizeBottomChange,
            onApplyResize = onApplyResize,
            canApplyResize = canApplyResize,
            resultingMapWidth = resultingMapWidth,
            resultingMapHeight = resultingMapHeight,
            showUnsafeResizeWarning = showUnsafeResizeWarning,
            mapUsageLevelNames = mapUsageLevelNames,
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
            onTargetTypeChange = onTargetTypeChange,
            selectedSpawnPointType = selectedSpawnPointType,
            onSpawnPointTypeChange = onSpawnPointTypeChange,
            backgroundImageLoaded = backgroundImageLoaded,
            onLoadBackgroundImage = onLoadBackgroundImage,
            onClearBackgroundImage = onClearBackgroundImage,
            mapOverlayAlpha = mapOverlayAlpha,
            onMapOverlayAlphaChange = onMapOverlayAlphaChange,
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
            onChangeAllNoPlayToPath = onChangeAllNoPlayToPath,
            selectedTargetName = selectedTargetName,
            onTargetNameChange = onTargetNameChange,
            selectedTargetType = selectedTargetType,
            onTargetTypeChange = onTargetTypeChange,
            selectedSpawnPointType = selectedSpawnPointType,
            onSpawnPointTypeChange = onSpawnPointTypeChange,
            backgroundImageLoaded = backgroundImageLoaded,
            onLoadBackgroundImage = onLoadBackgroundImage,
            onClearBackgroundImage = onClearBackgroundImage,
            showMapFlowOverlay = showMapFlowOverlay,
            onToggleMapFlowOverlay = onToggleMapFlowOverlay,
            showMapPathPreviewOverlay = showMapPathPreviewOverlay,
            onToggleMapPathPreviewOverlay = onToggleMapPathPreviewOverlay,
            onUndo = onUndo,
            canUndo = canUndo,
            onRedo = onRedo,
            canRedo = canRedo,
            onOpenAreaClipboard = onOpenAreaClipboard,
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
    mapToolingInfo: String,
    onMapToolingInfoChange: (String) -> Unit,
    allowNoBuildableTiles: Boolean,
    onAllowNoBuildableTilesChange: (Boolean) -> Unit,
    allowNoDirectPath: Boolean,
    onAllowNoDirectPathChange: (Boolean) -> Unit,
    mapWidth: Int,
    mapHeight: Int,
    resizeLeft: String,
    onResizeLeftChange: (String) -> Unit,
    resizeRight: String,
    onResizeRightChange: (String) -> Unit,
    resizeTop: String,
    onResizeTopChange: (String) -> Unit,
    resizeBottom: String,
    onResizeBottomChange: (String) -> Unit,
    onApplyResize: () -> Unit,
    canApplyResize: Boolean,
    resultingMapWidth: Int,
    resultingMapHeight: Int,
    showUnsafeResizeWarning: Boolean,
    mapUsageLevelNames: List<String>,
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
    onTargetTypeChange: (de.egril.defender.model.TargetType) -> Unit = {},
    selectedSpawnPointType: de.egril.defender.model.SpawnPointType = de.egril.defender.model.SpawnPointType.LAND,
    onSpawnPointTypeChange: (de.egril.defender.model.SpawnPointType) -> Unit = {},
    backgroundImageLoaded: Boolean = false,
    onLoadBackgroundImage: () -> Unit = {},
    onClearBackgroundImage: () -> Unit = {},
    mapOverlayAlpha: Float = 0.7f,
    onMapOverlayAlphaChange: (Float) -> Unit = {},
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp),
        ) {
            // Official map info banner
            if (map.isOfficial) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        de.egril.defender.ui.icon
                            .InfoIcon(size = 20.dp)
                        Text(
                            text = stringResource(Res.string.official_map_info),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            // Header with collapse button
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.editing_map, map.name.ifEmpty { map.id }),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    // Official badge
                    if (map.isOfficial) {
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = stringResource(Res.string.official),
                                    fontSize = 10.sp,
                                )
                            },
                            colors =
                                AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    labelColor = MaterialTheme.colorScheme.primary,
                                ),
                            modifier = Modifier.height(24.dp),
                        )
                    }
                }

                Button(
                    onClick = onCollapse,
                    modifier = Modifier.height(32.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        de.egril.defender.ui.icon
                            .TriangleUpIcon(size = 12.dp)
                        Text(stringResource(Res.string.collapse), fontSize = 12.sp)
                    }
                }
            }

            // Map name input
            OutlinedTextField(
                value = mapName,
                onValueChange = onMapNameChange,
                label = { Text(stringResource(Res.string.map_name)) },
                enabled = !map.isOfficial || de.egril.defender.OfficialEditMode.enabled,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )

            // Author input
            OutlinedTextField(
                value = mapAuthor,
                onValueChange = onMapAuthorChange,
                label = { Text(stringResource(Res.string.author_optional)) },
                enabled = !map.isOfficial || de.egril.defender.OfficialEditMode.enabled,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )

            OutlinedTextField(
                value = mapToolingInfo,
                onValueChange = onMapToolingInfoChange,
                label = { Text(stringResource(Res.string.map_tooling_info)) },
                enabled = !map.isOfficial || de.egril.defender.OfficialEditMode.enabled,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                singleLine = true,
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(Res.string.allow_no_buildable_tiles),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = allowNoBuildableTiles,
                    onCheckedChange = onAllowNoBuildableTilesChange,
                    enabled = !map.isOfficial || de.egril.defender.OfficialEditMode.enabled,
                )
            }
            if (allowNoBuildableTiles && map.getBuildAreas().isEmpty()) {
                Text(
                    text = stringResource(Res.string.allow_no_buildable_tiles_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(Res.string.allow_no_direct_path),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = allowNoDirectPath,
                    onCheckedChange = onAllowNoDirectPathChange,
                    enabled = !map.isOfficial || de.egril.defender.OfficialEditMode.enabled,
                )
            }
            if (allowNoDirectPath) {
                Text(
                    text = stringResource(Res.string.allow_no_direct_path_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "${stringResource(Res.string.map_size)}: $mapWidth x $mapHeight",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(Res.string.resize_map_hint),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = resizeLeft,
                            onValueChange = { if (it.isEmpty() || it == "-" || it.matches(Regex("-?[0-9]+"))) onResizeLeftChange(it) },
                            label = { Text(stringResource(Res.string.columns_left)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = resizeRight,
                            onValueChange = { if (it.isEmpty() || it == "-" || it.matches(Regex("-?[0-9]+"))) onResizeRightChange(it) },
                            label = { Text(stringResource(Res.string.columns_right)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = resizeTop,
                            onValueChange = { if (it.isEmpty() || it == "-" || it.matches(Regex("-?[0-9]+"))) onResizeTopChange(it) },
                            label = { Text(stringResource(Res.string.rows_top)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = resizeBottom,
                            onValueChange = { if (it.isEmpty() || it == "-" || it.matches(Regex("-?[0-9]+"))) onResizeBottomChange(it) },
                            label = { Text(stringResource(Res.string.rows_bottom)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                    }
                    Text(
                        text = stringResource(Res.string.resulting_map_size, resultingMapWidth, resultingMapHeight),
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            if (canApplyResize) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                    )
                    if (showUnsafeResizeWarning) {
                        Text(
                            text =
                                "${stringResource(Res.string.map_resize_in_use_warning)} ${stringResource(Res.string.used_in_levels)}: ${mapUsageLevelNames.joinToString(", ")}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Button(
                        onClick = onApplyResize,
                        enabled = canApplyResize && (!map.isOfficial || de.egril.defender.OfficialEditMode.enabled),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(Res.string.apply))
                    }
                }
            }

            // Tile type selector
            Text(
                text = stringResource(Res.string.select_tile_type),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 4.dp),
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(TileType.entries.toList()) { tileType ->
                    TileTypeButton(
                        tileType = tileType,
                        selected = selectedTileType == tileType,
                        onClick = { onTileTypeChange(tileType) },
                    )
                }
            }

            // River properties (shown when RIVER tile is selected)
            if (selectedTileType == TileType.RIVER) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "River Properties",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )

                        // Flow direction selector
                        Text(
                            text = "Flow Direction:",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(de.egril.defender.model.RiverFlow.entries) { flow ->
                                Button(
                                    onClick = { onRiverFlowChange(flow) },
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor =
                                                if (selectedRiverFlow == flow) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.secondary
                                                },
                                        ),
                                    modifier = Modifier.height(32.dp),
                                ) {
                                    Text(flow.name.replace("_", " "), fontSize = 10.sp)
                                }

                                if (selectedTileType == TileType.SPAWN_POINT) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        colors =
                                            CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            ),
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = stringResource(Res.string.spawn_point_type),
                                                style = MaterialTheme.typography.bodySmall,
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                de.egril.defender.model.SpawnPointType.entries.forEach { type ->
                                                    val label =
                                                        when (type) {
                                                            de.egril.defender.model.SpawnPointType.LAND -> stringResource(Res.string.spawn_point_type_land)
                                                            de.egril.defender.model.SpawnPointType.WATER -> stringResource(Res.string.spawn_point_type_water)
                                                        }
                                                    Button(
                                                        onClick = { onSpawnPointTypeChange(type) },
                                                        colors =
                                                            ButtonDefaults.buttonColors(
                                                                containerColor =
                                                                    if (selectedSpawnPointType == type) {
                                                                        MaterialTheme.colorScheme.primary
                                                                    } else {
                                                                        MaterialTheme.colorScheme.secondary
                                                                    },
                                                            ),
                                                        modifier = Modifier.height(32.dp),
                                                    ) {
                                                        Text(label, fontSize = 10.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Flow speed selector
                        Text(
                            text = "Flow Speed:",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Button(
                                onClick = { onRiverSpeedChange(1) },
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
                                onClick = { onRiverSpeedChange(2) },
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

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                onTileTypeChange(TileType.SPAWN_POINT)
                                onSpawnPointTypeChange(de.egril.defender.model.SpawnPointType.WATER)
                            },
                            modifier = Modifier.height(32.dp),
                        ) {
                            Text(
                                "${stringResource(Res.string.spawn_point)} (${stringResource(Res.string.spawn_point_type_water)})",
                                fontSize = 10.sp,
                            )
                        }
                    }
                }
            }

            // Target properties (shown when TARGET tile is selected)
            if (selectedTileType == TileType.TARGET) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(Res.string.target_name_label),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        OutlinedTextField(
                            value = selectedTargetName,
                            onValueChange = onTargetNameChange,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = stringResource(Res.string.target_type_label),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            de.egril.defender.model.TargetType.entries.forEach { type ->
                                val label =
                                    when (type) {
                                        de.egril.defender.model.TargetType.STANDARD -> stringResource(Res.string.target_type_standard)
                                        de.egril.defender.model.TargetType.SINGLE_HIT -> stringResource(Res.string.target_type_single_hit)
                                    }
                                Button(
                                    onClick = { onTargetTypeChange(type) },
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor =
                                                if (selectedTargetType == type) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.secondary
                                                },
                                        ),
                                    modifier = Modifier.height(32.dp),
                                ) {
                                    Text(label, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Replace tile types button
            Button(
                onClick = onChangeAllNoPlayToPath,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            ) {
                Text(stringResource(Res.string.replace_tiles))
            }

            // Background image controls
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(Res.string.map_background_image),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onLoadBackgroundImage,
                            modifier = Modifier.weight(1f).height(32.dp),
                        ) {
                            Text(stringResource(Res.string.map_background_image_load), fontSize = 11.sp)
                        }
                        if (backgroundImageLoaded) {
                            OutlinedButton(
                                onClick = onClearBackgroundImage,
                                modifier = Modifier.weight(1f).height(32.dp),
                            ) {
                                Text(stringResource(Res.string.map_background_image_clear), fontSize = 11.sp)
                            }
                        }
                    }
                    if (backgroundImageLoaded) {
                        Text(
                            text = stringResource(Res.string.map_background_image_opacity),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Slider(
                            value = mapOverlayAlpha,
                            onValueChange = onMapOverlayAlphaChange,
                            valueRange = 0.1f..1.0f,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            ZoomControls(
                map = map,
                zoomLevel = zoomLevel,
                onZoomIn = onZoomIn,
                onZoomOut = onZoomOut,
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
    onChangeAllNoPlayToPath: () -> Unit = {},
    selectedTargetName: String = "",
    onTargetNameChange: (String) -> Unit = {},
    selectedTargetType: de.egril.defender.model.TargetType = de.egril.defender.model.TargetType.STANDARD,
    onTargetTypeChange: (de.egril.defender.model.TargetType) -> Unit = {},
    selectedSpawnPointType: de.egril.defender.model.SpawnPointType = de.egril.defender.model.SpawnPointType.LAND,
    onSpawnPointTypeChange: (de.egril.defender.model.SpawnPointType) -> Unit = {},
    backgroundImageLoaded: Boolean = false,
    onLoadBackgroundImage: () -> Unit = {},
    onClearBackgroundImage: () -> Unit = {},
    showMapFlowOverlay: Boolean = false,
    onToggleMapFlowOverlay: () -> Unit = {},
    showMapPathPreviewOverlay: Boolean = false,
    onToggleMapPathPreviewOverlay: () -> Unit = {},
    onUndo: () -> Unit = {},
    canUndo: Boolean = false,
    onRedo: () -> Unit = {},
    canRedo: Boolean = false,
    onOpenAreaClipboard: () -> Unit = {},
) {
    var showRiverPropertiesDialog by remember { mutableStateOf(false) }
    var showTargetPropertiesDialog by remember { mutableStateOf(false) }
    var showSpawnPointPropertiesDialog by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp)
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Tile type dropdown - styled to look like a dropdown
            Box(
                modifier =
                    Modifier
                        .widthIn(min = 180.dp, max = 240.dp),
            ) {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = getTileColor(selectedTileType).copy(alpha = 0.3f),
                        ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(16.dp)
                                        .background(getTileColor(selectedTileType), shape = MaterialTheme.shapes.small),
                            )
                            Text(
                                text = selectedTileType.name,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (selectedTileType == TileType.RIVER) {
                                RiverFlowIndicator(
                                    flowDirection = selectedRiverFlow,
                                    flowSpeed = selectedRiverSpeed,
                                    size = 14.dp,
                                )
                            }
                            if (selectedTileType == TileType.TARGET) {
                                val typeLabel =
                                    when (selectedTargetType) {
                                        de.egril.defender.model.TargetType.STANDARD -> stringResource(Res.string.target_type_standard)
                                        de.egril.defender.model.TargetType.SINGLE_HIT -> stringResource(Res.string.target_type_single_hit)
                                    }
                                Text(
                                    text = typeLabel,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        de.egril.defender.ui.icon
                            .TriangleDownIcon(size = 10.dp)
                    }
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    TileType.entries.forEach { tileType ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .size(16.dp)
                                                .background(getTileColor(tileType), shape = MaterialTheme.shapes.small),
                                    )
                                    Text(tileType.name)
                                }
                            },
                            onClick = {
                                onTileTypeChange(tileType)
                                expanded = false
                                if (tileType == TileType.RIVER) {
                                    showRiverPropertiesDialog = true
                                } else if (tileType == TileType.TARGET) {
                                    showTargetPropertiesDialog = true
                                } else if (tileType == TileType.SPAWN_POINT) {
                                    showSpawnPointPropertiesDialog = true
                                }
                            },
                        )
                    }
                }
            }

            if (selectedTileType == TileType.TARGET || selectedTileType == TileType.SPAWN_POINT) {
                val editLabel = stringResource(Res.string.edit)
                IconButton(
                    onClick = {
                        if (selectedTileType == TileType.TARGET) {
                            showTargetPropertiesDialog = true
                        } else {
                            showSpawnPointPropertiesDialog = true
                        }
                    },
                    modifier = Modifier.size(32.dp).semantics { contentDescription = editLabel },
                ) {
                    de.egril.defender.ui.icon
                        .PencilIcon(size = 16.dp)
                }
            }

            val replaceTilesLabel = stringResource(Res.string.replace_tiles)
            IconButton(
                onClick = onChangeAllNoPlayToPath,
                modifier = Modifier.size(32.dp).semantics { contentDescription = replaceTilesLabel },
            ) {
                de.egril.defender.ui.icon
                    .ToolsIcon(size = 16.dp)
            }

            val bgImageLabel =
                if (backgroundImageLoaded) {
                    stringResource(Res.string.map_background_image_clear)
                } else {
                    stringResource(Res.string.map_background_image_load)
                }
            IconButton(
                onClick = if (backgroundImageLoaded) onClearBackgroundImage else onLoadBackgroundImage,
                modifier = Modifier.size(32.dp).semantics { contentDescription = bgImageLabel },
            ) {
                if (backgroundImageLoaded) {
                    de.egril.defender.ui.icon
                        .CrossIcon(size = 16.dp, tint = MaterialTheme.colorScheme.primary)
                } else {
                    de.egril.defender.ui.icon
                        .DownloadIcon(size = 16.dp)
                }
            }

            val expandLabel = stringResource(Res.string.expand)
            IconButton(
                onClick = onExpand,
                modifier = Modifier.size(32.dp).semantics { contentDescription = expandLabel },
            ) {
                de.egril.defender.ui.icon
                    .LeftArrowIcon(size = 16.dp)
            }
            CompactToggleChip(
                label = stringResource(Res.string.map_flow_validator),
                active = showMapFlowOverlay,
                onClick = onToggleMapFlowOverlay,
            )
            CompactToggleChip(
                label = stringResource(Res.string.map_path_preview),
                active = showMapPathPreviewOverlay,
                onClick = onToggleMapPathPreviewOverlay,
            )
            AssistChip(
                onClick = onUndo,
                enabled = canUndo,
                label = { Text(stringResource(Res.string.undo), fontSize = 11.sp) },
            )
            AssistChip(
                onClick = onRedo,
                enabled = canRedo,
                label = { Text(stringResource(Res.string.redo), fontSize = 11.sp) },
            )
            AssistChip(
                onClick = onOpenAreaClipboard,
                label = { Text(stringResource(Res.string.area_clipboard), fontSize = 11.sp) },
            )
        }
    }

    // River properties dialog
    if (showRiverPropertiesDialog) {
        AlertDialog(
            onDismissRequest = { showRiverPropertiesDialog = false },
            title = { Text(stringResource(Res.string.river_properties)) },
            text = {
                Column {
                    Text(stringResource(Res.string.flow_direction), style = MaterialTheme.typography.bodyMedium)

                    // Display flow directions in 2 rows (4 items per row)
                    val flows = de.egril.defender.model.RiverFlow.entries
                    val firstRowFlows = flows.take(4)
                    val secondRowFlows = flows.drop(4)

                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        // First row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            firstRowFlows.forEach { flow ->
                                Button(
                                    onClick = { onRiverFlowChange(flow) },
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

                        // Second row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            secondRowFlows.forEach { flow ->
                                Button(
                                    onClick = { onRiverFlowChange(flow) },
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Button(
                            onClick = { onRiverSpeedChange(1) },
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
                            onClick = { onRiverSpeedChange(2) },
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

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            onTileTypeChange(TileType.SPAWN_POINT)
                            onSpawnPointTypeChange(de.egril.defender.model.SpawnPointType.WATER)
                            showRiverPropertiesDialog = false
                        },
                        modifier = Modifier.height(32.dp),
                    ) {
                        Text(
                            "${stringResource(Res.string.spawn_point)} (${stringResource(Res.string.spawn_point_type_water)})",
                            fontSize = 10.sp,
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showRiverPropertiesDialog = false }) {
                    Text(stringResource(Res.string.ok))
                }
            },
        )
    }

    // Target properties dialog
    if (showTargetPropertiesDialog) {
        var localName by remember(showTargetPropertiesDialog) { mutableStateOf(selectedTargetName) }
        AlertDialog(
            onDismissRequest = { showTargetPropertiesDialog = false },
            title = { Text(stringResource(Res.string.target_name_label)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = localName,
                        onValueChange = { localName = it },
                        label = { Text(stringResource(Res.string.target_name_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Text(stringResource(Res.string.target_type_label), style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        de.egril.defender.model.TargetType.entries.forEach { type ->
                            val label =
                                when (type) {
                                    de.egril.defender.model.TargetType.STANDARD -> stringResource(Res.string.target_type_standard)
                                    de.egril.defender.model.TargetType.SINGLE_HIT -> stringResource(Res.string.target_type_single_hit)
                                }
                            Button(
                                onClick = { onTargetTypeChange(type) },
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor =
                                            if (selectedTargetType == type) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.secondary
                                            },
                                    ),
                                modifier = Modifier.height(36.dp),
                            ) {
                                Text(label, fontSize = 11.sp)
                            }

                            if (showSpawnPointPropertiesDialog) {
                                AlertDialog(
                                    onDismissRequest = { showSpawnPointPropertiesDialog = false },
                                    title = { Text(stringResource(Res.string.spawn_point_type)) },
                                    text = {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                de.egril.defender.model.SpawnPointType.entries.forEach { type ->
                                                    val label =
                                                        when (type) {
                                                            de.egril.defender.model.SpawnPointType.LAND -> stringResource(Res.string.spawn_point_type_land)
                                                            de.egril.defender.model.SpawnPointType.WATER -> stringResource(Res.string.spawn_point_type_water)
                                                        }
                                                    Button(
                                                        onClick = { onSpawnPointTypeChange(type) },
                                                        colors =
                                                            ButtonDefaults.buttonColors(
                                                                containerColor =
                                                                    if (selectedSpawnPointType == type) {
                                                                        MaterialTheme.colorScheme.primary
                                                                    } else {
                                                                        MaterialTheme.colorScheme.secondary
                                                                    },
                                                            ),
                                                        modifier = Modifier.height(36.dp),
                                                    ) {
                                                        Text(label, fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        Button(onClick = { showSpawnPointPropertiesDialog = false }) {
                                            Text(stringResource(Res.string.ok))
                                        }
                                    },
                                    dismissButton = {
                                        Button(onClick = { showSpawnPointPropertiesDialog = false }) {
                                            Text(stringResource(Res.string.cancel))
                                        }
                                    },
                                )
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
                    Text(stringResource(Res.string.ok))
                }
            },
            dismissButton = {
                Button(onClick = { showTargetPropertiesDialog = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun CompactToggleChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label, fontSize = 11.sp) },
        colors =
            AssistChipDefaults.assistChipColors(
                containerColor =
                    if (active) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                labelColor =
                    if (active) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            ),
    )
}

@Composable
fun ZoomControls(
    map: EditorMap,
    zoomLevel: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
) {
    // Zoom controls
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${stringResource(Res.string.click_hexagons_to_paint)} (${map.width}x${map.height})",
            style = MaterialTheme.typography.bodySmall,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onZoomOut,
                modifier = Modifier.height(32.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    MagnifyingGlassIcon(size = 14.dp, tint = Color.White)
                    Text("-", fontSize = 12.sp)
                }
            }
            Text(
                text = "${(zoomLevel * 100).toInt()}%",
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Button(
                onClick = onZoomIn,
                modifier = Modifier.height(32.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    MagnifyingGlassIcon(size = 14.dp, tint = Color.White)
                    Text("+", fontSize = 12.sp)
                }
            }
        }
    }
}
