package de.egril.defender.ui.editor.map

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntSize
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.EditorTargetInfo
import de.egril.defender.editor.TileType
import de.egril.defender.model.Position
import de.egril.defender.model.RiverTile
import de.egril.defender.model.TargetType
import de.egril.defender.ui.hexagon.BaseGridCell
import de.egril.defender.ui.hexagon.HexagonMinimapFromEditorMap
import de.egril.defender.ui.hexagon.HexagonalMapConfig
import de.egril.defender.ui.hexagon.HexagonalMapView
import de.egril.defender.ui.hexagon.MinimapConfig
import de.egril.defender.ui.constrainMapOffsets
import de.egril.defender.ui.editor.ConfirmationDialog
import de.egril.defender.ui.editor.SaveAsDialog
import de.egril.defender.ui.editor.getTileColor
import de.egril.defender.ui.editor.RiverFlowIndicator
import de.egril.defender.utils.screenToHexGridPosition
import com.hyperether.resources.stringResource
import de.egril.defender.ui.icon.InfoIcon
import defender_of_egril.composeapp.generated.resources.*

/**
 * Converts a human-readable map name into a stable map ID component, e.g. "My Map" → "my_map".
 */
private fun nameToMapId(name: String): String {
    val sanitized = name.trim().lowercase()
        .replace(" ", "_")
        .replace(Regex("[^a-z0-9_]"), "")
        .replace(Regex("_+"), "_")
    return if (sanitized.isNotEmpty()) "map_$sanitized" else ""
}

/**
 * View for editing a map
 */
@Composable
fun MapEditorView(
    map: EditorMap,
    onSave: (EditorMap, String?) -> Unit,
    onCancel: () -> Unit
) {
    var tiles by remember { mutableStateOf(map.tiles.toMutableMap()) }
    var riverTiles by remember { mutableStateOf(map.riverTiles.toMutableMap()) }
    var targetInfoMap by remember { mutableStateOf(map.targetInfoMap.toMutableMap()) }
    var selectedTileType by remember { mutableStateOf(TileType.PATH) }
    var selectedRiverFlow by remember { mutableStateOf(de.egril.defender.model.RiverFlow.EAST) }
    var selectedRiverSpeed by remember { mutableStateOf(1) }
    var selectedTargetName by remember { mutableStateOf("") }
    var selectedTargetType by remember { mutableStateOf(TargetType.STANDARD) }
    var editTargetKey by remember { mutableStateOf<String?>(null) }  // Key of a tile being edited in the inline dialog
    var mapName by remember { mutableStateOf(map.name) }
    var mapAuthor by remember { mutableStateOf(map.author) }
    var showSaveAsDialog by remember { mutableStateOf(false) }
    var showChangeAllDialog by remember { mutableStateOf(false) }
    var showRiverPropertiesDialog by remember { mutableStateOf(false) }
    var zoomLevel by remember { mutableStateOf(1.0f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var lastPaintedPos by remember { mutableStateOf<Position?>(null) }
    var isHeaderExpanded by remember { mutableStateOf(true) }
    
    // Track container and content sizes for constraint calculation
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var actualContentSize by remember { mutableStateOf(IntSize.Zero) }
    
    // Create updated map for minimap that reflects current tiles state
    val currentMap = remember(tiles, riverTiles, targetInfoMap) {
        map.copy(tiles = tiles.toMap(), riverTiles = riverTiles.toMap(), targetInfoMap = targetInfoMap.toMap())
    }
    
    // Hexagon dimensions - using same constants as game (40.dp)
    val hexSize = 40.dp
    
    // Calculate header height based on expanded/collapsed state
    val headerHeight = if (isHeaderExpanded) 280.dp else 56.dp

    // Brush paint callback - called when user drags in brush mode
    val onBrushPaint: (position: Position) -> Unit = { position ->

        if (lastPaintedPos == null || lastPaintedPos != position) {
            println("Brush paint at content coords: $position")

            val key = "${position.x},${position.y}"
            tiles = tiles.toMutableMap().apply {
                this[key] = selectedTileType
            }
            
            // If painting a river tile, add river data
            if (selectedTileType == TileType.RIVER) {
                riverTiles = riverTiles.toMutableMap().apply {
                    this[key] = de.egril.defender.model.RiverTile(
                        position = position,
                        flowDirection = selectedRiverFlow,
                        flowSpeed = selectedRiverSpeed
                    )
                }
            } else {
                // Remove river data if not a river tile
                riverTiles = riverTiles.toMutableMap().apply {
                    remove(key)
                }
            }

            // Update target info map
            if (selectedTileType == TileType.TARGET) {
                targetInfoMap = targetInfoMap.toMutableMap().apply {
                    this[key] = EditorTargetInfo(name = selectedTargetName, type = selectedTargetType)
                }
            } else {
                targetInfoMap = targetInfoMap.toMutableMap().apply {
                    remove(key)
                }
            }
            
            lastPaintedPos = position
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Map grid layer (below header)
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Spacer to account for header height (dynamic based on expanded/collapsed state)
            Spacer(modifier = Modifier.height(headerHeight))

            val hexSizePx = with(LocalDensity.current) { hexSize.toPx() }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                HexagonalMapView(
                    gridWidth = map.width,
                    gridHeight = map.height,
                    config = HexagonalMapConfig(
                        hexSize = hexSize.value,
                        enableKeyboardNavigation = true,  // Enable keyboard navigation for editor
                        enablePanNavigation = false,  // Disable pan navigation (use brush mode instead)
                        enableBrushMode = true,  // Enable brush mode for tile painting
                        keyboardPanSpeed = 50f,  // Increased for better responsiveness
                        enableZoomMode = true  // Zoom now works with brush painting
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
                    modifier = Modifier
                        .fillMaxSize()
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
                        }
                ) { position ->
                    val key = "${position.x},${position.y}"
                    val tileType = tiles[key] ?: TileType.NO_PLAY
                    val riverTile = riverTiles[key]
                    BaseGridCell(
                        hexSize = hexSize,
                        backgroundColor = getTileColor(tileType),
                        borderColor = Color.Black,
                        borderWidth = 1.5.dp,
                        onClick = {
                            if (selectedTileType == TileType.TARGET && tileType == TileType.TARGET) {
                                // Clicking an already-TARGET tile while in TARGET mode opens edit dialog
                                editTargetKey = key
                            } else {
                                tiles = tiles.toMutableMap().apply {
                                    this[key] = selectedTileType
                                }
                                
                                // If painting a river tile, add river data
                                if (selectedTileType == TileType.RIVER) {
                                    riverTiles = riverTiles.toMutableMap().apply {
                                        this[key] = RiverTile(
                                            position = position,
                                            flowDirection = selectedRiverFlow,
                                            flowSpeed = selectedRiverSpeed
                                        )
                                    }
                                } else {
                                    // Remove river data if not a river tile
                                    riverTiles = riverTiles.toMutableMap().apply {
                                        remove(key)
                                    }
                                }

                                // Update target info map
                                if (selectedTileType == TileType.TARGET) {
                                    targetInfoMap = targetInfoMap.toMutableMap().apply {
                                        this[key] = EditorTargetInfo(name = selectedTargetName, type = selectedTargetType)
                                    }
                                } else {
                                    targetInfoMap = targetInfoMap.toMutableMap().apply {
                                        remove(key)
                                    }
                                }
                            }
                        },
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "${position.x},${position.y}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                            
                            // Show target name if this is a target tile
                            val targetInfo = targetInfoMap[key]
                            if (tileType == TileType.TARGET && targetInfo != null && targetInfo.name.isNotBlank()) {
                                Text(
                                    text = targetInfo.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Yellow
                                )
                            }

                            // Show river flow indicator if this is a river tile
                            if (tileType == TileType.RIVER && riverTile != null) {
                                RiverFlowIndicator(
                                    flowDirection = riverTile.flowDirection,
                                    flowSpeed = riverTile.flowSpeed,
                                    size = 20.dp
                                )
                            }
                        }
                    }
                }

                MapControls(
                    mapControlState = MapControlState(
                        zoomLevel = zoomLevel,
                        offsetX = offsetX,
                        offsetY = offsetY
                    ),
                    onStateChange = { newState ->
                        val newScale = newState.zoomLevel
                        val (constrainedX, constrainedY) = constrainMapOffsets(
                            newState.offsetX, 
                            newState.offsetY, 
                            newScale,
                            containerSize,
                            actualContentSize
                        )
                        zoomLevel = newScale
                        offsetX = constrainedX
                        offsetY = constrainedY
                    }
                ) {
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
                        contentSize = actualContentSize,
                        onViewportDrag = { newOffsetX, newOffsetY ->
                            offsetX = newOffsetX
                            offsetY = newOffsetY
                        }
                    )
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
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        // For user maps: if the name changed, derive a new ID from the new name
                        // so that the JSON and PNG filenames match the new name.
                        val (newId, oldId) = if (!map.isOfficial && mapName.trim() != map.name.trim()) {
                            val derived = nameToMapId(mapName)
                            if (derived.isNotEmpty() && derived != map.id) Pair(derived, map.id) else Pair(map.id, null)
                        } else {
                            Pair(map.id, null)
                        }
                        val updatedMap = map.copy(
                            id = newId,
                            name = mapName,
                            author = mapAuthor,
                            tiles = tiles.toMap(),
                            riverTiles = riverTiles.toMap(),
                            targetInfoMap = targetInfoMap.toMap()
                        )
                        // Validate and set readyToUse flag
                        val validatedMap = updatedMap.copy(readyToUse = updatedMap.validateReadyToUse())
                        onSave(validatedMap, oldId)
                    },
                    enabled = !map.isOfficial || de.egril.defender.OfficialEditMode.enabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(Res.string.save_map))
                }
                
                Button(
                    onClick = { showSaveAsDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(Res.string.save_as_new))
                }
                
                Button(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        }

        // Header overlay (on top with elevated z-index)
        MapEditorHeader(
            map = map,
            mapName = mapName,
            onMapNameChange = { mapName = it },
            mapAuthor = mapAuthor,
            onMapAuthorChange = { mapAuthor = it },
            selectedTileType = selectedTileType,
            onTileTypeChange = { selectedTileType = it },
            selectedRiverFlow = selectedRiverFlow,
            onRiverFlowChange = { selectedRiverFlow = it },
            selectedRiverSpeed = selectedRiverSpeed,
            onRiverSpeedChange = { selectedRiverSpeed = it },
            zoomLevel = zoomLevel,
            onZoomIn = { zoomLevel = minOf(3.0f, zoomLevel + 0.1f) },
            onZoomOut = { zoomLevel = maxOf(0.5f, zoomLevel - 0.1f) },
            onChangeAllNoPlayToPath = { showChangeAllDialog = true },
            isExpanded = isHeaderExpanded,
            onToggleExpanded = { isHeaderExpanded = !isHeaderExpanded },
            selectedTargetName = selectedTargetName,
            onTargetNameChange = { selectedTargetName = it },
            selectedTargetType = selectedTargetType,
            onTargetTypeChange = { selectedTargetType = it }
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
                val newId = nameToMapId(newName).ifEmpty {
                    "map_copy_${kotlin.random.Random.nextInt(10000, 99999)}"
                }
                val newMap = map.copy(
                    id = newId,
                    name = newName,
                    author = mapAuthor,
                    tiles = tiles.toMap(),
                    riverTiles = riverTiles.toMap(),
                    targetInfoMap = targetInfoMap.toMap(),
                    isOfficial = false  // Save as new always creates a user map
                )
                // Validate and set readyToUse flag
                val validatedMap = newMap.copy(readyToUse = newMap.validateReadyToUse())
                onSave(validatedMap, null)  // null oldId: this is a brand-new map, not a rename
                showSaveAsDialog = false
            }
        )
    }
    
    if (showChangeAllDialog) {
        ConfirmationDialog(
            title = stringResource(Res.string.change_all_no_play_confirm_title),
            message = stringResource(Res.string.change_all_no_play_confirm_message),
            onDismiss = { showChangeAllDialog = false },
            onConfirm = {
                // Replace all NO_PLAY tiles with PATH tiles
                tiles = tiles.toMutableMap().apply {
                    // Iterate through all positions in the map
                    for (x in 0 until map.width) {
                        for (y in 0 until map.height) {
                            val key = "$x,$y"
                            if (this[key] == TileType.NO_PLAY || this[key] == null) {
                                this[key] = TileType.PATH
                            }
                        }
                    }
                }
                showChangeAllDialog = false
            }
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
                Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text(stringResource(Res.string.target_name_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Text(stringResource(Res.string.target_type_label), style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)) {
                        TargetType.entries.forEach { type ->
                            val label = when (type) {
                                TargetType.STANDARD -> stringResource(Res.string.target_type_standard)
                                TargetType.SINGLE_HIT -> stringResource(Res.string.target_type_single_hit)
                            }
                            Button(
                                onClick = { editType = type },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (editType == type)
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
                    targetInfoMap = targetInfoMap.toMutableMap().apply {
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
            }
        )
    }
}
