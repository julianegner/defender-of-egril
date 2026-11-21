package de.egril.defender.ui.editor.map

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.*
import de.egril.defender.model.Position
import de.egril.defender.model.RiverTile
import de.egril.defender.model.SpawnPointType
import de.egril.defender.ui.editor.RiverFlowIndicator
import de.egril.defender.ui.editor.getTileColor
import de.egril.defender.ui.hexagon.BaseGridCell
import de.egril.defender.ui.hexagon.HexagonalGridConstants
import de.egril.defender.ui.hexagon.HexagonalMapConfig
import de.egril.defender.ui.hexagon.HexagonalMapView
import de.egril.defender.utils.screenToHexGridPosition
import defender_of_egril.composeapp.generated.resources.*

/**
 * Hex grid of [MapEditorView] inside its weighted box: tile painting, cell rendering,
 * zoom/pan controls and the minimap.
 */
@Composable
internal fun MapEditorViewState.MapEditorViewGrid(
    currentMap: EditorMap,
    backgroundImagePainter: BitmapPainter?,
    pathPreviews: List<MapPathPreview>,
    highlightedNarrowPaths: Set<Position> = emptySet(),
) {
    val previewCells = remember(pathPreviews) { pathPreviews.flatMap { it.path }.toSet() }
    val replacementHighlightCells =
        if (showTileReplacementDialog && replacementLimitToArea) {
            rectangleTiles(
                parsePosition(replacementFromX, replacementFromY),
                parsePosition(replacementToX, replacementToY),
            )
        } else {
            emptySet()
        }
    val copyHighlightCells =
        if (showAreaClipboardDialog) {
            rectangleTiles(
                parsePosition(copyFromX, copyFromY),
                parsePosition(copyToX, copyToY),
            )
        } else {
            emptySet()
        }
    val pasteHighlightCell = if (showAreaClipboardDialog) parsePosition(pasteAtX, pasteAtY) else null
    val ringStartHighlightCell = if (showFillRiverRingDialog) parsePosition(ringStartX, ringStartY) else null
    val ambiguousPreviewCells = remember(pathPreviews) { pathPreviews.filter { it.isAmbiguous }.flatMap { it.path }.toSet() }
    val unreachableSpawns = remember(pathPreviews) { pathPreviews.filter { !it.isReachable }.map { it.spawn }.toSet() }

    // Hexagon dimensions - using same constants as game (40.dp)
    val hexSize = 40.dp

    val activeZoneId = selectedZoneId?.takeIf { isZoneDrawingMode && tileZones.any { it.id == selectedZoneId } }

    // In zone drawing mode, painting edits only the selected zone and never falls back to the base map.
    fun paintZoneTile(position: Position) {
        val zoneId = activeZoneId ?: return
        val baseType = tiles["${position.x},${position.y}"]
        val updated =
            tileZones.map { zone ->
                if (zone.id == zoneId) {
                    when (zoneBrush) {
                        ZoneBrush.ADD_KEEP_TYPE ->
                            addTileToZone(zone, position, baseType, riverTiles["${position.x},${position.y}"])
                        ZoneBrush.PAINT_TYPE, ZoneBrush.ERASE ->
                            paintTileZone(
                                zone,
                                position,
                                baseType,
                                if (zoneBrush == ZoneBrush.ERASE) null else selectedTileType,
                                selectedRiverFlow,
                                selectedRiverSpeed,
                            )
                    }
                } else {
                    zone
                }
            }
        if (updated != tileZones) {
            rememberForUndo()
            tileZones = updated
        }
    }

    // Writes a clicked tile into the coordinate fields chosen via a pick button
    fun applyPickedPosition(position: Position) {
        val target = pickTarget ?: return
        when (target) {
            PositionPickTarget.REPLACE_FROM -> {
                replacementFromX = position.x.toString()
                replacementFromY = position.y.toString()
            }
            PositionPickTarget.REPLACE_TO -> {
                replacementToX = position.x.toString()
                replacementToY = position.y.toString()
            }
            PositionPickTarget.COPY_FROM -> {
                copyFromX = position.x.toString()
                copyFromY = position.y.toString()
            }
            PositionPickTarget.COPY_TO -> {
                copyToX = position.x.toString()
                copyToY = position.y.toString()
            }
            PositionPickTarget.PASTE_AT -> {
                pasteAtX = position.x.toString()
                pasteAtY = position.y.toString()
            }
            PositionPickTarget.RING_START -> {
                ringStartX = position.x.toString()
                ringStartY = position.y.toString()
            }
        }
        // A "from" corner is usually followed by its "to" corner
        pickTarget = target.nextPickTarget()
    }

    // Brush paint callback - called when user drags in brush mode
    val onBrushPaint: (position: Position) -> Unit = fn@{ position ->
        // While picking a coordinate on the map, dragging must not paint tiles
        if (pickTarget != null) return@fn

        if (isZoneDrawingMode) {
            if (lastPaintedPos != position) {
                if (activeZoneId != null) paintZoneTile(position)
                lastPaintedPos = position
            }
        } else if (lastPaintedPos == null || lastPaintedPos != position) {
            rememberForUndo()

            val key = "${position.x},${position.y}"
            tiles =
                tiles.toMutableMap().apply {
                    this[key] = selectedTileType
                }

            // If painting a river tile, add river data
            if (selectedTileType == TileType.RIVER) {
                riverTiles =
                    riverTiles.toMutableMap().apply {
                        this[key] =
                            de.egril.defender.model.RiverTile(
                                position = position,
                                flowDirection = selectedRiverFlow,
                                flowSpeed = selectedRiverSpeed,
                            )
                    }
            } else {
                // Remove river data if not a river tile
                riverTiles =
                    riverTiles.toMutableMap().apply {
                        remove(key)
                    }
            }

            // Update target info map
            if (selectedTileType == TileType.TARGET) {
                targetInfoMap =
                    targetInfoMap.toMutableMap().apply {
                        this[key] = EditorTargetInfo(name = selectedTargetName, type = selectedTargetType)
                    }
            } else {
                targetInfoMap =
                    targetInfoMap.toMutableMap().apply {
                        remove(key)
                    }
            }

            if (selectedTileType == TileType.SPAWN_POINT) {
                spawnPointInfoMap =
                    spawnPointInfoMap.toMutableMap().apply {
                        this[key] = selectedSpawnPointType
                    }
            } else {
                spawnPointInfoMap =
                    spawnPointInfoMap.toMutableMap().apply {
                        remove(key)
                    }
            }

            lastPaintedPos = position
        }
    }
    val latestOnBrushPaint by rememberUpdatedState(onBrushPaint)

    val density = LocalDensity.current
    val hexSizePx = with(density) { hexSize.toPx() }
    val horizontalSpacingPx = with(density) { HexagonalGridConstants.HORIZONTAL_SPACING.dp.toPx() }
    val verticalSpacingAdjustmentPx = with(density) { HexagonalGridConstants.VERTICAL_SPACING_ADJUSTMENT.dp.toPx() }

    // Background reference image (displayed behind the map grid)
    if (backgroundImagePainter != null) {
        androidx.compose.foundation.Image(
            painter = backgroundImagePainter,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
        )
    }

    HexagonalMapView(
        gridWidth = mapWidth,
        gridHeight = mapHeight,
        config =
            HexagonalMapConfig(
                hexSize = hexSize.value,
                enableKeyboardNavigation = true, // Enable keyboard navigation for editor
                enablePanNavigation = false, // Disable pan navigation (use brush mode instead)
                enableBrushMode = true, // Enable brush mode for tile painting
                keyboardPanSpeed = 50f, // Increased for better responsiveness
                enableZoomMode = true, // Zoom now works with brush painting
            ),
        scale = zoomLevel,
        offsetX = offsetX,
        offsetY = offsetY,
        onScaleChange = { newScale -> zoomLevel = newScale },
        onOffsetChange = { newX, newY ->
            offsetX = newX
            offsetY = newY
        },
        onActualContentSizeChange = { actualContentSize = it },
        onBrushPaint = onBrushPaint,
        modifier =
            Modifier
                .fillMaxSize()
                .alpha(if (backgroundImagePainter != null) mapOverlayAlpha else 1f)
                .onSizeChanged { containerSize = it }
                .pointerInput(containerSize, actualContentSize, zoomLevel) {
                    detectDragGestures { change, _ ->
                        val pointerPos = change.position

                        // Adjust pointer position for centering
                        // When content is smaller than container, it's centered
                        // The centering must account for the scaled content size
                        val scaledWidth = actualContentSize.width * zoomLevel
                        val scaledHeight = actualContentSize.height * zoomLevel
                        val adjustedX = pointerPos.x - (containerSize.width - scaledWidth) / 2f
                        val adjustedY = pointerPos.y - (containerSize.height - scaledHeight) / 2f
                        val adjustedPointerPos = Offset(adjustedX, adjustedY)

                        val tilePos =
                            screenToHexGridPosition(
                                adjustedPointerPos,
                                offsetX,
                                offsetY,
                                zoomLevel,
                                hexSizePx,
                                horizontalSpacingPx,
                                verticalSpacingAdjustmentPx,
                            )
                        if (tilePos != null) {
                            latestOnBrushPaint(tilePos)
                        }
                    }
                },
        overlayContent = { measuredContentSize ->
            if (showCrosshair) {
                MapCrosshairOverlay(contentSize = measuredContentSize)
            }
        },
    ) { position ->
        val key = "${position.x},${position.y}"
        val activeZone = activeZoneId?.let { id -> tileZones.firstOrNull { it.id == id } }
        val zoneTileType = activeZone?.tiles?.get(position)
        val otherZoneIndex =
            if (showOtherZones) {
                tileZones.indexOfFirst { it.id != selectedZoneId && position in it.tiles }
            } else {
                -1
            }
        // Show the zone's alternative tile while the zone is being edited.
        val tileType = zoneTileType ?: tiles[key] ?: TileType.NO_PLAY
        val riverTile = if (zoneTileType != null) activeZone.riverTiles[position] else riverTiles[key]
        val isWaterSpawnPoint =
            tileType == TileType.SPAWN_POINT &&
                spawnPointInfoMap[key] == SpawnPointType.WATER
        val tileBackgroundColor =
            if (isWaterSpawnPoint) {
                Color(0xFF8A2BE2)
            } else {
                getTileColor(tileType)
            }
        BaseGridCell(
            hexSize = hexSize,
            backgroundColor = tileBackgroundColor,
            borderColor =
                when {
                    zoneTileType != null -> TILE_ZONE_HIGHLIGHT_COLOR
                    otherZoneIndex >= 0 -> tileZoneColor(otherZoneIndex)
                    position == pasteHighlightCell -> PASTE_HIGHLIGHT_COLOR
                    position == ringStartHighlightCell -> RING_START_HIGHLIGHT_COLOR
                    position in copyHighlightCells -> COPY_HIGHLIGHT_COLOR
                    position in replacementHighlightCells -> REPLACE_HIGHLIGHT_COLOR
                    position in highlightedNarrowPaths -> NARROW_PATH_HIGHLIGHT_COLOR
                    position in unreachableSpawns -> Color.Red
                    position in ambiguousPreviewCells -> Color(0xFFFFC107)
                    position in previewCells -> Color(0xFF00BCD4)
                    else -> Color.Black
                },
            borderWidth =
                when {
                    zoneTileType != null -> 3.dp
                    otherZoneIndex >= 0 -> 3.dp
                    position == pasteHighlightCell ||
                        position == ringStartHighlightCell ||
                        position in copyHighlightCells ||
                        position in replacementHighlightCells ||
                        position in highlightedNarrowPaths -> 3.dp
                    position in previewCells || position in unreachableSpawns -> 2.5.dp
                    else -> 1.5.dp
                },
            onClick = {
                if (pickTarget != null) {
                    applyPickedPosition(position)
                } else if (isZoneDrawingMode) {
                    if (activeZoneId != null) paintZoneTile(position)
                } else if (selectedTileType == TileType.TARGET && tileType == TileType.TARGET) {
                    // Clicking an already-TARGET tile while in TARGET mode opens edit dialog
                    editTargetKey = key
                } else {
                    rememberForUndo()
                    tiles =
                        tiles.toMutableMap().apply {
                            this[key] = selectedTileType
                        }

                    // If painting a river tile, add river data
                    if (selectedTileType == TileType.RIVER) {
                        riverTiles =
                            riverTiles.toMutableMap().apply {
                                this[key] =
                                    RiverTile(
                                        position = position,
                                        flowDirection = selectedRiverFlow,
                                        flowSpeed = selectedRiverSpeed,
                                    )
                            }
                    } else {
                        // Remove river data if not a river tile
                        riverTiles =
                            riverTiles.toMutableMap().apply {
                                remove(key)
                            }
                    }

                    // Update target info map
                    if (selectedTileType == TileType.TARGET) {
                        targetInfoMap =
                            targetInfoMap.toMutableMap().apply {
                                this[key] = EditorTargetInfo(name = selectedTargetName, type = selectedTargetType)
                            }
                    } else {
                        targetInfoMap =
                            targetInfoMap.toMutableMap().apply {
                                remove(key)
                            }
                    }

                    if (selectedTileType == TileType.SPAWN_POINT) {
                        spawnPointInfoMap =
                            spawnPointInfoMap.toMutableMap().apply {
                                this[key] = selectedSpawnPointType
                            }
                    } else {
                        spawnPointInfoMap =
                            spawnPointInfoMap.toMutableMap().apply {
                                remove(key)
                            }
                    }
                }
            },
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "${position.x},${position.y}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                )

                // Show target name if this is a target tile
                val targetInfo = targetInfoMap[key]
                if (tileType == TileType.TARGET && targetInfo != null && targetInfo.name.isNotBlank()) {
                    Text(
                        text = targetInfo.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Yellow,
                    )
                }

                // Show river flow indicator if this is a river tile
                if (tileType == TileType.RIVER && riverTile != null) {
                    RiverFlowIndicator(
                        flowDirection = riverTile.flowDirection,
                        flowSpeed = riverTile.flowSpeed,
                        size = 20.dp,
                    )
                }

                if (isWaterSpawnPoint) {
                    Text(
                        text = stringResource(Res.string.spawn).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )
                }
            }
        }
    }
}
