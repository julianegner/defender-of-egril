package com.defenderofegril.ui.editor.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import com.defenderofegril.editor.EditorMap
import com.defenderofegril.editor.TileType
import com.defenderofegril.ui.HexagonShape
import com.defenderofegril.ui.HexagonalMapConfig
import com.defenderofegril.ui.HexagonalMapView
import com.defenderofegril.ui.icon.PushpinIcon
import com.defenderofegril.ui.editor.SaveAsDialog
import com.defenderofegril.ui.editor.getTileColor
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
    
    // Track the last painted tile to avoid redundant updates
    var lastPaintedTile by remember { mutableStateOf<String?>(null) }
    
    // Track the container's position in root coordinates
    var containerPositionInRoot by remember { mutableStateOf(Offset.Zero) }
    
    // Get density for coordinate conversions
    val density = androidx.compose.ui.platform.LocalDensity.current
    
    // Helper function to find which tile is at a given position (in root coordinates)
    fun getTileAtPosition(position: Offset): String? {
        val hexRadiusPx = with(density) { (hexWidth / 2f * zoomLevel).dp.toPx() }
        val closest = tilePositions.entries.minByOrNull { (_, tilePos) ->
            val dx = position.x - tilePos.x
            val dy = position.y - tilePos.y
            dx * dx + dy * dy
        }
        return closest?.let { (key, tilePos) ->
            val dx = position.x - tilePos.x
            val dy = position.y - tilePos.y
            val distance = sqrt(dx * dx + dy * dy)
            if (distance < hexRadiusPx) key else null
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
                    .onGloballyPositioned { coordinates ->
                        // Track container position for coordinate conversion
                        containerPositionInRoot = coordinates.positionInRoot()
                    }
                    .pointerInput(selectedTileType) {
                        // Brush painting: detect drag gestures and paint tiles
                        detectDragGestures(
                            onDragStart = { offset ->
                                // Convert container-relative coordinates to root coordinates
                                val rootOffset = Offset(
                                    containerPositionInRoot.x + offset.x,
                                    containerPositionInRoot.y + offset.y
                                )
                                // Paint the tile at the start position
                                val tileKey = getTileAtPosition(rootOffset)
                                if (tileKey != null) {
                                    tiles = tiles.toMutableMap().apply {
                                        this[tileKey] = selectedTileType
                                    }
                                    lastPaintedTile = tileKey
                                }
                            },
                            onDrag = { change, _ ->
                                // Convert container-relative coordinates to root coordinates
                                val rootOffset = Offset(
                                    containerPositionInRoot.x + change.position.x,
                                    containerPositionInRoot.y + change.position.y
                                )
                                // Paint tiles as the pointer moves (only if different from last)
                                val tileKey = getTileAtPosition(rootOffset)
                                if (tileKey != null && tileKey != lastPaintedTile) {
                                    tiles = tiles.toMutableMap().apply {
                                        this[tileKey] = selectedTileType
                                    }
                                    lastPaintedTile = tileKey
                                }
                            },
                            onDragEnd = {
                                // Clear last painted tile when drag ends
                                lastPaintedTile = null
                            }
                        )
                    }
            ) {
                HexagonalMapView(
                    gridWidth = map.width,
                    gridHeight = map.height,
                    config = HexagonalMapConfig(
                        hexSize = hexSize,
                        enableKeyboardNavigation = true,  // Enable keyboard navigation for editor
                        enablePanNavigation = false  // Disable pan navigation for editor (brush only)
                    ),
                    scale = zoomLevel,
                    offsetX = offsetX,
                    offsetY = offsetY,
                    onScaleChange = { newScale -> zoomLevel = newScale },
                    onOffsetChange = { newX, newY ->
                        offsetX = newX
                        offsetY = newY
                    },
                    modifier = Modifier.fillMaxSize()
                ) { hexWidthParam, hexHeightParam, verticalSpacing ->
                    for (y in 0 until map.height) {
                        Row(
                            modifier = Modifier.offset(
                                x = if (y % 2 == 1) (hexWidthParam * 0.42f).dp else 0.dp,
                                y = (-(y-1)).dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy((-10).dp)
                        ) {
                            for (x in 0 until map.width) {
                                val key = "$x,$y"
                                val tileType = tiles[key] ?: TileType.NO_PLAY
                                
                                Box(
                                    modifier = Modifier
                                        .width((hexWidthParam).dp)
                                        .height((hexHeightParam).dp)
                                        .clip(HexagonShape())
                                        .background(getTileColor(tileType))
                                        .border(1.5.dp, Color.Black, HexagonShape())
                                        .onGloballyPositioned { coordinates ->
                                            // Track tile center position for brush painting
                                            val bounds = coordinates.size
                                            val position = coordinates.positionInRoot()
                                            val centerX = position.x + bounds.width / 2f
                                            val centerY = position.y + bounds.height / 2f
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
