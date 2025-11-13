package com.defenderofegril.ui.editor.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import com.defenderofegril.editor.EditorMap
import com.defenderofegril.editor.TileType
import com.defenderofegril.model.Position
import com.defenderofegril.ui.HexagonMinimapFromEditorMap
import com.defenderofegril.ui.HexagonShape
import com.defenderofegril.ui.HexagonalMapConfig
import com.defenderofegril.ui.HexagonalMapView
import com.defenderofegril.ui.MinimapConfig
import com.defenderofegril.ui.icon.PushpinIcon
import com.defenderofegril.ui.editor.SaveAsDialog
import com.defenderofegril.ui.editor.getTileColor
import com.defenderofegril.ui.gameplay.BaseGridCell
import com.defenderofegril.ui.gameplay.GridCell
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*
import kotlin.math.sqrt

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
    
    // Hexagon dimensions - using same constants as game
    val hexSize = 32f  // Radius of hexagon (not scaled here, scaling handled by HexagonalMapView)
    val sqrt3 = sqrt(3.0).toFloat()
    val hexWidth = hexSize * sqrt3  // Width of hexagon (flat-to-flat)
    val hexHeight = hexSize * 2f    // Height of hexagon (point-to-point)
    
    // Track tile positions for brush painting
    val tilePositions = remember { mutableStateMapOf<String, Offset>() }
    
    // Get density for coordinate conversions
    val density = androidx.compose.ui.platform.LocalDensity.current
    val hexRadiusPx = with(density) { (hexWidth / 2f).dp.toPx() }

    // Brush paint callback - called when user drags in brush mode
    val onBrushPaint: (Float, Float) -> Unit = { contentX, contentY ->
        // Find the closest tile to the content position
        val closest = tilePositions.entries.minByOrNull { (_, tilePos) ->
            val dx = contentX - tilePos.x
            val dy = contentY - tilePos.y
            dx * dx + dy * dy
        }
        
        closest?.let { (key, tilePos) ->
            val dx = contentX - tilePos.x
            val dy = contentY - tilePos.y
            val distance = sqrt(dx * dx + dy * dy)
            // Check if within hex radius (with some tolerance)
            if (distance < hexRadiusPx * 1.5f) {
                tiles = tiles.toMutableMap().apply {
                    this[key] = selectedTileType
                }
            }
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
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                // Track container size for minimap viewport
                var containerSize by remember { mutableStateOf(IntSize.Zero) }
                // Track actual content size for minimap viewport calculations
                var actualContentSize by remember { mutableStateOf(IntSize.Zero) }
/******************************************************************************************/
                HexagonalMapView(
                    gridWidth = map.width,
                    gridHeight = map.height,
                    config = HexagonalMapConfig(
                        hexSize = hexSize,
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
                    modifier = Modifier.fillMaxSize().onSizeChanged { containerSize = it }
                ) { hexWidthParam, hexHeightParam, verticalSpacing, onTilePositioned ->
                    for (y in 0 until map.height) {
                        Row(
                            modifier = Modifier.offset(
                                x = if (y % 2 == 1) (hexWidthParam * 0.42f).dp else 0.dp,
                                y = (-(y-1)).dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy((-10).dp)
                        ) {
                            for (x in 0 until map.width) {
/******************************************************************************************/
                                val key = "$x,$y"
                                val tileType = tiles[key] ?: TileType.NO_PLAY


                                val position = Position(x, y)
                                // BaseGridCell
                                Box(
                                    modifier = Modifier
                                        .width((hexWidthParam).dp)
                                        .height((hexHeightParam).dp)
                                        .clip(HexagonShape())
                                        .background(getTileColor(tileType))
                                        .border(1.5.dp, Color.Black, HexagonShape())
                                        .onGloballyPositioned { coordinates ->
                                            // Track tile center position for brush painting
                                            // Report position in content coordinates (before transformations)
                                            val bounds = coordinates.size
                                            val position = coordinates.positionInWindow()
                                            val centerX = position.x + bounds.width / 2f
                                            val centerY = position.y + bounds.height / 2f
                                            onTilePositioned(key, Offset(centerX, centerY))
                                            tilePositions[key] = Offset(centerX, centerY)
                                        }
                                        .clickable {
                                            tiles = tiles.toMutableMap().apply {
                                                this[key] = selectedTileType
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (tileType == TileType.WAYPOINT) {
                                        PushpinIcon(size = 20.dp)
                                    }
                                }
                            }
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
