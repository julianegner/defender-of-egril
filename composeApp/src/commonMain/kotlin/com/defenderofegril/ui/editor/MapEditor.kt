package com.defenderofegril.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.defenderofegril.editor.EditorMap
import com.defenderofegril.editor.EditorStorage
import com.defenderofegril.editor.TileType
import com.defenderofegril.model.Level
import com.defenderofegril.model.Position
import com.defenderofegril.ui.*
import kotlin.math.sqrt

/**
 * Main content for the Map Editor tab
 */
@Composable
fun MapEditorContent() {
    val maps = remember { mutableStateOf(EditorStorage.getAllMaps()) }
    var selectedMapId by remember { mutableStateOf<String?>(null) }
    var editingMap by remember { mutableStateOf<EditorMap?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    
    if (editingMap != null) {
        // Map editing view
        MapEditorView(
            map = editingMap!!,
            onSave = { updatedMap ->
                EditorStorage.saveMap(updatedMap)
                maps.value = EditorStorage.getAllMaps()
                editingMap = null
            },
            onCancel = { editingMap = null }
        )
    } else {
        // Map list view
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Maps",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Button(onClick = { showCreateDialog = true }) {
                    Text("Create New Map")
                }
            }
            
            Text(
                text = "Select a map to edit:",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(maps.value) { map ->
                    MapListCard(
                        map = map,
                        isSelected = selectedMapId == map.id,
                        onSelect = {
                            selectedMapId = map.id
                            editingMap = map
                        },
                        onDelete = {
                            EditorStorage.deleteMap(map.id)
                            maps.value = EditorStorage.getAllMaps()
                        }
                    )
                }
            }
        }
    }
    
    if (showCreateDialog) {
        CreateMapDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, width, height ->
                // Generate ID from name with underscores
                val sanitizedName = name.trim().replace(" ", "_").replace(Regex("[^a-zA-Z0-9_]"), "")
                val newId = if (sanitizedName.isNotEmpty()) {
                    "map_$sanitizedName"
                } else {
                    "map_custom_${kotlin.random.Random.nextInt(10000, 99999)}"
                }
                val newMap = EditorMap(
                    id = newId,
                    name = name,
                    width = width,
                    height = height,
                    tiles = emptyMap()
                )
                EditorStorage.saveMap(newMap)
                maps.value = EditorStorage.getAllMaps()
                showCreateDialog = false
                editingMap = newMap
            }
        )
    }
}

/**
 * Card displaying a map in the map list
 */
@Composable
fun MapListCard(
    map: EditorMap,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = map.name.ifEmpty { "Map ${map.id}" },
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (map.readyToUse) {
                        CheckmarkIcon(
                            size = 16.dp,
                            tint = Color.Green
                        )
                    } else {
                        Text(
                            text = "✗",
                            color = Color.Red,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
                Text(
                    text = "File: ${map.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Size: ${map.width}x${map.height}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = if (map.readyToUse) "Ready to use" else "Not ready",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (map.readyToUse) Color.Green else Color.Red
                )
            }
            
            // Minimap preview
            Box(
                modifier = Modifier
                    .weight(1f)
                    .width(120.dp)
                    .height(80.dp)
                    .padding(4.dp)
            ) {
                // Create a dummy level for the minimap (we only need it for the grid dimensions)
                val dummyLevel = remember(map.id) {
                    Level(
                        id = 0,
                        name = map.name,
                        gridWidth = map.width,
                        gridHeight = map.height,
                        startPositions = emptyList(),
                        targetPosition = Position(0, 0),
                        pathCells = emptySet(),
                        buildIslands = emptySet(),
                        attackerWaves = emptyList()
                    )
                }

                // Use HexagonMinimap with a direct map reference
                HexagonMinimapFromEditorMap(
                    map = map,
                    level = dummyLevel,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.weight(3f))

            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        }
    }
}

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
    val hexSize = 32.dp * zoomLevel  // Radius of hexagon with zoom applied
    val sqrt3 = sqrt(3.0).toFloat()
    val hexWidth = hexSize.value * sqrt3  // Width of hexagon (flat-to-flat)
    val hexHeight = hexSize.value * 2f    // Height of hexagon (point-to-point)
    val verticalSpacing = hexHeight * 0.75f  // For pointy-top hexagons
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Map grid layer (below header)
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Spacer to account for header height
            Spacer(modifier = Modifier.height(280.dp))
            
            var containerSize by remember { mutableStateOf(IntSize.Zero) }
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp)
                    .onSizeChanged { containerSize = it }
                    .mouseWheelZoom(
                        containerSize = containerSize,
                        scale = zoomLevel,
                        offsetX = offsetX,
                        offsetY = offsetY,
                        onScaleChange = { newScale -> zoomLevel = newScale },
                        onOffsetChange = { newX, newY -> 
                            offsetX = newX
                            offsetY = newY
                        }
                    )
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            // Apply pan
                            offsetX += pan.x
                            offsetY += pan.y
                            
                            // Apply zoom (for pinch gestures on mobile)
                            if (zoom != 1f) {
                                zoomLevel = (zoomLevel * zoom).coerceIn(0.5f, 3f)
                            }
                        }
                    }
            ) {
                Column(
                    modifier = Modifier.graphicsLayer(
                        scaleX = zoomLevel,
                        scaleY = zoomLevel,
                        translationX = offsetX,
                        translationY = offsetY
                    ),
                    verticalArrangement = Arrangement.spacedBy((-hexHeight + verticalSpacing - 7f).dp)
                ) {
                    for (y in 0 until map.height) {
                        Row(
                            modifier = Modifier.offset(
                                x = if (y % 2 == 1) (hexWidth * 0.42f).dp else 0.dp,
                                y = (-(y-1)).dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy((-10).dp)
                        ) {
                            for (x in 0 until map.width) {
                                val key = "$x,$y"
                                val tileType = tiles[key] ?: TileType.NO_PLAY
                                
                                Box(
                                    modifier = Modifier
                                        .width((hexWidth).dp)
                                        .height((hexHeight).dp)
                                        .clip(HexagonShape())
                                        .background(getTileColor(tileType))
                                        .border(1.5.dp, Color.Black, HexagonShape())
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
                    Text("Save Map")
                }
                
                Button(
                    onClick = { showSaveAsDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save As New")
                }
                
                Button(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
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
            title = "Save Map As New",
            label = "Map Name",
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

/**
 * Header for the map editor with controls
 */
@Composable
fun MapEditorHeader(
    map: EditorMap,
    mapName: String,
    onMapNameChange: (String) -> Unit,
    selectedTileType: TileType,
    onTileTypeChange: (TileType) -> Unit,
    zoomLevel: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp)
        ) {
            // Header
            Text(
                text = "Editing: ${map.name.ifEmpty { map.id }}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Map name input
            OutlinedTextField(
                value = mapName,
                onValueChange = onMapNameChange,
                label = { Text("Map Name") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            
            // Tile type selector
            Text(
                text = "Select Tile Type:",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // All tile types are selectable
                items(TileType.values().toList()) { tileType ->
                    TileTypeButton(
                        tileType = tileType,
                        selected = selectedTileType == tileType,
                        onClick = { onTileTypeChange(tileType) }
                    )
                }
            }
            
            // Zoom controls
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Click hexagons to paint (${map.width}x${map.height}). Use Ctrl+Scroll to zoom:",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onZoomOut,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            MagnifyingGlassIcon(size = 14.dp, tint = Color.White)
                            Text("-", fontSize = 12.sp)
                        }
                    }
                    Text(
                        text = "${(zoomLevel * 100).toInt()}%",
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Button(
                        onClick = onZoomIn,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            MagnifyingGlassIcon(size = 14.dp, tint = Color.White)
                            Text("+", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Card for selecting a map in the level editor
 */
@Composable
fun MapSelectionCard(
    map: EditorMap,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = map.name.ifEmpty { map.id },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${map.width}x${map.height}",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp
            )
            
            // Mini-map preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .padding(4.dp)
            ) {
                // Create a dummy level for the minimap (we only need it for the grid dimensions)
                val dummyLevel = remember(map.id) {
                    Level(
                        id = 0,
                        name = map.name,
                        gridWidth = map.width,
                        gridHeight = map.height,
                        startPositions = emptyList(),
                        targetPosition = Position(0, 0),
                        pathCells = emptySet(),
                        buildIslands = emptySet(),
                        attackerWaves = emptyList()
                    )
                }

                // Use HexagonMinimap with a direct map reference
                HexagonMinimapFromEditorMap(
                    map = map,
                    level = dummyLevel,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Status indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (map.readyToUse) {
                    CheckmarkIcon(size = 12.dp, tint = Color.Green)
                } else {
                    Text("✗", color = Color.Red, fontSize = 12.sp)
                }
                Text(
                    text = if (map.readyToUse) "Ready" else "Not ready",
                    fontSize = 10.sp,
                    color = if (map.readyToUse) Color.Green else Color.Red
                )
            }
        }
    }
}
