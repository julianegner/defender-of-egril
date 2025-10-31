package com.defenderofegril.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.defenderofegril.editor.EditorStorage
import com.defenderofegril.editor.EditorMap
import com.defenderofegril.editor.TileType
import com.defenderofegril.model.AttackerType
import com.defenderofegril.model.DefenderType
import com.defenderofegril.model.Position
import com.defenderofegril.model.getHexNeighbors
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
    
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        // Title
        Text(
            text = "Level Editor",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
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
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Content based on selected tab
        when (currentTab) {
            EditorTab.MAP_EDITOR -> MapEditorContent()
            EditorTab.LEVEL_EDITOR -> LevelEditorContent()
            EditorTab.LEVEL_SEQUENCE -> LevelSequenceContent()
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Back button
        Button(onClick = onBack) {
            Text("Back to World Map")
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
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = map.name.ifEmpty { "Map ${map.id}" },
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Size: ${map.width}x${map.height}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Tiles: ${map.tiles.size}",
                                style = MaterialTheme.typography.bodySmall
                            )
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
                val newId = "map_custom_${kotlin.random.Random.nextInt(10000, 99999)}"
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
    
    // Hexagon dimensions - using same constants as game
    val hexSize = 32.dp  // Radius of hexagon (center to corner)
    val sqrt3 = sqrt(3.0).toFloat()
    val hexWidth = hexSize.value * sqrt3  // Width of hexagon (flat-to-flat)
    val hexHeight = hexSize.value * 2f    // Height of hexagon (point-to-point)
    val verticalSpacing = hexHeight * 0.75f  // For pointy-top hexagons
    
    Column(
        modifier = Modifier.fillMaxSize()
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
        
        // Hexagonal grid view
        Text(
            text = "Click hexagons to paint (${map.width}x${map.height}):",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .verticalScroll(rememberScrollState())
                .padding(8.dp)
        ) {
            Column(
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
                                        tiles[key] = selectedTileType
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
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val updatedMap = map.copy(
                        name = mapName,
                        tiles = tiles.toMap()
                    )
                    onSave(updatedMap)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Save Map")
            }
            
            Button(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
        }
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                selectedLevelId = level.id
                                editingLevel = level
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedLevelId == level.id) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
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
                                text = "Map: ${level.mapId} | Coins: ${level.startCoins} | HP: ${level.startHealthPoints}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Enemies: ${level.enemySpawns.size}",
                                style = MaterialTheme.typography.bodySmall
                            )
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
                val newId = "level_custom_${kotlin.random.Random.nextInt(10000, 99999)}"
                val newLevel = com.defenderofegril.editor.EditorLevel(
                    id = newId,
                    mapId = EditorStorage.getAllMaps().firstOrNull()?.id ?: "map_30x8",
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
    
    val maps = remember { EditorStorage.getAllMaps() }
    
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
            
            // Map selection
            item {
                Text(
                    text = "Map:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(maps) { map ->
                        Button(
                            onClick = { selectedMapId = map.id },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedMapId == map.id) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("${map.name.ifEmpty { map.id }} (${map.width}x${map.height})")
                        }
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
                    Button(onClick = { showEnemyDialog = true }) {
                        Text("Add Enemy")
                    }
                }
            }
            
            items(enemySpawns.size) { index ->
                val spawn = enemySpawns[index]
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("${spawn.attackerType.displayName} Lv${spawn.level}")
                            Text("Turn ${spawn.spawnTurn} • HP: ${spawn.healthPoints}", fontSize = 12.sp)
                        }
                        Button(
                            onClick = { enemySpawns.removeAt(index) }
                        ) {
                            Text("Remove")
                        }
                    }
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = availableTowers.contains(tower),
                        onCheckedChange = { checked ->
                            if (checked) availableTowers.add(tower)
                            else availableTowers.remove(tower)
                        }
                    )
                    Text("${tower.displayName} (Cost: ${tower.baseCost}, Damage: ${tower.baseDamage})")
                }
            }
        }
        
        // Save/Cancel buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
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
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
        }
    }
    
    if (showEnemyDialog) {
        AddEnemyDialog(
            onDismiss = { showEnemyDialog = false },
            onAdd = { enemyType, level, turn ->
                enemySpawns.add(
                    com.defenderofegril.editor.EditorEnemySpawn(enemyType, level, turn)
                )
                showEnemyDialog = false
            }
        )
    }
}

@Composable
fun AddEnemyDialog(
    onDismiss: () -> Unit,
    onAdd: (com.defenderofegril.model.AttackerType, Int, Int) -> Unit
) {
    var selectedType by remember { mutableStateOf(com.defenderofegril.model.AttackerType.GOBLIN) }
    var level by remember { mutableStateOf("1") }
    var turn by remember { mutableStateOf("1") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Enemy") },
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
                OutlinedTextField(
                    value = turn,
                    onValueChange = { if (it.all { c -> c.isDigit() }) turn = it },
                    label = { Text("Spawn Turn") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(selectedType, level.toIntOrNull() ?: 1, turn.toIntOrNull() ?: 1)
                }
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
