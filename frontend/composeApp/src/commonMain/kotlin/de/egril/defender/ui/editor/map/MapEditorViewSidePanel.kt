package de.egril.defender.ui.editor.map

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.*
import de.egril.defender.model.Position
import de.egril.defender.model.RiverTile
import de.egril.defender.model.SpawnPointType
import de.egril.defender.ui.editor.level.analyzeLevelMapConsistency
import de.egril.defender.ui.editor.level.analyzeMapFlow
import de.egril.defender.ui.icon.CheckmarkIcon
import de.egril.defender.ui.icon.PencilIcon
import de.egril.defender.ui.icon.PushpinIcon
import de.egril.defender.ui.icon.ResizeIcon
import de.egril.defender.ui.icon.RightArrowIcon
import de.egril.defender.ui.icon.ToolsIcon
import defender_of_egril.composeapp.generated.resources.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Right-hand tools panel of [MapEditorView]: header, resize, tile replacement, area clipboard,
 * tile zones and map validation sections.
 */
@Composable
internal fun MapEditorViewState.MapEditorViewSidePanel(
    currentMap: EditorMap,
    backgroundImagePainter: BitmapPainter?,
    coroutineScope: CoroutineScope,
    pathPreviews: List<MapPathPreview>,
) {
    val mapFlowSummary = remember(currentMap) { analyzeMapFlow(currentMap) }
    val levelsUsingMap =
        remember(map.id) {
            EditorStorage.getAllLevels().filter { it.mapId == map.id }
        }
    val mapUsageIssues =
        remember(currentMap, levelsUsingMap) {
            levelsUsingMap
                .map { level -> level to analyzeLevelMapConsistency(level, currentMap) }
                .filter { it.second.hasIssues }
        }
    val parsedResizeLeft = resizeLeft.toIntOrNull() ?: 0
    val parsedResizeRight = resizeRight.toIntOrNull() ?: 0
    val parsedResizeTop = resizeTop.toIntOrNull() ?: 0
    val parsedResizeBottom = resizeBottom.toIntOrNull() ?: 0
    val resizedWidthPreview = mapWidth + parsedResizeLeft + parsedResizeRight
    val resizedHeightPreview = mapHeight + parsedResizeTop + parsedResizeBottom
    val canApplyResize =
        resizedWidthPreview > 0 &&
            resizedHeightPreview > 0 &&
            MapSizeLimits.isWithinLimits(resizedWidthPreview, resizedHeightPreview)
    val showUnsafeResizeWarning =
        levelsUsingMap.isNotEmpty() &&
            !isSafeEndExpansion(
                leftDelta = parsedResizeLeft,
                rightDelta = parsedResizeRight,
                topDelta = parsedResizeTop,
                bottomDelta = parsedResizeBottom,
            )

    val parsedFromX = replacementFromX.toIntOrNull()
    val parsedFromY = replacementFromY.toIntOrNull()
    val parsedToX = replacementToX.toIntOrNull()
    val parsedToY = replacementToY.toIntOrNull()
    val isAreaInputValid =
        !replacementLimitToArea ||
            (parsedFromX != null && parsedFromY != null && parsedToX != null && parsedToY != null)

    fun applyTileReplacement() {
        if (!isAreaInputValid) return
        rememberForUndo()
        val area =
            if (replacementLimitToArea) {
                TileReplacementArea(
                    from = Position(parsedFromX!!, parsedFromY!!),
                    to = Position(parsedToX!!, parsedToY!!),
                )
            } else {
                null
            }

        val (updatedTiles, changedKeys) =
            replaceTilesByType(
                tiles = tiles,
                mapWidth = mapWidth,
                mapHeight = mapHeight,
                sourceTileType = replacementSourceTileType,
                targetTileType = replacementTargetTileType,
                area = area,
            )
        tiles = updatedTiles.toMutableMap()

        riverTiles = riverTiles.filterKeys { key -> tiles[key] == TileType.RIVER }.toMutableMap()
        targetInfoMap = targetInfoMap.filterKeys { key -> tiles[key] == TileType.TARGET }.toMutableMap()
        spawnPointInfoMap = spawnPointInfoMap.filterKeys { key -> tiles[key] == TileType.SPAWN_POINT }.toMutableMap()

        if (replacementTargetTileType == TileType.RIVER) {
            riverTiles =
                riverTiles.toMutableMap().apply {
                    changedKeys.forEach { key ->
                        if (this[key] == null) {
                            val parts = key.split(",")
                            val x = parts.getOrNull(0)?.toIntOrNull()
                            val y = parts.getOrNull(1)?.toIntOrNull()
                            if (x != null && y != null) {
                                this[key] =
                                    RiverTile(
                                        position = Position(x, y),
                                        flowDirection = replacementRiverFlow,
                                        flowSpeed = replacementRiverSpeed,
                                    )
                            }
                        }
                    }
                }
        } else if (replacementTargetTileType == TileType.TARGET) {
            targetInfoMap =
                targetInfoMap.toMutableMap().apply {
                    changedKeys.forEach { key ->
                        if (this[key] == null) {
                            this[key] = EditorTargetInfo()
                        }
                    }
                }
        } else if (replacementTargetTileType == TileType.SPAWN_POINT) {
            spawnPointInfoMap =
                spawnPointInfoMap.toMutableMap().apply {
                    changedKeys.forEach { key ->
                        if (this[key] == null) {
                            this[key] = SpawnPointType.LAND
                        }
                    }
                }
        }

        showTileReplacementDialog = false
    }

    val replaceTilesContent: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(Res.string.replace_tiles_source))
            Box {
                OutlinedButton(
                    onClick = { sourceTileDropdownExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(replacementSourceTileType.name)
                }
                DropdownMenu(
                    expanded = sourceTileDropdownExpanded,
                    onDismissRequest = { sourceTileDropdownExpanded = false },
                ) {
                    TileType.entries.forEach { tileType ->
                        DropdownMenuItem(
                            text = { Text(tileType.name) },
                            onClick = {
                                replacementSourceTileType = tileType
                                sourceTileDropdownExpanded = false
                            },
                        )
                    }
                }
            }

            Text(stringResource(Res.string.replace_tiles_target))
            Box {
                OutlinedButton(
                    onClick = { targetTileDropdownExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(replacementTargetTileType.name)
                }
                DropdownMenu(
                    expanded = targetTileDropdownExpanded,
                    onDismissRequest = { targetTileDropdownExpanded = false },
                ) {
                    TileType.entries.forEach { tileType ->
                        DropdownMenuItem(
                            text = { Text(tileType.name) },
                            onClick = {
                                replacementTargetTileType = tileType
                                targetTileDropdownExpanded = false
                            },
                        )
                    }
                }
            }

            if (replacementTargetTileType == TileType.RIVER) {
                Text(stringResource(Res.string.flow_direction), style = MaterialTheme.typography.bodyMedium)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    de.egril.defender.model.RiverFlow.entries.forEach { flow ->
                        Button(
                            onClick = { replacementRiverFlow = flow },
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor =
                                        if (replacementRiverFlow == flow) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.secondary
                                        },
                                ),
                            modifier = Modifier.height(32.dp),
                        ) {
                            Text(flow.name.replace("_", " "), fontSize = 10.sp)
                        }
                    }
                }

                Text(stringResource(Res.string.flow_speed), style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(1 to Res.string.speed_slow, 2 to Res.string.speed_fast).forEach { (speed, label) ->
                        Button(
                            onClick = { replacementRiverSpeed = speed },
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor =
                                        if (replacementRiverSpeed == speed) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.secondary
                                        },
                                ),
                            modifier = Modifier.height(32.dp),
                        ) {
                            Text(stringResource(label), fontSize = 10.sp)
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Checkbox(
                    checked = replacementLimitToArea,
                    onCheckedChange = {
                        replacementLimitToArea = it
                        pickTarget = if (it) PositionPickTarget.REPLACE_FROM else null
                    },
                )
                Text(stringResource(Res.string.replace_tiles_limit_to_area))
            }

            if (replacementLimitToArea) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.pickTargetBorder(pickTarget == PositionPickTarget.REPLACE_FROM, REPLACE_HIGHLIGHT_COLOR),
                ) {
                    OutlinedTextField(
                        value = replacementFromX,
                        onValueChange = { replacementFromX = it },
                        label = { Text(stringResource(Res.string.x_coordinate)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = replacementFromY,
                        onValueChange = { replacementFromY = it },
                        label = { Text(stringResource(Res.string.y_coordinate)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                }
                PositionPickButton(
                    active = pickTarget == PositionPickTarget.REPLACE_FROM,
                    onClick = { pickTarget = togglePickTarget(pickTarget, PositionPickTarget.REPLACE_FROM) },
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.pickTargetBorder(pickTarget == PositionPickTarget.REPLACE_TO, REPLACE_HIGHLIGHT_COLOR),
                ) {
                    OutlinedTextField(
                        value = replacementToX,
                        onValueChange = { replacementToX = it },
                        label = { Text(stringResource(Res.string.x_coordinate)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = replacementToY,
                        onValueChange = { replacementToY = it },
                        label = { Text(stringResource(Res.string.y_coordinate)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                }
                PositionPickButton(
                    active = pickTarget == PositionPickTarget.REPLACE_TO,
                    onClick = { pickTarget = togglePickTarget(pickTarget, PositionPickTarget.REPLACE_TO) },
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showTileReplacementDialog = false }) {
                    Text(stringResource(Res.string.cancel))
                }
                Button(onClick = { applyTileReplacement() }, enabled = isAreaInputValid) {
                    Text(stringResource(Res.string.apply))
                }
            }
        }
    }

    val parsedCopyFromX = copyFromX.toIntOrNull()
    val parsedCopyFromY = copyFromY.toIntOrNull()
    val parsedCopyToX = copyToX.toIntOrNull()
    val parsedCopyToY = copyToY.toIntOrNull()
    val parsedPasteAtX = pasteAtX.toIntOrNull()
    val parsedPasteAtY = pasteAtY.toIntOrNull()

    fun pasteAreaClipboard() {
        val clipboard = areaClipboard ?: return
        if (parsedPasteAtX == null || parsedPasteAtY == null) return
        rememberForUndo()
        val pasted =
            pasteMapRegion(
                clipboard = clipboard,
                at = Position(parsedPasteAtX, parsedPasteAtY),
                mapWidth = mapWidth,
                mapHeight = mapHeight,
                tiles = tiles,
                riverTiles = riverTiles,
                targetInfoMap = targetInfoMap,
                spawnPointInfoMap = spawnPointInfoMap,
            )
        tiles = pasted.tiles
        riverTiles = pasted.riverTiles
        targetInfoMap = pasted.targetInfoMap
        spawnPointInfoMap = pasted.spawnPointInfoMap
        showAreaClipboardDialog = false
    }

    val areaClipboardContent: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(Res.string.copy_region))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.pickTargetBorder(pickTarget == PositionPickTarget.COPY_FROM, COPY_HIGHLIGHT_COLOR),
            ) {
                OutlinedTextField(copyFromX, { copyFromX = it }, label = { Text(stringResource(Res.string.x_coordinate)) }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(copyFromY, { copyFromY = it }, label = { Text(stringResource(Res.string.y_coordinate)) }, modifier = Modifier.weight(1f), singleLine = true)
            }
            PositionPickButton(
                active = pickTarget == PositionPickTarget.COPY_FROM,
                onClick = { pickTarget = togglePickTarget(pickTarget, PositionPickTarget.COPY_FROM) },
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.pickTargetBorder(pickTarget == PositionPickTarget.COPY_TO, COPY_HIGHLIGHT_COLOR),
            ) {
                OutlinedTextField(copyToX, { copyToX = it }, label = { Text(stringResource(Res.string.to_x)) }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(copyToY, { copyToY = it }, label = { Text(stringResource(Res.string.to_y)) }, modifier = Modifier.weight(1f), singleLine = true)
            }
            PositionPickButton(
                active = pickTarget == PositionPickTarget.COPY_TO,
                onClick = { pickTarget = togglePickTarget(pickTarget, PositionPickTarget.COPY_TO) },
            )
            Button(
                onClick = {
                    if (parsedCopyFromX == null || parsedCopyFromY == null || parsedCopyToX == null || parsedCopyToY == null) return@Button
                    areaClipboard =
                        copyMapRegion(
                            from = Position(parsedCopyFromX, parsedCopyFromY),
                            to = Position(parsedCopyToX, parsedCopyToY),
                            tiles = tiles,
                            riverTiles = riverTiles,
                            targetInfoMap = targetInfoMap,
                            spawnPointInfoMap = spawnPointInfoMap,
                        )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    areaClipboard?.let {
                        stringResource(Res.string.region_copied_size, it.width, it.height)
                    } ?: stringResource(Res.string.copy_region),
                )
            }
            Text(stringResource(Res.string.paste_region))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.pickTargetBorder(pickTarget == PositionPickTarget.PASTE_AT, PASTE_HIGHLIGHT_COLOR),
            ) {
                OutlinedTextField(pasteAtX, { pasteAtX = it }, label = { Text(stringResource(Res.string.x_coordinate)) }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(pasteAtY, { pasteAtY = it }, label = { Text(stringResource(Res.string.y_coordinate)) }, modifier = Modifier.weight(1f), singleLine = true)
            }
            PositionPickButton(
                active = pickTarget == PositionPickTarget.PASTE_AT,
                onClick = { pickTarget = togglePickTarget(pickTarget, PositionPickTarget.PASTE_AT) },
            )
            if (pickTarget != null) {
                Text(stringResource(Res.string.pick_position_hint))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showAreaClipboardDialog = false }) {
                    Text(stringResource(Res.string.cancel))
                }
                Button(
                    onClick = { pasteAreaClipboard() },
                    enabled = areaClipboard != null && parsedPasteAtX != null && parsedPasteAtY != null,
                ) {
                    Text(stringResource(Res.string.paste_region))
                }
            }
        }
    }

    val renderMapEditorHeader: @Composable () -> Unit = {
        MapEditorHeader(
            map = currentMap,
            mapWidth = mapWidth,
            mapHeight = mapHeight,
            mapName = mapName,
            onMapNameChange = { mapName = it },
            mapAuthor = mapAuthor,
            onMapAuthorChange = { mapAuthor = it },
            mapToolingInfo = mapToolingInfo,
            onMapToolingInfoChange = { mapToolingInfo = it },
            allowNoBuildableTiles = allowNoBuildableTiles,
            onAllowNoBuildableTilesChange = { allowNoBuildableTiles = it },
            allowNoDirectPath = allowNoDirectPath,
            onAllowNoDirectPathChange = { allowNoDirectPath = it },
            zoomLevel = zoomLevel,
            onZoomIn = { zoomLevel = minOf(3.0f, zoomLevel + 0.1f) },
            onZoomOut = { zoomLevel = maxOf(0.5f, zoomLevel - 0.1f) },
            isExpanded = isHeaderExpanded,
            onToggleExpanded = { isHeaderExpanded = !isHeaderExpanded },
            backgroundImageLoaded = backgroundImagePainter != null,
            onLoadBackgroundImage = {
                coroutineScope.launch {
                    val bytes = pickBackgroundImageBytes()
                    if (bytes != null) {
                        backgroundImageBytes = bytes
                    }
                }
            },
            onClearBackgroundImage = { backgroundImageBytes = null },
            onOpenMapPreview = {
                val dialogState =
                    stateForOpeningMapPreviewDialog(
                        MapPreviewDialogState(
                            generationRunning = mapImageGenerationRunning,
                            generationSuccess = mapImageGenerationSuccess,
                            generationError = mapImageGenerationError,
                            previewRegenerating = mapPreviewRegenerating,
                            previewError = mapPreviewError,
                            hasPreviewPainter = mapPreviewPainter != null,
                        ),
                    )
                mapImageGenerationRunning = dialogState.generationRunning
                mapImageGenerationSuccess = dialogState.generationSuccess
                mapImageGenerationError = dialogState.generationError
                mapImageProgress = emptyList()
                mapPreviewRegenerating = dialogState.previewRegenerating
                mapPreviewError = dialogState.previewError
                showMapImageGenerationDialog = true
            },
            mapOverlayAlpha = mapOverlayAlpha,
            onMapOverlayAlphaChange = { mapOverlayAlpha = it },
            showCrosshair = showCrosshair,
            onToggleCrosshair = { showCrosshair = !showCrosshair },
            showNarrowPaths = showNarrowPaths,
            onToggleNarrowPaths = { showNarrowPaths = !showNarrowPaths },
            onUndo = {
                undoHistory.lastOrNull()?.let { snapshot ->
                    undoHistory = undoHistory.dropLast(1)
                    redoHistory = (redoHistory + currentSnapshot()).takeLast(40)
                    restoreSnapshot(snapshot)
                }
            },
            canUndo = undoHistory.isNotEmpty(),
            onRedo = {
                redoHistory.lastOrNull()?.let { snapshot ->
                    redoHistory = redoHistory.dropLast(1)
                    undoHistory = (undoHistory + currentSnapshot()).takeLast(40)
                    restoreSnapshot(snapshot)
                }
            },
            canRedo = redoHistory.isNotEmpty(),
        )
    }

    CollapsibleSidePanel(
        expanded = isSidePanelExpanded,
        onToggleExpanded = { isSidePanelExpanded = !isSidePanelExpanded },
        title = stringResource(Res.string.map_tools_panel),
    ) {
        TileTypeSelector(
            selectedTileType = selectedTileType,
            onTileTypeChange = {
                selectedTileType = it
                zoneBrush = ZoneBrush.PAINT_TYPE
            },
            selectedRiverFlow = selectedRiverFlow,
            selectedRiverSpeed = selectedRiverSpeed,
            selectedTargetName = selectedTargetName,
            onTargetNameChange = { selectedTargetName = it },
            selectedTargetType = selectedTargetType,
            onTargetTypeChange = { selectedTargetType = it },
            selectedSpawnPointType = selectedSpawnPointType,
            onSpawnPointTypeChange = { selectedSpawnPointType = it },
        )
        if (selectedTileType == TileType.RIVER) {
            RiverPropertiesSection(
                selectedRiverFlow = selectedRiverFlow,
                onRiverFlowChange = { selectedRiverFlow = it },
                selectedRiverSpeed = selectedRiverSpeed,
                onRiverSpeedChange = { selectedRiverSpeed = it },
                onWaterSpawnPointClick = {
                    selectedTileType = TileType.SPAWN_POINT
                    selectedSpawnPointType = SpawnPointType.WATER
                },
                onFillRiverRing = {
                    ringStartX = (mapWidth / 2).coerceAtLeast(0).toString()
                    ringStartY = (mapHeight / 2).coerceAtLeast(0).toString()
                    ringClockwise = true
                    ringInnerToOuter = true
                    showFillRiverRingDialog = true
                },
            )
        }
        renderMapEditorHeader()
        CollapsibleSidePanelSection(
            title = stringResource(Res.string.resize_map),
            expanded = showResizeSection,
            onToggleExpanded = { showResizeSection = !showResizeSection },
            icon = { ResizeIcon(size = 16.dp) },
        ) {
            ResizeMapSection(
                map = currentMap,
                mapWidth = mapWidth,
                mapHeight = mapHeight,
                resizeLeft = resizeLeft,
                onResizeLeftChange = { resizeLeft = it },
                resizeRight = resizeRight,
                onResizeRightChange = { resizeRight = it },
                resizeTop = resizeTop,
                onResizeTopChange = { resizeTop = it },
                resizeBottom = resizeBottom,
                onResizeBottomChange = { resizeBottom = it },
                onApplyResize = {
                    if (canApplyResize) {
                        rememberForUndo()
                        val resized =
                            applyResizeToMapData(
                                width = mapWidth,
                                height = mapHeight,
                                leftDelta = parsedResizeLeft,
                                rightDelta = parsedResizeRight,
                                topDelta = parsedResizeTop,
                                bottomDelta = parsedResizeBottom,
                                tiles = tiles,
                                riverTiles = riverTiles,
                                targetInfoMap = targetInfoMap,
                                spawnPointInfoMap = spawnPointInfoMap,
                            )
                        mapWidth = resized.width
                        mapHeight = resized.height
                        tiles = resized.tiles
                        riverTiles = resized.riverTiles
                        targetInfoMap = resized.targetInfoMap
                        spawnPointInfoMap = resized.spawnPointInfoMap
                        tileZones = shiftTileZones(tileZones, parsedResizeLeft, parsedResizeTop, resized.width, resized.height)
                        replacementToX = (resized.width - 1).coerceAtLeast(0).toString()
                        replacementToY = (resized.height - 1).coerceAtLeast(0).toString()
                        resizeLeft = "0"
                        resizeRight = "0"
                        resizeTop = "0"
                        resizeBottom = "0"
                    }
                },
                canApplyResize = canApplyResize,
                resultingMapWidth = resizedWidthPreview,
                resultingMapHeight = resizedHeightPreview,
                showUnsafeResizeWarning = showUnsafeResizeWarning,
                mapUsageLevelNames = levelsUsingMap.map { it.title.ifBlank { it.id } },
            )
        }
        CollapsibleSidePanelSection(
            title = stringResource(Res.string.replace_tiles),
            expanded = showTileReplacementDialog,
            onToggleExpanded = {
                if (!showTileReplacementDialog) {
                    replacementSourceTileType = TileType.NO_PLAY
                    replacementTargetTileType = TileType.PATH
                    replacementLimitToArea = false
                    replacementFromX = "0"
                    replacementFromY = "0"
                    replacementToX = (mapWidth - 1).coerceAtLeast(0).toString()
                    replacementToY = (mapHeight - 1).coerceAtLeast(0).toString()
                }
                showTileReplacementDialog = !showTileReplacementDialog
                pickTarget = null
            },
            icon = { ToolsIcon(size = 16.dp) },
        ) {
            replaceTilesContent()
        }
        CollapsibleSidePanelSection(
            title = stringResource(Res.string.area_clipboard),
            expanded = showAreaClipboardDialog,
            onToggleExpanded = {
                showAreaClipboardDialog = !showAreaClipboardDialog
                pickTarget = if (showAreaClipboardDialog) PositionPickTarget.COPY_FROM else null
            },
            icon = { PencilIcon(size = 16.dp) },
        ) {
            areaClipboardContent()
        }
        CollapsibleSidePanelSection(
            title = stringResource(Res.string.tile_zones),
            icon = { PushpinIcon(size = 16.dp) },
            expanded = showTileZonePanel,
            onToggleExpanded = {
                if (showTileZonePanel) {
                    isZoneDrawingMode = false
                    zoneBrush = ZoneBrush.PAINT_TYPE
                }
                showTileZonePanel = !showTileZonePanel
            },
        ) {
            TileZonePanel(
                map = currentMap,
                zones = tileZones,
                selectedZoneId = selectedZoneId,
                showOtherZones = showOtherZones,
                isZoneDrawingMode = isZoneDrawingMode,
                zoneBrush = zoneBrush,
                onZonesChange = {
                    rememberForUndo()
                    tileZones = it
                },
                onSelectZone = {
                    selectedZoneId = it
                    if (it == null) {
                        isZoneDrawingMode = false
                        zoneBrush = ZoneBrush.PAINT_TYPE
                    }
                },
                onCopyZone = { source ->
                    val copied = copyTileZone(tileZones, source)
                    rememberForUndo()
                    tileZones = tileZones + copied
                    selectedZoneId = copied.id
                },
                onShowOtherZonesChange = { showOtherZones = it },
                onToggleZoneDrawingMode = {
                    isZoneDrawingMode = !isZoneDrawingMode
                    zoneBrush = ZoneBrush.PAINT_TYPE
                },
                onZoneBrushChange = { zoneBrush = it },
            )
        }

        CollapsibleSidePanelSection(
            title = stringResource(Res.string.map_flow_validator),
            icon = { CheckmarkIcon(size = 16.dp) },
            expanded = showMapFlowOverlay,
            onToggleExpanded = { showMapFlowOverlay = !showMapFlowOverlay },
        ) {
            MapFlowValidatorCard(summary = mapFlowSummary)
            if (mapUsageIssues.isNotEmpty()) {
                MapUsageConsistencyCard(issues = mapUsageIssues)
            }
        }

        CollapsibleSidePanelSection(
            title = stringResource(Res.string.map_path_preview),
            icon = { RightArrowIcon(size = 16.dp) },
            expanded = showMapPathPreviewOverlay,
            onToggleExpanded = { showMapPathPreviewOverlay = !showMapPathPreviewOverlay },
        ) {
            MapPathPreviewCard(previews = pathPreviews)
        }
    }
}
