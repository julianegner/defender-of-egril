package de.egril.defender.ui.editor.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.EditorJsonSerializer
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.EditorStorage
import de.egril.defender.editor.EditorTargetInfo
import de.egril.defender.editor.MapSizeLimits
import de.egril.defender.editor.MapTemplateDefinition
import de.egril.defender.editor.TileReplacementArea
import de.egril.defender.editor.TileType
import de.egril.defender.editor.pickBackgroundImageBytes
import de.egril.defender.editor.replaceTilesByType
import de.egril.defender.model.Position
import de.egril.defender.model.RiverFlow
import de.egril.defender.model.RiverTile
import de.egril.defender.model.SpawnPointType
import de.egril.defender.model.TargetType
import de.egril.defender.model.getHexDirectionTo
import de.egril.defender.model.getHexNeighbors
import de.egril.defender.ui.MapImageProvider
import de.egril.defender.ui.constrainMapOffsets
import de.egril.defender.ui.editor.ConfirmationDialog
import de.egril.defender.ui.editor.RiverFlowIndicator
import de.egril.defender.ui.editor.SaveAsDialog
import de.egril.defender.ui.editor.getTileColor
import de.egril.defender.ui.editor.level.DensityBand
import de.egril.defender.ui.editor.level.MapFlowSummary
import de.egril.defender.ui.editor.level.MapLaneShape
import de.egril.defender.ui.editor.level.TravelBand
import de.egril.defender.ui.editor.level.analyzeLevelMapConsistency
import de.egril.defender.ui.editor.level.analyzeMapFlow
import de.egril.defender.ui.hexagon.BaseGridCell
import de.egril.defender.ui.hexagon.HexagonMinimapFromEditorMap
import de.egril.defender.ui.hexagon.HexagonalGridConstants
import de.egril.defender.ui.hexagon.HexagonalMapConfig
import de.egril.defender.ui.hexagon.HexagonalMapView
import de.egril.defender.ui.hexagon.MinimapConfig
import de.egril.defender.ui.icon.CheckmarkIcon
import de.egril.defender.ui.icon.PencilIcon
import de.egril.defender.ui.icon.PushpinIcon
import de.egril.defender.ui.icon.ResizeIcon
import de.egril.defender.ui.icon.RightArrowIcon
import de.egril.defender.ui.icon.ToolsIcon
import de.egril.defender.utils.screenToHexGridPosition
import defender_of_egril.composeapp.generated.resources.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/**
 * View for editing a map
 */
@Composable
fun MapEditorView(
    map: EditorMap,
    onSave: (EditorMap, String?, ByteArray?) -> Unit,
    onCancel: () -> Unit,
) {
    var mapWidth by remember { mutableStateOf(map.width) }
    var mapHeight by remember { mutableStateOf(map.height) }
    var tiles by remember { mutableStateOf(map.tiles.toMutableMap()) }
    var riverTiles by remember { mutableStateOf(map.riverTiles.toMutableMap()) }
    var targetInfoMap by remember { mutableStateOf(map.targetInfoMap.toMutableMap()) }
    var spawnPointInfoMap by remember { mutableStateOf(map.spawnPointInfoMap.toMutableMap()) }
    var selectedTileType by remember { mutableStateOf(TileType.PATH) }
    var selectedRiverFlow by remember { mutableStateOf(de.egril.defender.model.RiverFlow.EAST) }
    var selectedRiverSpeed by remember { mutableStateOf(1) }
    var selectedTargetName by remember { mutableStateOf("") }
    var selectedTargetType by remember { mutableStateOf(TargetType.STANDARD) }
    var selectedSpawnPointType by remember { mutableStateOf(SpawnPointType.LAND) }
    var editTargetKey by remember { mutableStateOf<String?>(null) } // Key of a tile being edited in the inline dialog
    var mapName by remember { mutableStateOf(map.name) }
    var mapAuthor by remember { mutableStateOf(map.author) }
    var mapToolingInfo by remember { mutableStateOf(map.mapToolingInfo) }
    var allowNoBuildableTiles by remember { mutableStateOf(map.allowNoBuildableTiles) }
    var allowNoDirectPath by remember { mutableStateOf(map.allowNoDirectPath) }
    var tileZones by remember { mutableStateOf(map.tileZones) }
    var showTileZonePanel by remember { mutableStateOf(false) }
    var showOtherZones by remember { mutableStateOf(false) }
    var selectedZoneId by remember { mutableStateOf<String?>(null) }
    var isZoneDrawingMode by remember { mutableStateOf(false) }
    var zoneBrush by remember { mutableStateOf(ZoneBrush.PAINT_TYPE) }
    var showSaveAsDialog by remember { mutableStateOf(false) }
    var showSaveTemplateDialog by remember { mutableStateOf(false) }
    var showTileReplacementDialog by remember { mutableStateOf(false) }
    var showFillRiverRingDialog by remember { mutableStateOf(false) }
    var fillRiverRingError by remember { mutableStateOf(false) }
    var ringStartX by remember { mutableStateOf("0") }
    var ringStartY by remember { mutableStateOf("0") }
    var ringClockwise by remember { mutableStateOf(true) }
    var ringInnerToOuter by remember { mutableStateOf(true) }
    var templateName by remember { mutableStateOf(map.name) }
    var replacementSourceTileType by remember { mutableStateOf(TileType.NO_PLAY) }
    var replacementTargetTileType by remember { mutableStateOf(TileType.PATH) }
    var replacementLimitToArea by remember { mutableStateOf(false) }
    var replacementFromX by remember { mutableStateOf("0") }
    var replacementFromY by remember { mutableStateOf("0") }
    var replacementToX by remember { mutableStateOf((map.width - 1).coerceAtLeast(0).toString()) }
    var replacementToY by remember { mutableStateOf((map.height - 1).coerceAtLeast(0).toString()) }
    var sourceTileDropdownExpanded by remember { mutableStateOf(false) }
    var targetTileDropdownExpanded by remember { mutableStateOf(false) }
    var replacementRiverFlow by remember { mutableStateOf(de.egril.defender.model.RiverFlow.EAST) }
    var replacementRiverSpeed by remember { mutableStateOf(1) }
    var communityUploadStatus by remember { mutableStateOf<String?>(null) }
    var isUploadingToCommunity by remember { mutableStateOf(false) }
    var showCommunityUploadConfirm by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var zoomLevel by remember { mutableStateOf(1.0f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var lastPaintedPos by remember { mutableStateOf<Position?>(null) }
    var isHeaderExpanded by remember { mutableStateOf(false) }
    var isSidePanelExpanded by remember { mutableStateOf(true) }
    var backgroundImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var mapPreviewBytes by remember { mutableStateOf<ByteArray?>(null) }
    var mapPreviewPainter by remember { mutableStateOf<BitmapPainter?>(null) }
    var mapPreviewRegenerating by remember { mutableStateOf(false) }
    var mapPreviewError by remember { mutableStateOf<String?>(null) }
    var showMapImageGenerationDialog by remember { mutableStateOf(false) }
    var mapImageGenerationRunning by remember { mutableStateOf(false) }
    var mapImageGenerationSuccess by remember { mutableStateOf<Boolean?>(null) }
    var mapImageGenerationError by remember { mutableStateOf<String?>(null) }
    var mapImageProgress by remember { mutableStateOf<List<ImageGenerationProgress>>(emptyList()) }
    var mapImagePreviews by remember { mutableStateOf<List<MapImagePreview>>(emptyList()) }
    var mapImagePreviewIndex by remember { mutableStateOf(0) }
    var mapOverlayAlpha by remember { mutableStateOf(0.7f) }
    var resizeLeft by remember { mutableStateOf("0") }
    var resizeRight by remember { mutableStateOf("0") }
    var resizeTop by remember { mutableStateOf("0") }
    var resizeBottom by remember { mutableStateOf("0") }
    var undoHistory by remember { mutableStateOf(listOf<MapEditorSnapshot>()) }
    var redoHistory by remember { mutableStateOf(listOf<MapEditorSnapshot>()) }
    var areaClipboard by remember { mutableStateOf<MapRegionClipboard?>(null) }
    var showAreaClipboardDialog by remember { mutableStateOf(false) }
    var showResizeSection by remember { mutableStateOf(false) }
    var showMapFlowOverlay by remember { mutableStateOf(false) }
    var showMapPathPreviewOverlay by remember { mutableStateOf(false) }
    var showCrosshair by remember { mutableStateOf(false) }
    var copyFromX by remember { mutableStateOf("0") }
    var copyFromY by remember { mutableStateOf("0") }
    var copyToX by remember { mutableStateOf((map.width - 1).coerceAtLeast(0).toString()) }
    var copyToY by remember { mutableStateOf((map.height - 1).coerceAtLeast(0).toString()) }
    var pasteAtX by remember { mutableStateOf("0") }
    var pasteAtY by remember { mutableStateOf("0") }
    var pickTarget by remember { mutableStateOf<PositionPickTarget?>(null) }
    val backgroundImagePainter =
        remember(backgroundImageBytes) {
            backgroundImageBytes?.let { bytes ->
                val bitmap = MapImageProvider.decodeImageBitmap(bytes)
                if (bitmap != null) BitmapPainter(bitmap) else null
            }
        }

    // Track container and content sizes for constraint calculation
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var actualContentSize by remember { mutableStateOf(IntSize.Zero) }

    // Create updated map for minimap that reflects current tiles state
    val currentMap =
        remember(mapWidth, mapHeight, tiles, riverTiles, targetInfoMap, spawnPointInfoMap, mapToolingInfo, allowNoBuildableTiles, allowNoDirectPath, tileZones) {
            map.copy(
                tileZones = tileZones,
                width = mapWidth,
                height = mapHeight,
                tiles = tiles.toMap(),
                riverTiles = riverTiles.toMap(),
                targetInfoMap = targetInfoMap.toMap(),
                spawnPointInfoMap = spawnPointInfoMap.toMap(),
                mapToolingInfo = mapToolingInfo,
                allowNoBuildableTiles = allowNoBuildableTiles,
                allowNoDirectPath = allowNoDirectPath,
            )
        }

    fun refreshMapPreviewImage() {
        coroutineScope.launch {
            mapPreviewError = null
            val preferredIds = listOf(currentMap.id, map.id).distinct().filter { it.isNotBlank() }
            val existingPreviewBytes =
                preferredIds.firstNotNullOfOrNull { id ->
                    MapImageProvider.loadMapImageBytes(id)
                }
            var nextPreviewBytes = existingPreviewBytes

            if (nextPreviewBytes == null) {
                try {
                    val (pixels, width, height) =
                        withContext(Dispatchers.Default) {
                            EditorStorage.generateMapPixels(currentMap)
                        }
                    withContext(Dispatchers.Default) {
                        EditorStorage.compressAndSaveMapImage(currentMap, pixels, width, height)
                    }
                    nextPreviewBytes =
                        preferredIds.firstNotNullOfOrNull { id ->
                            MapImageProvider.loadMapImageBytes(id)
                        }
                } catch (e: Exception) {
                    mapPreviewError = e.message ?: "Failed to generate map preview"
                    nextPreviewBytes = null
                }
            }

            val decodedPainter =
                nextPreviewBytes?.let { bytes ->
                    val bitmap = MapImageProvider.decodeImageBitmap(bytes)
                    bitmap?.let { BitmapPainter(it) }
                }

            mapPreviewBytes = nextPreviewBytes
            mapPreviewPainter = decodedPainter
        }
    }

    suspend fun loadMapPreviewFromDisk(): BitmapPainter? =
        withContext(Dispatchers.Default) {
            val preferredIds = listOf(currentMap.id, map.id).distinct().filter { it.isNotBlank() }
            val bytes =
                preferredIds.firstNotNullOfOrNull { id ->
                    MapImageProvider.loadMapImageBytes(id)
                }
            val painter =
                bytes?.let { imageBytes ->
                    val bitmap = MapImageProvider.decodeImageBitmap(imageBytes)
                    bitmap?.let { BitmapPainter(it) }
                }
            mapPreviewBytes = bytes
            mapPreviewPainter = painter
            mapImageGenerationSuccess = bytes != null
            if (bytes == null) {
                mapImageGenerationError = "Map image not found"
            } else {
                mapImageGenerationError = null
            }
            painter
        }

    fun regenerateMapPreview() {
        if (mapImageGenerationRunning) return
        mapImageGenerationRunning = true
        mapImageGenerationSuccess = null
        mapImageGenerationError = null
        mapImageProgress = emptyList()

        coroutineScope.launch {
            try {
                val saveResult = withContext(Dispatchers.Default) { EditorStorage.saveMapData(currentMap) }
                processMapImages(saveResult) { mapImageProgress = it }
                mapImagePreviews = loadMapImagePreviews(mapImageFileNames(currentMap))
                mapImagePreviewIndex = 0
                val refreshedPainter = loadMapPreviewFromDisk()
                mapPreviewPainter = refreshedPainter
                mapImageGenerationSuccess = refreshedPainter != null
                if (refreshedPainter == null) {
                    mapImageGenerationError = "Map image could not be loaded after regeneration"
                    mapImageGenerationSuccess = false
                }
            } catch (e: Exception) {
                mapImageGenerationSuccess = false
                mapImageGenerationError = e.message ?: "Failed to regenerate map preview"
            } finally {
                mapImageGenerationRunning = false
            }
        }
    }

    val mapFlowSummary = remember(currentMap) { analyzeMapFlow(currentMap) }
    val pathPreviews = remember(currentMap) { currentMap.previewPaths() }
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

    // Hexagon dimensions - using same constants as game (40.dp)
    val hexSize = 40.dp

    fun currentSnapshot(): MapEditorSnapshot =
        MapEditorSnapshot(
            width = mapWidth,
            height = mapHeight,
            tiles = tiles.toMutableMap(),
            riverTiles = riverTiles.toMutableMap(),
            targetInfoMap = targetInfoMap.toMutableMap(),
            spawnPointInfoMap = spawnPointInfoMap.toMutableMap(),
            mapName = mapName,
            mapAuthor = mapAuthor,
            mapToolingInfo = mapToolingInfo,
            allowNoBuildableTiles = allowNoBuildableTiles,
            allowNoDirectPath = allowNoDirectPath,
            tileZones = tileZones,
        )

    fun restoreSnapshot(snapshot: MapEditorSnapshot) {
        mapWidth = snapshot.width
        mapHeight = snapshot.height
        tiles = snapshot.tiles.toMutableMap()
        riverTiles = snapshot.riverTiles.toMutableMap()
        targetInfoMap = snapshot.targetInfoMap.toMutableMap()
        spawnPointInfoMap = snapshot.spawnPointInfoMap.toMutableMap()
        mapName = snapshot.mapName
        mapAuthor = snapshot.mapAuthor
        mapToolingInfo = snapshot.mapToolingInfo
        allowNoBuildableTiles = snapshot.allowNoBuildableTiles
        allowNoDirectPath = snapshot.allowNoDirectPath
        tileZones = snapshot.tileZones
        replacementToX = (snapshot.width - 1).coerceAtLeast(0).toString()
        replacementToY = (snapshot.height - 1).coerceAtLeast(0).toString()
        copyToX = replacementToX
        copyToY = replacementToY
    }

    fun rememberForUndo() {
        undoHistory = (undoHistory + currentSnapshot()).takeLast(40)
        redoHistory = emptyList()
    }

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

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        // Map grid layer
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            val density = LocalDensity.current
            val hexSizePx = with(density) { hexSize.toPx() }
            val horizontalSpacingPx = with(density) { HexagonalGridConstants.HORIZONTAL_SPACING.dp.toPx() }
            val verticalSpacingAdjustmentPx = with(density) { HexagonalGridConstants.VERTICAL_SPACING_ADJUSTMENT.dp.toPx() }
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(8.dp),
                ) {
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
                                        position in replacementHighlightCells -> 3.dp
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

                    MapControls(
                        mapControlState =
                            MapControlState(
                                zoomLevel = zoomLevel,
                                offsetX = offsetX,
                                offsetY = offsetY,
                            ),
                        onStateChange = { newState ->
                            val newScale = newState.zoomLevel
                            val (constrainedX, constrainedY) =
                                constrainMapOffsets(
                                    newState.offsetX,
                                    newState.offsetY,
                                    newScale,
                                    containerSize,
                                    actualContentSize,
                                )
                            zoomLevel = newScale
                            offsetX = constrainedX
                            offsetY = constrainedY
                        },
                    ) {
                        // Minimap
                        HexagonMinimapFromEditorMap(
                            map = currentMap,
                            modifier = Modifier.size(150.dp),
                            config =
                                MinimapConfig(
                                    showViewport = true,
                                    minimapSizeDp = 150f,
                                ),
                            scale = zoomLevel,
                            offsetX = offsetX,
                            offsetY = offsetY,
                            containerSize = containerSize,
                            contentSize = actualContentSize,
                            onViewportDrag = { newOffsetX, newOffsetY ->
                                offsetX = newOffsetX
                                offsetY = newOffsetY
                            },
                        )
                    }
                }

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

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        // For user maps: if the name changed, derive a new ID from the new name
                        // so that the JSON and PNG filenames match the new name.
                        val (newId, oldId) =
                            if (!map.isOfficial && mapName.trim() != map.name.trim()) {
                                val derived = nameToMapId(mapName)
                                if (derived.isNotEmpty() && derived != map.id) Pair(derived, map.id) else Pair(map.id, null)
                            } else {
                                Pair(map.id, null)
                            }
                        val updatedMap =
                            map.copy(
                                id = newId,
                                name = mapName,
                                author = mapAuthor,
                                mapToolingInfo = mapToolingInfo,
                                allowNoBuildableTiles = allowNoBuildableTiles,
                                allowNoDirectPath = allowNoDirectPath,
                                tileZones = tileZones,
                                width = mapWidth,
                                height = mapHeight,
                                tiles = tiles.toMap(),
                                riverTiles = riverTiles.toMap(),
                                targetInfoMap = targetInfoMap.toMap(),
                                spawnPointInfoMap = spawnPointInfoMap.toMap(),
                            )
                        // Validate and set readyToUse flag
                        val validatedMap = updatedMap.copy(readyToUse = updatedMap.validateReadyToUse())
                        onSave(validatedMap, oldId, backgroundImageBytes)
                    },
                    enabled = !map.isOfficial || de.egril.defender.OfficialEditMode.enabled,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(Res.string.save_map))
                }

                Button(
                    onClick = { showSaveAsDialog = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(Res.string.save_as_new))
                }

                Button(
                    onClick = {
                        templateName = mapName.ifBlank { map.name }
                        showSaveTemplateDialog = true
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(Res.string.save_as_template))
                }

                Button(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(Res.string.cancel))
                }
            }

            // Community upload button - only shown for non-official maps when user is authenticated
            val iamState by de.egril.defender.iam.IamService.state
            if (!map.isOfficial && iamState.isAuthenticated) {
                val currentMapJson =
                    remember(map.id, mapWidth, mapHeight, tiles.hashCode(), riverTiles.hashCode(), targetInfoMap.hashCode(), spawnPointInfoMap.hashCode(), mapToolingInfo, allowNoBuildableTiles, allowNoDirectPath, tileZones) {
                        val updatedMap =
                            map.copy(
                                width = mapWidth,
                                height = mapHeight,
                                tiles = tiles.toMap(),
                                riverTiles = riverTiles.toMap(),
                                targetInfoMap = targetInfoMap.toMap(),
                                spawnPointInfoMap = spawnPointInfoMap.toMap(),
                                mapToolingInfo = mapToolingInfo,
                                allowNoBuildableTiles = allowNoBuildableTiles,
                                allowNoDirectPath = allowNoDirectPath,
                                tileZones = tileZones,
                            )
                        de.egril.defender.editor.EditorJsonSerializer
                            .serializeMap(updatedMap)
                    }
                val storedCommunityJson =
                    remember(map.id) {
                        de.egril.defender.editor.EditorStorage
                            .getStoredCommunityMapJson(map.id)
                    }
                val storedCommunityMap =
                    remember(map.id) {
                        de.egril.defender.editor.EditorStorage
                            .getCommunityMap(map.id)
                    }
                val isMyUpload = storedCommunityMap?.communityAuthorUsername == iamState.username
                val isChanged = storedCommunityJson != null && storedCommunityJson != currentMapJson

                fun doMapUpload(token: String) {
                    isUploadingToCommunity = true
                    communityUploadStatus = null
                    coroutineScope.launch {
                        val success =
                            de.egril.defender.save.BackendCommunityService
                                .uploadCommunityFile("MAP", map.id, currentMapJson, token)
                        if (success) {
                            val updatedMap =
                                map.copy(
                                    width = mapWidth,
                                    height = mapHeight,
                                    tiles = tiles.toMap(),
                                    riverTiles = riverTiles.toMap(),
                                    targetInfoMap = targetInfoMap.toMap(),
                                    spawnPointInfoMap = spawnPointInfoMap.toMap(),
                                    mapToolingInfo = mapToolingInfo,
                                    allowNoBuildableTiles = allowNoBuildableTiles,
                                    allowNoDirectPath = allowNoDirectPath,
                                    tileZones = tileZones,
                                )
                            de.egril.defender.editor.EditorStorage.saveCommunityMap(
                                updatedMap,
                                iamState.username ?: "",
                            )
                            communityUploadStatus = "success"
                        } else {
                            communityUploadStatus = "error"
                        }
                        isUploadingToCommunity = false
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (storedCommunityJson == null) {
                        Button(
                            onClick = { showCommunityUploadConfirm = true },
                            enabled = !isUploadingToCommunity,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                if (isUploadingToCommunity) {
                                    stringResource(Res.string.community_uploading)
                                } else {
                                    stringResource(Res.string.upload_as_community_map)
                                },
                            )
                        }
                    } else if (isMyUpload && isChanged) {
                        Button(
                            onClick = {
                                val token =
                                    de.egril.defender.iam.IamService
                                        .getToken() ?: return@Button
                                doMapUpload(token)
                            },
                            enabled = !isUploadingToCommunity,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                if (isUploadingToCommunity) {
                                    stringResource(Res.string.community_uploading)
                                } else {
                                    stringResource(Res.string.update_community_map)
                                },
                            )
                        }
                    }
                }
                communityUploadStatus?.let { status ->
                    Text(
                        text =
                            if (status == "success") {
                                stringResource(Res.string.community_upload_success)
                            } else {
                                stringResource(Res.string.community_upload_failed)
                            },
                        color =
                            if (status == "success") {
                                Color(0xFF2E7D32)
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }

                // Confirmation dialog before first community map upload
                if (showCommunityUploadConfirm) {
                    de.egril.defender.ui.editor.ConfirmationDialog(
                        title = stringResource(Res.string.upload_community_confirm_title),
                        message = stringResource(Res.string.upload_community_map_confirm_message, iamState.username ?: ""),
                        extraContent = {
                            de.egril.defender.ui.editor
                                .CommunityUploadLicenseNotice()
                        },
                        requireLicenseAgreement = true,
                        onDismiss = { showCommunityUploadConfirm = false },
                        onConfirm = {
                            showCommunityUploadConfirm = false
                            val token =
                                de.egril.defender.iam.IamService
                                    .getToken() ?: return@ConfirmationDialog
                            doMapUpload(token)
                        },
                    )
                }
            }
        }
    }

    LaunchedEffect(showMapImageGenerationDialog) {
        if (showMapImageGenerationDialog && shouldLoadMapPreviewFromDisk(mapPreviewPainter != null)) {
            mapPreviewPainter = loadMapPreviewFromDisk()
        }
        if (showMapImageGenerationDialog) {
            mapImagePreviews = loadMapImagePreviews(mapImageFileNames(currentMap))
            mapImagePreviewIndex = 0
        }
    }

    if (showMapImageGenerationDialog) {
        AlertDialog(
            onDismissRequest = { if (!mapImageGenerationRunning) showMapImageGenerationDialog = false },
            title = { Text(stringResource(Res.string.map_image_generation_title, currentMap.name)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MapImageProgressRows(mapImageProgress)
                    if (mapImageGenerationSuccess == false) {
                        Text(stringResource(Res.string.map_image_generation_error, mapImageGenerationError ?: ""))
                    }
                    if (mapImagePreviews.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(Res.string.map_image_generation_preview_label))
                            MapImageCarousel(
                                previews = mapImagePreviews,
                                index = mapImagePreviewIndex.coerceIn(0, mapImagePreviews.lastIndex),
                                onIndexChange = { mapImagePreviewIndex = it },
                            )
                        }
                    }
                    if (currentMap.tileZones.any { it.movable }) {
                        MovableZoneImageHint()
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { regenerateMapPreview() },
                        enabled = !mapImageGenerationRunning,
                    ) {
                        Text(stringResource(Res.string.regenerate_map_image))
                    }
                    TextButton(onClick = { showMapImageGenerationDialog = false }) {
                        Text(stringResource(Res.string.map_image_generation_close))
                    }
                }
            },
        )
    }

    // The modal dialog would swallow map clicks, so it is hidden while the start tile is being picked
    if (showFillRiverRingDialog && pickTarget != PositionPickTarget.RING_START) {
        AlertDialog(
            onDismissRequest = {
                if (pickTarget == PositionPickTarget.RING_START) pickTarget = null
                showFillRiverRingDialog = false
            },
            title = { Text(stringResource(Res.string.fill_river_ring_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ringStartX,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("-?[0-9]+"))) ringStartX = it },
                        label = { Text(stringResource(Res.string.ring_start_x)) },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = ringStartY,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("-?[0-9]+"))) ringStartY = it },
                        label = { Text(stringResource(Res.string.ring_start_y)) },
                        singleLine = true,
                    )
                    PositionPickButton(
                        active = false,
                        onClick = { pickTarget = PositionPickTarget.RING_START },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = ringClockwise, onClick = { ringClockwise = true }, label = { Text(stringResource(Res.string.clockwise)) })
                        FilterChip(selected = !ringClockwise, onClick = { ringClockwise = false }, label = { Text(stringResource(Res.string.counterclockwise)) })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = ringInnerToOuter, onClick = { ringInnerToOuter = true }, label = { Text(stringResource(Res.string.inner_to_outer)) })
                        FilterChip(selected = !ringInnerToOuter, onClick = { ringInnerToOuter = false }, label = { Text(stringResource(Res.string.outer_to_inner)) })
                    }
                    Text(
                        text = stringResource(Res.string.fill_river_ring_hint),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (fillRiverRingError) {
                        Text(
                            text = stringResource(Res.string.fill_river_ring_error_not_water),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val startX = ringStartX.toIntOrNull() ?: return@Button
                    val startY = ringStartY.toIntOrNull() ?: return@Button
                    val startKey = "$startX,$startY"
                    if (tiles[startKey] != TileType.RIVER) {
                        fillRiverRingError = true
                        return@Button
                    }
                    fillRiverRingError = false
                    val updated =
                        applyRiverRing(
                            tiles = tiles,
                            riverTiles = riverTiles,
                            mapWidth = mapWidth,
                            mapHeight = mapHeight,
                            start = Position(startX, startY),
                            clockwise = ringClockwise,
                            innerToOuter = ringInnerToOuter,
                            flowSpeed = selectedRiverSpeed,
                        )
                    if (updated.second != riverTiles) {
                        rememberForUndo()
                        tiles = updated.first
                        riverTiles = updated.second
                    }
                    if (pickTarget == PositionPickTarget.RING_START) pickTarget = null
                    showFillRiverRingDialog = false
                }) { Text(stringResource(Res.string.apply)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    fillRiverRingError = false
                    if (pickTarget == PositionPickTarget.RING_START) pickTarget = null
                    showFillRiverRingDialog = false
                }) { Text(stringResource(Res.string.cancel)) }
            },
        )
    }

    if (showSaveAsDialog) {
        SaveAsDialog(
            title = stringResource(Res.string.save_map_as_new),
            label = stringResource(Res.string.map_name),
            currentValue = mapName,
            onDismiss = { showSaveAsDialog = false },
            onSave = { newName ->
                // Save as new map with ID based on name
                val newId =
                    nameToMapId(newName).ifEmpty {
                        "map_copy_${kotlin.random.Random.nextInt(10000, 99999)}"
                    }
                val newMap =
                    map.copy(
                        id = newId,
                        name = newName,
                        author = mapAuthor,
                        mapToolingInfo = mapToolingInfo,
                        allowNoBuildableTiles = allowNoBuildableTiles,
                        allowNoDirectPath = allowNoDirectPath,
                        tileZones = tileZones,
                        width = mapWidth,
                        height = mapHeight,
                        tiles = tiles.toMap(),
                        riverTiles = riverTiles.toMap(),
                        targetInfoMap = targetInfoMap.toMap(),
                        spawnPointInfoMap = spawnPointInfoMap.toMap(),
                        isOfficial = false, // Save as new always creates a user map
                    )
                // Validate and set readyToUse flag
                val validatedMap = newMap.copy(readyToUse = newMap.validateReadyToUse())
                onSave(validatedMap, null, backgroundImageBytes) // null oldId: this is a brand-new map, not a rename
                showSaveAsDialog = false
            },
        )
    }

    if (showSaveTemplateDialog) {
        SaveAsDialog(
            title = stringResource(Res.string.save_map_template),
            label = stringResource(Res.string.template_name),
            currentValue = templateName,
            onDismiss = { showSaveTemplateDialog = false },
            onSave = { newName ->
                val templateId = createTemplateId(newName.ifBlank { mapName.ifBlank { map.name } })
                EditorStorage.saveMapTemplate(
                    MapTemplateDefinition(
                        id = templateId,
                        name = newName.ifBlank { mapName.ifBlank { map.name } },
                        templateMap =
                            currentMap.copy(
                                id = templateId,
                                name = newName.ifBlank { mapName.ifBlank { map.name } },
                                isOfficial = false,
                            ),
                    ),
                )
                showSaveTemplateDialog = false
            },
        )
    }

    // Edit target dialog - opens when clicking an existing TARGET tile while in TARGET mode
    editTargetKey?.let { editKey ->
        val existingInfo = targetInfoMap[editKey]
        var editName by remember(editKey) { mutableStateOf(existingInfo?.name ?: "") }
        var editType by remember(editKey) { mutableStateOf(existingInfo?.type ?: TargetType.STANDARD) }
        AlertDialog(
            onDismissRequest = { editTargetKey = null },
            title = { Text(stringResource(Res.string.target_name_label)) },
            text = {
                Column(
                    verticalArrangement =
                        androidx.compose.foundation.layout.Arrangement
                            .spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text(stringResource(Res.string.target_name_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Text(stringResource(Res.string.target_type_label), style = MaterialTheme.typography.bodyMedium)
                    Row(
                        horizontalArrangement =
                            androidx.compose.foundation.layout.Arrangement
                                .spacedBy(4.dp),
                    ) {
                        TargetType.entries.forEach { type ->
                            val label =
                                when (type) {
                                    TargetType.STANDARD -> stringResource(Res.string.target_type_standard)
                                    TargetType.SINGLE_HIT -> stringResource(Res.string.target_type_single_hit)
                                }
                            Button(
                                onClick = { editType = type },
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor =
                                            if (editType == type) {
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
                Button(onClick = {
                    rememberForUndo()
                    targetInfoMap =
                        targetInfoMap.toMutableMap().apply {
                            this[editKey] = EditorTargetInfo(name = editName, type = editType)
                        }
                    editTargetKey = null
                }) {
                    Text(stringResource(Res.string.ok))
                }
            },
            dismissButton = {
                Button(onClick = { editTargetKey = null }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }
}
