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
import androidx.compose.ui.unit.IntSize
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.TileType
import de.egril.defender.model.Position
import de.egril.defender.ui.BaseGridCell
import de.egril.defender.ui.HexagonMinimapFromEditorMap
import de.egril.defender.ui.HexagonalMapConfig
import de.egril.defender.ui.HexagonalMapView
import de.egril.defender.ui.MinimapConfig
import de.egril.defender.ui.icon.PushpinIcon
import de.egril.defender.ui.editor.SaveAsDialog
import de.egril.defender.ui.editor.getTileColor
import de.egril.defender.utils.screenToHexGridPosition
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*

/**
 * View for editing a map
 */
@Composable
fun MapEditorView(
    map: EditorMap,
    onSave: (EditorMap) -> Unit,
    onCancel: () -> Unit
) {
    var tiles by remember { mutableStateOf(map.tiles.toMutableMap()) }
    var selectedTileType by remember { mutableStateOf(TileType.PATH) }
    var mapName by remember { mutableStateOf(map.name) }
    var showSaveAsDialog by remember { mutableStateOf(false) }
    var zoomLevel by remember { mutableStateOf(1.0f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var lastPaintedPos by remember { mutableStateOf<Position?>(null) }
    
    // Create updated map for minimap that reflects current tiles state
    val currentMap = remember(tiles) {
        map.copy(tiles = tiles.toMap())
    }
    
    // Hexagon dimensions - using same constants as game (40.dp)
    val hexSize = 40.dp

    // Brush paint callback - called when user drags in brush mode
    val onBrushPaint: (position: Position) -> Unit = { position ->

        if (lastPaintedPos == null || lastPaintedPos != position) {
            println("Brush paint at content coords: $position")

            val key = "${position.x},${position.y}"
            tiles = tiles.toMutableMap().apply {
                this[key] = selectedTileType
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
            // Spacer to account for header height
            Spacer(modifier = Modifier.height(280.dp))

            val hexSizePx = with(LocalDensity.current) { hexSize.toPx() }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                // Track container size for minimap viewport
                var containerSize by remember { mutableStateOf(IntSize.Zero) }
                var actualContentSize by remember { mutableStateOf(IntSize.Zero) }
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
                    BaseGridCell(
                        hexSize = hexSize,
                        backgroundColor = getTileColor(tileType),
                        borderColor = Color.Black,
                        borderWidth = 1.5.dp,
                        onClick = {
                            tiles = tiles.toMutableMap().apply {
                                this[key] = selectedTileType
                            }
                        },
                    ) {
                        Text(
                            text = "${position.x},${position.y}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                        if (tileType == TileType.WAYPOINT) {
                            PushpinIcon(size = 20.dp)
                        }
                    }
                }

                MapControls(
                    mapControlState = MapControlState(
                        zoomLevel = zoomLevel,
                        offsetX = offsetX,
                        offsetY = offsetY
                    )
                ) {
                    // Update zoom and offsets from controls
                    zoomLevel = it.zoomLevel
                    offsetX = it.offsetX
                    offsetY = it.offsetY

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
                        val updatedMap = map.copy(
                            name = mapName,
                            tiles = tiles.toMap()
                        )
                        // Validate and set readyToUse flag
                        val validatedMap = updatedMap.copy(readyToUse = updatedMap.validateReadyToUse())
                        onSave(validatedMap)
                    },
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
            selectedTileType = selectedTileType,
            onTileTypeChange = { selectedTileType = it },
            zoomLevel = zoomLevel,
            onZoomIn = { zoomLevel = minOf(3.0f, zoomLevel + 0.1f) },
            onZoomOut = { zoomLevel = maxOf(0.5f, zoomLevel - 0.1f) }
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
                val sanitizedName = newName.trim().replace(" ", "_").replace(Regex("[^a-zA-Z0-9_]"), "")
                val newId = if (sanitizedName.isNotEmpty()) {
                    "map_$sanitizedName"
                } else {
                    "map_copy_${kotlin.random.Random.nextInt(10000, 99999)}"
                }
                val newMap = map.copy(
                    id = newId,
                    name = newName,
                    tiles = tiles.toMap()
                )
                // Validate and set readyToUse flag
                val validatedMap = newMap.copy(readyToUse = newMap.validateReadyToUse())
                onSave(validatedMap)
                showSaveAsDialog = false
            }
        )
    }
}
