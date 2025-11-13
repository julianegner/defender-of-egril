package com.defenderofegril.ui.editor.map

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
import com.defenderofegril.editor.EditorMap
import com.defenderofegril.editor.TileType
import com.defenderofegril.model.Position
import com.defenderofegril.ui.BaseGridCell
import com.defenderofegril.ui.HexagonMinimapFromEditorMap
import com.defenderofegril.ui.HexagonalMapConfig
import com.defenderofegril.ui.HexagonalMapView
import com.defenderofegril.ui.MinimapConfig
import com.defenderofegril.ui.icon.PushpinIcon
import com.defenderofegril.ui.editor.SaveAsDialog
import com.defenderofegril.ui.editor.getTileColor
import com.defenderofegril.utils.screenToHexGridPosition
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
                        keyboardPanSpeed = 50f  // Increased for better responsiveness
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
                        .pointerInput(containerSize, actualContentSize) {
                            detectDragGestures { change, _ ->
                                val pointerPos = change.position

                                // No centering adjustment needed - offsetY handles all positioning
                                val tilePos = screenToHexGridPosition(pointerPos, offsetX, offsetY, zoomLevel, hexSizePx)
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
                        /*
                        modifier = Modifier
                                .pointerInput(offsetX, offsetY,) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        onBrushPaint(position)
                                    },
                                    onDrag = { change, offset ->
                                        println("Drag at $position")
                                        onBrushPaint(position)
                                    },
                                    onDragEnd = {
                                        lastPaintedPos = null
                                    }
                                )
                        }
                         */
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

                HexagonMinimapFromEditorMap(
                    map = map,
                    modifier = Modifier.size(150.dp).align(Alignment.BottomEnd),
                    config = MinimapConfig(
                        showViewport = true,
                        minimapSizeDp = 150f
                    ),
                    scale = zoomLevel,
                    offsetX = offsetX,
                    offsetY = offsetY,
                    containerSize = containerSize
                )
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
