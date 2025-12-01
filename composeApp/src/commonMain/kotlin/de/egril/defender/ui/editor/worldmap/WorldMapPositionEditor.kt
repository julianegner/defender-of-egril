@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui.editor.worldmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.egril.defender.editor.EditorStorage
import de.egril.defender.editor.WorldMapData
import de.egril.defender.editor.WorldMapLocationData
import de.egril.defender.editor.WorldMapPathData
import de.egril.defender.editor.WorldMapPoint
import de.egril.defender.model.Position
import de.egril.defender.ui.settings.AppSettings
import org.jetbrains.compose.resources.painterResource
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.*

/**
 * World Map Position Editor content - allows placing locations on the world map visually.
 * Uses the new WorldMapData model with locations and curved paths.
 */
@Composable
fun WorldMapPositionEditorContent() {
    var worldMapData by remember { mutableStateOf(EditorStorage.getWorldMapData()) }
    var allLevels by remember { mutableStateOf(EditorStorage.getAllLevels()) }
    var selectedLocationId by remember { mutableStateOf<String?>(null) }
    var hoveredPosition by remember { mutableStateOf<WorldMapPoint?>(null) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var showAddLocationDialog by remember { mutableStateOf(false) }
    var showEditPathDialog by remember { mutableStateOf<WorldMapPathData?>(null) }
    
    val isDarkMode = AppSettings.isDarkMode.value
    
    // Reload data
    LaunchedEffect(Unit) {
        worldMapData = EditorStorage.getWorldMapData()
        allLevels = EditorStorage.getAllLevels()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // Title
        Text(
            text = stringResource(Res.string.world_map_positions),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Instructions
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Text(
                text = stringResource(Res.string.world_map_position_instructions),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(8.dp)
            )
        }
        
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left side: World Map with clickable positions
            Card(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { containerSize = it }
                ) {
                    WorldMapCanvas(
                        worldMapData = worldMapData,
                        allLevels = allLevels,
                        selectedLocationId = selectedLocationId,
                        hoveredPosition = hoveredPosition,
                        containerSize = containerSize,
                        isDarkMode = isDarkMode,
                        onPositionClick = { point ->
                            // Update position for selected location
                            if (selectedLocationId != null) {
                                val location = worldMapData.locations.find { it.id == selectedLocationId }
                                if (location != null) {
                                    val updatedLocation = location.copy(position = point)
                                    EditorStorage.saveWorldMapLocation(updatedLocation)
                                    worldMapData = EditorStorage.getWorldMapData()
                                }
                            }
                        },
                        onHoverChange = { point ->
                            hoveredPosition = point
                        }
                    )
                    
                    // Hover tooltip
                    if (hoveredPosition != null) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                            )
                        ) {
                            Text(
                                text = "Position: ${hoveredPosition!!.x}, ${hoveredPosition!!.y}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
            
            // Right side: Locations and Paths lists
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    // Add Location button
                    Button(
                        onClick = { showAddLocationDialog = true },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Text("+ Add Location")
                    }
                    
                    Text(
                        text = "Locations",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(worldMapData.locations) { location ->
                            LocationListItem(
                                location = location,
                                isSelected = location.id == selectedLocationId,
                                onClick = {
                                    selectedLocationId = if (selectedLocationId == location.id) null else location.id
                                },
                                onDelete = {
                                    EditorStorage.deleteWorldMapLocation(location.id)
                                    worldMapData = EditorStorage.getWorldMapData()
                                    if (selectedLocationId == location.id) {
                                        selectedLocationId = null
                                    }
                                }
                            )
                        }
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Text(
                        text = "Paths (${worldMapData.paths.size})",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(worldMapData.paths) { path ->
                            PathListItem(
                                path = path,
                                locations = worldMapData.locations,
                                onClick = { showEditPathDialog = path },
                                onDelete = {
                                    EditorStorage.deleteWorldMapPath(path.fromLocationId, path.toLocationId)
                                    worldMapData = EditorStorage.getWorldMapData()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Add Location Dialog
    if (showAddLocationDialog) {
        AddLocationDialog(
            allLevels = allLevels,
            existingLocations = worldMapData.locations,
            onDismiss = { showAddLocationDialog = false },
            onConfirm = { newLocation ->
                EditorStorage.saveWorldMapLocation(newLocation)
                worldMapData = EditorStorage.getWorldMapData()
                showAddLocationDialog = false
            }
        )
    }
    
    // Edit Path Dialog
    if (showEditPathDialog != null) {
        EditPathDialog(
            path = showEditPathDialog!!,
            onDismiss = { showEditPathDialog = null },
            onConfirm = { updatedPath ->
                EditorStorage.saveWorldMapPath(updatedPath)
                worldMapData = EditorStorage.getWorldMapData()
                showEditPathDialog = null
            }
        )
    }
}

/**
 * Canvas showing the world map background with location markers and paths
 */
@Composable
private fun WorldMapCanvas(
    worldMapData: WorldMapData,
    allLevels: List<de.egril.defender.editor.EditorLevel>,
    selectedLocationId: String?,
    hoveredPosition: WorldMapPoint?,
    containerSize: IntSize,
    isDarkMode: Boolean,
    onPositionClick: (WorldMapPoint) -> Unit,
    onHoverChange: (WorldMapPoint?) -> Unit
) {
    // Get the painter to access image dimensions
    val mapPainter = painterResource(Res.drawable.world_map_background)
    val imageAspectRatio = mapPainter.intrinsicSize.width / mapPainter.intrinsicSize.height
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Background image
        Image(
            painter = mapPainter,
            contentDescription = "World Map",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        
        // Canvas overlay for markers and interaction
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(imageAspectRatio) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val offset = event.changes.firstOrNull()?.position ?: continue
                            
                            when (event.type) {
                                PointerEventType.Move, PointerEventType.Enter -> {
                                    // Calculate actual image bounds within container (accounting for ContentScale.Fit)
                                    val containerAspectRatio = size.width.toFloat() / size.height.toFloat()
                                    val (imageWidth, imageHeight, imageOffsetX, imageOffsetY) = if (containerAspectRatio > imageAspectRatio) {
                                        val h = size.height.toFloat()
                                        val w = h * imageAspectRatio
                                        val ox = (size.width - w) / 2f
                                        listOf(w, h, ox, 0f)
                                    } else {
                                        val w = size.width.toFloat()
                                        val h = w / imageAspectRatio
                                        val oy = (size.height - h) / 2f
                                        listOf(w, h, 0f, oy)
                                    }
                                    
                                    // Calculate position as permille (0-1000) relative to image bounds
                                    val x = (((offset.x - imageOffsetX) / imageWidth) * 1000).toInt().coerceIn(0, 1000)
                                    val y = (((offset.y - imageOffsetY) / imageHeight) * 1000).toInt().coerceIn(0, 1000)
                                    onHoverChange(WorldMapPoint(x, y))
                                }
                                PointerEventType.Exit -> {
                                    onHoverChange(null)
                                }
                                else -> {}
                            }
                        }
                    }
                }
                .pointerInput(imageAspectRatio) {
                    detectTapGestures { offset ->
                        // Calculate actual image bounds within container (accounting for ContentScale.Fit)
                        val containerAspectRatio = size.width.toFloat() / size.height.toFloat()
                        val (imageWidth, imageHeight, imageOffsetX, imageOffsetY) = if (containerAspectRatio > imageAspectRatio) {
                            val h = size.height.toFloat()
                            val w = h * imageAspectRatio
                            val ox = (size.width - w) / 2f
                            listOf(w, h, ox, 0f)
                        } else {
                            val w = size.width.toFloat()
                            val h = w / imageAspectRatio
                            val oy = (size.height - h) / 2f
                            listOf(w, h, 0f, oy)
                        }
                        
                        // Calculate position as permille (0-1000) relative to image bounds
                        val x = (((offset.x - imageOffsetX) / imageWidth) * 1000).toInt().coerceIn(0, 1000)
                        val y = (((offset.y - imageOffsetY) / imageHeight) * 1000).toInt().coerceIn(0, 1000)
                        onPositionClick(WorldMapPoint(x, y))
                    }
                }
        ) {
            val markerRadius = 15f
            
            // Calculate actual image bounds within container (accounting for ContentScale.Fit)
            val containerAspectRatio = size.width / size.height
            val (imageWidth, imageHeight, imageOffsetX, imageOffsetY) = if (containerAspectRatio > imageAspectRatio) {
                val h = size.height
                val w = h * imageAspectRatio
                val ox = (size.width - w) / 2f
                listOf(w, h, ox, 0f)
            } else {
                val w = size.width
                val h = w / imageAspectRatio
                val oy = (size.height - h) / 2f
                listOf(w, h, 0f, oy)
            }
            
            // Draw paths first (so they appear behind locations)
            val roadColor = if (isDarkMode) Color(0xFF8B4513) else Color(0xFFA0522D)
            val locationById = worldMapData.locations.associateBy { it.id }
            
            for (path in worldMapData.paths) {
                val fromLocation = locationById[path.fromLocationId] ?: continue
                val toLocation = locationById[path.toLocationId] ?: continue
                
                val startX = imageOffsetX + (fromLocation.position.x / 1000f) * imageWidth
                val startY = imageOffsetY + (fromLocation.position.y / 1000f) * imageHeight
                val endX = imageOffsetX + (toLocation.position.x / 1000f) * imageWidth
                val endY = imageOffsetY + (toLocation.position.y / 1000f) * imageHeight
                
                if (path.controlPoints.isEmpty()) {
                    // Draw straight line
                    drawLine(
                        color = roadColor,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f), 0f)
                    )
                } else {
                    // Draw curved path through control points
                    var prevX = startX
                    var prevY = startY
                    
                    for (cp in path.controlPoints) {
                        val cpX = imageOffsetX + (cp.x / 1000f) * imageWidth
                        val cpY = imageOffsetY + (cp.y / 1000f) * imageHeight
                        
                        drawLine(
                            color = roadColor,
                            start = Offset(prevX, prevY),
                            end = Offset(cpX, cpY),
                            strokeWidth = 3f,
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f), 0f)
                        )
                        
                        prevX = cpX
                        prevY = cpY
                    }
                    
                    // Draw final segment
                    drawLine(
                        color = roadColor,
                        start = Offset(prevX, prevY),
                        end = Offset(endX, endY),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f), 0f)
                    )
                }
            }
            
            // Draw all location markers
            for (location in worldMapData.locations) {
                val x = imageOffsetX + (location.position.x / 1000f) * imageWidth
                val y = imageOffsetY + (location.position.y / 1000f) * imageHeight
                
                val isSelected = location.id == selectedLocationId
                val hasPlayableLevels = location.levelIds.any { levelId ->
                    EditorStorage.isLevelReadyToPlay(levelId)
                }
                
                val markerColor = when {
                    isSelected -> Color(0xFF2196F3) // Blue for selected
                    hasPlayableLevels -> if (isDarkMode) Color(0xFF4CAF50) else Color(0xFF2E7D32) // Green for playable
                    else -> if (isDarkMode) Color(0xFF9E9E9E) else Color(0xFF757575) // Grey for not playable
                }
                
                // Draw marker circle
                drawCircle(
                    color = markerColor,
                    radius = if (isSelected) markerRadius * 1.3f else markerRadius,
                    center = Offset(x, y)
                )
                
                // Draw border
                drawCircle(
                    color = if (isDarkMode) Color.White else Color.Black,
                    radius = if (isSelected) markerRadius * 1.3f else markerRadius,
                    center = Offset(x, y),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                )
            }
            
            // Draw hover indicator for selected location
            if (selectedLocationId != null && hoveredPosition != null) {
                val x = imageOffsetX + (hoveredPosition.x / 1000f) * imageWidth
                val y = imageOffsetY + (hoveredPosition.y / 1000f) * imageHeight
                
                drawCircle(
                    color = Color(0x802196F3), // Semi-transparent blue
                    radius = markerRadius * 1.5f,
                    center = Offset(x, y)
                )
            }
        }
    }
}

/**
 * List item for a location
 */
@Composable
private fun LocationListItem(
    location: WorldMapLocationData,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = location.name,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Position: ${location.position.x}, ${location.position.y} | ${location.levelIds.size} levels",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Delete button
            TextButton(
                onClick = onDelete,
                contentPadding = PaddingValues(4.dp)
            ) {
                Text(
                    text = "X",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Red
                )
            }
        }
    }
}

/**
 * List item for a path
 */
@Composable
private fun PathListItem(
    path: WorldMapPathData,
    locations: List<WorldMapLocationData>,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val fromName = locations.find { it.id == path.fromLocationId }?.name ?: path.fromLocationId
    val toName = locations.find { it.id == path.toLocationId }?.name ?: path.toLocationId
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$fromName → $toName",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${path.controlPoints.size} control points",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Delete button
            TextButton(
                onClick = onDelete,
                contentPadding = PaddingValues(4.dp)
            ) {
                Text(
                    text = "X",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Red
                )
            }
        }
    }
}

/**
 * Dialog for adding a new location
 */
@Composable
private fun AddLocationDialog(
    allLevels: List<de.egril.defender.editor.EditorLevel>,
    existingLocations: List<WorldMapLocationData>,
    onDismiss: () -> Unit,
    onConfirm: (WorldMapLocationData) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedLevelIds by remember { mutableStateOf(setOf<String>()) }
    
    // Get levels not yet assigned to any location
    val assignedLevelIds = existingLocations.flatMap { it.levelIds }.toSet()
    val availableLevels = allLevels.filter { it.id !in assignedLevelIds }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Location") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Location Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Select Levels:", style = MaterialTheme.typography.bodyMedium)
                
                LazyColumn(
                    modifier = Modifier.height(200.dp)
                ) {
                    items(availableLevels) { level ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedLevelIds = if (level.id in selectedLevelIds) {
                                        selectedLevelIds - level.id
                                    } else {
                                        selectedLevelIds + level.id
                                    }
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = level.id in selectedLevelIds,
                                onCheckedChange = {
                                    selectedLevelIds = if (it) {
                                        selectedLevelIds + level.id
                                    } else {
                                        selectedLevelIds - level.id
                                    }
                                }
                            )
                            Text(level.title.ifEmpty { level.id })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && selectedLevelIds.isNotEmpty()) {
                        val locationId = name.lowercase().replace(" ", "_")
                        onConfirm(WorldMapLocationData(
                            id = locationId,
                            name = name,
                            position = WorldMapPoint(500, 500), // Center by default
                            levelIds = selectedLevelIds.toList()
                        ))
                    }
                },
                enabled = name.isNotBlank() && selectedLevelIds.isNotEmpty()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog for editing path control points
 */
@Composable
private fun EditPathDialog(
    path: WorldMapPathData,
    onDismiss: () -> Unit,
    onConfirm: (WorldMapPathData) -> Unit
) {
    var controlPoints by remember { mutableStateOf(path.controlPoints.toMutableList()) }
    var newX by remember { mutableStateOf("") }
    var newY by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Path Control Points") },
        text = {
            Column {
                Text(
                    "Path: ${path.fromLocationId} → ${path.toLocationId}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Control Points (0-1000):", style = MaterialTheme.typography.bodySmall)
                
                LazyColumn(
                    modifier = Modifier.height(150.dp)
                ) {
                    items(controlPoints.size) { index ->
                        val cp = controlPoints[index]
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${index + 1}: (${cp.x}, ${cp.y})")
                            TextButton(onClick = {
                                controlPoints = controlPoints.toMutableList().apply { removeAt(index) }
                            }) {
                                Text("X", color = Color.Red)
                            }
                        }
                    }
                }
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text("Add Control Point:", style = MaterialTheme.typography.bodySmall)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newX,
                        onValueChange = { newX = it.filter { c -> c.isDigit() } },
                        label = { Text("X") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = newY,
                        onValueChange = { newY = it.filter { c -> c.isDigit() } },
                        label = { Text("Y") },
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            val x = newX.toIntOrNull()?.coerceIn(0, 1000)
                            val y = newY.toIntOrNull()?.coerceIn(0, 1000)
                            if (x != null && y != null) {
                                controlPoints = controlPoints.toMutableList().apply { add(WorldMapPoint(x, y)) }
                                newX = ""
                                newY = ""
                            }
                        }
                    ) {
                        Text("+")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(path.copy(controlPoints = controlPoints))
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
