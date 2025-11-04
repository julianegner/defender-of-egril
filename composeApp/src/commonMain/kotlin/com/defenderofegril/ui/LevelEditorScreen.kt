package com.defenderofegril.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.defenderofegril.editor.EditorStorage
import com.defenderofegril.editor.EditorMap
import com.defenderofegril.editor.TileType
import com.defenderofegril.model.AttackerType
import com.defenderofegril.model.DefenderType
import com.defenderofegril.model.Level
import com.defenderofegril.model.Position
import com.defenderofegril.model.getHexNeighbors
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class EditorTab {
    MAP_EDITOR,
    LEVEL_EDITOR,
    LEVEL_SEQUENCE
}

@Composable
fun LevelEditorScreen(
    onBack: () -> Unit
) {
    var currentTab by remember { mutableStateOf(EditorTab.LEVEL_EDITOR) }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Content area (below header)
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            // Spacer for header
            Spacer(modifier = Modifier.height(140.dp))
            
            // Content based on selected tab
            when (currentTab) {
                EditorTab.MAP_EDITOR -> MapEditorContent()
                EditorTab.LEVEL_EDITOR -> LevelEditorContent()
                EditorTab.LEVEL_SEQUENCE -> LevelSequenceContent()
            }
        }
        
        // Main header (on top with elevated z-index)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp)
            ) {
                // Title and Back button row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Level Editor",
                        style = MaterialTheme.typography.titleLarge
                    )
                    
                    Button(onClick = onBack) {
                        Text("← Back to World Map")
                    }
                }
                
                // Tab buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { currentTab = EditorTab.MAP_EDITOR },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentTab == EditorTab.MAP_EDITOR) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("Map Editor")
                    }
                    
                    Button(
                        onClick = { currentTab = EditorTab.LEVEL_EDITOR },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentTab == EditorTab.LEVEL_EDITOR) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("Level Editor")
                    }
                    
                    Button(
                        onClick = { currentTab = EditorTab.LEVEL_SEQUENCE },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentTab == EditorTab.LEVEL_SEQUENCE) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("Level Sequence")
                    }
                }
            }
        }
    }
}

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
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                selectedMapId = map.id
                                editingMap = map
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedMapId == map.id) 
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
                                        Text(
                                            text = "✓",
                                            color = Color.Green,
                                            style = MaterialTheme.typography.titleSmall
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
                                onClick = {
                                    EditorStorage.deleteMap(map.id)
                                    maps.value = EditorStorage.getAllMaps()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Delete")
                            }
                        }
                    }
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
                                    Text(
                                        text = getTileSymbol(tileType),
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
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
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f)
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
                    onValueChange = { mapName = it },
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
                            onClick = { selectedTileType = tileType }
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
                            onClick = { zoomLevel = maxOf(0.5f, zoomLevel - 0.1f) },
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("🔍-", fontSize = 12.sp)
                        }
                        Text(
                            text = "${(zoomLevel * 100).toInt()}%",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Button(
                            onClick = { zoomLevel = minOf(3.0f, zoomLevel + 0.1f) },
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("🔍+", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
    
    if (showSaveAsDialog) {
        SaveAsMapDialog(
            currentName = mapName,
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


@Composable
fun TileTypeButton(
    tileType: TileType,
    selected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) getTileColor(tileType) else MaterialTheme.colorScheme.secondary
        )
    ) {
        Text("${getTileSymbol(tileType)} ${tileType.name}")
    }
}

fun getTileColor(tileType: TileType): Color {
    return when (tileType) {
        TileType.PATH -> Color(0xFF8B4513)        // Brown
        TileType.BUILD_AREA -> Color(0xFF90EE90)  // Light green
        TileType.ISLAND -> Color(0xFF228B22)      // Forest green
        TileType.NO_PLAY -> Color(0xFF404040)     // Dark gray
        TileType.SPAWN_POINT -> Color(0xFFFF0000) // Red
        TileType.TARGET -> Color(0xFF0000FF)      // Blue
        TileType.WAYPOINT -> Color(0xFFFFFF00)    // Yellow
    }
}

fun getTileSymbol(tileType: TileType): String {
    return when (tileType) {
        TileType.PATH -> "➡"
        TileType.BUILD_AREA -> "🏗"
        TileType.ISLAND -> "🏝"
        TileType.NO_PLAY -> "⬛"
        TileType.SPAWN_POINT -> "🚪"
        TileType.TARGET -> "🎯"
        TileType.WAYPOINT -> "📍"
    }
}

@Composable
fun CreateMapDialog(
    onDismiss: () -> Unit,
    onCreate: (String, Int, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var width by remember { mutableStateOf("30") }
    var height by remember { mutableStateOf("8") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Map") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Map Name") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = width,
                    onValueChange = { if (it.all { c -> c.isDigit() }) width = it },
                    label = { Text("Width") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = height,
                    onValueChange = { if (it.all { c -> c.isDigit() }) height = it },
                    label = { Text("Height") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = width.toIntOrNull() ?: 30
                    val h = height.toIntOrNull() ?: 8
                    onCreate(name, w, h)
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun LevelEditorContent() {
    val levels = remember { mutableStateOf(EditorStorage.getAllLevels()) }
    var selectedLevelId by remember { mutableStateOf<String?>(null) }
    var editingLevel by remember { mutableStateOf<com.defenderofegril.editor.EditorLevel?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    
    if (editingLevel != null) {
        // Level editing view
        LevelEditorView(
            level = editingLevel!!,
            onSave = { updatedLevel ->
                EditorStorage.saveLevel(updatedLevel)
                levels.value = EditorStorage.getAllLevels()
                editingLevel = null
            },
            onCancel = { editingLevel = null }
        )
    } else {
        // Level list view
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Levels",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Button(onClick = { showCreateDialog = true }) {
                    Text("Create New Level")
                }
            }
            
            Text(
                text = "Select a level to edit:",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(levels.value) { level ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedLevelId == level.id) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { 
                                        selectedLevelId = level.id
                                        editingLevel = level
                                    }
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = level.title,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                if (level.subtitle.isNotEmpty()) {
                                    Text(
                                        text = level.subtitle,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Text(
                                    text = "File: ${level.id}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Map: ${level.mapId} | Coins: ${level.startCoins} | HP: ${level.startHealthPoints}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "Enemies: ${level.enemySpawns.size}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Button(
                                onClick = {
                                    EditorStorage.deleteLevel(level.id)
                                    levels.value = EditorStorage.getAllLevels()
                                    if (selectedLevelId == level.id) {
                                        selectedLevelId = null
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showCreateDialog) {
        CreateLevelDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title ->
                // Generate ID from title with underscores
                val sanitizedTitle = title.trim().replace(" ", "_").replace(Regex("[^a-zA-Z0-9_]"), "")
                val newId = if (sanitizedTitle.isNotEmpty()) {
                    "level_$sanitizedTitle"
                } else {
                    "level_custom_${kotlin.random.Random.nextInt(10000, 99999)}"
                }
                // Get first ready-to-use map
                val firstReadyMap = EditorStorage.getAllMaps().filter { it.readyToUse }.firstOrNull()
                val newLevel = com.defenderofegril.editor.EditorLevel(
                    id = newId,
                    mapId = firstReadyMap?.id ?: "map_30x8",
                    title = title,
                    subtitle = "",
                    startCoins = 100,
                    startHealthPoints = 10,
                    enemySpawns = emptyList(),
                    availableTowers = com.defenderofegril.model.DefenderType.entries.filter { 
                        it != com.defenderofegril.model.DefenderType.DRAGONS_LAIR 
                    }.toSet()
                )
                EditorStorage.saveLevel(newLevel)
                levels.value = EditorStorage.getAllLevels()
                showCreateDialog = false
                editingLevel = newLevel
            }
        )
    }
}

@Composable
fun LevelEditorView(
    level: com.defenderofegril.editor.EditorLevel,
    onSave: (com.defenderofegril.editor.EditorLevel) -> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf(level.title) }
    var subtitle by remember { mutableStateOf(level.subtitle) }
    var startCoins by remember { mutableStateOf(level.startCoins.toString()) }
    var startHP by remember { mutableStateOf(level.startHealthPoints.toString()) }
    var selectedMapId by remember { mutableStateOf(level.mapId) }
    var enemySpawns by remember { mutableStateOf(level.enemySpawns.toMutableList()) }
    var availableTowers by remember { mutableStateOf(level.availableTowers.toMutableSet()) }
    var showEnemyDialog by remember { mutableStateOf(false) }
    var showEnemyDialogForTurn by remember { mutableStateOf(1) }
    var showSaveAsDialog by remember { mutableStateOf(false) }
    
    // Get only ready-to-use maps for selection
    val maps = remember { EditorStorage.getAllMaps().filter { it.readyToUse } }
    
    // Check if Ewhad is already in spawn list
    val ewhadCount = enemySpawns.count { it.attackerType == com.defenderofegril.model.AttackerType.EWHAD }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Editing Level: ${level.title}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Title and subtitle
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Level Title") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            item {
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("Subtitle (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Map selection with mini-maps
            item {
                Text(
                    text = "Map:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(maps) { map ->
                        MapSelectionCard(
                            map = map,
                            isSelected = selectedMapId == map.id,
                            onClick = { selectedMapId = map.id }
                        )
                    }
                }
            }
            
            // Start coins and HP
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startCoins,
                        onValueChange = { if (it.all { c -> c.isDigit() }) startCoins = it },
                        label = { Text("Start Coins") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = startHP,
                        onValueChange = { if (it.all { c -> c.isDigit() }) startHP = it },
                        label = { Text("Start HP") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Enemies section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enemies (${enemySpawns.size}):",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = { 
                        // Add a new turn (next available)
                        val nextTurn = (enemySpawns.maxOfOrNull { it.spawnTurn } ?: 0) + 1
                        enemySpawns = enemySpawns.toMutableList().apply {
                            add(com.defenderofegril.editor.EditorEnemySpawn(
                                com.defenderofegril.model.AttackerType.GOBLIN, 
                                1, 
                                nextTurn
                            ))
                        }
                    }) {
                        Text("➕ Add Turn")
                    }
                }
            }
            
            // Group enemies by spawn turn
            val turnGroups = enemySpawns.groupBy { it.spawnTurn }.entries.sortedBy { it.key }
            val turns = turnGroups.map { it.key }
            
            turnGroups.forEachIndexed { index, entry ->
                val turn = entry.key
                val spawnsInTurn = entry.value
                item {
                    val turnIndex = index
                    SpawnTurnSection(
                        turn = turn,
                        spawns = spawnsInTurn,
                        onRemoveEnemy = { spawn ->
                            enemySpawns = enemySpawns.toMutableList().apply { remove(spawn) }
                        },
                        onCopyTurn = {
                            // Copy all enemies from this turn to a new turn (next available)
                            val maxTurn = enemySpawns.maxOfOrNull { it.spawnTurn } ?: 0
                            enemySpawns = enemySpawns.toMutableList().apply {
                                spawnsInTurn.forEach { spawn ->
                                    add(spawn.copy(spawnTurn = maxTurn + 1))
                                }
                            }
                        },
                        onAddEnemy = {
                            // Show dialog to add enemy to this specific turn
                            showEnemyDialog = true
                            showEnemyDialogForTurn = turn
                        },
                        onMoveTurnUp = {
                            if (turnIndex > 0) {
                                val prevTurn = turns[turnIndex - 1]
                                enemySpawns = enemySpawns.map { spawn ->
                                    when (spawn.spawnTurn) {
                                        turn -> spawn.copy(spawnTurn = prevTurn)
                                        prevTurn -> spawn.copy(spawnTurn = turn)
                                        else -> spawn
                                    }
                                }.toMutableList()
                            }
                        },
                        onMoveTurnDown = {
                            if (turnIndex < turns.size - 1) {
                                val nextTurn = turns[turnIndex + 1]
                                enemySpawns = enemySpawns.map { spawn ->
                                    when (spawn.spawnTurn) {
                                        turn -> spawn.copy(spawnTurn = nextTurn)
                                        nextTurn -> spawn.copy(spawnTurn = turn)
                                        else -> spawn
                                    }
                                }.toMutableList()
                            }
                        },
                        canMoveUp = turnIndex > 0,
                        canMoveDown = turnIndex < turns.size - 1,
                        ewhadCount = ewhadCount
                    )
                }
            }
            
            // Towers section
            item {
                Text(
                    text = "Available Towers:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }
            
            items(com.defenderofegril.model.DefenderType.entries.filter { it != com.defenderofegril.model.DefenderType.DRAGONS_LAIR }) { tower ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = availableTowers.contains(tower),
                        onCheckedChange = { checked ->
                            if (checked) availableTowers.add(tower)
                            else availableTowers.remove(tower)
                        }
                    )
                    Box(modifier = Modifier
                        .size(32.dp)
                        .clip(HexagonShape())
                        .background(Color(0xFF2196F3)) // same color as in the game
                    ) {
                        TowerTypeIcon(defenderType = tower)
                    }

                    Text("${tower.displayName} (Cost: ${tower.baseCost}, Damage: ${tower.baseDamage})")
                }
            }
        }
        
        // Save/Cancel buttons
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val updatedLevel = level.copy(
                            title = title,
                            subtitle = subtitle,
                            mapId = selectedMapId,
                            startCoins = startCoins.toIntOrNull() ?: 100,
                            startHealthPoints = startHP.toIntOrNull() ?: 10,
                            enemySpawns = enemySpawns.toList(),
                            availableTowers = availableTowers.toSet()
                        )
                        onSave(updatedLevel)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save Level")
                }
                
                Button(
                    onClick = { showSaveAsDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save As New")
                }
            }
            
            Button(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    }
    
    if (showEnemyDialog) {
        AddEnemyDialog(
            ewhadCount = ewhadCount,
            turn = showEnemyDialogForTurn,
            onDismiss = { showEnemyDialog = false },
            onAdd = { enemyType, level ->
                enemySpawns = enemySpawns.toMutableList().apply {
                    add(com.defenderofegril.editor.EditorEnemySpawn(enemyType, level, showEnemyDialogForTurn))
                }
                showEnemyDialog = false
            }
        )
    }
    
    if (showSaveAsDialog) {
        SaveAsLevelDialog(
            currentTitle = title,
            onDismiss = { showSaveAsDialog = false },
            onSave = { newTitle ->
                // Generate ID from title with underscores
                val sanitizedTitle = newTitle.trim().replace(" ", "_").replace(Regex("[^a-zA-Z0-9_]"), "")
                val newId = if (sanitizedTitle.isNotEmpty()) {
                    "level_$sanitizedTitle"
                } else {
                    "level_copy_${kotlin.random.Random.nextInt(10000, 99999)}"
                }
                val newLevel = level.copy(
                    id = newId,
                    title = newTitle,
                    subtitle = subtitle,
                    mapId = selectedMapId,
                    startCoins = startCoins.toIntOrNull() ?: 100,
                    startHealthPoints = startHP.toIntOrNull() ?: 10,
                    enemySpawns = enemySpawns.toList(),
                    availableTowers = availableTowers.toSet()
                )
                onSave(newLevel)
                showSaveAsDialog = false
            }
        )
    }
}

@Composable
fun AddEnemyDialog(
    ewhadCount: Int = 0,
    turn: Int,
    onDismiss: () -> Unit,
    onAdd: (com.defenderofegril.model.AttackerType, Int) -> Unit
) {
    var selectedType by remember { mutableStateOf(com.defenderofegril.model.AttackerType.GOBLIN) }
    var level by remember { mutableStateOf("1") }
    
    // Check if trying to add Ewhad when one already exists
    val canAddEwhad = selectedType != com.defenderofegril.model.AttackerType.EWHAD || ewhadCount == 0
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Enemy to Turn $turn") },
        text = {
            Column {
                Text("Enemy Type:", modifier = Modifier.padding(bottom = 4.dp))
                LazyColumn(
                    modifier = Modifier.height(150.dp).padding(bottom = 8.dp)
                ) {
                    items(com.defenderofegril.model.AttackerType.entries) { type ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { selectedType = type }.padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedType == type,
                                onClick = { selectedType = type }
                            )
                            Text("${type.displayName} (HP: ${type.health})")
                        }
                    }
                }
                OutlinedTextField(
                    value = level,
                    onValueChange = { if (it.all { c -> c.isDigit() }) level = it },
                    label = { Text("Enemy Level") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                Text("HP: ${selectedType.health * (level.toIntOrNull() ?: 1)}", fontSize = 12.sp)
                if (!canAddEwhad) {
                    Text(
                        text = "⚠️ Ewhad can only be spawned once per level!",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(selectedType, level.toIntOrNull() ?: 1)
                },
                enabled = canAddEwhad
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CreateLevelDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Level") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Level Title") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onCreate(title) }) {
                Text("Create")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun LevelSequenceContent() {
    val sequence = remember { mutableStateOf(EditorStorage.getLevelSequence()) }
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Level Sequence",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Arrange level order:",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sequence.value.sequence.size) { index ->
                val levelId = sequence.value.sequence[index]
                val level = EditorStorage.getLevel(levelId)
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "${index + 1}. ${level?.title ?: levelId}",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = {
                                    EditorStorage.moveLevelUp(levelId)
                                    sequence.value = EditorStorage.getLevelSequence()
                                },
                                enabled = index > 0
                            ) {
                                Text("↑")
                            }
                            
                            Button(
                                onClick = {
                                    EditorStorage.moveLevelDown(levelId)
                                    sequence.value = EditorStorage.getLevelSequence()
                                },
                                enabled = index < sequence.value.sequence.size - 1
                            ) {
                                Text("↓")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SaveAsMapDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Map As New") },
        text = {
            Column {
                Text("Enter a name for the new map:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Map Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (newName.isNotBlank()) onSave(newName) },
                enabled = newName.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SaveAsLevelDialog(
    currentTitle: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var newTitle by remember { mutableStateOf(currentTitle) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Level As New") },
        text = {
            Column {
                Text("Enter a title for the new level:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Level Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (newTitle.isNotBlank()) onSave(newTitle) },
                enabled = newTitle.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

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
                Text(
                    text = if (map.readyToUse) "✓" else "✗",
                    color = if (map.readyToUse) Color.Green else Color.Red,
                    fontSize = 12.sp
                )
                Text(
                    text = if (map.readyToUse) "Ready" else "Not ready",
                    fontSize = 10.sp,
                    color = if (map.readyToUse) Color.Green else Color.Red
                )
            }
        }
    }
}


@Composable
fun SpawnTurnSection(
    turn: Int,
    spawns: List<com.defenderofegril.editor.EditorEnemySpawn>,
    onRemoveEnemy: (com.defenderofegril.editor.EditorEnemySpawn) -> Unit,
    onCopyTurn: () -> Unit,
    onAddEnemy: () -> Unit,
    onMoveTurnUp: () -> Unit,
    onMoveTurnDown: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    ewhadCount: Int
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            // Turn header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (expanded) "▼" else "▶",
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Turn $turn",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "(${spawns.size} enemies)",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = onMoveTurnUp,
                        enabled = canMoveUp,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("↑", fontSize = 12.sp)
                    }
                    Button(
                        onClick = onMoveTurnDown,
                        enabled = canMoveDown,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("↓", fontSize = 12.sp)
                    }
                    Button(
                        onClick = onCopyTurn,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "Copy Turn", 
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
            }
            
            // Enemy list (collapsible)
            if (expanded) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Add Enemy button at the top
                    Button(
                        onClick = onAddEnemy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("➕ Add Enemy to Turn $turn")
                    }
                    
                    spawns.forEach { spawn ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.LightGray.copy(alpha = 0.2f))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Enemy icon
                                Box(modifier = Modifier.size(24.dp)) {
                                    EnemyTypeIcon(attackerType = spawn.attackerType)
                                }
                                
                                Column {
                                    Text(
                                        text = "${spawn.attackerType.displayName} Lv${spawn.level}",
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "HP: ${spawn.healthPoints}",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                            
                            Button(
                                onClick = { onRemoveEnemy(spawn) },
                                modifier = Modifier.height(36.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("🗑️", fontSize = 12.sp)
                                    Text("Remove", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
