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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.EditorJsonSerializer
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.EditorStorage
import de.egril.defender.editor.EditorTargetInfo
import de.egril.defender.editor.MapTemplateDefinition
import de.egril.defender.editor.TileReplacementArea
import de.egril.defender.editor.TileType
import de.egril.defender.editor.pickBackgroundImageBytes
import de.egril.defender.editor.replaceTilesByType
import de.egril.defender.model.Position
import de.egril.defender.model.RiverTile
import de.egril.defender.model.SpawnPointType
import de.egril.defender.model.TargetType
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
import de.egril.defender.ui.hexagon.HexagonalMapConfig
import de.egril.defender.ui.hexagon.HexagonalMapView
import de.egril.defender.ui.hexagon.MinimapConfig
import de.egril.defender.utils.screenToHexGridPosition
import defender_of_egril.composeapp.generated.resources.*
import kotlinx.coroutines.launch

/**
 * Converts a human-readable map name into a stable map ID component, e.g. "My Map" → "my_map".
 */
private fun nameToMapId(name: String): String {
    val sanitized =
        name
            .trim()
            .lowercase()
            .replace(" ", "_")
            .replace(Regex("[^a-z0-9_]"), "")
            .replace(Regex("_+"), "_")
    return if (sanitized.isNotEmpty()) "map_$sanitized" else ""
}

internal data class ResizedMapData(
    val width: Int,
    val height: Int,
    val tiles: MutableMap<String, TileType>,
    val riverTiles: MutableMap<String, RiverTile>,
    val targetInfoMap: MutableMap<String, EditorTargetInfo>,
    val spawnPointInfoMap: MutableMap<String, SpawnPointType>,
)

private data class MapEditorSnapshot(
    val width: Int,
    val height: Int,
    val tiles: MutableMap<String, TileType>,
    val riverTiles: MutableMap<String, RiverTile>,
    val targetInfoMap: MutableMap<String, EditorTargetInfo>,
    val spawnPointInfoMap: MutableMap<String, SpawnPointType>,
    val mapName: String,
    val mapAuthor: String,
    val mapToolingInfo: String,
    val allowNoBuildableTiles: Boolean,
    val allowNoDirectPath: Boolean,
)

private data class MapRegionClipboard(
    val width: Int,
    val height: Int,
    val tiles: Map<String, TileType>,
    val riverTiles: Map<String, RiverTile>,
    val targetInfoMap: Map<String, EditorTargetInfo>,
    val spawnPointInfoMap: Map<String, SpawnPointType>,
)

private data class MapPathPreview(
    val spawn: Position,
    val target: Position?,
    val path: List<Position>,
    val isReachable: Boolean,
    val isAmbiguous: Boolean,
)

internal fun applyResizeToMapData(
    width: Int,
    height: Int,
    leftDelta: Int,
    rightDelta: Int,
    topDelta: Int,
    bottomDelta: Int,
    tiles: Map<String, TileType>,
    riverTiles: Map<String, RiverTile>,
    targetInfoMap: Map<String, EditorTargetInfo>,
    spawnPointInfoMap: Map<String, SpawnPointType>,
): ResizedMapData {
    val newWidth = width + leftDelta + rightDelta
    val newHeight = height + topDelta + bottomDelta
    require(newWidth > 0 && newHeight > 0)

    fun shiftedPosition(position: Position): Position? {
        val shifted = Position(position.x + leftDelta, position.y + topDelta)
        return shifted.takeIf { it.x in 0 until newWidth && it.y in 0 until newHeight }
    }

    val resizedTiles = mutableMapOf<String, TileType>()
    tiles.forEach { (key, value) ->
        val (x, y) = key.split(",").let { it[0].toInt() to it[1].toInt() }
        shiftedPosition(Position(x, y))?.let { resizedTiles["${it.x},${it.y}"] = value }
    }

    val resizedRiverTiles = mutableMapOf<String, RiverTile>()
    riverTiles.forEach { (_, riverTile) ->
        shiftedPosition(riverTile.position)?.let { shifted ->
            resizedRiverTiles["${shifted.x},${shifted.y}"] =
                riverTile.copy(position = shifted)
        }
    }

    val resizedTargetInfo = mutableMapOf<String, EditorTargetInfo>()
    targetInfoMap.forEach { (key, info) ->
        val (x, y) = key.split(",").let { it[0].toInt() to it[1].toInt() }
        shiftedPosition(Position(x, y))?.let { resizedTargetInfo["${it.x},${it.y}"] = info }
    }

    val resizedSpawnPointInfo = mutableMapOf<String, SpawnPointType>()
    spawnPointInfoMap.forEach { (key, type) ->
        val (x, y) = key.split(",").let { it[0].toInt() to it[1].toInt() }
        shiftedPosition(Position(x, y))?.let { resizedSpawnPointInfo["${it.x},${it.y}"] = type }
    }

    return ResizedMapData(
        width = newWidth,
        height = newHeight,
        tiles = resizedTiles,
        riverTiles = resizedRiverTiles,
        targetInfoMap = resizedTargetInfo,
        spawnPointInfoMap = resizedSpawnPointInfo,
    )
}

internal fun isSafeEndExpansion(
    leftDelta: Int,
    rightDelta: Int,
    topDelta: Int,
    bottomDelta: Int,
): Boolean = leftDelta == 0 && topDelta == 0 && rightDelta >= 0 && bottomDelta >= 0

private fun createTemplateId(name: String): String = nameToMapId(name).removePrefix("map_").let { "template_$it" }

private fun EditorMap.previewPaths(): List<MapPathPreview> {
    val traversableCells = getPathCells() + getSpawnPoints() + getTargets() + getRiverCells()
    val targets = getTargets().toSet()
    return getSpawnPoints().sortedWith(compareBy(Position::y, Position::x)).map { spawn ->
        val path = findShortestPath(spawn, targets, traversableCells, width, height)
        val isAmbiguous =
            path.size > 1 &&
                path.dropLast(1).anyIndexed { index, current ->
                    val previous = path.getOrNull(index - 1)
                    current
                        .getHexNeighbors()
                        .count { neighbor ->
                            neighbor != previous &&
                                neighbor in traversableCells &&
                                neighbor.isInside(width, height)
                        } > 1
                }
        MapPathPreview(
            spawn = spawn,
            target = path.lastOrNull()?.takeIf { it in targets },
            path = path,
            isReachable = path.isNotEmpty(),
            isAmbiguous = isAmbiguous,
        )
    }
}

private fun findShortestPath(
    start: Position,
    targets: Set<Position>,
    traversableCells: Set<Position>,
    width: Int,
    height: Int,
): List<Position> {
    if (start in targets) return listOf(start)
    val queue = ArrayDeque<Position>()
    val visited = mutableSetOf(start)
    val previous = mutableMapOf<Position, Position?>()
    queue.add(start)
    previous[start] = null
    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        current
            .getHexNeighbors()
            .filter { it.isInside(width, height) && it in traversableCells }
            .forEach { neighbor ->
                if (!visited.add(neighbor)) return@forEach
                previous[neighbor] = current
                if (neighbor in targets) {
                    val path = mutableListOf<Position>()
                    var cursor: Position? = neighbor
                    while (cursor != null) {
                        path += cursor
                        cursor = previous[cursor]
                    }
                    return path.reversed()
                }
                queue.add(neighbor)
            }
    }
    return emptyList()
}

private fun copyMapRegion(
    from: Position,
    to: Position,
    tiles: Map<String, TileType>,
    riverTiles: Map<String, RiverTile>,
    targetInfoMap: Map<String, EditorTargetInfo>,
    spawnPointInfoMap: Map<String, SpawnPointType>,
): MapRegionClipboard {
    val minX = minOf(from.x, to.x)
    val maxX = maxOf(from.x, to.x)
    val minY = minOf(from.y, to.y)
    val maxY = maxOf(from.y, to.y)

    fun relativeKey(
        x: Int,
        y: Int,
    ): String = "${x - minX},${y - minY}"

    val copiedTiles =
        tiles
            .mapNotNull { (key, tileType) ->
                val (x, y) = key.split(",").let { it[0].toInt() to it[1].toInt() }
                if (x in minX..maxX && y in minY..maxY) relativeKey(x, y) to tileType else null
            }.toMap()
    val copiedRiverTiles =
        riverTiles
            .mapNotNull { (_, riverTile) ->
                val x = riverTile.position.x
                val y = riverTile.position.y
                if (x in minX..maxX && y in minY..maxY) {
                    val relative = Position(x - minX, y - minY)
                    relativeKey(x, y) to riverTile.copy(position = relative)
                } else {
                    null
                }
            }.toMap()
    val copiedTargetInfo =
        targetInfoMap
            .mapNotNull { (key, info) ->
                val (x, y) = key.split(",").let { it[0].toInt() to it[1].toInt() }
                if (x in minX..maxX && y in minY..maxY) relativeKey(x, y) to info else null
            }.toMap()
    val copiedSpawnInfo =
        spawnPointInfoMap
            .mapNotNull { (key, type) ->
                val (x, y) = key.split(",").let { it[0].toInt() to it[1].toInt() }
                if (x in minX..maxX && y in minY..maxY) relativeKey(x, y) to type else null
            }.toMap()
    return MapRegionClipboard(
        width = maxX - minX + 1,
        height = maxY - minY + 1,
        tiles = copiedTiles,
        riverTiles = copiedRiverTiles,
        targetInfoMap = copiedTargetInfo,
        spawnPointInfoMap = copiedSpawnInfo,
    )
}

private fun pasteMapRegion(
    clipboard: MapRegionClipboard,
    at: Position,
    mapWidth: Int,
    mapHeight: Int,
    tiles: Map<String, TileType>,
    riverTiles: Map<String, RiverTile>,
    targetInfoMap: Map<String, EditorTargetInfo>,
    spawnPointInfoMap: Map<String, SpawnPointType>,
): ResizedMapData {
    val updatedTiles = tiles.toMutableMap()
    val updatedRiverTiles = riverTiles.toMutableMap()
    val updatedTargetInfoMap = targetInfoMap.toMutableMap()
    val updatedSpawnPointInfoMap = spawnPointInfoMap.toMutableMap()

    fun destinationPosition(relativeKey: String): Position? {
        val (x, y) = relativeKey.split(",").let { it[0].toInt() to it[1].toInt() }
        val destination = Position(at.x + x, at.y + y)
        return destination.takeIf { it.isInside(mapWidth, mapHeight) }
    }

    clipboard.tiles.forEach { (relativeKey, tileType) ->
        val destination = destinationPosition(relativeKey) ?: return@forEach
        val destinationKey = "${destination.x},${destination.y}"
        updatedTiles[destinationKey] = tileType
        if (tileType != TileType.RIVER) updatedRiverTiles.remove(destinationKey)
        if (tileType != TileType.TARGET) updatedTargetInfoMap.remove(destinationKey)
        if (tileType != TileType.SPAWN_POINT) updatedSpawnPointInfoMap.remove(destinationKey)
    }
    clipboard.riverTiles.forEach { (relativeKey, riverTile) ->
        val destination = destinationPosition(relativeKey) ?: return@forEach
        val destinationKey = "${destination.x},${destination.y}"
        updatedRiverTiles[destinationKey] = riverTile.copy(position = destination)
    }
    clipboard.targetInfoMap.forEach { (relativeKey, info) ->
        val destination = destinationPosition(relativeKey) ?: return@forEach
        updatedTargetInfoMap["${destination.x},${destination.y}"] = info
    }
    clipboard.spawnPointInfoMap.forEach { (relativeKey, type) ->
        val destination = destinationPosition(relativeKey) ?: return@forEach
        updatedSpawnPointInfoMap["${destination.x},${destination.y}"] = type
    }

    return ResizedMapData(
        width = mapWidth,
        height = mapHeight,
        tiles = updatedTiles,
        riverTiles = updatedRiverTiles,
        targetInfoMap = updatedTargetInfoMap,
        spawnPointInfoMap = updatedSpawnPointInfoMap,
    )
}

private fun Position.isInside(
    width: Int,
    height: Int,
): Boolean = x in 0 until width && y in 0 until height

private inline fun <T> Iterable<T>.anyIndexed(predicate: (Int, T) -> Boolean): Boolean {
    var index = 0
    for (item in this) {
        if (predicate(index, item)) return true
        index++
    }
    return false
}

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
    var showSaveAsDialog by remember { mutableStateOf(false) }
    var showSaveTemplateDialog by remember { mutableStateOf(false) }
    var showTileReplacementDialog by remember { mutableStateOf(false) }
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
    var showRiverPropertiesDialog by remember { mutableStateOf(false) }
    var communityUploadStatus by remember { mutableStateOf<String?>(null) }
    var isUploadingToCommunity by remember { mutableStateOf(false) }
    var showCommunityUploadConfirm by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var zoomLevel by remember { mutableStateOf(1.0f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var lastPaintedPos by remember { mutableStateOf<Position?>(null) }
    var isHeaderExpanded by remember { mutableStateOf(false) }
    var backgroundImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var mapOverlayAlpha by remember { mutableStateOf(0.7f) }
    var resizeLeft by remember { mutableStateOf("0") }
    var resizeRight by remember { mutableStateOf("0") }
    var resizeTop by remember { mutableStateOf("0") }
    var resizeBottom by remember { mutableStateOf("0") }
    var undoHistory by remember { mutableStateOf(listOf<MapEditorSnapshot>()) }
    var redoHistory by remember { mutableStateOf(listOf<MapEditorSnapshot>()) }
    var areaClipboard by remember { mutableStateOf<MapRegionClipboard?>(null) }
    var showAreaClipboardDialog by remember { mutableStateOf(false) }
    var showMapFlowOverlay by remember { mutableStateOf(false) }
    var showMapPathPreviewOverlay by remember { mutableStateOf(false) }
    var showCrosshair by remember { mutableStateOf(false) }
    var copyFromX by remember { mutableStateOf("0") }
    var copyFromY by remember { mutableStateOf("0") }
    var copyToX by remember { mutableStateOf((map.width - 1).coerceAtLeast(0).toString()) }
    var copyToY by remember { mutableStateOf((map.height - 1).coerceAtLeast(0).toString()) }
    var pasteAtX by remember { mutableStateOf("0") }
    var pasteAtY by remember { mutableStateOf("0") }
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
        remember(mapWidth, mapHeight, tiles, riverTiles, targetInfoMap, spawnPointInfoMap, mapToolingInfo, allowNoBuildableTiles, allowNoDirectPath) {
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
            )
        }
    val mapFlowSummary = remember(currentMap) { analyzeMapFlow(currentMap) }
    val pathPreviews = remember(currentMap) { currentMap.previewPaths() }
    val previewCells = remember(pathPreviews) { pathPreviews.flatMap { it.path }.toSet() }
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
    val canApplyResize = resizedWidthPreview > 0 && resizedHeightPreview > 0
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
        replacementToX = (snapshot.width - 1).coerceAtLeast(0).toString()
        replacementToY = (snapshot.height - 1).coerceAtLeast(0).toString()
        copyToX = replacementToX
        copyToY = replacementToY
    }

    fun rememberForUndo() {
        undoHistory = (undoHistory + currentSnapshot()).takeLast(40)
        redoHistory = emptyList()
    }

    // Calculate header height based on expanded/collapsed state
    val headerHeight = if (isHeaderExpanded) 430.dp else 72.dp

    // Brush paint callback - called when user drags in brush mode
    val onBrushPaint: (position: Position) -> Unit = { position ->

        if (lastPaintedPos == null || lastPaintedPos != position) {
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

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        // Map grid layer (below header)
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Spacer to account for header height (dynamic based on expanded/collapsed state)
            Spacer(modifier = Modifier.height(headerHeight))

            val hexSizePx = with(LocalDensity.current) { hexSize.toPx() }
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
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

                                    val tilePos = screenToHexGridPosition(adjustedPointerPos, offsetX, offsetY, zoomLevel, hexSizePx)
                                    if (tilePos != null) {
                                        onBrushPaint(tilePos)
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
                    val tileType = tiles[key] ?: TileType.NO_PLAY
                    val riverTile = riverTiles[key]
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
                                position in unreachableSpawns -> Color.Red
                                position in ambiguousPreviewCells -> Color(0xFFFFC107)
                                position in previewCells -> Color(0xFF00BCD4)
                                else -> Color.Black
                            },
                        borderWidth =
                            when {
                                position in previewCells || position in unreachableSpawns -> 2.5.dp
                                else -> 1.5.dp
                            },
                        onClick = {
                            if (selectedTileType == TileType.TARGET && tileType == TileType.TARGET) {
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
                                    text = "SPAWN",
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

                if (isHeaderExpanded) {
                    Column(
                        modifier =
                            Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OverlayToggleButton(
                            label = stringResource(Res.string.map_flow_validator),
                            isActive = showMapFlowOverlay,
                            onClick = { showMapFlowOverlay = !showMapFlowOverlay },
                        )
                        OverlayToggleButton(
                            label = stringResource(Res.string.map_path_preview),
                            isActive = showMapPathPreviewOverlay,
                            onClick = { showMapPathPreviewOverlay = !showMapPathPreviewOverlay },
                        )
                        OverlayToggleButton(
                            label = stringResource(Res.string.map_crosshair),
                            isActive = showCrosshair,
                            onClick = { showCrosshair = !showCrosshair },
                        )
                        Button(
                            onClick = {
                                val snapshot = undoHistory.lastOrNull() ?: return@Button
                                undoHistory = undoHistory.dropLast(1)
                                redoHistory = (redoHistory + currentSnapshot()).takeLast(40)
                                restoreSnapshot(snapshot)
                            },
                            enabled = undoHistory.isNotEmpty(),
                        ) {
                            Text(stringResource(Res.string.undo))
                        }
                        Button(
                            onClick = {
                                val snapshot = redoHistory.lastOrNull() ?: return@Button
                                redoHistory = redoHistory.dropLast(1)
                                undoHistory = (undoHistory + currentSnapshot()).takeLast(40)
                                restoreSnapshot(snapshot)
                            },
                            enabled = redoHistory.isNotEmpty(),
                        ) {
                            Text(stringResource(Res.string.redo))
                        }
                        Button(
                            onClick = { showAreaClipboardDialog = true },
                        ) {
                            Text(stringResource(Res.string.area_clipboard))
                        }
                    }
                }

                if (showMapFlowOverlay || showMapPathPreviewOverlay) {
                    Column(
                        modifier =
                            Modifier
                                .align(Alignment.TopStart)
                                .padding(
                                    start = if (isHeaderExpanded) 180.dp else 8.dp,
                                    top = if (isHeaderExpanded) 8.dp else 80.dp,
                                    end = 8.dp,
                                ).widthIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (showMapFlowOverlay) {
                            MapFlowValidatorCard(summary = mapFlowSummary)
                            if (mapUsageIssues.isNotEmpty()) {
                                MapUsageConsistencyCard(issues = mapUsageIssues)
                            }
                        }
                        if (showMapPathPreviewOverlay) {
                            MapPathPreviewCard(previews = pathPreviews)
                        }
                    }
                }

                /*
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.BottomEnd)
                ) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 8.dp, end = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        if (de.egril.defender.ui.settings.AppSettings.showControlPad.value) {
                            // Directional pad
                            de.egril.defender.ui.ControlPad(
                                onUp = {
                                    offsetY += 30f
                                },
                                onDown = {
                                    offsetY -= 30f
                                },
                                onLeft = {
                                    offsetX += 30f
                                },
                                onRight = {
                                    offsetX -= 30f
                                }
                            )

                            // Zoom controls
                            de.egril.defender.ui.ZoomControls(
                                onZoomIn = {
                                    zoomLevel = (zoomLevel + 0.1f).coerceIn(0.5f, 3.0f)
                                },
                                onZoomOut = {
                                    zoomLevel = (zoomLevel - 0.1f).coerceIn(0.5f, 3.0f)
                                }
                            )
                        }

                        // Minimap
                        HexagonMinimapFromEditorMap(
                            map = currentMap,
                            modifier = Modifier.size(150.dp),
                            config = MinimapConfig(
                                showViewport = true,
                                minimapSizeDp = 150f
                            ),
                            scale = zoomLevel,
                            offsetX = offsetX,
                            offsetY = offsetY,
                            containerSize = containerSize,
                            contentSize = actualContentSize
                        )
                    }
                }
                 */
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
                    remember(map.id, mapWidth, mapHeight, tiles.hashCode(), riverTiles.hashCode(), targetInfoMap.hashCode(), spawnPointInfoMap.hashCode(), mapToolingInfo, allowNoBuildableTiles, allowNoDirectPath) {
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

        // Header overlay (on top with elevated z-index)
        MapEditorHeader(
            map = currentMap,
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
            selectedTileType = selectedTileType,
            onTileTypeChange = { selectedTileType = it },
            selectedRiverFlow = selectedRiverFlow,
            onRiverFlowChange = { selectedRiverFlow = it },
            selectedRiverSpeed = selectedRiverSpeed,
            onRiverSpeedChange = { selectedRiverSpeed = it },
            zoomLevel = zoomLevel,
            onZoomIn = { zoomLevel = minOf(3.0f, zoomLevel + 0.1f) },
            onZoomOut = { zoomLevel = maxOf(0.5f, zoomLevel - 0.1f) },
            onChangeAllNoPlayToPath = {
                replacementSourceTileType = TileType.NO_PLAY
                replacementTargetTileType = TileType.PATH
                replacementLimitToArea = false
                replacementFromX = "0"
                replacementFromY = "0"
                replacementToX = (mapWidth - 1).coerceAtLeast(0).toString()
                replacementToY = (mapHeight - 1).coerceAtLeast(0).toString()
                showTileReplacementDialog = true
            },
            isExpanded = isHeaderExpanded,
            onToggleExpanded = { isHeaderExpanded = !isHeaderExpanded },
            selectedTargetName = selectedTargetName,
            onTargetNameChange = { selectedTargetName = it },
            selectedTargetType = selectedTargetType,
            onTargetTypeChange = { selectedTargetType = it },
            selectedSpawnPointType = selectedSpawnPointType,
            onSpawnPointTypeChange = { selectedSpawnPointType = it },
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
            mapOverlayAlpha = mapOverlayAlpha,
            onMapOverlayAlphaChange = { mapOverlayAlpha = it },
            showMapFlowOverlay = showMapFlowOverlay,
            onToggleMapFlowOverlay = { showMapFlowOverlay = !showMapFlowOverlay },
            showMapPathPreviewOverlay = showMapPathPreviewOverlay,
            onToggleMapPathPreviewOverlay = { showMapPathPreviewOverlay = !showMapPathPreviewOverlay },
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
            onOpenAreaClipboard = { showAreaClipboardDialog = true },
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

    if (showTileReplacementDialog) {
        val parsedFromX = replacementFromX.toIntOrNull()
        val parsedFromY = replacementFromY.toIntOrNull()
        val parsedToX = replacementToX.toIntOrNull()
        val parsedToY = replacementToY.toIntOrNull()
        val isAreaInputValid =
            !replacementLimitToArea ||
                (parsedFromX != null && parsedFromY != null && parsedToX != null && parsedToY != null)

        AlertDialog(
            onDismissRequest = { showTileReplacementDialog = false },
            title = { Text(stringResource(Res.string.replace_tiles)) },
            text = {
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
                        val flows = de.egril.defender.model.RiverFlow.entries
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            flows.chunked(4).forEach { rowFlows ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    rowFlows.forEach { flow ->
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
                                            modifier = Modifier.height(32.dp).weight(1f),
                                        ) {
                                            Text(flow.name.replace("_", " "), fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }

                        Text(stringResource(Res.string.flow_speed), style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(
                                onClick = { replacementRiverSpeed = 1 },
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor =
                                            if (replacementRiverSpeed == 1) {
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
                                onClick = { replacementRiverSpeed = 2 },
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor =
                                            if (replacementRiverSpeed == 2) {
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
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Checkbox(
                            checked = replacementLimitToArea,
                            onCheckedChange = { replacementLimitToArea = it },
                        )
                        Text(stringResource(Res.string.replace_tiles_limit_to_area))
                    }

                    if (replacementLimitToArea) {
                        Text(stringResource(Res.string.replace_tiles_area_from))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

                        Text(stringResource(Res.string.replace_tiles_area_to))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        rememberForUndo()
                        val area =
                            if (replacementLimitToArea) {
                                if (parsedFromX == null || parsedFromY == null || parsedToX == null || parsedToY == null) {
                                    return@Button
                                }
                                TileReplacementArea(
                                    from = Position(parsedFromX, parsedFromY),
                                    to = Position(parsedToX, parsedToY),
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
                    },
                    enabled = isAreaInputValid,
                ) {
                    Text(stringResource(Res.string.apply))
                }
            },
            dismissButton = {
                Button(onClick = { showTileReplacementDialog = false }) {
                    Text(stringResource(Res.string.cancel))
                }
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

    if (showAreaClipboardDialog) {
        val parsedCopyFromX = copyFromX.toIntOrNull()
        val parsedCopyFromY = copyFromY.toIntOrNull()
        val parsedCopyToX = copyToX.toIntOrNull()
        val parsedCopyToY = copyToY.toIntOrNull()
        val parsedPasteAtX = pasteAtX.toIntOrNull()
        val parsedPasteAtY = pasteAtY.toIntOrNull()
        AlertDialog(
            onDismissRequest = { showAreaClipboardDialog = false },
            title = { Text(stringResource(Res.string.area_clipboard)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(Res.string.copy_region))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(copyFromX, { copyFromX = it }, label = { Text(stringResource(Res.string.x_coordinate)) }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(copyFromY, { copyFromY = it }, label = { Text(stringResource(Res.string.y_coordinate)) }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(copyToX, { copyToX = it }, label = { Text(stringResource(Res.string.to_x)) }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(copyToY, { copyToY = it }, label = { Text(stringResource(Res.string.to_y)) }, modifier = Modifier.weight(1f), singleLine = true)
                    }
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(pasteAtX, { pasteAtX = it }, label = { Text(stringResource(Res.string.x_coordinate)) }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(pasteAtY, { pasteAtY = it }, label = { Text(stringResource(Res.string.y_coordinate)) }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = areaClipboard ?: return@Button
                        if (parsedPasteAtX == null || parsedPasteAtY == null) return@Button
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
                    },
                    enabled = areaClipboard != null && parsedPasteAtX != null && parsedPasteAtY != null,
                ) {
                    Text(stringResource(Res.string.paste_region))
                }
            },
            dismissButton = {
                Button(onClick = { showAreaClipboardDialog = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun MapFlowValidatorCard(summary: MapFlowSummary) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(Res.string.map_flow_validator),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = if (summary.isReady) stringResource(Res.string.ready_to_use) else stringResource(Res.string.not_ready),
                color = if (summary.isReady) Color(0xFF2E7D32) else Color.Red,
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                MapFlowMetric(stringResource(Res.string.lane_shape), summary.laneShape.localizedLabel())
                MapFlowMetric(stringResource(Res.string.build_coverage), summary.buildCoverage.localizedLabel())
                MapFlowMetric(stringResource(Res.string.travel_length), summary.travelLength.localizedLabel())
            }
            Text(
                text = "${stringResource(Res.string.dead_corridor)}: ${summary.longestDeadCorridor}",
                style = MaterialTheme.typography.bodySmall,
                color = if (summary.longestDeadCorridor >= 8) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${stringResource(Res.string.connectivity)}: ${summary.spawnCount}/${summary.targetCount}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun MapPathPreviewCard(previews: List<MapPathPreview>) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(Res.string.map_path_preview),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text =
                    stringResource(
                        Res.string.path_preview_summary,
                        previews.count { it.isReachable },
                        previews.count { !it.isReachable },
                        previews.count { it.isAmbiguous },
                    ),
                style = MaterialTheme.typography.bodySmall,
            )
            previews.forEach { preview ->
                Text(
                    text =
                        buildString {
                            append("(${preview.spawn.x}, ${preview.spawn.y})")
                            append(" -> ")
                            append(
                                preview.target?.let { "(${it.x}, ${it.y})" }
                                    ?: stringResource(Res.string.unreachable),
                            )
                            append(" (${preview.path.size.coerceAtLeast(0)})")
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        when {
                            !preview.isReachable -> Color.Red
                            preview.isAmbiguous -> Color(0xFFFFC107)
                            else -> Color(0xFF00838F)
                        },
                )
            }
        }
    }
}

@Composable
private fun MapUsageConsistencyCard(issues: List<Pair<de.egril.defender.editor.EditorLevel, de.egril.defender.ui.editor.level.LevelConsistencySummary>>) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(Res.string.used_level_checks),
                style = MaterialTheme.typography.titleSmall,
            )
            issues.forEach { (level, summary) ->
                Text(
                    text = "${level.title.ifBlank { level.id }}: ${summary.issueCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun MapFlowMetric(
    label: String,
    value: String,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * Draws a red hairline crosshair centered on the map's tile content (not the screen/canvas),
 * so it stays anchored to the exact center of the map regardless of zoom or pan.
 *
 * This is rendered via the [HexagonalMapView] `overlayContent` slot, which applies the same
 * scale/translation transform as the tile grid itself.
 */
@Composable
private fun MapCrosshairOverlay(contentSize: IntSize) {
    if (contentSize.width == 0 || contentSize.height == 0) return
    val density = LocalDensity.current
    val contentWidthDp = with(density) { contentSize.width.toDp() }
    val contentHeightDp = with(density) { contentSize.height.toDp() }
    androidx.compose.foundation.Canvas(
        modifier = Modifier.requiredSize(contentWidthDp, contentHeightDp),
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        drawLine(
            color = Color.Red,
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = 2f,
        )
        drawLine(
            color = Color.Red,
            start = Offset(centerX, 0f),
            end = Offset(centerX, size.height),
            strokeWidth = 2f,
        )
    }
}

@Composable
private fun OverlayToggleButton(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
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
        Text(label)
    }
}

@Composable
private fun MapLaneShape.localizedLabel(): String =
    when (this) {
        MapLaneShape.STRAIGHT -> stringResource(Res.string.lane_shape_straight)
        MapLaneShape.BRANCHING -> stringResource(Res.string.lane_shape_branching)
    }

@Composable
private fun DensityBand.localizedLabel(): String =
    when (this) {
        DensityBand.SPARSE -> stringResource(Res.string.density_sparse)
        DensityBand.GOOD -> stringResource(Res.string.rating_good)
        DensityBand.DENSE -> stringResource(Res.string.density_dense)
    }

@Composable
private fun TravelBand.localizedLabel(): String =
    when (this) {
        TravelBand.SHORT -> stringResource(Res.string.travel_short)
        TravelBand.GOOD -> stringResource(Res.string.rating_good)
        TravelBand.LONG -> stringResource(Res.string.travel_long)
    }
