package de.egril.defender.ui.editor.map

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.MapValidationIssue
import de.egril.defender.model.MIN_ROTATABLE_REGION_TILES
import de.egril.defender.editor.TileType
import de.egril.defender.ui.TooltipWrapper
import de.egril.defender.ui.editor.RiverFlowIndicator
import de.egril.defender.ui.editor.getTileColor
import de.egril.defender.ui.icon.CrosshairIcon
import de.egril.defender.ui.icon.MagnifyingGlassIcon
import de.egril.defender.ui.icon.RedCircleIcon
import de.egril.defender.ui.icon.WaterIcon
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
    zoomLevel: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    selectedTileType: TileType = TileType.PATH,
    onTileTypeChange: (TileType) -> Unit = {},
    selectedRiverFlow: de.egril.defender.model.RiverFlow = de.egril.defender.model.RiverFlow.NORTH_EAST,
    onRiverFlowChange: (de.egril.defender.model.RiverFlow) -> Unit = {},
    selectedRiverSpeed: Int = 1,
    onRiverSpeedChange: (Int) -> Unit = {},
    selectedTargetName: String = "",
    onTargetNameChange: (String) -> Unit = {},
    selectedTargetType: de.egril.defender.model.TargetType = de.egril.defender.model.TargetType.STANDARD,
    onTargetTypeChange: (de.egril.defender.model.TargetType) -> Unit = {},
    selectedSpawnPointType: de.egril.defender.model.SpawnPointType = de.egril.defender.model.SpawnPointType.LAND,
    onSpawnPointTypeChange: (de.egril.defender.model.SpawnPointType) -> Unit = {},
    backgroundImageLoaded: Boolean = false,
    onLoadBackgroundImage: () -> Unit = {},
    onClearBackgroundImage: () -> Unit = {},
    onOpenMapPreview: () -> Unit = {},
    mapOverlayAlpha: Float = 0.7f,
    onMapOverlayAlphaChange: (Float) -> Unit = {},
    showCrosshair: Boolean = false,
    onToggleCrosshair: () -> Unit = {},
    onUndo: () -> Unit = {},
    canUndo: Boolean = false,
    onRedo: () -> Unit = {},
    canRedo: Boolean = false,
) {
    CollapsedMapEditorHeader(
        map = map,
        mapWidth = mapWidth,
        mapHeight = mapHeight,
        onExpand = onToggleExpanded,
        backgroundImageLoaded = backgroundImageLoaded,
        onLoadBackgroundImage = onLoadBackgroundImage,
        onClearBackgroundImage = onClearBackgroundImage,
        onOpenMapPreview = onOpenMapPreview,
        showCrosshair = showCrosshair,
        onToggleCrosshair = onToggleCrosshair,
        onUndo = onUndo,
        canUndo = canUndo,
        onRedo = onRedo,
        canRedo = canRedo,
    )

    if (isExpanded) {
        Dialog(
            onDismissRequest = onToggleExpanded,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.9f),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()).padding(8.dp),
                ) {
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
                        selectedTileType = selectedTileType,
                        onTileTypeChange = onTileTypeChange,
                        selectedRiverFlow = selectedRiverFlow,
                        onRiverFlowChange = onRiverFlowChange,
                        selectedRiverSpeed = selectedRiverSpeed,
                        onRiverSpeedChange = onRiverSpeedChange,
                        zoomLevel = zoomLevel,
                        onZoomIn = onZoomIn,
                        onZoomOut = onZoomOut,
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
                }
            }
        }
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
    selectedTileType: TileType,
    onTileTypeChange: (TileType) -> Unit,
    selectedRiverFlow: de.egril.defender.model.RiverFlow,
    onRiverFlowChange: (de.egril.defender.model.RiverFlow) -> Unit,
    selectedRiverSpeed: Int,
    onRiverSpeedChange: (Int) -> Unit,
    zoomLevel: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
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
            if (allowNoBuildableTiles && !map.hasBuildablePlacementTiles()) {
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
    map: EditorMap,
    mapWidth: Int,
    mapHeight: Int,
    onExpand: () -> Unit,
    backgroundImageLoaded: Boolean = false,
    onLoadBackgroundImage: () -> Unit = {},
    onClearBackgroundImage: () -> Unit = {},
    onOpenMapPreview: () -> Unit = {},
    showCrosshair: Boolean = false,
    onToggleCrosshair: () -> Unit = {},
    onUndo: () -> Unit = {},
    canUndo: Boolean = false,
    onRedo: () -> Unit = {},
    canRedo: Boolean = false,
) {
    var showNotReadyDialog by remember { mutableStateOf(false) }
    val validationIssues = remember(map) { map.getValidationIssues() }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        FlowRow(
            modifier =
                Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp)
                    .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val bgImageLabel =
                if (backgroundImageLoaded) {
                    stringResource(Res.string.map_background_image_clear)
                } else {
                    stringResource(Res.string.map_background_image_load)
                }
            TooltipWrapper(text = bgImageLabel) {
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
            }

            val mapPreviewLabel = stringResource(Res.string.map_preview)
            TooltipWrapper(text = mapPreviewLabel) {
                IconButton(
                    onClick = onOpenMapPreview,
                    modifier = Modifier.size(32.dp).semantics { contentDescription = mapPreviewLabel },
                ) {
                    de.egril.defender.ui.icon
                        .MagnifyingGlassIcon(size = 16.dp)
                }
            }

            val expandLabel = stringResource(Res.string.expand)
            TooltipWrapper(text = expandLabel) {
                IconButton(
                    onClick = onExpand,
                    modifier = Modifier.size(32.dp).semantics { contentDescription = expandLabel },
                ) {
                    de.egril.defender.ui.icon
                        .LeftArrowIcon(size = 16.dp)
                }
            }
            MapEditorToolToggles(
                showCrosshair = showCrosshair,
                onToggleCrosshair = onToggleCrosshair,
                onUndo = onUndo,
                canUndo = canUndo,
                onRedo = onRedo,
                canRedo = canRedo,
            )
            if (validationIssues.isNotEmpty()) {
                val notReadyLabel = stringResource(Res.string.map_not_ready_indicator)
                TooltipWrapper(text = notReadyLabel) {
                    IconButton(
                        onClick = { showNotReadyDialog = true },
                        modifier = Modifier.size(32.dp).semantics { contentDescription = notReadyLabel },
                    ) {
                        RedCircleIcon(size = 16.dp)
                    }
                }
            }
        }
    }

    // "Map not ready" details dialog, opened via the red dot indicator in the header.
    if (showNotReadyDialog) {
        AlertDialog(
            onDismissRequest = { showNotReadyDialog = false },
            title = { Text(stringResource(Res.string.map_not_ready_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    validationIssues.forEach { issue ->
                        val issueText =
                            when (issue) {
                                MapValidationIssue.INVALID_MAP_DATA -> stringResource(Res.string.map_validation_invalid_map_data)
                                MapValidationIssue.UNSUPPORTED_SIZE -> stringResource(Res.string.map_validation_unsupported_size)
                                MapValidationIssue.NO_SPAWN_POINT -> stringResource(Res.string.map_validation_no_spawn_point)
                                MapValidationIssue.NO_TARGET -> stringResource(Res.string.map_validation_no_target)
                                MapValidationIssue.NO_BUILDABLE_TILES -> stringResource(Res.string.map_validation_no_buildable_tiles)
                                MapValidationIssue.NO_PATH_FROM_SPAWN_TO_TARGET ->
                                    stringResource(Res.string.map_validation_no_path_from_spawn_to_target)
                                MapValidationIssue.ROTATABLE_ZONE_TOO_SMALL ->
                                    stringResource(Res.string.map_validation_rotatable_zone_too_small, MIN_ROTATABLE_REGION_TILES)
                            }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("•")
                            Text(issueText)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showNotReadyDialog = false }) {
                    Text(stringResource(Res.string.ok))
                }
            },
        )
    }

}

/**
 * Overlay toggles, undo/redo and the area clipboard. Used by the collapsed header, and by the
 * side panel while the header is expanded, so each control is shown exactly once.
 */
@Composable
internal fun MapEditorToolToggles(
    showCrosshair: Boolean,
    onToggleCrosshair: () -> Unit,
    onUndo: () -> Unit,
    canUndo: Boolean,
    onRedo: () -> Unit,
    canRedo: Boolean,
) {
    val mapCrosshairLabel = stringResource(Res.string.map_crosshair)
    TooltipWrapper(text = mapCrosshairLabel) {
        IconButton(
            onClick = onToggleCrosshair,
            modifier = Modifier.semantics { contentDescription = mapCrosshairLabel },
            colors =
                IconButtonDefaults.iconButtonColors(
                    contentColor =
                        if (showCrosshair) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                ),
        ) {
            CrosshairIcon(contentDescription = mapCrosshairLabel, size = 20.dp)
        }
    }
    val undoLabel = stringResource(Res.string.undo)
    TooltipWrapper(text = undoLabel) {
        AssistChip(
            onClick = onUndo,
            enabled = canUndo,
            label = { Text(undoLabel, fontSize = 11.sp) },
        )
    }
    val redoLabel = stringResource(Res.string.redo)
    TooltipWrapper(text = redoLabel) {
        AssistChip(
            onClick = onRedo,
            enabled = canRedo,
            label = { Text(redoLabel, fontSize = 11.sp) },
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
internal fun OverlayToggleButton(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    icon: (@Composable () -> Unit)? = null,
) {
    Button(
        onClick = onClick,
        colors =
            ButtonDefaults.buttonColors(
                containerColor =
                    if (isActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                contentColor =
                    if (isActive) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            ),
    ) {
        if (icon != null) {
            icon()
        } else {
            Text(label)
        }
    }
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
