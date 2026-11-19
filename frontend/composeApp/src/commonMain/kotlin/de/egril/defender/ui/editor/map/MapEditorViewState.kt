package de.egril.defender.ui.editor.map

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.IntSize
import de.egril.defender.editor.*
import de.egril.defender.model.Position
import de.egril.defender.model.SpawnPointType
import de.egril.defender.model.TargetType

/**
 * Editable state of [MapEditorView]. The view creates it once with `remember` (no keys), so the
 * initial values come from the map the view was first opened with.
 */
internal class MapEditorViewState(val map: EditorMap) {
    var mapWidth by mutableStateOf(map.width)
    var mapHeight by mutableStateOf(map.height)
    var tiles by mutableStateOf(map.tiles.toMutableMap())
    var riverTiles by mutableStateOf(map.riverTiles.toMutableMap())
    var targetInfoMap by mutableStateOf(map.targetInfoMap.toMutableMap())
    var spawnPointInfoMap by mutableStateOf(map.spawnPointInfoMap.toMutableMap())
    var selectedTileType by mutableStateOf(TileType.PATH)
    var selectedRiverFlow by mutableStateOf(de.egril.defender.model.RiverFlow.EAST)
    var selectedRiverSpeed by mutableStateOf(1)
    var selectedTargetName by mutableStateOf("")
    var selectedTargetType by mutableStateOf(TargetType.STANDARD)
    var selectedSpawnPointType by mutableStateOf(SpawnPointType.LAND)
    var editTargetKey by mutableStateOf<String?>(null) // Key of a tile being edited in the inline dialog
    var mapName by mutableStateOf(map.name)
    var mapAuthor by mutableStateOf(map.author)
    var mapToolingInfo by mutableStateOf(map.mapToolingInfo)
    var allowNoBuildableTiles by mutableStateOf(map.allowNoBuildableTiles)
    var allowNoDirectPath by mutableStateOf(map.allowNoDirectPath)
    var tileZones by mutableStateOf(map.tileZones)
    var showTileZonePanel by mutableStateOf(false)
    var showOtherZones by mutableStateOf(false)
    var selectedZoneId by mutableStateOf<String?>(null)
    var isZoneDrawingMode by mutableStateOf(false)
    var zoneBrush by mutableStateOf(ZoneBrush.PAINT_TYPE)
    var showSaveAsDialog by mutableStateOf(false)
    var showSaveTemplateDialog by mutableStateOf(false)
    var showTileReplacementDialog by mutableStateOf(false)
    var showFillRiverRingDialog by mutableStateOf(false)
    var fillRiverRingError by mutableStateOf(false)
    var ringStartX by mutableStateOf("0")
    var ringStartY by mutableStateOf("0")
    var ringClockwise by mutableStateOf(true)
    var ringInnerToOuter by mutableStateOf(true)
    var templateName by mutableStateOf(map.name)
    var replacementSourceTileType by mutableStateOf(TileType.NO_PLAY)
    var replacementTargetTileType by mutableStateOf(TileType.PATH)
    var replacementLimitToArea by mutableStateOf(false)
    var replacementFromX by mutableStateOf("0")
    var replacementFromY by mutableStateOf("0")
    var replacementToX by mutableStateOf((map.width - 1).coerceAtLeast(0).toString())
    var replacementToY by mutableStateOf((map.height - 1).coerceAtLeast(0).toString())
    var sourceTileDropdownExpanded by mutableStateOf(false)
    var targetTileDropdownExpanded by mutableStateOf(false)
    var replacementRiverFlow by mutableStateOf(de.egril.defender.model.RiverFlow.EAST)
    var replacementRiverSpeed by mutableStateOf(1)
    var communityUploadStatus by mutableStateOf<String?>(null)
    var isUploadingToCommunity by mutableStateOf(false)
    var showCommunityUploadConfirm by mutableStateOf(false)
    var zoomLevel by mutableStateOf(1.0f)
    var offsetX by mutableStateOf(0f)
    var offsetY by mutableStateOf(0f)
    var lastPaintedPos by mutableStateOf<Position?>(null)
    var isHeaderExpanded by mutableStateOf(false)
    var isSidePanelExpanded by mutableStateOf(true)
    var backgroundImageBytes by mutableStateOf<ByteArray?>(null)
    var mapPreviewBytes by mutableStateOf<ByteArray?>(null)
    var mapPreviewPainter by mutableStateOf<BitmapPainter?>(null)
    var mapPreviewRegenerating by mutableStateOf(false)
    var mapPreviewError by mutableStateOf<String?>(null)
    var showMapImageGenerationDialog by mutableStateOf(false)
    var mapImageGenerationRunning by mutableStateOf(false)
    var mapImageGenerationSuccess by mutableStateOf<Boolean?>(null)
    var mapImageGenerationError by mutableStateOf<String?>(null)
    var mapImageProgress by mutableStateOf<List<ImageGenerationProgress>>(emptyList())
    var mapImagePreviews by mutableStateOf<List<MapImagePreview>>(emptyList())
    var mapImagePreviewIndex by mutableStateOf(0)
    var mapOverlayAlpha by mutableStateOf(0.7f)
    var resizeLeft by mutableStateOf("0")
    var resizeRight by mutableStateOf("0")
    var resizeTop by mutableStateOf("0")
    var resizeBottom by mutableStateOf("0")
    var undoHistory by mutableStateOf(listOf<MapEditorSnapshot>())
    var redoHistory by mutableStateOf(listOf<MapEditorSnapshot>())
    var areaClipboard by mutableStateOf<MapRegionClipboard?>(null)
    var showAreaClipboardDialog by mutableStateOf(false)
    var showResizeSection by mutableStateOf(false)
    var showMapFlowOverlay by mutableStateOf(false)
    var showMapPathPreviewOverlay by mutableStateOf(false)
    var showCrosshair by mutableStateOf(false)
    var copyFromX by mutableStateOf("0")
    var copyFromY by mutableStateOf("0")
    var copyToX by mutableStateOf((map.width - 1).coerceAtLeast(0).toString())
    var copyToY by mutableStateOf((map.height - 1).coerceAtLeast(0).toString())
    var pasteAtX by mutableStateOf("0")
    var pasteAtY by mutableStateOf("0")
    var pickTarget by mutableStateOf<PositionPickTarget?>(null)

    // Track container and content sizes for constraint calculation
    var containerSize by mutableStateOf(IntSize.Zero)
    var actualContentSize by mutableStateOf(IntSize.Zero)

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
}
